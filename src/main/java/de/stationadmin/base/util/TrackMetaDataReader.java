/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;

import de.stationadmin.base.track.DetailedTrack;

/**
 * Strategy interface for reading track meta data (artist, title, album) from
 * local audio files. Implementations exist for each supported audio format.
 * Register new implementations in {@link TrackMetaDataReaderFactory}.
 *
 * @author Frank Korf
 */
public interface TrackMetaDataReader {

  /**
   * Returns <code>true</code> if this reader is able to handle the given file
   * (typically decided by file extension).
   *
   * @param file the audio file to check
   * @return <code>true</code> if this reader supports the file
   */
  boolean supports(File file);

  /**
   * Reads artist, title and album from the given audio file.
   *
   * @param file the audio file to read
   * @return a {@link DetailedTrack} populated with at least artist and title,
   *         or <code>null</code> if the file contains no usable meta data or
   *         reading fails
   */
  DetailedTrack readMetaData(File file);

}
