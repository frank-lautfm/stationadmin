/**
 * 
 */
package de.stationadmin.base.track;

import java.util.Date;

import de.stationadmin.lfm.backend.Track;


/**
 * Title with more detailed information as retrieved via search
 * 
 * @author Frank Korf
 */
public class DetailedTrack extends BasicTrack {
  private String album;
  private String genre;
  private int year;
  private boolean privateTrack;
  private Date uploadDate;
  private boolean ownTrack = false;

  public DetailedTrack() {
    super();
  }

  public DetailedTrack(BasicTrack title) {
    super(title);
    if (title instanceof DetailedTrack) {
      DetailedTrack t = (DetailedTrack) title;
      this.album = t.getAlbum();
      this.genre = t.getGenre();
      this.year = t.getYear();
      this.privateTrack = t.isPrivateTrack();
      this.uploadDate = t.getUploadDate();
      this.ownTrack = t.isOwnTrack();
    }
  }

  public DetailedTrack(de.stationadmin.lfm.backend.Track track) {
    this.setId(track.getId());
    this.update(track);
  }
  
  public void update(Track track) {
    super.update(track);
    this.album = track.getAlbum();
    this.genre = track.getGenre();
    this.year = track.getReleaseYear();
    this.privateTrack = track.isPrivateTrack();
    this.uploadDate = track.getCreatedAt(); 
    this.ownTrack = track.isOwn();
  }


  public de.stationadmin.lfm.backend.Track asLfmAPITrack() {
    de.stationadmin.lfm.backend.Track track = new de.stationadmin.lfm.backend.Track();
    track.setId(this.getId());
    track.setAlbum(this.getAlbum());
    track.setArtist(this.getArtist());
    track.setDuration(this.getLength());
    track.setGenre(this.getGenre());
    track.setPrivateTrack(this.isPrivateTrack());
    track.setReleaseYear(this.getYear());
    track.setOwn(this.ownTrack);
    switch (this.getType()) {
    case TYPE_JINGLE:
      track.setType("jingle");
      break;
    case TYPE_MUSIC:
      track.setType("song");
      break;
    case TYPE_WORD:
      track.setType("moderation");
      break;
    }
    track.setTitle(this.getTitle());
    return track;
  }

  /**
   * Gets the album name
   * 
   * @return album
   */
  public String getAlbum() {
    return album;
  }

  /**
   * Gets the genre of the title
   * 
   * @return the genre
   */
  public String getGenre() {
    return genre;
  }

  /**
   * Gets the year
   * 
   * @return year or 0 if not known
   */
  public int getYear() {
    return year;
  }

  /**
   * Checks if the title is marked as private track
   * 
   * @return <code>true</code> if private track
   */
  public boolean isPrivateTrack() {
    return privateTrack;
  }

  /**
   * @param album
   *          the album to set
   */
  public void setAlbum(String album) {
    this.album = album;
  }

  /**
   * @param genre
   *          the genre to set
   */
  public void setGenre(String genre) {
    this.genre = genre;
  }

  /**
   * @param privateTrack
   *          the privateTrack to set
   */
  public void setPrivateTrack(boolean privateTrack) {
    this.privateTrack = privateTrack;
  }

  /**
   * @param year
   *          the year to set
   */
  public void setYear(int year) {
    this.year = year;
  }

  /**
   * @return the uploadDate
   */
  public Date getUploadDate() {
    return uploadDate;
  }

  /**
   * @param uploadDate
   *          the uploadDate to set
   */
  public void setUploadDate(Date uploadDate) {
    this.uploadDate = uploadDate;
  }

  public boolean isOwnTrack() {
    return ownTrack;
  }

  public void setOwnTrack(boolean ownTitle) {
    this.ownTrack = ownTitle;
  }

}
