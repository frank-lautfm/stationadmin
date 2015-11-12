/**
 * 
 */
package de.stationadmin.base.playlist.util;

import java.util.ArrayList;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.util.TimeFormat;

/**
 *
 * @author Frank Korf
 *
 */
public class PlaylistEntry {
  private Playlist playlist;
  private List<Entry> entries;

  public PlaylistEntry(Playlist playlist, Entry entry) {
    super();
    this.playlist = playlist;
    this.entries = new ArrayList<Playlist.Entry>();
    this.add(entry);
  }

  public void add(Entry entry) {
    this.entries.add(entry);
  }

  public Playlist getPlaylist() {
    return playlist;
  }
  
  public Entry getEntry() {
    return this.entries.get(0);
  }

  public List<Entry> getEntries() {
    return entries;
  }

  @Override
  public String toString() {
    return playlist.getDisplayName() + ": " + TimeFormat.format(entries.get(0).getStart(), true);
  }

}
