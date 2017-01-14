/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;
import java.util.TimeZone;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnore;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;
import org.codehaus.jackson.map.annotate.JsonDeserialize;
import org.codehaus.jackson.map.annotate.JsonSerialize;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ScheduledEvent")
public class ScheduledEvent implements Comparable<ScheduledEvent>{
  private int id;
  @JsonProperty("playlist_id")
  private int playlistId;
  @JsonProperty("start_at")
  @JsonSerialize(using = ScheduledEventDateSerializer.class)
  @JsonDeserialize(using = CustomJsonDateDeserializer.class)
  private Date startTimeServer;
  
  @JsonIgnore
  private Date startTime;
  private int duration;

  /**
   * @return the playlistId
   */
  public int getPlaylistId() {
    return playlistId;
  }

  /**
   * @param playlistId
   *          the playlistId to set
   */
  public void setPlaylistId(int playlistId) {
    this.playlistId = playlistId;
  }

  /**
   * @return the duration
   */
  public int getDuration() {
    return duration;
  }

  /**
   * @param duration
   *          the duration to set
   */
  public void setDuration(int duration) {
    this.duration = duration;
  }

  /**
   * @return the startTime
   */
  public Date getStartTime() {
    if(this.startTime == null && this.startTimeServer != null) {
      this.startTime = new Date(this.startTimeServer.getTime() + TimeZone.getDefault().getRawOffset());
    }
    return startTime;
  }

  /**
   * @param startTime
   *          the startTime to set
   */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
    this.startTimeServer = startTime;
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @param id
   *          the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return the startTimeServer
   */
  public Date getStartTimeServer() {
    return startTimeServer;
  }

  public Date getEndTime() {
    return new Date(getStartTime().getTime() + 1000 * 60 * duration);
  }
  
  /**
   * @param startTimeServer the startTimeServer to set
   */
  public void setStartTimeServer(Date startTimeServer) {
    this.startTimeServer = startTimeServer;
  }

  @Override
  public int compareTo(ScheduledEvent o) {
    return Long.compare(this.getStartTime().getTime(), o.getStartTime().getTime());
  }
}
