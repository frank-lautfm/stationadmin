/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule.Event;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 *
 */
public class ScheduleEventTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -7294010456651313311L;
  private TextProvider textProvider;
  private PlaylistRegistry playlistRegistry;
  private List<Event> events = new ArrayList<Event>();

  /**
   * @param textProvider
   */
  public ScheduleEventTableModel(PlaylistRegistry playlistRegistry, TextProvider textProvider) {
    super();
    this.playlistRegistry = playlistRegistry;
    this.textProvider = textProvider;
  }

  @Override
  public int getRowCount() {
    return this.events.size();
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    Event evt = this.events.get(rowIndex);

    switch (col) {
    case STARTTIME:
    case STARTDATE:
      return evt.getStartTime();
    case PLAYLIST:
      Playlist playlist = this.playlistRegistry.getPlaylist(evt.getPlaylistId());
      return playlist != null ? playlist.getName() : "<Playlist " + evt.getPlaylistId() + ">";
    case DURATION:
      return evt.getDuration();
    }

    // TODO Auto-generated method stub
    return null;
  }

  /**
   * @return the events
   */
  public List<Event> getEvents() {
    return events;
  }

  /**
   * @param events
   *          the events to set
   */
  public void setEvents(List<Event> events) {
    this.events = events;
    this.fireTableDataChanged();
  }

  public enum Column {
    STARTDATE, STARTTIME, PLAYLIST, DURATION
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    return textProvider.getString("scheduleeditor.event." + col.name().toLowerCase());
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case STARTTIME:
    case STARTDATE:
      return Date.class;
    case PLAYLIST:
      return String.class;
    case DURATION:
      return Integer.class;
    }
    return super.getColumnClass(columnIndex);
  }

}
