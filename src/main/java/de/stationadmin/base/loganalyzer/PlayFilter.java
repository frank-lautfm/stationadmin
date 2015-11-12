/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.base.util.AbstractBean;

/**
 * Tool class for filter {@link Play} entries by various conditions
 * 
 * @author korf
 */
public class PlayFilter extends AbstractBean {
  private static final Weekday[] weekdays = { null, Weekday.SUNDAY, Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY,
      Weekday.FRIDAY, Weekday.SATURDAY };
  private Date fromTime, toTime;
  private String artist;
  private String title;
  private Playlist playlist;

  private Calendar cal = Calendar.getInstance();

  private Schedule schedule;
  private int[][] scheduleTable;

  public Date getFromTime() {
    return fromTime;
  }

  public void setFromTime(Date fromTime) {
    Date old = this.fromTime;
    this.fromTime = fromTime;
    this.firePropertyChange("fromTime", old, fromTime);
  }

  public Date getToTime() {
    return toTime;
  }

  public void setToTime(Date toTime) {
    Date old = this.toTime;
    this.toTime = toTime;
    this.firePropertyChange("toTime", old, toTime);
  }

  public String getArtist() {
    return artist;
  }

  public void setArtist(String artist) {
    String old = this.artist;
    this.artist = artist;
    this.firePropertyChange("artist", old, artist);
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    String old = this.title;
    this.title = title;
    this.firePropertyChange("title", old, title);
  }

  public Playlist getPlaylist() {
    return playlist;
  }

  public void setPlaylist(Playlist playlist) {
    Playlist old = this.playlist;
    this.playlist = playlist;
    this.firePropertyChange("playlist", old, playlist);
  }

  public Schedule getSchedule() {
    return schedule;
  }

  public void setSchedule(Schedule schedule) {
    this.schedule = schedule;
    this.scheduleTable = new int[7][24];
    for (Weekday weekday : Weekday.values()) {
      for (Entry entry : this.schedule.getEntriesOf(weekday)) {
        for (int h = entry.getHour(); h < 24; h++) {
          this.scheduleTable[weekday.ordinal()][h] = entry.getPlaylistId();
        }
      }
    }
  }

  public boolean accept(Play play) {
    boolean accept = true;
    accept = accept && (this.fromTime == null || play.getStartTime().getTime() >= this.fromTime.getTime());
    accept = accept && (this.toTime == null || play.getStartTime().getTime() < this.toTime.getTime());
    accept = accept && (this.artist == null || play.getTrack().getArtist().toLowerCase().contains(this.artist.toLowerCase()));
    accept = accept && (this.title == null || play.getTrack().getTitle().toLowerCase().contains(this.title.toLowerCase()));
    if (accept && this.playlist != null && this.schedule != null) {
      cal.setTime(play.getStartTime());
      Weekday weekday = weekdays[cal.get(Calendar.DAY_OF_WEEK)];
      int hour = cal.get(Calendar.HOUR_OF_DAY);
      accept = playlist.getId() == scheduleTable[weekday.ordinal()][hour];
    }

    return accept;
  }

  public List<Play> apply(List<Play> plays) {
    List<Play> filtered = new ArrayList<Play>();
    for (Play play : plays) {
      if (accept(play)) {
        filtered.add(play);
      }
    }
    return filtered;
  }

}
