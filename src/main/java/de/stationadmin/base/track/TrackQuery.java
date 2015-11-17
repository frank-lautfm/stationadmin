/**
 * 
 */
package de.stationadmin.base.track;

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang.StringUtils;

/**
 * @author korf
 * 
 */
public class TrackQuery {
  private String artist;
  private String title;
  private String album;
  private String genre;
  private Boolean ownTracks;
  private Boolean privateTracks;
  private int year;
  private String type;
  private String[] tags;

  private int page = 1;
  private String orderBy = "artist";
  private boolean orderAscending = true;

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

  public String getAlbum() {
    return album;
  }

  public void setAlbum(String album) {
    this.album = album;
  }

  public String getGenre() {
    return genre;
  }

  public void setGenre(String genre) {
    this.genre = genre;
  }

  public Boolean getOwnTracks() {
    return ownTracks;
  }

  public void setOwnTracks(Boolean ownTracks) {
    this.ownTracks = ownTracks;
  }

  public Boolean getPrivateTracks() {
    return privateTracks;
  }

  public void setPrivateTracks(Boolean privateTracks) {
    this.privateTracks = privateTracks;
  }

  public int getYear() {
    return year;
  }

  public void setYear(int year) {
    this.year = year;
  }

  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public int getPage() {
    return page;
  }

  public void setPage(int page) {
    this.page = page;
  }

  public String getOrderBy() {
    return orderBy;
  }

  public void setOrderBy(String orderBy) {
    this.orderBy = orderBy;
  }

  public boolean isOrderAscending() {
    return orderAscending;
  }

  public void setOrderAscending(boolean orderAsc) {
    this.orderAscending = orderAsc;
  }

  public Map<String, String> asFilterMap() {
    Map<String, String> map = new HashMap<String, String>();
    if (StringUtils.isNotEmpty(this.artist)) {
      map.put("artist", this.artist);
    }
    if (StringUtils.isNotEmpty(this.title)) {
      map.put("title", this.title);
    }
    if (StringUtils.isNotEmpty(this.album)) {
      map.put("album", this.album);
    }
    if (StringUtils.isNotEmpty(this.genre)) {
      map.put("genre", this.genre);
    }
    if (StringUtils.isNotEmpty(this.type)) {
      map.put("track_type", this.type);
    }
    if (year > 0) {
      map.put("release_year", Integer.toString(year));
    }
    if (this.ownTracks != null) {
      map.put("own", this.ownTracks.toString());
    }
    if (this.privateTracks != null) {
      map.put("private", this.privateTracks.toString());
    }
    if (this.tags != null && tags.length > 0) {
      String value = tags[0];
      for (int i = 1; i < tags.length; i++) {
        value += "," + tags[i];
      }
      map.put("tags", value);
    }

    return map;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

}
