/**
 * 
 */
package de.stationadmin.base.playlist.trackimport;

import java.io.File;

import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.jaudiotagger.audio.AudioFile;
import org.jaudiotagger.audio.AudioFileIO;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.util.MP3Util;

/**
 * 
 * @author Frank Korf
 * 
 */
public class MP3TrackImportTask extends TrackImportTask {
  private static final Logger log = LogManager.getLogger(MP3TrackImportTask.class);
  private boolean resolved = false;
  private File file;
  private Tag tag;

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

  public MP3TrackImportTask(File file, Tag tag) {
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
        AudioFile audioFile = AudioFileIO.read(file);
        Tag t = audioFile.getTag();
        if (t != null) {
          artist = StringUtils.trimToNull(t.getFirst(FieldKey.ARTIST));
          title = StringUtils.trimToNull(t.getFirst(FieldKey.TITLE));
          album = StringUtils.trimToNull(t.getFirst(FieldKey.ALBUM));
          this.tag = t;
        }
      } catch (Exception e) {
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
  public Tag getTag() {
    return tag;
  }

}
