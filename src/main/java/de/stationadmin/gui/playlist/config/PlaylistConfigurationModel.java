/**
 * 
 */
package de.stationadmin.gui.playlist.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.table.AbstractTableModel;
import javax.swing.table.TableModel;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.AbstractValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.AutoFillRule;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.ShuffleScriptMeta;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.Advice;
import de.stationadmin.base.playlist.shuffle.TagSequenceAdvice;
import de.stationadmin.base.playlist.shuffle.TitleNameLimitAdvice;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.NonObservingPresentationModel;

/**
 * 
 * @author Frank Korf
 * 
 */
@SuppressWarnings("unchecked")
public class PlaylistConfigurationModel extends PresentationModel<Playlist> {

  private static final long serialVersionUID = 4865584693941336972L;
  private static final Logger log = Logger.getLogger(PlaylistConfigurationModel.class);
  private AbstractValueModel tags;
  private AbstractValueModel generateTags;
  private TableModel weightTableModel = new GenerateWeightTableModel();
  private ValueModel advices = new ValueHolder(null, true);
  private TagManager tagManager;
  private ValueHolder titleNameAdviceLimit = new ValueHolder(0);
  private ValueModel trackOrderType = new ValueHolder(TrackOrderOption.MANUAL);
  private PresentationModel<AutoFillRule> autoFillModel;
  private Settings settings;
  private TextProvider textProvider;

  private List<ShuffleScriptMeta> shuffleScripts;
  private ShuffleScriptModel shuffleScript;

  private List<PlaylistProfile> profiles;
  private IndirectListModel<String> profileListModel = new IndirectListModel<>();

