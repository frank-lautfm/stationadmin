/**
 * 
 */
package de.stationadmin.gui.playlist;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.JumpListener;

/**
 * <code>PlaylistEntryJumpTarget</code> carries a playlist and a title as
 * information for {@link JumpListener}s when a jump to a specific title within
 * a playlist is requested.
 * 
 * @author Frank Korf
 */
public class PlaylistEntryJumpTarget {
  private Playlist playlist;
  private BasicTrack title;
  private int startTime = -1;

  public PlaylistEntryJumpTarget(Playlist playlist, BasicTrack title) {
    super();
    this.playlist = playlist;
    this.title = title;
  }

  public PlaylistEntryJumpTarget(Playlist playlist, BasicTrack title, int startTime) {
    this(playlist, title);
    this.startTime = startTime;
  }

  /**
   * @return the playlist
   */
  public Playlist getPlaylist() {
    return playlist;
  }

  /**
   * @return the title
   */
  public BasicTrack getTitle() {
    return title;
  }

  public Entry getEntry() {
    if (this.playlist != null && this.title != null) {
      // if start time is given, try to find title based on start time
      if (this.startTime > -1) {
        for (Entry entry : this.playlist.getEntries()) {
          if(entry.getStart() >= this.startTime && entry.getTrackId() == this.title.getId()) {
            return entry;
          }
        }
      }
      // otherwise search first occurrence
      int idx = this.playlist.indexOf(title.getId());
      if (idx > -1) {
        return this.playlist.getEntry(idx);
      }
    }
    return null;
  }

  /**
   * @return the startTime
   */
  public int getStartTime() {
    return startTime;
  }

}
