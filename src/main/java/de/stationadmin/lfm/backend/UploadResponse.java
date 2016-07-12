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
@XmlRootElement(name = "TrackUpload")
public class UploadResponse {
  private int id;
  private String type;
  @JsonProperty("private")
  private boolean privateTrack;
  @JsonProperty("created_at")
  private Date createdAt;
  @JsonProperty("updated_at")
  private Date updatedAt;
 

  @JsonProperty("original_filename")
  private String originalFilename;
  
  private Track track;

  @JsonProperty("_links")
  private TrackLinks links;

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @param id the id to set
   */
  public void setId(int id) {
    this.id = id;
  }

  /**
   * @return the type
   */
  public String getType() {
    return type;
  }

  /**
   * @param type the type to set
   */
  public void setType(String type) {
    this.type = type;
  }

  /**
   * @return the privateTrack
   */
  public boolean isPrivateTrack() {
    return privateTrack;
  }

  /**
   * @param privateTrack the privateTrack to set
   */
  public void setPrivateTrack(boolean privateTrack) {
    this.privateTrack = privateTrack;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @param createdAt the createdAt to set
   */
  public void setCreatedAt(Date createdAt) {
    this.createdAt = createdAt;
  }

  /**
   * @return the updatedAt
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @param updatedAt the updatedAt to set
   */
  public void setUpdatedAt(Date updatedAt) {
    this.updatedAt = updatedAt;
  }

  /**
   * @return the originalFilename
   */
  public String getOriginalFilename() {
    return originalFilename;
  }

  /**
   * @param originalFilename the originalFilename to set
   */
  public void setOriginalFilename(String originalFilename) {
    this.originalFilename = originalFilename;
  }

  /**
   * @return the links
   */
  public TrackLinks getLinks() {
    return links;
  }

  /**
   * @param links the links to set
   */
  public void setLinks(TrackLinks links) {
    this.links = links;
  }

  /**
   * @return the track
   */
  public Track getTrack() {
    return track;
  }

  /**
   * @param track the track to set
   */
  public void setTrack(Track track) {
    this.track = track;
  }

}
