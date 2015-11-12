/**
 * 
 */
package de.stationadmin.base.util;

import java.io.File;

import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v1.ID3V1Tag;
import org.blinkenlights.jid3.v2.ID3V2Tag;

import de.stationadmin.base.track.DetailedTrack;

/**
 *
 * @author Frank Korf
 *
 */
public class MP3Util {

  public static DetailedTrack getTitleInformation(ID3Tag tag) {
    String artist = null;
    String title = null;
    String album = null;

    if (tag instanceof ID3V1Tag) {
      artist = ((ID3V1Tag) tag).getArtist();
      title = ((ID3V1Tag) tag).getTitle();
      album = ((ID3V1Tag) tag).getAlbum();
    }
    if (tag instanceof ID3V2Tag) {
      artist = ((ID3V2Tag) tag).getArtist();
      title = ((ID3V2Tag) tag).getTitle();
      album = ((ID3V2Tag) tag).getAlbum();
    }
    if (artist != null && title != null) {
      DetailedTrack t = new DetailedTrack();
      t.setArtist(artist);
      t.setTitle(title);
      t.setAlbum(album);
      return t;
    }

    return null;
  }

  public static DetailedTrack getTitleInformation(File file) {
    String artist = null;
    String title = null;
    String album = null;

    try {
      MP3File mp3File = new MP3File(file);
      for (ID3Tag tag : mp3File.getTags()) {
        if (tag instanceof ID3V1Tag) {
          artist = ((ID3V1Tag) tag).getArtist();
          title = ((ID3V1Tag) tag).getTitle();
          album = ((ID3V1Tag) tag).getAlbum();
        }
        if (tag instanceof ID3V2Tag) {
          artist = ((ID3V2Tag) tag).getArtist();
          title = ((ID3V2Tag) tag).getTitle();
          album = ((ID3V2Tag) tag).getAlbum();
        }
        if (artist != null && title != null) {
          break;
        }
      }
    } catch (ID3Exception e) {
    }

    if (artist != null && title != null) {
      DetailedTrack t = new DetailedTrack();
      t.setArtist(artist);
      t.setTitle(title);
      t.setAlbum(album);
      return t;
    }

    return null;

  }

}
