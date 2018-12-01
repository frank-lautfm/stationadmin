package de.stationadmin.gui.playlist.config;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultCellEditor;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.apache.commons.lang3.StringUtils;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;

public class ShuffleOptionsPanel extends JPanel {
  private static final long serialVersionUID = -5555289920437431308L;
  private JPanel bucketOptsPanel;
  private JPanel stationAdminOptsPanel;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  public ShuffleOptionsPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.model = model;
    this.setLayout(new BorderLayout());

    this.bucketOptsPanel = createBucketOptsPanel();
    this.stationAdminOptsPanel = createStationAdminOptsPanel();

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
    if (model.getBufferedModel("shuffleType").getValue().equals(PlaylistConfigurationModel.SHUFFLE_BUCKET)) {
      this.add(bucketOptsPanel, BorderLayout.CENTER);
    } else {
      this.add(stationAdminOptsPanel, BorderLayout.CENTER);
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
    JPanel panel = new JPanel(new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "8dlu,pref,8dlu,pref,5dlu,pref, 10dlu,pref,5dlu,70dlu,5dlu"));
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
      tfPanel.add(new JLabel(this.ctx.getTextProvider().getString("playlistcfg.property.shuffleAvoidRepeat3")), cc.xy(3, 1));
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

    }

    model.getBufferedModel("shuffleOpts").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        HashMap<String, Object> opts = getOptions();
        maxArtistTracksHolder.setValue(opts.containsKey("maxTracksPerArtist") ? opts.get("maxTracksPerArtist") : 0);
        maxArtistTracksHolder.setValue(opts.containsKey("avoidRepeat") ? opts.get("avoidRepeat") : 2);
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