  /**
   * @param bean
   */
  public PlaylistConfigurationModel(Playlist playlist, TagManager tagManager, Settings settings, List<ShuffleScriptMeta> shuffleScripts, List<PlaylistProfile> profiles,
      TextProvider textProvider) {
    super(playlist);
    this.settings = settings;
    this.tags = new TagsModel();
    this.generateTags = new TrackTagsModel();
    this.shuffleScript = new ShuffleScriptModel();
    this.tagManager = tagManager;
    this.textProvider = textProvider;
    this.shuffleScripts = shuffleScripts;
    this.profiles = profiles;
    this.initAdvices(playlist);
    this.initTrackOrderType(playlist);
    this.updateProfileListModel();

    PropertyChangeListener adviceUpdater = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ArrayList<String> adviceStrings = new ArrayList<String>();
        ArrayList<Advice> advices = (ArrayList<Advice>) PlaylistConfigurationModel.this.advices.getValue();
        for (Advice advice : advices) {
          try {
            adviceStrings.add(advice.toJSON());
          } catch (Exception e) {
            log.error("advice serialization error", e);
          }
        }
        if (((Integer) titleNameAdviceLimit.getValue()).intValue() > 0) {
          try {
            adviceStrings.add(new TitleNameLimitAdvice(((Integer) titleNameAdviceLimit.getValue()).intValue()).toJSON());
          } catch (Exception e) {
            log.error("advice serialization error", e);
          }
        }

        getBufferedModel("generateAdvices").setValue(adviceStrings.size() > 0 ? adviceStrings.toArray(new String[adviceStrings.size()]) : null);
      }
    };

    this.advices.addValueChangeListener(adviceUpdater);
    this.titleNameAdviceLimit.addValueChangeListener(adviceUpdater);

    // init shuffle opts
    // - create a copy we can work on
    Map<String, Object> shuffleOpts = playlist.getShuffleOpts() != null ? new HashMap<>(playlist.getShuffleOpts()) : new HashMap<>();
    this.getBufferedModel("shuffleOpts").setValue(shuffleOpts);

    this.getBufferedModel("shuffleType").addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ShuffleScriptMeta current = (ShuffleScriptMeta) shuffleScript.getValue();
        if (current != null) {
          Map<String, Object> opts;
          if (current.getDefaultOpts() != null) {
            opts = current.getDefaultOpts();
          } else {
            opts = new HashMap<>();
          }
          if (current.isSupportsGlobalOpts()) {
            PlaylistService.updateGlobalShuffleOpts(opts, PlaylistConfigurationModel.this.settings);
          }
          getBufferedModel("shuffleOpts").setValue(opts);
        }
        updateProfileListModel();
      }
    });

    this.autoFillModel = new NonObservingPresentationModel<AutoFillRule>(this.getBufferedModel("autoFillRule"), getTriggerChannel());

  }

  private void initTrackOrderType(Playlist playlist) {
    TrackOrderOption trackOrder = TrackOrderOption.MANUAL;
    if (playlist.isShuffle()) {
      trackOrder = TrackOrderOption.SHUFFLE_SERVER;
    } else if (playlist.isGenerate()) {
      trackOrder = TrackOrderOption.GENERATE;
    } else if (playlist.isLocalShuffleAllowed()) {
      trackOrder = TrackOrderOption.SHUFFLE_LOCAL;
    }
    this.trackOrderType.setValue(trackOrder);
    this.trackOrderType.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        TrackOrderOption option = (TrackOrderOption) evt.getNewValue();
        switch (option) {
        case MANUAL:
          getBufferedModel("shuffle").setValue(false);
          getBufferedModel("localShuffleAllowed").setValue(false);
          getBufferedModel("generateTags").setValue(null);
          break;
        case GENERATE:
          getBufferedModel("shuffle").setValue(false);
          getBufferedModel("localShuffleAllowed").setValue(false);
          break;
        case SHUFFLE_LOCAL:
          getBufferedModel("shuffle").setValue(false);
          getBufferedModel("localShuffleAllowed").setValue(true);
          getBufferedModel("generateTags").setValue(null);
          break;
        case SHUFFLE_SERVER:
          getBufferedModel("shuffle").setValue(true);
          getBufferedModel("localShuffleAllowed").setValue(false);
          getBufferedModel("generateTags").setValue(null);
          break;
        }
        updateProfileListModel();

      }
    });
  }

  private void updateProfileListModel() {
    ArrayList<String> activeProfiles = new ArrayList<>();

    TrackOrderOption option = (TrackOrderOption) trackOrderType.getValue();
    String shuffleType = (String) getBufferedModel("shuffleType").getValue();
    String currentSelection = (String) getBufferedModel("profileId").getValue();

    for (PlaylistProfile profile : profiles) {
      switch (profile.getType()) {
      case Generate:
        if (option.equals(TrackOrderOption.GENERATE)) {
          activeProfiles.add(profile.getId());
        }
        break;
      case LocalShuffle:
        if (option.equals(TrackOrderOption.SHUFFLE_LOCAL)) {
          activeProfiles.add(profile.getId());
        }
        break;
      case StationAdminShuffle:
        if (option.equals(TrackOrderOption.SHUFFLE_SERVER)) {
          ShuffleScriptMeta script = PlaylistService.getShuffleScriptMeta(shuffleScripts, shuffleType);
          if (script.getAutomationAlgorithm() != null && script.getAutomationAlgorithm().equals("stationadmin_shuffle")) {
            activeProfiles.add(profile.getId());
          }
        }
        break;
      }
    }

    int numActive = activeProfiles.size();

    String newSelection = currentSelection;
    if (activeProfiles.size() == 0 && currentSelection != null) {
      newSelection = null;
    } else if (activeProfiles.size() > 0 && (currentSelection == null || !activeProfiles.contains(currentSelection))) {
      newSelection = activeProfiles.get(0);
    }

    activeProfiles.add(0, null);

    profileListModel.setList(activeProfiles);

    if (newSelection != currentSelection) {
      getBufferedModel("profileId").setValue(newSelection);
    }

  }

  static String tagsToString(Set<String> tagSet) {
    if (tagSet == null) {
      return null;
    }
    String[] tags = tagSet.toArray(new String[tagSet.size()]);
    Arrays.sort(tags);
    StringBuilder buf = new StringBuilder();
    for (String tag : tags) {
      if (buf.length() > 0) {
        buf.append('\n');
      }
      buf.append(tag);
    }
    return buf.toString();
  }

  void initAdvices(Playlist playlist) {
    ArrayList<Advice> advices = new ArrayList<Advice>();
    if (playlist.getGenerateAdvices() != null) {
      List<String> adviceStrings = new ArrayList<String>(Arrays.asList(playlist.getGenerateAdvices()));
      Collections.sort(adviceStrings);
      for (String adv : adviceStrings) {
        try {
          JSONObject json = new JSONObject(adv);
          int type = json.getInt("type");
          switch (type) {
          case 1:
            advices.add(new TagSequenceAdvice(this.tagManager, json));
            break;
          case 2:
            TitleNameLimitAdvice tAdv = new TitleNameLimitAdvice(json);
            this.titleNameAdviceLimit.setValue(tAdv.getNumTitles());
            break;
          }
        } catch (Exception e) {
          log.error("Advice instantiation error for " + adv, e);
        }

      }
    }
    this.advices.setValue(advices);
  }

  /**
   * @see com.jgoodies.binding.PresentationModel#getModel(java.lang.String)
   */
  @Override
  public AbstractValueModel getModel(String propertyName) {
    if (propertyName.equals("tags")) {
      return this.tags;
    }
    if (propertyName.equals("generateTags")) {
      return this.generateTags;
    }
    return super.getModel(propertyName);
  }

  /**
   * @see com.jgoodies.binding.PresentationModel#getModel(java.lang.String,
   *      java.lang.String, java.lang.String)
   */
  @Override
  public AbstractValueModel getModel(String propertyName, String getterName, String setterName) {
    if (propertyName.equals("tags")) {
      return this.tags;
    }
    if (propertyName.equals("generateTags")) {
      return this.generateTags;
    }
    return super.getModel(propertyName, getterName, setterName);
  }

  /**
   * @return the weightTableModel
   */
  public TableModel getWeightTableModel() {
    return weightTableModel;
  }

  public class GenerateWeightTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -8675947172136215033L;

    private List<WeightTag> entries = new ArrayList<PlaylistConfigurationModel.WeightTag>();

    GenerateWeightTableModel() {
      this.rebuild();
      getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          rebuild();
        }
      });
    }

    @Override
    public int getColumnCount() {
      return 3;
    }

    @Override
    public String getColumnName(int column) {
      // FIXME
      switch (column) {
      case 0:
        return "Tag";
      case 1:
        return "Gewichtung";
      case 2:
        return "Max";
      }
      return null;
    }

    @Override
    public int getRowCount() {
      return entries.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      WeightTag entry = this.entries.get(row);
      switch (col) {
      case 0:
        return entry.getTag();
      case 1:
        return entry.getWeight();
      case 2:
        return entry.getMaxFraction();
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
    }

    public void rebuild() {
      this.entries.clear();
      if (getBean() != null) {
        String value = (String) getBufferedModel("generatePushTag").getValue();
        if (value != null && value.trim().length() > 0) {

          String[] tags = StringUtils.split(value, ";");
          for (String tag : tags) {
            int pushFactor = 2;
            float maxFraction = 1f;
            String[] fields = StringUtils.split(tag, ":");
            if (fields.length > 1) {
              tag = fields[0];
              try {
                pushFactor = Integer.parseInt(fields[1]);
              } catch (NumberFormatException e) {
              }
              if (fields.length > 2) {
                try {
                  maxFraction = Float.parseFloat(fields[2]);
                } catch (NumberFormatException e) {
                }

              }
            }
            this.entries.add(new WeightTag(tag, pushFactor, maxFraction));
          }
        }
        this.entries.add(new WeightTag(null, 0, 1f));
      }
      this.fireTableDataChanged();
    }

    void updatePlaylist() {
      if (getBean() != null) {
        StringBuilder builder = new StringBuilder();
        for (WeightTag entry : entries) {
          if (entry.getTag() != null && entry.getWeight() != 0) {
            if (builder.length() > 0) {
              builder.append(';');
            }
            builder.append(entry.getTag() + ":" + entry.getWeight() + ":" + entry.getMaxFraction());
          }
        }
        getBean().setGeneratePushTag(builder.toString());
        // ((HashMap<String,Object>)getBufferedModel("shuffleOpts").getValue()).put("pushTags",
        // builder.toString());
      }
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      WeightTag entry = this.entries.get(rowIndex);
      switch (columnIndex) {
      case 0:
        entry.setTag((String) aValue);
        break;
      case 1:
        if (aValue instanceof String) {
          try {
            aValue = Integer.parseInt((String) aValue);
          } catch (NumberFormatException e) {
          }
        }
        entry.setWeight((Integer) aValue);
        break;
      case 2:
        entry.setMaxFraction((Float) aValue);
        break;
      }
      updatePlaylist();
      if (this.entries.get(this.entries.size() - 1).getTag() != null) {
        this.entries.add(new WeightTag(null, 0, 1f));
        this.fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
      }
    }

    @Override
    public Class<?> getColumnClass(int col) {
      switch (col) {
      case 0:
        return String.class;
      case 1:
        return Integer.class;
      case 2:
        return Float.class;
      default:
        return super.getColumnClass(col);
      }
    }

  }

  protected class TagsModel extends AbstractValueModel {
    private static final long serialVersionUID = -4179007408882775573L;

    /**
     * @see com.jgoodies.binding.value.ValueModel#getValue()
     */
    @Override
    public Object getValue() {
      if (getBean() != null) {
        return tagsToString(getBean().getTags());
      }
      return null;
    }

    /**
     * @see com.jgoodies.binding.value.ValueModel#setValue(java.lang.Object)
     */
    @Override
    @SuppressWarnings("rawtypes")
    public void setValue(Object value) {
      if (getBean() != null) {
        if (value instanceof String) {
          String txt = (String) value;
          String[] tags = StringUtils.split(txt, "\r\n");
          getBean().setTags(new HashSet<String>(Arrays.asList(tags)));
        } else if (value instanceof HashSet) {
          getBean().setTags((HashSet) value);

        } else {
          getBean().setTags(new HashSet<String>());
        }
      }
    }

  }

  protected class TrackTagsModel extends AbstractValueModel {
    private static final long serialVersionUID = -7885567350534821978L;

    /**
     * @see com.jgoodies.binding.value.ValueModel#getValue()
     */
    @Override
    public List<String> getValue() {
      if (getBean() != null) {
        ArrayList<String> list = new ArrayList<String>();
        String titleTagStr = getBean().getGenerateTags();
        if (titleTagStr != null) {
          String[] tags = StringUtils.split(titleTagStr, ";");
          for (String tag : tags) {
            list.add(tag);
          }
        }
        return list;
      }
      return null;
    }

    /**
     * @see com.jgoodies.binding.value.ValueModel#setValue(java.lang.Object)
     */
    @Override
    public void setValue(Object value) {
      if (getBean() != null) {
        if (value instanceof List<?>) {
          List<String> list = (List<String>) value;
          StringBuilder buf = new StringBuilder();
          for (String tag : list) {
            if (tagManager.getTag(tag) != null) {
              if (buf.length() > 0) {
                buf.append(';');
              }
              buf.append(tag);
            }
          }
          getBean().setGenerateTags(buf.toString());
        } else {
          getBean().setGenerateTags(null);
        }
      }
    }

  }

  static class WeightTag {
    private String tag;
    private int weight;
    private float maxFraction;

    /**
     * @param tag
     * @param weight
     */
    public WeightTag(String tag, int weight, float maxFraction) {
      super();
      this.tag = tag;
      this.weight = weight;
      this.maxFraction = maxFraction;
    }

    /**
     * @return the tag
     */
    public String getTag() {
      return tag;
    }

    /**
     * @return the weight
     */
    public int getWeight() {
      return weight;
    }

    /**
     * @param tag the tag to set
     */
    public void setTag(String tag) {
      this.tag = tag;
    }

    /**
     * @param weight the weight to set
     */
    public void setWeight(int weight) {
      this.weight = weight;
    }

    public float getMaxFraction() {
      return maxFraction;
    }

    public void setMaxFraction(float maxFraction) {
      this.maxFraction = maxFraction;
    }

  }

  class ShuffleScriptModel extends AbstractValueModel {
    private ShuffleScriptMeta script;
    private String shuffleType;

    @Override
    public Object getValue() {
      String shuffleType = getBufferedModel("shuffleType").getString();
      if (shuffleType != null) {
        if (!shuffleType.equals(this.shuffleType)) {
          this.script = PlaylistService.getShuffleScriptMeta(shuffleScripts, shuffleType);
          this.shuffleType = shuffleType;
        }
        return script;

      } else {
        return null;
      }
    }

    @Override
    public void setValue(Object value) {
      ShuffleScriptMeta script = (ShuffleScriptMeta) value;
      if (script != null) {
        getBufferedModel("shuffleType").setValue(script.getKey());
        this.script = script;
        this.shuffleType = script.getKey();
      } else {
        getBufferedModel("shuffleType").setValue(null);
      }
    }
  }

  public ValueModel getAdvices() {
    return advices;
  }

  public ValueHolder getTitleNameAdviceLimit() {
    return titleNameAdviceLimit;
  }

  public ValueModel getTrackOrderType() {
    return trackOrderType;
  }

  public PresentationModel<AutoFillRule> getAutoFillModel() {
    return autoFillModel;
  }

  public List<String> validate() {
    List<String> messages = new ArrayList<>();
    validateShuffleTagPattern(messages);
    return messages;

  }

  private void validateShuffleTagPattern(List<String> messages) {
    if (trackOrderType.getValue().equals(TrackOrderOption.SHUFFLE_SERVER) && this.shuffleScript.getValue() != null
        && ((ShuffleScriptMeta) this.shuffleScript.getValue()).getKey().equals(ShuffleScriptMeta.BUCKET)) {
      HashMap<String, Object> opts = (HashMap<String, Object>) getBufferedModel("shuffleOpts").getValue();
      if (!opts.containsKey("pattern") || opts.get("pattern").equals("")) {
        messages.add(textProvider.getString("playlist.cfg.validation.tagPatternMissing"));
      } else {
        String[] tags = StringUtils.split((String) opts.get("pattern"), ",");
        StringBuffer missingTags = new StringBuffer();
        for (String tag : tags) {
          if (!(tag.equals("song") || tag.equals("jingle") || tag.equals("moderation"))) {
            if (!(tagManager.getTag(tag) instanceof StaticTag)) {
              if (missingTags.length() > 0) {
                missingTags.append(',');
              }
              missingTags.append(tag);
            }
          }
        }
        if (missingTags.length() > 0) {
          messages.add(textProvider.getString("playlist.cfg.validation.invalidTags", missingTags.toString()));
        }
      }
    }

  }

  public ValueModel getShuffleScript() {
    return shuffleScript;
  }

  public List<ShuffleScriptMeta> getShuffleScripts() {
    return shuffleScripts;
  }

  public IndirectListModel<String> getProfileListModel() {
    return profileListModel;
  }

  String getProfileName(String id) {
    // TODO improve
    for (PlaylistProfile p : profiles) {
      if (p.getId().equals(id)) {
        return p.getName();
      }
    }
    return id;
  }

}
