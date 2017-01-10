/**
 * 
 */
package de.stationadmin.gui.logs;

import java.sql.Date;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.gui.TextProvider;
import de.stationadmin.lfm.backend.LogEntry;

/**
 * @author korf
 *
 */
public class LogEntryTableModel extends AbstractTableModel {


  private static final long serialVersionUID = -9135094392797079319L;

  private TextProvider textProvider;
  private LogEntry[] entries = new LogEntry[0];

  /**
   * @param textProvider
   */
  public LogEntryTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }


  @Override
  public int getRowCount() {
    return entries.length;
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    LogEntry entry = this.entries[rowIndex];
    Column col = Column.values()[columnIndex];
    switch (col) {
    case CREATED_AT:
      return entry.getCreatedAt();
    case LEVEL:
      return entry.getLevel();
    case MSG:
      return entry.getMessage();
    case TYPE:
      return entry.getType();
    }
    return null;
  }

  public enum Column {
    CREATED_AT, LEVEL, TYPE, MSG
  }

  /**
   * @return the entries
   */
  public LogEntry[] getEntries() {
    return entries;
  }

  /**
   * @param entries
   *          the entries to set
   */
  public void setEntries(LogEntry[] entries) {
    this.entries = entries;
    this.fireTableDataChanged();
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("logviewer.column." + col.name().toLowerCase());
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case CREATED_AT:
      return Date.class;
    default:
      return String.class;
    }
  }

}
