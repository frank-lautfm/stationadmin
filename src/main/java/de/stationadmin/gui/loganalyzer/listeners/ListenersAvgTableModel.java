/**
 * 
 */
package de.stationadmin.gui.loganalyzer.listeners;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.loganalyzer.ListenersAvgEntry;
import de.stationadmin.base.loganalyzer.ListenersEntry;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 *
 */
public class ListenersAvgTableModel extends AbstractTableModel {

  enum Column {
    TIME, LISTENERS, FRACTION
  }

  private TextProvider textProvider;
  
  private List<ListenersAvgEntry> entries;
  private NumberFormat numFormat = NumberFormat.getIntegerInstance();

  @SuppressWarnings("unchecked")
  public ListenersAvgTableModel(TextProvider textProvider, ValueModel entriesHolder) {
    super();
    this.textProvider = textProvider;
    this.entries = (List<ListenersAvgEntry>)entriesHolder.getValue();
    entriesHolder.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        entries = (List<ListenersAvgEntry>)evt.getNewValue();
        fireTableDataChanged();
      }
    });
    
    numFormat.setMinimumIntegerDigits(2);
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch(col) {
    case LISTENERS:
      return Integer.class;
    case FRACTION: 
      return Integer.class;
    case TIME:
      return String.class;
    }
    return super.getColumnClass(columnIndex);
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("listeners.avg.column." + col.name().toLowerCase());
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.entries != null ? this.entries.size() : 0;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    ListenersAvgEntry entry = this.entries.get(rowIndex);
    switch(col) {
    case TIME:
      return this.numFormat.format(entry.getStartHour()) + " - " + this.numFormat.format(entry.getEndHour() + 1);
    case LISTENERS:
      return entry.getListeners();
    case FRACTION:
      return entry.getFraction();
    }
    return null;
  }

}
