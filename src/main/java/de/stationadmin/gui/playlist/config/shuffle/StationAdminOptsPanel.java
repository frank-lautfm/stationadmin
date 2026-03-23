package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.Console;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.util.EnumListCellRenderer;

public class StationAdminOptsPanel extends JPanel {
  private static final long serialVersionUID = -5555289920437431308L;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  public StationAdminOptsPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.model = model;
    this.setLayout(new BorderLayout());

    this.add(createStationAdminOptsPanel(), BorderLayout.CENTER);
  }


  @SuppressWarnings("unchecked")
  HashMap<String, Object> getOptions() {
    return (HashMap<String, Object>) model.getBufferedModel("shuffleOpts").getValue();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private JPanel createStationAdminOptsPanel() {
    JPanel panel = new JPanel(
        new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "8dlu,pref,7dlu,pref,7dlu,pref,5dlu,pref,10dlu,pref,5dlu,70dlu,7dlu,pref,7dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
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
    final ValueHolder excludePreviousTracks = new ValueHolder(getOptions().containsKey("excludePreviousTracks") && getOptions().get("excludePreviousTracks").equals(1) ? true : false);
    {
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat1")), cc.xywh(2, row, 3, 1));
      row += 2;

      JTextField avoidRepeatTf = BasicComponentFactory.createIntegerField(avoidRepeat, 0);
      avoidRepeatTf.setColumns(3);
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat2")), cc.xy(2, row));

      JPanel tfPanel = new JPanel(new FormLayout("pref,3dlu,pref,5dlu,pref", "pref"));
      tfPanel.add(avoidRepeatTf, cc.xy(1, 1));
      tfPanel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.hours")), cc.xy(3, 1));
      
      JCheckBox excludeCb = BasicComponentFactory.createCheckBox(excludePreviousTracks, this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat3"));
      tfPanel.add(excludeCb, cc.xy(5, 1));
      
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
      
      excludePreviousTracks.addValueChangeListener(new PropertyChangeListener() {
				
				@Override
				public void propertyChange(PropertyChangeEvent evt) {
					Boolean value = (Boolean)evt.getNewValue();
					if(value.booleanValue()) {
            getOptions().put("excludePreviousTracks", 1);
					}
					else {
            getOptions().remove("excludePreviousTracks");
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

    final boolean isExcludeAll = getOptions().containsKey("trackNameLimit") && Integer.valueOf(9999).equals(getOptions().get("trackNameLimit"));
    final ValueHolder trackNameLimit = new ValueHolder(isExcludeAll ? 0 : (getOptions().containsKey("trackNameLimit") ? getOptions().get("trackNameLimit") : 0));
    {
      // Wrap text-field row and checkbox in one sub-panel so they appear close together
      CellConstraints icc = new CellConstraints();
      JPanel trackNameLimitGroup = new JPanel(new FormLayout("pref", "pref,3dlu,pref"));

      JPanel titleNameLimitPanel = new JPanel(new FormLayout("pref,2dlu,pref,2dlu,pref", "pref"));
      titleNameLimitPanel.add(new JLabel(ctx.getTextProvider().getString("playlistcfg.advice.titlename.description.pre")), icc.xy(1, 1));
      final JTextField tf = BasicComponentFactory.createIntegerField(trackNameLimit, 0);
      tf.setColumns(2);
      if (isExcludeAll) {
        tf.setEditable(false);
      }
      titleNameLimitPanel.add(tf, icc.xy(3, 1));
      titleNameLimitPanel.add(new JLabel(ctx.getTextProvider().getString("playlistcfg.advice.titlename.description.post")), icc.xy(5, 1));

      trackNameLimitGroup.add(titleNameLimitPanel, icc.xy(1, 1));

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

      JCheckBox excludeAllCb = new JCheckBox(ctx.getTextProvider().getString("playlistcfg.advice.titlename.excludecompletely"));
      excludeAllCb.setSelected(isExcludeAll);
      excludeAllCb.addItemListener(new ItemListener() {
        @Override
        public void itemStateChanged(ItemEvent e) {
          if (e.getStateChange() == ItemEvent.SELECTED) {
            tf.setText("");
            tf.setEditable(false);
            trackNameLimit.setValue(0);
            getOptions().put("trackNameLimit", 9999);
          } else {
            tf.setEditable(true);
            getOptions().remove("trackNameLimit");
          }
        }
      });

      trackNameLimitGroup.add(excludeAllCb, icc.xy(1, 3));

      panel.add(trackNameLimitGroup, cc.xywh(2, row, 3, 1, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;
    }

    boolean containsNews = false;
    for (Entry entry : model.getBean().getEntries()) {
      if (entry.getTrackId() == 1) {
        containsNews = true;
        break;
      }
    }
    if (containsNews) {
      HashMap<String, Object> options = getOptions();
      if (!options.containsKey("newsInterval")) {
        options.put("newsInterval", 60);
      }
      if (!options.containsKey("newsMin")) {
        options.put("newsMin", 59);
      }
      if (!options.containsKey("newsMax")) {
        options.put("newsMax", 15);
      }
    } else {
      HashMap<String, Object> options = getOptions();
      options.remove("newsInterval");
      options.remove("newsMin");
      options.remove("newsMax");
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

    final ValueHolder newsMin = new ValueHolder(getOptions().containsKey("newsMin") ? getOptions().get("newsMin") : 59);
    final ValueHolder newsMax = new ValueHolder(getOptions().containsKey("newsMax") ? getOptions().get("newsMax") : 15);
    {
      panel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.news.time")), cc.xywh(2, row, 3, 1));

      JPanel timePanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      JTextField minTf = BasicComponentFactory.createIntegerField(newsMin);
      minTf.setColumns(2);
      JTextField maxTf = BasicComponentFactory.createIntegerField(newsMax);
      maxTf.setColumns(2);
      timePanel.add(minTf);
      timePanel.add(new JLabel(" - "));
      timePanel.add(maxTf);
      panel.add(timePanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));

      newsMin.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = evt.getNewValue() != null ? (Integer) evt.getNewValue() : 59;
          getOptions().put("newsMin", value.intValue() >= 0 && value < 60 ? value : 59);
        }
      });

      newsMax.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer value = evt.getNewValue() != null ? (Integer) evt.getNewValue() : 15;
          getOptions().put("newsMax", value.intValue() >= 0 && value < 60 ? value : 15);
        }
      });

      minTf.setEnabled(containsNews);
      maxTf.setEnabled(containsNews);

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
        newsMin.setValue(opts.containsKey("newsMin") ? opts.get("newsMin") : 59);
        newsMax.setValue(opts.containsKey("newsMax") ? opts.get("newsMax") : 15);
        tagWeightsModel.rebuild();
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
