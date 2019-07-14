package de.stationadmin.gui.playlist.config;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.ShuffleScriptMeta;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.EnumListCellRenderer;
import de.stationadmin.gui.util.HintLabel;

public class ShuffleOptionsPanel extends JPanel {
  private static final long serialVersionUID = -5555289920437431308L;
  private JPanel bucketOptsPanel;
  private JPanel stationAdminOptsPanel;
  private JPanel blockSelectionOptsPanel;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  public ShuffleOptionsPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.model = model;
    this.setLayout(new BorderLayout());

    this.bucketOptsPanel = createBucketOptsPanel();
    this.stationAdminOptsPanel = createStationAdminOptsPanel();
    this.blockSelectionOptsPanel = createBlockSelectOptsPanel();

    this.updatePanel();
    this.model.getBufferedComponentModel("shuffleType").addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updatePanel();
      }
    });
  }

  private void updatePanel() {
    this.removeAll();
    ShuffleScriptMeta script = (ShuffleScriptMeta) model.getShuffleScript().getValue();
    if (script != null && script.getOptsKey() != null) {
      if (script.getOptsKey().equalsIgnoreCase(ShuffleScriptMeta.BUCKET)) {
        this.add(bucketOptsPanel, BorderLayout.CENTER);
      } else if (script.getOptsKey().equalsIgnoreCase(ShuffleScriptMeta.BLOCKSELECT)) {
        this.add(blockSelectionOptsPanel, BorderLayout.CENTER);
      } else if (script.getOptsKey().equalsIgnoreCase(ShuffleScriptMeta.STATIONADMIN)) {
        this.add(stationAdminOptsPanel, BorderLayout.CENTER);
      }
    }
    this.invalidate();
    this.repaint();
  }

  private JPanel createBucketOptsPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "8dlu,pref,5dlu,pref,5dlu"));

    CellConstraints cc = new CellConstraints();
    int row = 2;

    final ValueHolder sequence = new ValueHolder();

    sequence.addPropertyChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        String[] tags = (String[]) sequence.getValue();
        StringBuilder buf = new StringBuilder();
        for (int i = 0; i < tags.length; i++) {
          if (tags[i].indexOf(',') > -1) {
            continue;
          }
          if (buf.length() > 0) {
            buf.append(",");
          }
          buf.append(tags[i]);
        }
        if (buf.length() > 0) {
          getOptions().put("pattern", buf.toString());
        } else {
          getOptions().remove("pattern");
        }
      }
    });

    updatePatternModel(sequence);

    List<String> tags = new ArrayList<>();
    tags.add("song");
    tags.add("jingle");
    tags.add("moderation");

    List<StaticTag> staticTags = ctx.getAdminClient().getTagManager().getStaticTags();
    Collections.sort(staticTags, (c1, c2) -> c1.getName().compareTo(c2.getName()));
    staticTags.forEach(t -> tags.add(t.getName()));

    TagSequenceEditor editor = new TagSequenceEditor(tags.toArray(new String[tags.size()]), sequence, true);

    panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.tagSequence")), cc.xy(2, row));
    row += 2;
    panel.add(editor, cc.xy(2, row));

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updatePatternModel(sequence);
      }
    });

    return panel;
  }

  private void updatePatternModel(ValueHolder sequence) {
    if (getOptions().containsKey("pattern")) {
      String pattern = (String) getOptions().get("pattern");
      String[] tags = StringUtils.split(pattern, ',');
      for (int i = 0; i < tags.length; i++) {
        tags[i] = tags[i].trim();
      }
      sequence.setValue(tags);
    } else {
      sequence.setValue(new String[0]);
    }

  }

  @SuppressWarnings("unchecked")
  HashMap<String, Object> getOptions() {
    return (HashMap<String, Object>) model.getBufferedModel("shuffleOpts").getValue();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private JPanel createStationAdminOptsPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "8dlu,pref,7dlu,pref,7dlu,pref,5dlu,pref,10dlu,pref,5dlu,70dlu,7dlu,pref,7dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    // Max tracks per artist
    final ValueHolder maxArtistTracksHolder = new ValueHolder(getOptions().containsKey("maxTracksPerArtist") ? getOptions().get("maxTracksPerArtist") : 0);
    {
      JTextField maxArtistTracksTf = BasicComponentFactory.createIntegerField(maxArtistTracksHolder, 0);
      maxArtistTracksTf.setColumns(3);
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.generateMaxArtistTitles")), cc.xy(2, row));
      panel.add(maxArtistTracksTf, cc.xy(4, 2, CellConstraints.LEFT, CellConstraints.CENTER));

      maxArtistTracksHolder.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = (Integer) evt.getNewValue();
          if (value.intValue() > 0) {
            getOptions().put("maxTracksPerArtist", value);
          } else {
            getOptions().remove("maxTracksPerArtist");
          }
        }
      });
      row += 2;
    }

    // block length
    final ValueHolder blockLengthHolder = new ValueHolder(getOptions().containsKey("blockLength") ? getOptions().get("blockLength") : 0);
    {
      JTextField blockLengthTf = BasicComponentFactory.createIntegerField(blockLengthHolder, 0);
      blockLengthTf.setColumns(3);
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleBlockLength")), cc.xy(2, row));

      JPanel tfPanel = new JPanel(new FormLayout("pref,3dlu,pref", "pref"));
      tfPanel.add(blockLengthTf, cc.xy(1, 1));
      tfPanel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.hours")), cc.xy(3, 1));
      panel.add(tfPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));

      panel.add(tfPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));

      blockLengthHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = (Integer) evt.getNewValue();
          if (value.intValue() > 0) {
            getOptions().put("blockLength", value);
          } else {
            getOptions().remove("blockLength");
          }
        }
      });
      row += 2;
    }

    // Max tracks per artist
    final ValueHolder avoidRepeat = new ValueHolder(getOptions().containsKey("avoidRepeat") ? getOptions().get("avoidRepeat") : 2);
    {
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat1")), cc.xywh(2, row, 3, 1));
      row += 2;

      JTextField avoidRepeatTf = BasicComponentFactory.createIntegerField(avoidRepeat, 0);
      avoidRepeatTf.setColumns(3);
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat2")), cc.xy(2, row));

      JPanel tfPanel = new JPanel(new FormLayout("pref,3dlu,pref", "pref"));
      tfPanel.add(avoidRepeatTf, cc.xy(1, 1));
      tfPanel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.hours")), cc.xy(3, 1));
      panel.add(tfPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));

      avoidRepeat.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = (Integer) evt.getNewValue();
          if (value.intValue() > 0) {
            getOptions().put("avoidRepeat", value);
          } else {
            getOptions().remove("avoidRepeat");
          }
        }
      });
      row += 2;
    }

    // Tag weights
    TagWeightsTableModel tagWeightsModel = new TagWeightsTableModel();
    {
      ArrayList<String> values = new ArrayList<String>();
      values.add(null);
      List<StaticTag> tags = ctx.getAdminClient().getTagManager().getStaticTags();
      Collections.sort(tags, (c1, c2) -> c1.getName().compareTo(c2.getName()));
      tags.forEach(t -> values.add(t.getName()));
      JComboBox tagCombo = new JComboBox(values.toArray());

      JComboBox weightCombo = new JComboBox(new Integer[] { -9, -3, -2, -1, 0, 1, 2, 3 });
      weightCombo.setRenderer(new TagWeightListCellRenderer(ctx.getTextProvider()));

      final TableCellRenderer weighRenderer = new DefaultTableCellRenderer() {
        private static final long serialVersionUID = 3085732842546961918L;

        @Override
        protected void setValue(Object value) {
          if (value != null && !value.equals(Integer.valueOf(0))) {
            setText(ctx.getTextProvider().getString("playlistcfg.property.generatePushTag.option." + value));
          } else {
            setText(" ");
          }
        }

      };

      panel.add(new JLabel(ctx.getTextProvider().getString("playlistcfg.property.generatePushTag")), cc.xy(2, row));
      row += 2;

      JXTable table = new JXTable(tagWeightsModel) {
        private static final long serialVersionUID = -2623802397206568002L;

        @Override
        public TableCellRenderer getCellRenderer(int row, int column) {
          if (column == 1) {
            return weighRenderer;
          }
          return super.getCellRenderer(row, column);
        }

      };
      table.getColumnModel().getColumn(0).setCellEditor(new DefaultCellEditor(tagCombo));
      table.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(weightCombo));

      panel.add(new JScrollPane(table), cc.xywh(2, row, 3, 1));
      row += 2;

    }

    final ValueHolder trackNameLimit = new ValueHolder(getOptions().containsKey("trackNameLimit") ? getOptions().get("trackNameLimit") : 0);
    {
      JPanel titleNameLimitPanel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref", "pref"));
      titleNameLimitPanel.add(new JLabel(ctx.getTextProvider().getString("playlistcfg.advice.titlename.description.pre")), cc.xy(1, 1));
      JTextField tf = BasicComponentFactory.createIntegerField(trackNameLimit, 0);
      tf.setColumns(2);
      titleNameLimitPanel.add(tf, cc.xy(3, 1));
      titleNameLimitPanel.add(new JLabel(ctx.getTextProvider().getString("playlistcfg.advice.titlename.description.post")), cc.xy(5, 1));

      panel.add(titleNameLimitPanel, cc.xywh(2, row, 3, 1, CellConstraints.FILL, CellConstraints.CENTER));

      trackNameLimit.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = (Integer) evt.getNewValue();
          if (value.intValue() > 0) {
            getOptions().put("trackNameLimit", value);
          } else {
            getOptions().remove("trackNameLimit");
          }
        }
      });

      row += 2;
    }
    
    boolean containsNews = false;
    for (Entry entry : model.getBean().getEntries()) {
      if(entry.getTrackId() == 1) {
        containsNews = true;
        break;
      }
    }


    final ValueHolder newsInterval = new ValueHolder(getOptions().containsKey("newsInterval") ? getOptions().get("newsInterval") : 60);
    {
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.news")), cc.xywh(2, row, 3, 1));
      SelectionInList<Integer> opts = new SelectionInList<>(new Integer[] { 60, 120, 180, 240, 300, 360 }, newsInterval);

      JComboBox<Integer> optsCmb = BasicComponentFactory.createComboBox(opts, new EnumListCellRenderer(ctx.getTextProvider(), "playlistcfg.property.newsInterval.option"));
      panel.add(optsCmb, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      optsCmb.setEnabled(containsNews);

      newsInterval.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = (Integer) evt.getNewValue();
          if (value.intValue() > 0) {
            getOptions().put("newsInterval", value);
          } else {
            getOptions().put("newsInterval", 60);
          }
        }
      });
      row += 2;

    }

    final ValueModel firstJingleAfterNews = new ValueHolder(getOptions().containsKey("firstJingleAfterNews") ? getOptions().get("firstJingleAfterNews") : false);
    {
      JCheckBox repeatJingleCb = BasicComponentFactory.createCheckBox(firstJingleAfterNews, ctx.getTextProvider().getString("playlistcfg.property.firstJingleAfterNews"));
      panel.add(repeatJingleCb, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      repeatJingleCb.setEnabled(containsNews && ctx.getAdminClient().getSettings().isShuffleProtectFirstJingle());

      firstJingleAfterNews.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Boolean value = (Boolean) evt.getNewValue();
          getOptions().put("firstJingleAfterNews", value);
        }
      });
      row += 2;

    }

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        HashMap<String, Object> opts = getOptions();
        maxArtistTracksHolder.setValue(opts.containsKey("maxTracksPerArtist") ? opts.get("maxTracksPerArtist") : 0);
        avoidRepeat.setValue(opts.containsKey("avoidRepeat") ? opts.get("avoidRepeat") : 2);
        trackNameLimit.setValue(opts.containsKey("trackNameLimit") ? opts.get("trackNameLimit") : 0);
        blockLengthHolder.setValue(opts.containsKey("blockLength") ? opts.get("blockLength") : 0);
        newsInterval.setValue(opts.containsKey("newsInterval") ? opts.get("newsInterval") : 60);
        firstJingleAfterNews.setValue(opts.containsKey("firstJingleAfterNews") ? opts.get("firstJingleAfterNews") : false);
        tagWeightsModel.rebuild();
      }

    });

    return panel;
  }

  @SuppressWarnings("unchecked")
  private JPanel createBlockSelectOptsPanel() {
    JPanel panel = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,8dlu,pref,5dlu,pref,8dlu:grow,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    // separator
    panel.add(new JLabel(ctx.getString("playlistcfg.property.blockselect.separator")), cc.xy(2, row));
    row += 2;

    final HashMap<String, Object> opts = getOptions();
    final ValueHolder separatorTrackId = new ValueHolder(opts.containsKey("separatorId") ? opts.get("separatorId") : -1);
    separatorTrackId.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        opts.put("separatorId", evt.getNewValue());
      }
    });
    List<Integer> separatorCandidates = new ArrayList<>();
    separatorCandidates.add(-1);
    for (Entry entry : model.getBean().getEntries()) {
      if (entry.getTrack().getType() == BasicTrack.TYPE_JINGLE && !separatorCandidates.contains(entry.getTrackId())) {
        separatorCandidates.add(entry.getTrackId());
      }
    }

    SelectionInList<Integer> separatorTrackSelection = new SelectionInList<>(separatorCandidates, separatorTrackId);
    JComboBox<Integer> separatorCmb = BasicComponentFactory.createComboBox(separatorTrackSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 7364200569540373171L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        int v = (Integer) value;
        if (v < 0) {
          setText("kein Jingle - Aufteilung nach Zeit");
        } else {
          BasicTrack track = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(v);
          if (track != null) {
            setText(track.getArtist() + " - " + track.getTitle());
          }
        }

        return component;
      }

    });
    panel.add(separatorCmb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    final ValueHolder includeSeparator = new ValueHolder(
        opts.containsKey("includeSeparatorTrack") ? (opts.get("includeSeparatorTrack").toString().equalsIgnoreCase("true") ? true : false) : false);
    includeSeparator.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        opts.put("includeSeparatorTrack", evt.getNewValue());
      }
    });
    JCheckBox includeSeparatorCb = BasicComponentFactory.createCheckBox(includeSeparator, ctx.getString("playlistcfg.property.blockselect.incseparator"));
    panel.add(includeSeparatorCb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    panel.add(new JLabel(ctx.getString("playlistcfg.property.blockselect.iterationhours")), cc.xy(2, row));
    row += 2;

    List<Integer> hours = new ArrayList<>();
    hours.add(0);
    hours.add(1);
    hours.add(2);
    hours.add(4);
    hours.add(6);
    hours.add(8);
    hours.add(12);
    hours.add(24);
    hours.add(168);

    final ValueHolder iterationStepHours = new ValueHolder(opts.containsKey("iterationStepHours") ? opts.get("iterationStepHours") : 0);
    iterationStepHours.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        opts.put("iterationStepHours", evt.getNewValue());
      }
    });
    SelectionInList<Integer> hoursSelection = new SelectionInList<>(hours, iterationStepHours);
    JComboBox<Integer> hoursCmb = BasicComponentFactory.createComboBox(hoursSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 7364200569540373171L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component component = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        int v = value instanceof Integer ? (Integer) value : 0;
        switch (v) {
        case 0:
          setText(ctx.getString("playlistcfg.property.blockselect.iterationhours.0"));
          break;
        case 1:
          setText(ctx.getString("playlistcfg.property.blockselect.iterationhours.1"));
          break;
        case 24:
          setText(ctx.getString("playlistcfg.property.blockselect.iterationhours.24"));
          break;
        case 168:
          setText(ctx.getString("playlistcfg.property.blockselect.iterationhours.168"));
          break;
        default:
          setText(ctx.getString("playlistcfg.property.blockselect.iterationhours.default", Integer.toString(v)));
          break;

        }
        return component;
      }

    });
    panel.add(hoursCmb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    JLabel hintLb = new HintLabel(ctx.getString("playlistcfg.property.blockselect.hint"));
    panel.add(hintLb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        HashMap<String, Object> opts = getOptions();
        separatorTrackId.setValue(opts.containsKey("separatorId") ? opts.get("separatorId") : -1);
        includeSeparator.setValue(opts.containsKey("includeSeparatorTrack") ? (opts.get("includeSeparatorTrack").toString().equalsIgnoreCase("true") ? true : false) : false);
        iterationStepHours.setValue(opts.containsKey("iterationStepHours") ? opts.get("iterationStepHours") : -1);
      }
    });

    return panel;
  }

  public class TagWeightsTableModel extends AbstractTableModel {
    private static final long serialVersionUID = -8675947172136215033L;

    private List<ShuffleTagWeight> entries = new ArrayList<ShuffleTagWeight>();

    TagWeightsTableModel() {
      this.rebuild();

    }

    @Override
    public int getColumnCount() {
      return 2;
    }

    @Override
    public String getColumnName(int column) {
      switch (column) {
      case 0:
        return "Tag";
      case 1:
        return "Gewichtung";
      }
      return null;
    }

    @Override
    public int getRowCount() {
      return entries.size();
    }

    @Override
    public Object getValueAt(int row, int col) {
      ShuffleTagWeight entry = this.entries.get(row);
      switch (col) {
      case 0:
        return entry.getTag();
      case 1:
        return entry.getWeight();
      }
      return null;
    }

    @Override
    public boolean isCellEditable(int rowIndex, int columnIndex) {
      return true;
    }

    @SuppressWarnings("unchecked")
    public void rebuild() {
      this.entries.clear();
      HashMap<String, Object> opts = getOptions();

      Object weightsObj = opts.get("tagWeights");
      if (weightsObj instanceof Map) {
        Map<String, Integer> weights = (Map<String, Integer>) weightsObj;
        ArrayList<String> keys = new ArrayList<String>(weights.keySet());
        Collections.sort(keys);
        for (String key : keys) {
          this.entries.add(new ShuffleTagWeight(key, weights.get(key)));
        }
      }

      this.entries.add(new ShuffleTagWeight(null, 0));
      this.fireTableDataChanged();
    }

    @Override
    public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
      ShuffleTagWeight entry = this.entries.get(rowIndex);
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
      }

      // update opts entry
      Map<String, Integer> newWeights = new HashMap<>();
      for (ShuffleTagWeight e : entries) {
        if (e.getTag() != null && e.getWeight() != 0) {
          newWeights.put(e.getTag(), e.getWeight());
        }
      }
      getOptions().put("tagWeights", newWeights);

      if (this.entries.get(this.entries.size() - 1).getTag() != null) {
        this.entries.add(new ShuffleTagWeight(null, 0));
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
      default:
        return super.getColumnClass(col);
      }
    }

  }

  static class ShuffleTagWeight {
    String tag;
    int weight;

    public ShuffleTagWeight(String tag, int weight) {
      this.tag = tag;
      this.weight = weight;
    }

    public String getTag() {
      return tag;
    }

    public void setTag(String tag) {
      this.tag = tag;
    }

    public int getWeight() {
      return weight;
    }

    public void setWeight(int weight) {
      this.weight = weight;
    }
  }

}
