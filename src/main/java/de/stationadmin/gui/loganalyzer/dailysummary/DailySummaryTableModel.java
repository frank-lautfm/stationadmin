/**
 * 
 */
package de.stationadmin.gui.loganalyzer.dailysummary;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.loganalyzer.DailySummary;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class DailySummaryTableModel extends AbstractTableModel {

  private static final long serialVersionUID = -4417263108984168279L;
  private TextProvider textProvider;

  private List<DailySummary> entries;
  
  @SuppressWarnings("unchecked")
  public DailySummaryTableModel(TextProvider textProvider, ValueModel entriesHolder) {
    super();
    this.textProvider = textProvider;
    this.entries = (List<DailySummary>)entriesHolder.getValue();
    entriesHolder.addValueChangeListener(new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        entries = (List<DailySummary>)evt.getNewValue();
        fireTableDataChanged();
      }
    });
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    switch (Column.values()[columnIndex]) {
    case DAY:
      return Date.class;
    case AVG_LISTENING_TIME:
    case DURATION:
    case LISTENERS:
    case UNIQS:
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
    return this.textProvider.getString("dailysummaries.column." + col.name().toLowerCase());
  }
  
  public DailySummary get(int row) {
    return this.entries.get(row);
  }

  @Override
  public int getRowCount() {
    return this.entries != null ? this.entries.size() : 0;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    DailySummary entry = this.entries.get(rowIndex);
    switch (Column.values()[columnIndex]) {
    case AVG_LISTENING_TIME:
      return entry.getAvgListeningTime() > 0 ? entry.getAvgListeningTime() : null;
    case DAY:
      return entry.getDay();
    case DURATION:
      return entry.getDuration() / 60;
    case UNIQS:
      return entry.getUniqs();
    case LISTENERS:
      return entry.getListeners() > 0 ? entry.getListeners() : null;
    }
    return null;
  }
  
  enum Column {
    DAY, DURATION, LISTENERS, UNIQS, AVG_LISTENING_TIME
  }
  
}
