/**
 *
 */
package de.stationadmin.base.util;

import java.io.File;

import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import de.stationadmin.base.track.DetailedTrack;

/**
 * {@link TrackMetaDataReader} implementation for FLAC audio files (.flac).
 * Uses the JAudiotagger library to read Vorbis comment tags embedded in
 * FLAC containers.
 *
 * @author Frank Korf
 */
public class FLACMetaDataReader implements TrackMetaDataReader {

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#supports(java.io.File)
   */
  @Override
  public boolean supports(File file) {
    if (file == null) {
      return false;
    }
    return file.getName().toLowerCase().endsWith(".flac");
  }

  /**
   * @see de.stationadmin.base.util.TrackMetaDataReader#readMetaData(java.io.File)
   */
  @Override
  public DetailedTrack readMetaData(File file) {
    try {
      AudioFile audioFile = AudioFileIO.read(file);
      Tag tag = audioFile.getTag();
      if (tag == null) {
        return null;
      }
      String artist = tag.getFirst(FieldKey.ARTIST);
      String title = tag.getFirst(FieldKey.TITLE);
      if (artist == null || artist.trim().length() == 0
          || title == null || title.trim().length() == 0) {
        return null;
      }
      DetailedTrack track = new DetailedTrack();
      track.setArtist(artist.trim());
      track.setTitle(title.trim());
      String album = tag.getFirst(FieldKey.ALBUM);
      if (album != null && album.trim().length() > 0) {
        track.setAlbum(album.trim());
      }
      return track;
    } catch (Exception e) {
      // silently ignore unreadable files
      return null;
    }
  }

}
