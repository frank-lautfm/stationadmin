/**
 * 
 */
package de.stationadmin.base.track;

import java.io.Serializable;
import java.util.HashSet;
import java.util.Set;

import de.stationadmin.base.playlist.Playlist;

/**
 * @author korf
 * 
 */
public class TrackPlaylists implements Serializable, TrackAttachment {
  private static final long serialVersionUID = 5847727875400132734L;

  /**
   * set of playlist ids in which the title occurs - contains only the state of
   * the commited playlists
   */
  private Set<Integer> playlistIds = new HashSet<Integer>();
  /** set of ids of uncommitted playlists from which this title was removed */
  private transient Set<Integer> removed = new HashSet<Integer>();
  /** set of ids of uncommitted playlists to which this title was added */
  private transient Set<Integer> added = new HashSet<Integer>();

  private transient PlaylistStatistics playlistStatistics = new PlaylistStatistics();

  public class PlaylistStatistics implements Comparable<PlaylistStatistics> {
    private transient int numberOfPlaylistsTotal = -1;
    private transient int numberOfPlaylistsOnline = -1;

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(PlaylistStatistics o) {
      return Integer.valueOf(this.getNumberOfPlaylistsTotal()).compareTo(o.getNumberOfPlaylistsTotal());
    }

    /**
     * @return the numberOfPlaylistsOsnline
     */
    public int getNumberOfPlaylistsOnline() {
      if (this.numberOfPlaylistsOnline < 0) {
        this.update();
      }
      return numberOfPlaylistsOnline;
    }

    /**
     * @return the numberOfPlaylistsTotal
     */
    public int getNumberOfPlaylistsTotal() {
      if (this.numberOfPlaylistsTotal < 0) {
        this.update();
      }
      return numberOfPlaylistsTotal;
    }

    void invalidate() {
      this.numberOfPlaylistsOnline = -1;
      this.numberOfPlaylistsTotal = -1;
    }

    public String toString() {
      if (this.numberOfPlaylistsOnline < 0) {
        this.update();
      }
      int archived = this.numberOfPlaylistsTotal - this.numberOfPlaylistsOnline;
      return (archived > 0 ? "(" + this.numberOfPlaylistsOnline + "/" + archived + ") " : "") + this.numberOfPlaylistsTotal;
    }

    private void update() {
      this.numberOfPlaylistsOnline = 0;
      this.numberOfPlaylistsTotal = 0;
      for (Integer id : getPlaylistIds()) {
        if (Playlist.isOnlinePlaylistId(id)) {
          this.numberOfPlaylistsOnline++;
        }
        this.numberOfPlaylistsTotal++;
      }
    }

  }

  /**
   * Registers a playlist for this title. Notice that this won't be saved as
   * long as the playlist has not been commited for this title.
   * 
   * @param playlistId
   */
  public void addPlaylist(int playlistId) {
    this.getRemoved().remove(playlistId);
    this.getAdded().add(playlistId);
    this.playlistStatistics.invalidate();
  }

  /**
   * Commits modifications for the playlist with the given id
   * 
   * @param playlistId
   */
  public void commitPlaylist(int playlistId) {
    if (this.getAdded().remove(playlistId)) {
      this.playlistIds.add(playlistId);
    }
    if (this.getRemoved().remove(playlistId)) {
      this.playlistIds.remove(playlistId);
    }
    this.playlistStatistics.invalidate();
  }

  public Set<Integer> getPlaylistIds() {
    Set<Integer> ids = new HashSet<Integer>(this.playlistIds);
    ids.addAll(getAdded());
    ids.removeAll(getRemoved());
    return ids;
  }

  /**
   * @return the playlistStatistics
   */
  public PlaylistStatistics getPlaylistStatistics() {
    return playlistStatistics;
  }

  private Set<Integer> getRemoved() {
    if (this.removed == null) {
      this.removed = new HashSet<Integer>();
    }
    return this.removed;
  }

  /**
   * Unregisters a playlist for this title. Notice that this won't be saved as
   * long as the playlist has not been commited for this title.
   * 
   * @param playlistId
   */
  public void removePlaylist(int playlistId) {
    this.getAdded().remove(playlistId);
    this.getRemoved().add(playlistId);
    this.playlistStatistics.invalidate();
  }

  /**
   * Resets modifications for the playlist with the given id
   * 
   * @param playlistId
   */
  public void resetPlaylist(int playlistId) {
    this.getAdded().remove(playlistId);
    this.getRemoved().remove(playlistId);
    this.playlistStatistics.invalidate();
  }

  private Set<Integer> getAdded() {
    if (this.added == null) {
      this.added = new HashSet<Integer>();
    }
    return this.added;
  }

  @Override
  public boolean isUsed() {
    return !(this.playlistIds.size() == 0 && (this.added == null || this.added.size() == 0) && (this.removed == null || this.removed.size() == 0));
  }

}
