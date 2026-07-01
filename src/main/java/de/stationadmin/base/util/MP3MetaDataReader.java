/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;

import de.stationadmin.base.track.DetailedTrack;

/**
 * {@link TrackMetaDataReader} implementation for MP3 files.
 * Delegates to the existing {@link MP3Util} helper.
 *
 * @author Frank Korf
 */
public class MP3MetaDataReader implements TrackMetaDataReader {

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#supports(java.io.File)
   */
  @Override
  public boolean supports(File file) {
    return file != null && file.getName().toLowerCase().endsWith(".mp3");
  }

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#readMetaData(java.io.File)
   */
  @Override
  public DetailedTrack readMetaData(File file) {
    return MP3Util.getTitleInformation(file);
  }

}
