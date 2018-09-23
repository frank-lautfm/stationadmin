/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class Song implements Serializable {
  private static final long serialVersionUID = -5618777926044518313L;

  private int id;
  private Artist artist;
  private String title;
  private String album;
  private String genre;
  private int releaseYear;
  private int length;
  private Date createdAt;
  private String type;

  private Date startedAt;
  private Date endsAt;

  public Song(JSONObject json) {
    this.id = JSONUtil.getInt(json, "id", 0);
    JSONObject artist = JSONUtil.getObject(json, "artist");
    if (artist != null) {
      this.artist = new Artist(artist);
    }
    this.title = JSONUtil.getString(json, "title");
    this.album = JSONUtil.getString(json, "album");
    this.genre = JSONUtil.getString(json, "genre");
    this.releaseYear = JSONUtil.getInt(json, "releaseyear", 0);
    this.length = JSONUtil.getInt(json, "length", 0);
    this.createdAt = JSONUtil.getDate(json, "created_at", JSONUtil.DEFAULT_DATE_FORMAT);
    this.startedAt = JSONUtil.getDate(json, "started_at", JSONUtil.DEFAULT_DATE_FORMAT);
    this.endsAt = JSONUtil.getDate(json, "ends_at", JSONUtil.DEFAULT_DATE_FORMAT);
    this.type = JSONUtil.getString(json, "type");
  }

  /**
   * @return the id
   */
  public int getId() {
    return id;
  }

  /**
   * @return the artist
   */
  public Artist getArtist() {
    return artist;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }

  /**
   * @return the album
   */
  public String getAlbum() {
    return album;
  }

  /**
   * @return the genre
   */
  public String getGenre() {
    return genre;
  }

  /**
   * @return the releaseYear
   */
  public int getReleaseYear() {
    return releaseYear;
  }

  /**
   * @return the length
   */
  public int getLength() {
    return length;
  }

  /**
   * @return the createdAt
   */
  public Date getCreatedAt() {
    return createdAt;
  }

  /**
   * @return the startedAt
   */
  public Date getStartedAt() {
    return startedAt;
  }

  /**
   * @return the endsAt
   */
  public Date getEndsAt() {
    return endsAt;
  }
  
  public String toString() {
    return this.artist.getName() + " - " + this.title;
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
  
  public boolean equals(Object obj) {
    if(obj instanceof Song) {
      Song song = (Song)obj;
      if(song.getId() != 0) {
        return this.id == song.getId();
      }
      else {
        return this.toString().equals(song.toString());
      }
      
    }
    return false;
  }

}
