package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.util.HintLabel;

public class BlockSelectPanel extends JPanel {
  private static final long serialVersionUID = -5555289920437431308L;
  private ClientContext ctx;
  private PlaylistConfigurationModel model;

  public BlockSelectPanel(ClientContext ctx, PlaylistConfigurationModel model) {
    this.ctx = ctx;
    this.model = model;
    this.init();
  }

  @SuppressWarnings("unchecked")
  HashMap<String, Object> getOptions() {
    return (HashMap<String, Object>) model.getBufferedModel("shuffleOpts").getValue();
  }


  @SuppressWarnings("unchecked")
  private void init() {
    setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref,5dlu,pref,8dlu,pref,5dlu,pref,8dlu:grow,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    // separator
    this.add(new JLabel(ctx.getString("playlistcfg.property.blockselect.separator")), cc.xy(2, row));
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
    this.add(separatorCmb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
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
    this.add(includeSeparatorCb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    this.add(new JLabel(ctx.getString("playlistcfg.property.blockselect.iterationhours")), cc.xy(2, row));
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
    this.add(hoursCmb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
    row += 2;

    JLabel hintLb = new HintLabel(ctx.getString("playlistcfg.property.blockselect.hint"));
    this.add(hintLb, cc.xy(2, row, CellConstraints.FILL, CellConstraints.CENTER));
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
