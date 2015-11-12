/**
 * 
 */
package de.stationadmin.base.schedule;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.base.schedule.Schedule.Weekday;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.Task;
import de.stationadmin.base.tasks.TaskExecutionResult;

/**
 * Exchanges a playlist in the schedule
 * 
 * @author korf
 */
public class PlaylistExchangeTask extends AbstractTask {
  private String playlistName;
  private Weekday weekday;
  private int hour;

  @Override
  public TaskExecutionResult execute(StationAdminClient client) {
    TaskExecutionResult result = new TaskExecutionResult();
    if (this.playlistName == null || this.weekday == null) {
      result.setSucceeded(false);
      result.addMessage(true, "task.configurationerror");
      return result;
    }

    // search playlist
    Playlist playlist = null;
    for (Playlist p : client.getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      if (p.getName().equalsIgnoreCase(this.playlistName)) {
        playlist = p;
      }
    }
    if (playlist == null) {
      result.setSucceeded(false);
      result.addMessage(true, "task.schedule.playlist.not_found", this.playlistName);
      return result;
    }

    Schedule schedule = client.getSchedule();

    Entry previousEntry = null;
    for (Entry entry : schedule.getEntriesOf(this.weekday)) {
      if (entry.getHour() == this.hour) {
        previousEntry = entry;
      }
    }
    if (previousEntry != null) {
      if (previousEntry.getPlaylistId() == playlist.getId()) {
        result.addMessage(false, "task.schedule.playlist.already_assigned", this.playlistName);
        return result;
      }
      schedule.removeEntry(previousEntry);
    }

    schedule.addEntry(new Entry(playlist.getId(), weekday, hour));
    try {
      schedule.submitToServer();
      schedule.save();
      result.addMessage(false, "task.schedule.playlist.exchanged", this.playlistName);
    } catch (Exception e) {
      result.setSucceeded(false);
      result.addMessage(true, "task.schedule.playlist.failed", playlist.getName(), e.getMessage() != null ? e.getMessage() : e.getLocalizedMessage());
    }

    return result;
  }

  public String getPlaylistName() {
    return playlistName;
  }

  public void setPlaylistName(String playlistName) {
    this.playlistName = playlistName;
  }

  public Weekday getWeekday() {
    return weekday;
  }

  public void setWeekday(Weekday weekday) {
    this.weekday = weekday;
  }

  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

}
