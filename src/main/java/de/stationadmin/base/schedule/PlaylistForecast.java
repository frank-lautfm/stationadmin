/**
 * 
 */
package de.stationadmin.base.schedule;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.playlist.validation.GVLValidator;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;

/**
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistForecast {
  private PlaylistRegistry playlistRegistry;
  private Schedule schedule;

  public PlaylistForecast(PlaylistRegistry playlistRegistry, Schedule schedule) {
    super();
    this.playlistRegistry = playlistRegistry;
    this.schedule = schedule;
  }

  public List<ScheduledTrack> generateForecast(Date start, int hours, int delay) {
    Weekday weekday = Weekday.getWeekday(start);
    Calendar cal = Calendar.getInstance();
    cal.setTime(start);
    int hour = cal.get(Calendar.HOUR_OF_DAY);

    cal.add(Calendar.HOUR_OF_DAY, hours);
    Date endTime = cal.getTime();

    List<Schedule.Entry> entries = schedule.getEntries();
    Collections.sort(entries);
    if (entries.size() < 2) {
      return null; // forecast impossible
    }

    int numEntries = entries.size();

    // find the entry of the last show before the requested time
    int idx = 0;
    while (idx < entries.size()) {
      Schedule.Entry entry = entries.get(idx);
      boolean dayBefore = entry.getWeekday().ordinal() < weekday.ordinal();
      boolean todayBeforeStart = entry.getWeekday() == weekday && entry.getHour() < hour;
      if (dayBefore || todayBeforeStart) {
        idx++;
      } else {
        break;
      }
    }

    // if entry.getHour is hour the idx is correct - otherwise correct it if we
    // jumped too far
    Schedule.Entry next = entries.get(idx % numEntries);
    if ((next.getWeekday() == weekday && next.getHour() > hour) || next.getWeekday() != weekday) {
      idx = idx > 0 ? idx - 1 : numEntries - 1;
    }
    // idx is now the index of the current playlist

    // check overlap from previous day
    if (idx > 0 && entries.get(idx - 1).getPlaylistId() == entries.get(idx).getPlaylistId()) {
      idx--;
    } else if (idx == 0 && entries.get(entries.size() - 1).getPlaylistId() == entries.get(idx).getPlaylistId()) {
      idx = entries.size() - 1;
    }

    Schedule.Entry currentEntry = entries.get(idx);

    // get the real start time for forecast
    cal.setTime(start);
    cal.set(Calendar.MINUTE, delay);
    cal.set(Calendar.SECOND, 0);
    if (!(currentEntry.getWeekday() == weekday && currentEntry.getHour() == hour)) {
      // decrease hour as long as we reached a time that matches the start entry
      // - theoretically this can span over multiple days
      int h = hour;
      Weekday w = weekday;
      while (!(currentEntry.getWeekday() == w && currentEntry.getHour() == h)) {
        cal.add(Calendar.HOUR_OF_DAY, -1);
        h--;
        if (h == -1) {
          h = 23;
          int previousDay = w.ordinal() - 1;
          w = previousDay >= 0 ? Weekday.values()[previousDay] : Weekday.SUNDAY;
        }
      }
    }

    ArrayList<ScheduledTrack> titles = new ArrayList<ScheduledTrack>();

    Context ctx = new Context();
    ctx.setScheduleEntry(currentEntry);

    int nextIdx = (idx + 1) % entries.size();

    while (cal.getTimeInMillis() < endTime.getTime()) {
      ScheduledTrack t = new ScheduledTrack(cal.getTime(), ctx.playlist, ctx.getNextTitle());
      if (cal.getTimeInMillis() / 60000 >= start.getTime() / 60000) {
        titles.add(t);
      }
      cal.add(Calendar.SECOND, t.getTitle().getLength());

      int h = cal.get(Calendar.HOUR_OF_DAY);
      int d = cal.get(Calendar.DAY_OF_WEEK);
      if (h == entries.get(nextIdx).getHour() && d == entries.get(nextIdx).getWeekday().calDay) {
        // only switch to next playlist if it differs from current - that may
        // not be the case when he just have a switch of the weekday (e. g. show
        // that spans from 23 to 1)
        if (ctx.playlist.getId() != entries.get(nextIdx).getPlaylistId()) {
          ctx.setScheduleEntry(entries.get(nextIdx));
        }
        idx = nextIdx;
        nextIdx = (idx + 1) % entries.size();
      }
    }

    return titles;
  }

  public void checkGVLRules(List<ScheduledTrack> titles, List<ScheduledTrack> violoations) {
    ArrayList<BasicTrack> titleList = new ArrayList<BasicTrack>();

    for (ScheduledTrack t : titles) {
      titleList.add(t.getTitle());
    }

    Playlist playlist = new Playlist(new TrackRegistry(), PlaylistType.TEMPORARY);
    playlist.setTracks(titleList);

    GVLValidator validator = new GVLValidator();
    List<Playlist.Entry> playlistViolations = new ArrayList<Entry>();
    if (!validator.validate(playlist, playlistViolations)) {
      List<Playlist.Entry> entries = playlist.getEntries();
      for (Playlist.Entry entry : playlistViolations) {
        int idx = entries.indexOf(entry);
        if (idx > -1) {
          violoations.add(titles.get(idx));
        }
      }
    }
  }

  private class Context {
    Playlist playlist;
    List<Playlist.Entry> playlistEntries;
    int plIdx;

    void setScheduleEntry(Schedule.Entry scheduleEntry) {
      this.playlist = playlistRegistry.getPlaylist(scheduleEntry.getPlaylistId());
      this.playlistEntries = this.playlist.getEntries();
      if (this.playlist.isShuffle() || this.playlist.getId() == 0) {
        this.playlistEntries = new PlaylistShuffler().randomize(this.playlistEntries);
      }
      this.plIdx = 0;

    }

    BasicTrack getNextTitle() {
      Playlist.Entry plEntry = this.playlistEntries.get(this.plIdx);
      BasicTrack title = this.playlist.getTrackRegistry().getTrack(plEntry.getTrackId());
      this.plIdx++;
      if (this.plIdx == this.playlistEntries.size()) {
        this.plIdx = 0;
      }
      return title;
    }
  }

  public static class ScheduledTrack {
    private Date time;
    private Playlist playlist;
    private BasicTrack title;

    public ScheduledTrack(Date time, Playlist playlist, BasicTrack title) {
      super();
      this.time = time;
      this.playlist = playlist;
      this.title = title;
    }

    /**
     * @return the time
     */
    public Date getTime() {
      return time;
    }

    /**
     * @return the playlist
     */
    public Playlist getPlaylist() {
      return playlist;
    }

    /**
     * @return the title
     */
    public BasicTrack getTitle() {
      return title;
    }

    /**
     * @see java.lang.Object#toString()
     */
    @Override
    public String toString() {
      return this.time + " " + this.title + " (" + this.playlist.getDisplayName() + ")";
    }

  }

}
