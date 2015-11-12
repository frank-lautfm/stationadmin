/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.loganalyzer.ItemFrequency;
import de.stationadmin.base.loganalyzer.PlayStatistics;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 *
 */
public class FrequentArtistTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 6709327008918771272L;
  private TextProvider textProvider;
  private List<ItemFrequency<String>> items;

  public FrequentArtistTableModel(TextProvider textProvider, PlayStatistics stats) {
    super();
    this.textProvider = textProvider;
    this.items = stats.getFrequentArtists();
    stats.addPropertyChangeListener("frequentArtists", new PropertyChangeListener() {
      
      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        items = (List<ItemFrequency<String>>)evt.getNewValue();
        fireTableDataChanged();
        
      }
    });
  }

  @Override
  public int getRowCount() {
    return this.items != null ? this.items.size() : 0;
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ItemFrequency<String> item = this.items.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return item.getFrequency();
    case 1:
      return item.getItem();

    }
    return null;
  }

  @Override
  public String getColumnName(int column) {
    return this.textProvider.getString(column == 0 ? "frequency.column.frequency" : "frequency.column.artist");
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if(columnIndex == 0) {
      return Integer.class;
    }
    else {
      return String.class;
    }
  }

}
