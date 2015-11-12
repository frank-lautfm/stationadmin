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
@XmlRootElement(name = "Track")
public class Track {
  private int id;
  private String artist;
  private String title;
  private String type;
  @JsonProperty("private")
  private boolean privateTrack;
  private String genre;
  private int duration;
  @JsonProperty("release_year")
  private int releaseYear;
  @JsonProperty("created_at")
  private Date createdAt;
  @JsonProperty("updated_at")
  private Date updatedAt;
  private boolean own;
  private String[] tags;
  
  private String album;
  
  @JsonProperty("_links")
  private TrackLinks links;
  
  public int getId() {
    return id;
  }
  public void setId(int id) {
    this.id = id;
  }
  public String getArtist() {
    return artist;
  }
  public void setArtist(String artist) {
    this.artist = artist;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String title) {
    this.title = title;
  }
  public String getType() {
    return type;
  }
  public void setType(String type) {
    this.type = type;
  }
  public boolean isPrivateTrack() {
    return privateTrack;
  }
  public void setPrivateTrack(boolean privateTrack) {
    this.privateTrack = privateTrack;
  }
  public String getGenre() {
    return genre;
  }
  public void setGenre(String genre) {
    this.genre = genre;
  }
  public int getDuration() {
    return duration;
  }
  public void setDuration(int duration) {
    this.duration = duration;
  }
  public int getReleaseYear() {
    return releaseYear;
  }
  public void setReleaseYear(int releaseYear) {
    this.releaseYear = releaseYear;
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

  public String toString() {
    return this.id + " - " + this.artist + " - " + this.title;
  }
  public String getAlbum() {
    return album;
  }
  public void setAlbum(String album) {
    this.album = album;
  }
  public boolean isOwn() {
    return own;
  }
  public void setOwn(boolean own) {
    this.own = own;
  }
  public String[] getTags() {
    return tags;
  }
  public void setTags(String[] tags) {
    this.tags = tags;
  }
  /**
   * @return the self
   */
  public TrackLinks getLinks() {
    return links;
  }
  /**
   * @param self the self to set
   */
  public void setLinks(TrackLinks self) {
    this.links = self;
  }
}
