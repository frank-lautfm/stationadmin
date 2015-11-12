/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.TextProvider;

/**
 * Table model for unplayed titles
 * 
 * @author korf
 */
public class TrackTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 8659991094229248340L;

  enum Column {
    ARTIST, TITLE, LENGTH
  }

  private TextProvider textProvider;
  private List<RegisteredTrack> tracks = new ArrayList<RegisteredTrack>();

  public TrackTableModel(List<RegisteredTrack> titles) {
    super();
    this.tracks = titles;
  }

  public TrackTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return this.textProvider.getString("titletable.column." + col.name().toLowerCase());
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return tracks.size();
  }

  public List<RegisteredTrack> getTracks() {
    return tracks;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    RegisteredTrack title = this.tracks.get(rowIndex);
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ARTIST:
      return title.getArtist();
    case TITLE:
      return title.getTitle();
    case LENGTH:
      return TimeFormat.format(title.getLength(), false);

    }
    return null;
  }

  public void setTracks(List<RegisteredTrack> titles) {
    this.tracks = titles;
    this.fireTableDataChanged();
  }

}
