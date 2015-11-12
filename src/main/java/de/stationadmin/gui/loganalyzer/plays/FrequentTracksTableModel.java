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
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class FrequentTracksTableModel extends AbstractTableModel {

  private static final long serialVersionUID = 6709327008918771272L;
  private TextProvider textProvider;
  private List<ItemFrequency<DetailedTrack>> items;

  public FrequentTracksTableModel(TextProvider textProvider, PlayStatistics stats) {
    super();
    this.textProvider = textProvider;
    this.items = stats.getFrequentTracks();
    stats.addPropertyChangeListener("frequentTracks", new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        items = (List<ItemFrequency<DetailedTrack>>) evt.getNewValue();
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
    return 3;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    ItemFrequency<DetailedTrack> item = this.items.get(rowIndex);
    switch (columnIndex) {
    case 0:
      return item.getFrequency();
    case 1:
      return item.getItem().getArtist();
    case 2:
      return item.getItem().getTitle();

    }
    return null;
  }

  @Override
  public String getColumnName(int column) {
    switch (column) {
    case 0:
      return this.textProvider.getString("frequency.column.frequency");
    case 1:
      return this.textProvider.getString("frequency.column.artist");
    case 2:
      return this.textProvider.getString("frequency.column.title");
    }
    return null;

  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    if (columnIndex == 0) {
      return Integer.class;
    } else {
      return String.class;
    }
  }

}
