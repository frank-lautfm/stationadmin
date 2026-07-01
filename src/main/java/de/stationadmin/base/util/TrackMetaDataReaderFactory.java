/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.stationadmin.base.track.DetailedTrack;

/**
 * Factory that selects the appropriate {@link TrackMetaDataReader} for a given
 * audio file and delegates the meta data reading to it.
 * <p>
 * To support additional audio formats, add a new {@link JAudioTaggerMetaDataReader}
 * instance to the {@code READERS} list with the desired file extensions and also
 * add the extensions to {@code SUPPORTED_EXTENSIONS}.
 * </p>
 *
 * @author Frank Korf
 */
public class TrackMetaDataReaderFactory {

  /** Lower-case extensions (including leading dot) of all supported audio formats. */
  public static final Set<String> SUPPORTED_EXTENSIONS = new HashSet<String>(
      Arrays.asList(".mp3", ".aac", ".m4a", ".flac", ".wav")
  );

  private static final List<TrackMetaDataReader> READERS = Arrays.asList(
      new JAudioTaggerMetaDataReader(".mp3"),
      new JAudioTaggerMetaDataReader(".aac", ".m4a"),
      new JAudioTaggerMetaDataReader(".flac"),
      new JAudioTaggerMetaDataReader(".wav")
  );

  /**
   * Returns {@code true} if the given file has an extension supported by this
   * factory (i.e. mp3, aac, m4a, flac, wav).
   *
   * @param file the file to test
   * @return {@code true} if the file format is supported
   */
  public static boolean isSupportedAudioFile(File file) {
    if (file == null) {
      return false;
    }
    String name = file.getName().toLowerCase();
    int dot = name.lastIndexOf('.');
    if (dot < 0) {
      return false;
    }
    return SUPPORTED_EXTENSIONS.contains(name.substring(dot));
  }

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
