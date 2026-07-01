/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;
import java.util.Arrays;
import java.util.List;

import de.stationadmin.base.track.DetailedTrack;

/**
 * Factory that selects the appropriate {@link TrackMetaDataReader} for a given
 * audio file and delegates the meta data reading to it.
 * <p>
 * To support additional audio formats, implement {@link TrackMetaDataReader}
 * and add an instance to the {@code READERS} list.
 * </p>
 *
 * @author Frank Korf
 */
public class TrackMetaDataReaderFactory {

  private static final List<TrackMetaDataReader> READERS = Arrays.asList(
      new MP3MetaDataReader(),
      new AACMetaDataReader()
  );

  /**
   * Reads meta data from the given audio file using the first registered
   * {@link TrackMetaDataReader} that supports the file format.
   *
   * @param file the audio file to read
   * @return a {@link DetailedTrack} with at least artist and title set, or
   *         <code>null</code> if no reader supports the file or the file
   *         contains no usable meta data
   */
  public static DetailedTrack readMetaData(File file) {
    if (file == null) {
      return null;
    }
    for (TrackMetaDataReader reader : READERS) {
      if (reader.supports(file)) {
        return reader.readMetaData(file);
      }
    }
    return null;
  }

}
