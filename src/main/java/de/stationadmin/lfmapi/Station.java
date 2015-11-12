/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class Station implements Serializable {
  private static final long serialVersionUID = 4728787710893154107L;
  private String name;
  private String description;
  private String djs;
  private String location;
  private String website;
  private String color;
  private Date updatedAt;
  private String[] genres;
  private boolean active;
  private String stationImageURL;
  private String playerImageURL;
  private String backgroundImageURL;
  private Playlist currentPlaylist;
  private int rank;

  public Station(JSONObject json) {
    this.name = JSONUtil.getString(json, "name");
    this.description = JSONUtil.getString(json, "description");
    this.djs = JSONUtil.getString(json, "djs");
    this.location = JSONUtil.getString(json, "location");
    this.website = JSONUtil.getString(json, "website");
    this.color = JSONUtil.getString(json, "color");
    this.updatedAt = JSONUtil.getDate(json, "updated_at", JSONUtil.DEFAULT_DATE_FORMAT);
    try {
      JSONArray genres = json.getJSONArray("genres");
      this.genres = new String[genres.length()];
      for (int i = 0; i < genres.length(); i++) {
        this.genres[i] = genres.getString(i);
      }
    } catch (JSONException e) {
    }
    this.active = JSONUtil.getBoolean(json, "active");
    JSONObject images = JSONUtil.getObject(json, "images");
    if (images != null) {
      this.stationImageURL = JSONUtil.getString(images, "station");
      this.playerImageURL = JSONUtil.getString(images, "player");
      this.backgroundImageURL = JSONUtil.getString(images, "background");
    }
    JSONObject playlist = JSONUtil.getObject(json, "current_playlist");
    if (playlist != null) {
      this.currentPlaylist = new Playlist(playlist);
    }

    this.rank = JSONUtil.getInt(json, "rank", -1);
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the description
   */
  public String getDescription() {
    return description;
  }

  /**
   * @return the djs
   */
  public String getDjs() {
    return djs;
  }

  /**
   * @return the location
   */
  public String getLocation() {
    return location;
  }

  /**
   * @return the website
   */
  public String getWebsite() {
    return website;
  }

  /**
   * @return the color
   */
  public String getColor() {
    return color;
  }

  /**
   * @return the updatedAt
   */
  public Date getUpdatedAt() {
    return updatedAt;
  }

  /**
   * @return the genres
   */
  public String[] getGenres() {
    return genres;
  }

  /**
   * @return the active
   */
  public boolean isActive() {
    return active;
  }

  /**
   * @return the stationImageURL
   */
  public String getStationImageURL() {
    return stationImageURL;
  }

  /**
   * @return the playerImageURL
   */
  public String getPlayerImageURL() {
    return playerImageURL;
  }

  /**
   * @return the backgroundImageURL
   */
  public String getBackgroundImageURL() {
    return backgroundImageURL;
  }

  /**
   * @return the currentPlaylist
   */
  public Playlist getCurrentPlaylist() {
    return currentPlaylist;
  }

  /**
   * @return the rank
   */
  public int getRank() {
    return rank;
  }

  public String toString() {
    return this.name;
  }
}
