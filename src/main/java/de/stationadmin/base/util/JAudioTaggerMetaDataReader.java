/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;

import de.stationadmin.base.track.DetailedTrack;

/**
 * {@link TrackMetaDataReader} implementation that uses the JAudiotagger library
 * to read audio meta data from any format supported by that library.
 * <p>
 * Instances are configured at construction time with the file extensions they
 * should handle. Use {@link TrackMetaDataReaderFactory} to obtain pre-configured
 * instances for the supported formats.
 * </p>
 * <p>
 * Example:
 * <pre>
 *   TrackMetaDataReader mp3Reader  = new JAudioTaggerMetaDataReader(".mp3");
 *   TrackMetaDataReader aacReader  = new JAudioTaggerMetaDataReader(".aac", ".m4a");
 *   TrackMetaDataReader flacReader = new JAudioTaggerMetaDataReader(".flac");
 *   TrackMetaDataReader wavReader  = new JAudioTaggerMetaDataReader(".wav");
 * </pre>
 * </p>
 *
 * @author Frank Korf
 */
public class JAudioTaggerMetaDataReader implements TrackMetaDataReader {

  private final String[] extensions;

  /**
   * Creates a new reader that handles the given file extensions.
   *
   * @param extensions one or more lower-case file extensions including the
   *                   leading dot, e.g. {@code ".mp3"} or {@code ".aac", ".m4a"}
   */
  public JAudioTaggerMetaDataReader(String... extensions) {
    this.extensions = extensions;
  }

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#supports(java.io.File)
   */
  @Override
  public boolean supports(File file) {
    if (file == null) {
      return false;
    }
    String name = file.getName().toLowerCase();
    for (String ext : extensions) {
      if (name.endsWith(ext)) {
        return true;
      }
    }
    return false;
  }

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#readMetaData(java.io.File)
   */
  @Override
  public DetailedTrack readMetaData(File file) {
    return MP3Util.getTitleInformation(file);
  }

}
