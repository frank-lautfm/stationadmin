package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeListener;
import java.beans.PropertyChangeSupport;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;
import de.stationadmin.gui.TextProvider;

public class TrackRuleGroupTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -4948651435271479161L;
  private PropertyChangeSupport pcs = new PropertyChangeSupport(this);
  private TextProvider textProvider;
  private List<TrackRuleGroup> displayedGroups;
  private List<TrackRuleGroup> groups;

  TrackRuleGroupTableModel(TextProvider textProvider) {
    this.textProvider = textProvider;
    this.groups = new ArrayList<>();
    updateDisplayedGroups();
  }
  
  private void updateDisplayedGroups() {
    this.displayedGroups = new ArrayList<TrackRuleGroup>(groups);
    this.displayedGroups.add(new TrackRuleGroup());
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (columnIndex) {
    case 1:
      return MultiMatchSelection.class;
    case 2:
      return Integer.class;
    default:
      return String.class;
    }
  }

  @Override
  public int getColumnCount() {
    return 3;
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
    case 0:
      return this.textProvider.getString("settings.playlistgen.table.rule.name");
    case 1:
      return this.textProvider.getString("settings.playlistgen.table.rule.selection");
    case 2:
      return this.textProvider.getString("settings.playlistgen.table.rule.distance");
    default:
      return null;
    }
  }

  @Override
  public int getRowCount() {
    return this.displayedGroups.size();
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    TrackRuleGroup entry = this.displayedGroups.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return entry.getName();
    case 1:
      return entry.getMultiMatchSelection();
    case 2:
      return entry.getMinDistance();
    }
    return null;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    TrackRuleGroup entry = this.displayedGroups.get(rowIndex);
    switch (columnIndex) {
    case 0:
      String old = entry.getName();
      entry.setName((String) aValue);
      pcs.firePropertyChange("name", old, aValue);
      break;
    case 1:
      entry.setMultiMatchSelection((MultiMatchSelection) aValue);
      break;
    case 2:
      try {
        entry.setMinDistance(Integer.parseInt(aValue.toString()));
        break;
      } catch (Exception e) {
        entry.setMinDistance(0);
      }
    }
    updateModel();
  }

  private void updateModel() {
    this.groups.clear();
    boolean hasEmpty = false;
    for (TrackRuleGroup group : displayedGroups) {
      if (StringUtils.isNotEmpty(group.getName())) {
        this.groups.add(group);
      } else {
        hasEmpty = true;
      }
    }
    if (!hasEmpty) {
      this.displayedGroups.add(new TrackRuleGroup());
      fireTableDataChanged();
    }
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  public void addNameChangeListener(PropertyChangeListener listener) {
    pcs.addPropertyChangeListener("name", listener);
  }

  public List<TrackRuleGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<TrackRuleGroup> groups) {
    this.groups = groups != null ? groups : new ArrayList<>();
    this.updateDisplayedGroups();
    this.fireTableDataChanged();
  }
}
