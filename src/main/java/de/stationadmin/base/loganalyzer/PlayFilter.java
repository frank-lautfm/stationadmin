/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.util.AbstractBean;

/**
 * Tool class for filter {@link Play} entries by various conditions
 * 
 * @author korf
 */
public class PlayFilter extends AbstractBean {
  private static final Weekday[] weekdays = { null, Weekday.SUNDAY, Weekday.MONDAY, Weekday.TUESDAY, Weekday.WEDNESDAY, Weekday.THURSDAY, Weekday.FRIDAY, Weekday.SATURDAY };
  private Date fromTime, toTime;
  private String artist;
  private String title;
  private Playlist playlist;
  private String tag;
  private boolean musicOnly = true;

  private Calendar cal = Calendar.getInstance();

  private Schedule schedule;
  private int[][] scheduleTable;

  private TagManager tagManager;

  public PlayFilter() {
  }

  public PlayFilter(TagManager tagManager) {
    this.tagManager = tagManager;
  }

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
    accept = accept && (!musicOnly || play.getTrack().getType() == BasicTrack.TYPE_MUSIC);
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
    if (accept && tag != null && tagManager != null) {
      try {
        accept = tagManager.isTagged(tag, play.getTrack().getId());
      } catch (IOException e) {
      }
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

  /**
   * @return the musicOnly
   */
  public boolean isMusicOnly() {
    return musicOnly;
  }

  /**
   * @param musicOnly the musicOnly to set
   */
  public void setMusicOnly(boolean musicOnly) {
    boolean old = this.musicOnly;
    this.musicOnly = musicOnly;
    this.firePropertyChange("musicOnly", old, musicOnly);
  }

  public String getTag() {
    return tag;
  }

  public void setTag(String tag) {
    String old = this.tag;
    this.tag = tag;
    this.firePropertyChange("tag", old, tag);
  }

  public TagManager getTagManager() {
    return tagManager;
  }

}
