package de.stationadmin.base.track;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import de.stationadmin.base.playlist.Playlist;

/**
 * Extension of title as used in {@link TrackRegistry} - stores in which
 * playlists a title occurs
 * <p>
 * Since playlists can be modified locally and are either commited or reseted to
 * their persisted state at some time, this class also needs to handle these
 * temporary states of uncommited playlists. <br>
 * The result of {{@link #getPlaylistIds()} reflects modifications in uncommited
 * playlists. However, if the class is persisted, these modifications are lost
 * unless the playlist has been commited.
 * 
 * @author korf
 */
public class RegisteredTrack extends DetailedTrack {

  /**
   * set of playlist ids in which the title occurs - contains only the state of
   * the commited playlists
   */
  private Set<Integer> playlistIds = new HashSet<Integer>();
  /** set of ids of uncommitted playlists from which this title was removed */
  private transient Set<Integer> removed = new HashSet<Integer>();
  /** set of ids of uncommitted playlists to which this title was added */
  private transient Set<Integer> added = new HashSet<Integer>();

  private List<TrackAlias> aliases;

  private int tagCnt = 0;

  private transient PlaylistStatistics playlistStatistics = new PlaylistStatistics();

  public RegisteredTrack() {
    super();
  }

  public RegisteredTrack(Title title) {
    super(title);
  }

  public RegisteredTrack(de.stationadmin.lfm.backend.Track track) {
    super(track);
  }

  public void addAlias(String artist, String title) {
    if (this.aliases == null) {
      this.aliases = new ArrayList<TrackAlias>();
    } else {
      // avoid dupes
      for (TrackAlias alias : this.aliases) {
        if (alias.getArtist().equals(artist) && alias.getTitle().equals(title)) {
          return;
        }
      }
    }
    this.aliases.add(new TrackAlias(artist, title));
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

  private Set<Integer> getAdded() {
    if (this.added == null) {
      this.added = new HashSet<Integer>();
    }
    return this.added;
  }

  /**
   * @return the aliases
   */
  public List<TrackAlias> getAliases() {
    return aliases;
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
   * Checks if this title is currently unused
   * 
   * @return s
   */
  public boolean isUnused() {
    return this.tagCnt == 0 && this.playlistIds.size() == 0 && (this.added == null || this.added.size() == 0)
        && (this.removed == null || this.removed.size() == 0) && this.isOwnTitle() == false;
  }

  public void removeAlias(TrackAlias alias) {
    if (this.aliases != null) {
      this.aliases.remove(alias);
    }
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

  public void tagCountInc() {
    this.tagCnt++;
  }

  public void tagCountDec() {
    if (this.tagCnt > 0) {
      this.tagCnt--;
    }
  }

  public int getTagCnt() {
    return tagCnt;
  }
  

}
