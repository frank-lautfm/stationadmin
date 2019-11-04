package de.stationadmin.gui.playlist.profile;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRule.FilterType;
import de.stationadmin.base.playlist.shuffle.TrackRule.TrackPosition;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.gui.TextProvider;

public class TrackRuleTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 3138031163355090477L;
  private TrackRegistry trackRegistry;
  private TextProvider textProvider;
  private List<TrackRuleGroup> groups;
  private List<TrackRule> displayedRules;
  private List<TrackRule> rules;

  TrackRuleTableModel(TextProvider textProvider, TrackRegistry trackRegistry) {
    this.textProvider = textProvider;
    this.trackRegistry = trackRegistry;
    this.groups = new ArrayList<>();
    this.rules = new ArrayList<>();
    updateDisplayedRules();
  }
  
  private void updateDisplayedRules() {
    this.displayedRules = new ArrayList<TrackRule>(rules);
    this.displayedRules.add(new TrackRule());
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
    case 0:
      return TrackRuleGroup.class;
    case 1:
      return BasicTrack.class;
    case 2:
      return TrackRule.TrackPosition.class;
    case 3:
      return TrackRule.FilterType.class;
    case 5:
      return Integer.class;
    default:
      return String.class;
    }
  }

  @Override
  public int getColumnCount() {
    return 6;
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
    case 0:
      return this.textProvider.getString("settings.playlistgen.table.rule.group");
    case 1:
      return this.textProvider.getString("settings.playlistgen.table.rule.track");
    case 2:
      return this.textProvider.getString("settings.playlistgen.table.rule.position");
    case 3:
      return this.textProvider.getString("settings.playlistgen.table.rule.bindingType");
    case 4:
      return this.textProvider.getString("settings.playlistgen.table.rule.bindTo");
    case 5:
      return this.textProvider.getString("settings.playlistgen.table.rule.distance");
    default:
      return null;
    }
  }

  @Override
  public int getRowCount() {
    return this.displayedRules.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    TrackRule entry = this.displayedRules.get(rowIndex);
    switch (columnIndex) {
    case 0:
      String name = entry.getGroupName();
      if (name != null) {
        for (TrackRuleGroup group : groups) {
          if (group.getName().equals(name)) {
            return group;
          }
        }
      }
      return null;
    case 1:
      return entry.getTrackId() > 0 ? trackRegistry.getTrack(entry.getTrackId()) : null;
    case 2:
      return entry.getPosition();
    case 3:
      return entry.getFilterType();
    case 4:
      return entry.getFilter();
    case 5:
      return entry.getMinDistance();
    }
    return null;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public void setValueAt(Object value, int rowIndex, int columnIndex) {
    TrackRule entry = this.displayedRules.get(rowIndex);
    switch (columnIndex) {
    case 0:
      if (value instanceof TrackRuleGroup) {
        entry.setGroupName(((TrackRuleGroup) value).getName());
      } else {
        entry.setGroupName((String) value);
      }
      break;
    case 1:
      if (value instanceof BasicTrack) {
        entry.setTrackId(((BasicTrack) value).getId());
      } else {
        entry.setTrackId(0);
      }
      break;
    case 2:
      entry.setPosition((TrackPosition) value);
      break;
    case 3:
      entry.setFilterType((FilterType) value);
      break;
    case 4:
      entry.setFilter((String) value);
      break;
    case 5:
      try {
        entry.setMinDistance(Integer.parseInt(value.toString()));
        break;
      } catch (Exception e) {
        entry.setMinDistance(0);
      }
    }
    updateModel();
  }

  private void updateModel() {
    this.rules.clear();
    boolean hasEmpty = false;
    for (TrackRule rule : displayedRules) {
      if (rule.getTrackId() > 0 && StringUtils.isNotEmpty(rule.getFilter())) {
        this.rules.add(rule);
      } else {
        hasEmpty = true;
      }
    }
    if (!hasEmpty) {
      this.displayedRules.add(new TrackRule());
      fireTableDataChanged();
    }
  }

  public void moveUp(int row) {
    if (row > 0 && displayedRules.get(row).getTrackId() > 0) {
      TrackRule rule = displayedRules.remove(row);
      displayedRules.add(row - 1, rule);
      updateModel();
      fireTableDataChanged();
    }
  }

  public void moveDown(int row) {
    moveUp(row + 1);
  }

  public List<TrackRuleGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<TrackRuleGroup> groups) {
    this.groups = groups != null ? groups : new ArrayList<>();
  }

  public List<TrackRule> getRules() {
    return rules;
  }

  public void setRules(List<TrackRule> rules) {
    this.rules = rules != null ? rules : new ArrayList<>();
    this.updateDisplayedRules();
    this.fireTableDataChanged();
  }

}
