/**
 * 
 */
package de.stationadmin.gui.loganalyzer.listeners;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.loganalyzer.ListenersEntry;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class ListenersTableModel extends AbstractTableModel {

  private static final long serialVersionUID = -4417263108984168279L;
  private TextProvider textProvider;

  private List<ListenersEntry> entries;
  
  @SuppressWarnings("unchecked")
  public ListenersTableModel(TextProvider textProvider, ValueModel entriesHolder) {
    super();
    this.textProvider = textProvider;
    this.entries = (List<ListenersEntry>)entriesHolder.getValue();
    entriesHolder.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        entries = (List<ListenersEntry>)evt.getNewValue();
        fireTableDataChanged();
      }
    });
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (Column.values()[columnIndex]) {
    case DATE:
      return Date.class;
    case TIME:
      return Date.class;
    case LISTENERS:
      return Integer.class;
    }
    return super.getColumnClass(columnIndex);
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("listeners.column." + col.name().toLowerCase());
  }

  @Override
  public int getRowCount() {
    return this.entries != null ? this.entries.size() : 0;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ListenersEntry entry = this.entries.get(rowIndex);
    switch (Column.values()[columnIndex]) {
    case DATE:
      return entry.getTime();
    case TIME:
      return entry.getTime();
    case LISTENERS:
      return entry.getListeners();
    }
    return null;
  }
  
  enum Column {
    DATE, TIME, LISTENERS
  }
  
}
