/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Schedule")
public class Schedule {
  private int id;
  @JsonProperty("station_id")
  private int stationId;
  @JsonProperty("base_playlist_id")
  private int basePlaylistId;

  @JsonProperty("created_at")
  private Date createdAt;
  @JsonProperty("updated_at")
  private Date updatedAt;

  @JsonProperty("updated_by")
  private int updatedBy;
  
  private ScheduleEntry[] entries;

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getStationId() {
    return stationId;
  }

  public void setStationId(int stationId) {
    this.stationId = stationId;
  }

  public int getBasePlaylistId() {
    return basePlaylistId;
  }

  public void setBasePlaylistId(int basePlaylistId) {
    this.basePlaylistId = basePlaylistId;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  public int getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdatedBy(int updatedBy) {
    this.updatedBy = updatedBy;
  }

  public ScheduleEntry[] getEntries() {
    return entries;
  }

  public void setEntries(ScheduleEntry[] entries) {
    this.entries = entries;
  }

}
