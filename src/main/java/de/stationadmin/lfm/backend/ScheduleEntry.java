/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
public class ScheduleEntry {

  @JsonProperty("playlist_id")
  private int playlistId;
  private int slot;
  private int duration;

  @JsonProperty("added_at")
  private Date addedAt;
  @JsonProperty("added_by")
  private int addedBy;
  
  public ScheduleEntry() {
    
  }

  public ScheduleEntry(int playlistId, int slot, int duration) {
    super();
    this.playlistId = playlistId;
    this.slot = slot;
    this.duration = duration;
  }

  public int getPlaylistId() {
    return playlistId;
  }

  public void setPlaylistId(int playlistId) {
    this.playlistId = playlistId;
  }

  public int getSlot() {
    return slot;
  }

  public void setSlot(int slot) {
    this.slot = slot;
  }

  public int getDuration() {
    return duration;
  }

  public void setDuration(int duration) {
    this.duration = duration;
  }

  public Date getAddedAt() {
    return addedAt;
  }

  public void setAddedAt(Date addedAt) {
    this.addedAt = addedAt;
  }

  public int getAddedBy() {
    return addedBy;
  }

  public void setAddedBy(int addedBy) {
    this.addedBy = addedBy;
  }

}
