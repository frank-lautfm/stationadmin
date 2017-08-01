/**
 * 
 */
package de.stationadmin.base.schedule;

import org.apache.log4j.Logger;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.TaskExecutionMessage;
import de.stationadmin.base.tasks.TaskExecutionResult;

/**
 * @author korf
 *
 */
public class ScheduleShuffleTask extends AbstractTask {
  private static final Logger log = Logger.getLogger(ScheduleShuffleTask.class);
  private String playlistTag = ScheduleShuffler.TAG_USED;
  private boolean slotLenghForPlaylistsRequired;

  public ScheduleShuffleTask() {
  }

  @Override
  public TaskExecutionResult execute(StationAdminClient client) {

    TaskExecutionResult result = new TaskExecutionResult();

    try {
      ScheduleShuffler shuffler = new ScheduleShuffler(client.getPlaylistService().getPlaylistRegistry(), client.getSchedule().getBasePlaylist().getId());
      shuffler.setPlaylistTag(this.playlistTag);
      shuffler.setSlotLenghForPlaylistsRequired(this.slotLenghForPlaylistsRequired);
      client.getSchedule().setEntries(shuffler.shuffle(client.getSchedule().getEntries()));
      client.getSchedule().submitToServer();
      client.getSchedule().save();
      result.setSucceeded(true);
      result.addMessage(new TaskExecutionMessage(false, "task.schedule.shuffle.success"));
    } catch (Exception e) {
      log.error("shuffling of schedule failed", e);
      result.setSucceeded(false);
      result.addMessage(new TaskExecutionMessage(false, "task.schedule.shuffle.failed", e.getMessage() != null ? e.getMessage() : e.toString()));
    }

    return result;
  }

  /**
   * @return the playlistTag
   */
  public String getPlaylistTag() {
    return playlistTag;
  }

  /**
   * @param playlistTag
   *          the playlistTag to set
   */
  public void setPlaylistTag(String playlistTag) {
    this.playlistTag = playlistTag;
  }

  /**
   * @return the slotLenghForPlaylistsRequired
   */
  public boolean isSlotLenghForPlaylistsRequired() {
    return slotLenghForPlaylistsRequired;
  }

  /**
   * @param slotLenghForPlaylistsRequired
   *          the slotLenghForPlaylistsRequired to set
   */
  public void setSlotLenghForPlaylistsRequired(boolean slotLenghForPlaylistsRequired) {
    this.slotLenghForPlaylistsRequired = slotLenghForPlaylistsRequired;
  }

}
