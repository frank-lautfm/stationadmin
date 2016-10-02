/**
 * 
 */
package de.stationadmin.base.track;

import java.io.Serializable;

/**
 * Holds an alias for a {@link RegisteredTrack}
 *
 * @author Frank Korf
 */
public class TrackAlias implements Serializable {
  private static final long serialVersionUID = -5005826116556142359L;
  private String artist;
  private String title;
  
  protected TrackAlias(String artist, String title) {
    super();
    this.artist = artist;
    this.title = title;
  }

  /**
   * @return the artist
   */
  public String getArtist() {
    return artist;
  }

  /**
   * @return the title
   */
  public String getTitle() {
    return title;
  }
  
  public String toString() {
    return this.artist + " - " + this.title;
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    if(obj instanceof TrackAlias) {
      return this.artist.equals(((TrackAlias)obj).getArtist()) && this.title.equals(((TrackAlias)obj).getTitle());
    }
    else {
      return false;
    }
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return this.artist.hashCode() ^ this.title.hashCode();
  }
  
  public boolean matches(String artist, String title) {
    boolean ok = BasicTrack.isArtistEqual(this.artist, artist)
        && BasicTrack.equals(this.title, title);
    return ok;
  }

  /**
   * @param artist the artist to set
   */
  public void setArtist(String artist) {
    this.artist = artist;
  }

  /**
   * @param title the title to set
   */
  public void setTitle(String title) {
    this.title = title;
  }


}
