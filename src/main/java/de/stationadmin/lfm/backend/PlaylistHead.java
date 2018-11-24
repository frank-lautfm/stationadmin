/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "PlaylistHead")
public class PlaylistHead {

  private int id;
  @JsonProperty("user_id")
  private int userId;
  @JsonProperty("updated_by")
  private int updatedBy;
  @JsonProperty("station_id")
  private int stationId;
  private String title;
  private String description;
  private String color = "#FFFFFF";
  private boolean shuffled = false;
  @JsonProperty("created_at")
  private Date createdAt;
  @JsonProperty("updated_at")
  private Date updatedAt;
  @JsonProperty("shuffle_opts")
  private Map<String,Object> shuffleOpts;
  
  
  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }

  public int getUserId() {
    return userId;
  }

  public void setUser_id(int user_id) {
    this.userId = user_id;
  }

  public int getUpdatedBy() {
    return updatedBy;
  }

  public void setUpdated_by(int updated_by) {
    this.updatedBy = updated_by;
  }

  public int getStationId() {
    return stationId;
  }

  public void setStation_id(int station_id) {
    this.stationId = station_id;
  }

  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getColor() {
    return color;
  }

  public void setColor(String color) {
    this.color = color;
  }

  public boolean isShuffled() {
    return shuffled;
  }

  public void setShuffled(boolean shuffled) {
    this.shuffled = shuffled;
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdated_at(Date updated_at) {
    this.updatedAt = updated_at;
  }

  public String toString() {
    return this.title;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  public Map<String, Object> getShuffleOpts() {
    return shuffleOpts;
  }

  public void setShuffleOpts(Map<String, Object> shuffleOpts) {
    this.shuffleOpts = shuffleOpts;
  }

}
