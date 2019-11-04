/**
 * 
 */
package de.stationadmin.gui.util;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.table.AbstractTableModel;

/**
 * @author korf
 *
 */
public class StringListTableModel extends AbstractTableModel {

  private static final long serialVersionUID = -362674090295560465L;
  private String heading;
  private List<String> list;
  private List<String> entries = new ArrayList<String>();

  public StringListTableModel(String heading) {
    this(heading, new ArrayList<>());
  }

  /**
   * @param heading
   * @param list
   */
  public StringListTableModel(String heading, List<String> list) {
    super();
    this.heading = heading;
    this.list = list;
    updateEntries();
  }

  private void updateEntries() {
    this.entries.clear();
    for (String str : this.list) {
      this.entries.add(str);
    }
    Collections.sort(this.entries);
    this.entries.add("");

  }

  @Override
  public int getRowCount() {
    return this.entries.size();
  }

  @Override
  public int getColumnCount() {
    return 1;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    return this.entries.get(rowIndex);
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    return this.heading;
  }

  @Override
  public boolean isCellEditable(int rowIndex, int columnIndex) {
    return true;
  }

  @Override
  public void setValueAt(Object aValue, int rowIndex, int columnIndex) {
    this.entries.set(rowIndex, (String) aValue);
    this.list.clear();
    for (String str : this.entries) {
      if (str.trim().length() > 0) {
        this.list.add(str);
      }
    }
    if (entries.size() > 0 && entries.get(entries.size() - 1).trim().length() > 0) {
      this.entries.add("");
      this.fireTableRowsInserted(this.entries.size() - 1, this.entries.size() - 1);
    }
  }

  /**
   * @return the list
   */
  public List<String> getList() {
    return list;
  }

  public void setList(List<String> list) {
    this.list = list != null ? list : new ArrayList<>();
    this.updateEntries();
    this.fireTableDataChanged();
  }

}
