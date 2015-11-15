/**
 * 
 */
package de.stationadmin.gui.playlist.forecast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.schedule.PlaylistForecast;
import de.stationadmin.base.schedule.PlaylistForecast.ScheduledTrack;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ForecastTableModel extends AbstractTableModel {
  public enum Column {
    INDEX, TIME, ARTIST, TITLE, LENGTH, PLAYLIST,

  }

  private static final long serialVersionUID = 8500250654473106105L;
  private ClientContext ctx;
  private List<PlaylistForecast.ScheduledTrack> tracks = new ArrayList<ScheduledTrack>();

  private BitSet gvlValidationErrors = new BitSet();

  public ForecastTableModel(ClientContext ctx) {
    super();
    this.ctx = ctx;
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return ctx.getString("forecasttable.column." + col.name().toLowerCase());
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.tracks.size();
  }

  /**
   * @return the titles
   */
  public List<PlaylistForecast.ScheduledTrack> getTracks() {
    return tracks;
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    PlaylistForecast.ScheduledTrack schedTitle = this.tracks.get(rowIndex);
    Column col = Column.values()[columnIndex];
    SimpleDateFormat fmt = new SimpleDateFormat("HH:mm");
    switch (col) {
    case INDEX:
      return (rowIndex + 1);
    case TIME:
      return fmt.format(schedTitle.getTime());
    case ARTIST:
      return schedTitle.getTitle().getArtist();
    case TITLE:
      return schedTitle.getTitle().getTitle();
    case LENGTH:
      return TimeFormat.format(schedTitle.getTitle().getLength(), false);
    case PLAYLIST:
      return schedTitle.getPlaylist().getDisplayName();
    }

    return null;
  }

  public boolean isGVLValidationError(int row) {
    return this.gvlValidationErrors.get(row);
  }
  
  /**
   * @param violations the violations to set
   */
  public void setGVLViolations(List<PlaylistForecast.ScheduledTrack> violations) {
    this.gvlValidationErrors.clear();
    for(ScheduledTrack violation : violations) {
      int idx = this.tracks.indexOf(violation);
      if(idx > 0) {
        this.gvlValidationErrors.set(idx);
      }
    }
  }

  /**
   * @param titles
   *          the titles to set
   */
  public void setTracks(List<PlaylistForecast.ScheduledTrack> titles) {
    this.tracks = titles;
    this.gvlValidationErrors.clear();
    this.fireTableDataChanged();
  }

}
