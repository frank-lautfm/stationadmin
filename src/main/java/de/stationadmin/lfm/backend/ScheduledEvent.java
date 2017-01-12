/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import javax.xml.bind.annotation.XmlRootElement;

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
public class ScheduledEvent {
  private int id;
  @JsonProperty("playlist_id")
  private int playlistId;
  @JsonProperty("start_at")
  @JsonSerialize(using = ScheduledEventDateSerializer.class)
  @JsonDeserialize(using = CustomJsonDateDeserializer.class)
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
    return startTime;
  }

  /**
   * @param startTime
   *          the startTime to set
   */
  public void setStartTime(Date startTime) {
    this.startTime = startTime;
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
}
