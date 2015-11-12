/**
 * 
 */
package de.stationadmin.base.playlist.trackimport;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.blinkenlights.jid3.ID3Exception;
import org.blinkenlights.jid3.ID3Tag;
import org.blinkenlights.jid3.MP3File;
import org.blinkenlights.jid3.v1.ID3V1Tag;
import org.blinkenlights.jid3.v2.ID3V2Tag;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.util.MP3Util;

/**
 * 
 * @author Frank Korf
 * 
 */
public class MP3TrackImportTask extends TrackImportTask {
  private static final Logger log = Logger.getLogger(MP3TrackImportTask.class);
  private boolean resolved = false;
  private File file;
  private ID3Tag tag;

  /**
   * Creates a new import task
   * 
   * @param file
   *          media file to import
   */
  public MP3TrackImportTask(File file) {
    super();
    this.file = file;
  }

  public MP3TrackImportTask(File file, ID3Tag tag) {
    super();
    this.file = file;
    this.tag = tag;
    if (tag != null) {
      DetailedTrack t = MP3Util.getTitleInformation(tag);
      if (t != null) {
        this.setArtist(t.getArtist());
        this.setTitle(t.getTitle());
        this.setAlbum(t.getAlbum());
        this.resolved = true;
      }
    }
  }

  /**
   * @return the file
   */
  public File getFile() {
    return file;
  }

  /**
   * @see de.stationadmin.base.playlist.trackimport.TrackImportTask#getSourceString()
   */
  @Override
  public String getSourceString() {
    return this.file.getName();
  }

  /**
   * @see de.stationadmin.base.playlist.trackimport.TrackImportTask#resolve()
   */
  @Override
  public void resolve() {
    if (resolved) {
      return;
    }
    this.resolved = true;

    try {
      String artist = null;
      String title = null;
      String album = null;

      try {
        MP3File mp3File = new MP3File(file);
        boolean hasID3V2Tags = false;
        for (ID3Tag tag : mp3File.getTags()) {
          if (tag instanceof ID3V1Tag && !hasID3V2Tags) {
            artist = StringUtils.trimToNull(((ID3V1Tag) tag).getArtist());
            title = StringUtils.trimToNull(((ID3V1Tag) tag).getTitle());
            album = StringUtils.trimToNull(((ID3V1Tag) tag).getAlbum());
            this.tag = tag;
          }
          if (tag instanceof ID3V2Tag) {
            hasID3V2Tags = true;
            artist = StringUtils.trimToNull(((ID3V2Tag) tag).getArtist());
            title = StringUtils.trimToNull(((ID3V2Tag) tag).getTitle());
            album = StringUtils.trimToNull(((ID3V2Tag) tag).getAlbum());
            this.tag = tag;
          }
        }
      } catch (ID3Exception e) {
        log.info("error while reading tags", e);
        this.setStatus(Status.NO_TAGS);
      }

      if (artist != null && title != null) {
        this.setArtist(artist);
        this.setTitle(title);
        this.setAlbum(album);
      } else {
        log.warn("no tag information found");
        this.setStatus(Status.NO_TAGS);
      }
    } catch (Throwable t) {
      log.warn("unable to read id3 tag from " + file, t);
      this.setStatus(Status.TAG_READ_ERROR);
    }

  }

  /**
   * @return the tag
   */
  public ID3Tag getTag() {
    return tag;
  }

}
