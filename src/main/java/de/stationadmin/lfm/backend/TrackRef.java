/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.util.Date;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackRef {
  @JsonProperty("track_id")
  private int trackId;
  @JsonProperty("added_by")
  private int addedBy;
  @JsonProperty("added_at")
  private Date addedAt;
  public int getTrackId() {
    return trackId;
  }
  public void setTrackId(int trackId) {
    this.trackId = trackId;
  }
  public int getAddedBy() {
    return addedBy;
  }
  public void setCreatedBy(int createdBy) {
    this.addedBy = createdBy;
  }
  public Date getAddedAt() {
    return addedAt;
  }
  public void setCreatedAt(Date createdAt) {
    this.addedAt = createdAt;
  }

}
