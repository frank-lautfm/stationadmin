/**
 * 
 */
package de.stationadmin.base.playlist.util;

import java.util.ArrayList;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;

/**
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistTitleSearch {

  private TrackRegistry titleRegistry;
  private PlaylistRegistry playlistRegistry;

  public PlaylistTitleSearch(TrackRegistry titleRegistry, PlaylistRegistry playlistRegistry) {
    super();
    this.titleRegistry = titleRegistry;
    this.playlistRegistry = playlistRegistry;
  }

  public List<PlaylistEntry> search(String query) {
    ArrayList<PlaylistEntry> entries = new ArrayList<PlaylistEntry>();
    List<RegisteredTrack> titles = this.titleRegistry.search(query);
    for (RegisteredTrack title : titles) {
      for (int playlistId : title.getPlaylistIds()) {
        Playlist playlist = this.playlistRegistry.getPlaylist(playlistId);
        if (playlist != null) {
          int idx = playlist.indexOf(title.getId());
          if (idx > -1) {
            Playlist.Entry entry = playlist.getEntries().get(idx);
            PlaylistEntry plEntry = new PlaylistEntry(playlist, entry);
            entries.add(plEntry);
            do {
              idx = playlist.indexOf(title.getId(), idx + 1);
              if (idx > -1) {
                entry = playlist.getEntries().get(idx);
                plEntry.add(entry);
              }
            } while (idx > -1);
          }
        }
      }
    }
    return entries;

  }

}
