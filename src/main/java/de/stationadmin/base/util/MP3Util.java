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
 *
 * @author Frank Korf
 *
 */
public class MP3Util {

  public static DetailedTrack getTitleInformation(Tag tag) {
    if (tag == null) {
      return null;
    }
    String artist = tag.getFirst(FieldKey.ARTIST);
    String title = tag.getFirst(FieldKey.TITLE);
    String album = tag.getFirst(FieldKey.ALBUM);

    if (artist != null && !artist.trim().isEmpty()
        && title != null && !title.trim().isEmpty()) {
      DetailedTrack t = new DetailedTrack();
      t.setArtist(artist.trim());
      t.setTitle(title.trim());
      if (album != null && !album.trim().isEmpty()) {
        t.setAlbum(album.trim());
      }
      return t;
    }

    return null;
  }

  public static DetailedTrack getTitleInformation(File file) {
    try {
      AudioFile audioFile = AudioFileIO.read(file);
      Tag tag = audioFile.getTag();
      return getTitleInformation(tag);
    } catch (Exception e) {
      // silently ignore unreadable files
    }
    return null;
  }

}
