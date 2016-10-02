/**
 * 
 */
package de.stationadmin.base.playlist.util;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;

/**
 * Searches for duplicate titles in a list of playlists
 * 
 * @author Frank Korf
 */
public class DupeFinder {

  /**
   * Finds duplicate titles in playlists
   * @param playlists
   * @return
   */
  public Map<BasicTrack, List<PlaylistEntry>> findDupes(List<Playlist> playlists) {

    HashMap<Integer, List<PlaylistEntry>> map = new HashMap<Integer, List<PlaylistEntry>>();
    for (Playlist playlist : playlists) {
      for (Entry entry : playlist.getEntries()) {
        List<PlaylistEntry> list = map.get(entry.getTrackId());
        if (list == null) {
          list = new ArrayList<PlaylistEntry>();
          map.put(entry.getTrackId(), list);
        }
        list.add(new PlaylistEntry(playlist, entry));
      }
    }

    Map<BasicTrack, List<PlaylistEntry>> dupes = new HashMap<BasicTrack, List<PlaylistEntry>>();
    for (int titleId : map.keySet()) {
      List<PlaylistEntry> list = map.get(titleId);
      if (list.size() > 1) {
        BasicTrack title = list.get(0).getPlaylist().getTrackRegistry().getTrack(titleId);
        if (title != null) {
          dupes.put(title, list);
        }
      }
    }

    return dupes;
  }

}
