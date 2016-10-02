/**
 * 
 */
package de.stationadmin.gui.mp3explorer;

import org.blinkenlights.jid3.v1.ID3V1Tag;
import org.blinkenlights.jid3.v2.ID3V2Tag;

import com.jgoodies.binding.beans.Model;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TitleModel extends Model {
  private static final long serialVersionUID = 6565522546652005173L;
  private MP3File file;
  private BasicTrack title;
  private TitleStatus status;

  public TitleModel(MP3File file) {
    super();
    this.file = file;
  }

  /**
   * @return the file
   */
  public MP3File getFile() {
    return file;
  }

  public String getID3Artist() {
    return (file.tag instanceof ID3V2Tag) ? ((ID3V2Tag) file.tag).getArtist() : (file.tag instanceof ID3V1Tag)
        ? ((ID3V1Tag) file.tag).getArtist()
        : null;
  }

  public String getID3Name() {
    return (file.tag instanceof ID3V2Tag) ? ((ID3V2Tag) file.tag).getTitle() : (file.tag instanceof ID3V1Tag)
        ? ((ID3V1Tag) file.tag).getTitle()
        : null;
  }

  /**
   * @return the status
   */
  public TitleStatus getStatus() {
    return status;
  }

  public String getArtist() {
    return this.title != null ? this.title.getArtist() : null;
  }

  public String getName() {
    return this.title != null ? this.title.getTitle() : null;
  }

  public String getAlbum() {
    return this.title instanceof RegisteredTrack ? ((RegisteredTrack) this.title).getAlbum() : null;
  }

  public boolean isOwnTitle() {
    return this.title instanceof RegisteredTrack ? ((RegisteredTrack) this.title).isOwnTrack() : Boolean.FALSE;
  }

  /**
   * @return the title
   */
  public BasicTrack getTitle() {
    return title;
  }

  /**
   * @param status
   *          the status to set
   */
  public void setStatus(TitleStatus status) {
    TitleStatus old = this.status;
    this.status = status;
    if (this.file != null) {
      this.file.setStatus(status);
    }
    this.firePropertyChange("status", old, status);
  }

  /**
   * @param title the title to set
   */
  public void setTitle(BasicTrack title) {
    String oldArtist = this.getArtist();
    String oldName = this.getName();
    String oldAlbum = this.getAlbum();
    boolean oldOwn = this.isOwnTitle();
    BasicTrack old =  this.title;
    this.title = title;
    this.firePropertyChange("artist", oldArtist, this.getArtist());
    this.firePropertyChange("name", oldName, this.getName());
    this.firePropertyChange("album", oldAlbum, this.getAlbum());
    this.firePropertyChange("ownTitle", oldOwn, this.isOwnTitle());
    this.firePropertyChange("title", old, title);
  }

}
