/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import org.apache.log4j.Logger;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.loganalyzer.ListenersEntry;
import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class PlaysTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -618290644751249178L;
  private static final Weekday[] weekdays = { null, Weekday.SUNDAY, Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY,
      Weekday.FRIDAY, Weekday.SATURDAY };

  private TextProvider textProvider;

  private LogAnalyzerService logAnalyzerService;

  private ValueModel playsHolder;
  private List<Play> plays = new ArrayList<Play>();
  private List<Integer> listeners = null;
  private String[][] scheduleTable;
  private Calendar cal = Calendar.getInstance();

  @SuppressWarnings("unchecked")
  public PlaysTableModel(ClientContext ctx, ValueModel playsHolder) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.plays = (List<Play>) playsHolder.getValue();
    this.playsHolder = playsHolder;
    this.logAnalyzerService = ctx.getAdminClient().getLogAnalyzerService();

    playsHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setPlays((List<Play>) PlaysTableModel.this.playsHolder.getValue());
      }
    });

    this.scheduleTable = new String[7][24];
    for (Weekday weekday : Weekday.values()) {
      for (Entry entry : ctx.getAdminClient().getSchedule().getEntriesOf(weekday)) {
        for (int h = entry.getHour(); h < 24; h++) {
          Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(entry.getPlaylistId());
          if (playlist != null) {
            this.scheduleTable[weekday.ordinal()][h] = playlist.getName();
          } else {
            Logger.getLogger(PlaysTableModel.class).warn("no playlist found with id " + entry.getPlaylistId());
          }
        }
      }
    }

  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case START_TIME:
      return Date.class;
    case LENGTH:
      return Integer.class;
    default:
      return String.class;
    }
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
    return this.textProvider.getString("play.column." + col.name().toLowerCase());
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.plays.size();
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Play play = this.plays.get(rowIndex);
    Column col = Column.values()[columnIndex];
    switch (col) {
    case START_DATE:
      return play.getStartTime();
    case START_TIME:
      return play.getStartTime();
    case SHOW:
      cal.setTime(play.getStartTime());
      Weekday weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK)];
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      return scheduleTable[weekday.ordinal()][hour];
    case ARTIST:
      return play.getTrack().getArtist();
    case TITLE:
      return play.getTrack().getTitle();
    case LENGTH:
      return play.getTrack().getLength();
    case LISTENERS:
      if (this.listeners == null) {
        this.resolveListeners();
      }
      return this.listeners.get(rowIndex);
    }
    return null;
  }

  private void resolveListeners() {
    this.listeners = new ArrayList<Integer>();
    Calendar cal = Calendar.getInstance();

    List<ListenersEntry> listenersOfDay = null;
    int loadedDay = -1;
    int lastIdx = 0;

    try {
      for (Play play : this.plays) {
        Date date = play.getStartTime();
        cal.setTime(date);
        if (cal.get(Calendar.DAY_OF_YEAR) != loadedDay) {
          listenersOfDay = this.logAnalyzerService.getListenersOf(date);
          loadedDay = cal.get(Calendar.DAY_OF_YEAR);
          lastIdx = 0;
        }
        if (lastIdx >= listenersOfDay.size() || listenersOfDay.get(lastIdx).getTime().getTime() > play.getStartTime().getTime()) {
          lastIdx = 0;
        }
        int listenerNum = 0;
        if (lastIdx < listenersOfDay.size()) {
          listenerNum = listenersOfDay.get(lastIdx).getListeners();
          while (lastIdx < listenersOfDay.size() - 1 && listenersOfDay.get(lastIdx + 1).getTime().getTime() < play.getStartTime().getTime()) {
            lastIdx++;
            listenerNum = listenersOfDay.get(lastIdx).getListeners();
          }
        }
        this.listeners.add(listenerNum);

      }
    } catch (IOException e) {

    }
  }

  public List<Play> getPlays() {
    return plays;
  }

  public void setPlays(List<Play> plays) {
    this.plays = plays;
    this.listeners = null;
    this.fireTableDataChanged();
  }

  public enum Column {
    START_DATE, START_TIME, SHOW, ARTIST, TITLE, LENGTH, LISTENERS
  }

}
