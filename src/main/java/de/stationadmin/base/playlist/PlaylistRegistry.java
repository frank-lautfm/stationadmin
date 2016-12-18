package de.stationadmin.base.playlist;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.util.AbstractBean;

/**
 * Registry for playlists
 * 
 * @author korf
 */
public class PlaylistRegistry extends AbstractBean {
  private List<Playlist> playlists = new ArrayList<Playlist>();
  private Map<Integer, Playlist> playlistsById = new HashMap<Integer, Playlist>();
  private Map<Integer, ExtendedPlaylistData> localData = new HashMap<Integer, ExtendedPlaylistData>();

  /**
   * Registers an additonal playlist
   * 
   * @param playlist
   */
  public void register(Playlist playlist) {
    if (playlist.getType() == PlaylistType.TEMPORARY) {
      throw new IllegalArgumentException("Temporary playlists must not be registered");
    }
    int oldNum = this.playlists.size();
    if (playlist.getId() >= 0) {
      if (playlist.getLocalData() != null) {
        this.localData.put(playlist.getId(), playlist.getLocalData());
      } else {
        ExtendedPlaylistData local = this.localData.get(playlist.getId());
        if (local == null) {
          local = new ExtendedPlaylistData(playlist.getId());
          this.localData.put(playlist.getId(), local);
        }
        playlist.setLocalData(local);
      }
      this.playlistsById.put(playlist.getId(), playlist);
    }
    if (!this.playlists.contains(playlist)) {
      this.playlists.add(playlist);
    }
    this.getPcs().firePropertyChange("numPlaylists", oldNum, this.playlists.size());
  }

  /**
   * Unregisters a playlist
   * @param playlist
   */
  public void unregister(Playlist playlist) {
    int oldNum = this.playlists.size();
    this.playlistsById.remove(playlist.getId());
    this.playlists.remove(playlist);
    this.getPcs().firePropertyChange("numPlaylists", oldNum, this.playlists.size());
  }

  /**
   * Removes all entries
   */
  public void clear() {
    int oldNum = this.playlists.size();
    this.playlistsById.clear();
    this.playlists.clear();
    this.getPcs().firePropertyChange("numPlaylists", oldNum, this.playlists.size());
  }

  /**
   * Gets the number of playlists
   * 
   * @return
   */
  public int getNumPlaylists() {
    return this.playlists.size();
  }

  /**
   * Gets all playlists
   * 
   * @return playlists
   */
  public List<Playlist> getAllPlaylists() {
    return new ArrayList<Playlist>(this.playlists);
  }

  /**
   * Gets all playlists of the given type
   * @param type type or <code>null</code> to return all playlists
   * @return matching playlists
   */
  public List<Playlist> getPlaylists(PlaylistType type) {
    List<Playlist> list = new ArrayList<Playlist>();
    for (Playlist playlist : this.playlists) {
      if (type == null || playlist.getType() == type) {
        list.add(playlist);
      }
    }
    return list;
  }

  /**
   * Gets a playlist by its id
   * 
   * @param id
   * @return playlist or <code>null</code> if not found
   */
  public Playlist getPlaylist(int id) {
    return this.playlistsById.get(id);
  }

  /**
   * Gets all tags that are used for the registered playlists
   * @return
   */
  public Set<String> getUsedTags() {
    HashSet<String> allTags = new HashSet<String>();
    for (ExtendedPlaylistData data : this.localData.values()) {
      allTags.addAll(data.getTags());
    }

    return allTags;
  }

  /**
   * Gets the extended playlist data as list
   * @return
   */
  public List<ExtendedPlaylistData> getLocalData() {
    ArrayList<ExtendedPlaylistData> dataList = new ArrayList<ExtendedPlaylistData>(this.localData.values());
    return dataList;
  }

  /**
   * Overwrites the current local data with the given data
   * @param dataList
   */
  public void setLocalData(List<ExtendedPlaylistData> dataList) {
    this.localData.clear();
    for (ExtendedPlaylistData data : dataList) {
      Playlist playlist = this.getPlaylist(data.getId());
      if (playlist != null) {
        playlist.setLocalData(data);
      }
      this.localData.put(data.getId(), data);
    }
  }

}
