/**
 * 
 */
package de.stationadmin.base.track.format;

import de.stationadmin.base.track.BasicTrack;

/**
 * Simple format "artist - title"
 * 
 * @author Frank Korf
 */
public class ArtistTracknameFormat implements TrackExportFormat {

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#fromString(java.lang.String)
   */
  @Override
  public BasicTrack fromString(String str) {
    int separatorLength = 3;
    // first try to find separator with blanks
    int separator = str.indexOf(" - ");
    if(separator < 0) {
      // on failure, try to find separator without blanks
      separator = str.indexOf("-");
      separatorLength = 1;
    }
    if(separator > 0 && separator < str.length() - 1) {
      String artist = str.substring(0, separator).trim();
      String title = str.substring(separator + separatorLength).trim();
      BasicTrack t = new BasicTrack();
      t.setArtist(artist);
      t.setTitle(title);
      return t;
    }
    return null;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#supports(java.lang.String)
   */
  @Override
  public boolean supports(String str) {
    int pos = str.indexOf('-');
    return pos > 0 && pos < str.length() - 1;
  }

  /**
   * @see de.stationadmin.base.track.format.TrackExportFormat#toString(de.stationadmin.base.track.BasicTrack)
   */
  @Override
  public String toString(BasicTrack title) {
    return title.getArtist() + " - " + title.getTitle();
  }

}
