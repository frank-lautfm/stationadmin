/**
 * 
 */
package de.stationadmin.base.playlist.validation;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;

/**
 * Checks a playlist for violations of the GVL rules:
 * <p>
 * "Der Webcaster darf innerhalb von drei Stunden seines Programms nicht übertragen: 
 * <p>
 * (a) mehr als drei verschiedene Titel von einem bestimmten Album, davon nicht mehr als 
 * zwei Titel aufeinanderfolgend; oder 
 * <br>
 * (b) mehr als vier verschiedene Titel eines bestimmten Künstlers oder einer Compilation 
 * von Musiktiteln, davon nicht mehr als drei aufeinanderfolgend.“
 * <p>
 * Notice that only rule b) can be checked - album information is not available in playlists.
 * 
 * @author Frank Korf
 */
public class GVLValidator implements PlaylistValidator {

  /**
   * @see de.stationadmin.base.playlist.validation.PlaylistValidator#validate(de.stationadmin.base.playlist.Playlist,
   *      java.util.List)
   */
  @Override
  public boolean validate(Playlist playlist, List<Entry> violations) {
    return this.validate(playlist, violations, true);
  }

  public boolean validate(Playlist playlist, List<Entry> violations, boolean checkRollover) {
    if(playlist.isShuffle()) {
      return true;
    }
    Map<String, List<TitleEntry>> artistTitleMap = this.buildArtistTitleMap(playlist);
    List<TitleEntry> titleViolationEntries;
    if(playlist.isShuffle()) {
      titleViolationEntries = this.checkNumTitles(artistTitleMap, playlist.getLength());
    }
    else {
      titleViolationEntries = this.checkDistance(artistTitleMap, playlist.getLength(), checkRollover);
    }

    for (TitleEntry titleEnty : titleViolationEntries) {
      violations.add(titleEnty.getEntry());
    }

    return titleViolationEntries.size() == 0;
  }

  private List<TitleEntry> checkNumTitles(Map<String, List<TitleEntry>> artistTitleMap, int totalLength) {
    ArrayList<TitleEntry> entries = new ArrayList<TitleEntry>();
    int hours = totalLength / (60 * 60);
    for(java.util.Map.Entry<String, List<TitleEntry>> entry : artistTitleMap.entrySet()) {
      if(entry.getValue().size() > 3 &&  entry.getValue().size() > hours) {
        entries.addAll(entry.getValue());
      }
    }
    return entries;
  }

  private List<TitleEntry> checkDistance(Map<String, List<TitleEntry>> artistTitleMap, int totalLength, boolean checkRollover) {
    ArrayList<TitleEntry> entries = new ArrayList<TitleEntry>();
    for (List<TitleEntry> list : artistTitleMap.values()) {
      if (isViolation(list, totalLength, checkRollover)) {
        entries.addAll(list);
      }
    }
    return entries;
  }

  private boolean isViolation(List<TitleEntry> entriesOfArtist, int totalLength, boolean checkRollover) {
    if (entriesOfArtist.size() < 4) {
      // always ok
      return false;
    }
    if (checkRollover) {
      // append first entry to the end of list
      entriesOfArtist = new ArrayList<TitleEntry>(entriesOfArtist);
      TitleEntry first = entriesOfArtist.get(0);
      entriesOfArtist.add(new TitleEntry(first.getPlaylist(), first.getEntry(), totalLength + first.getStartTime()));
    }

    int sequenceCnt = 1;
    for (int i = 1; i < entriesOfArtist.size(); i++) {
      TitleEntry e2 = entriesOfArtist.get(i);
      if (i >= 4) {
        TitleEntry e1 = entriesOfArtist.get(i - 4);
        if (e2.getStartTime() - e1.getStartTime() < 60 * 180) {
          // 4 titles of one artist within 180 minutes
          return true;
        }
      }
      TitleEntry e1 = entriesOfArtist.get(i - 1);
      if (e1.getStartTime() + e1.getTitle().getLength() == e2.getStartTime()) {
        sequenceCnt++;
      } else {
        sequenceCnt = 1;
      }
      if (sequenceCnt == 4) {
        // more than 3 titles of an artist in a row
        return true;
      }
    }

    return false;
  }

  private Map<String, List<TitleEntry>> buildArtistTitleMap(Playlist... playlists) {
    Map<String, List<TitleEntry>> map = new HashMap<String, List<TitleEntry>>();

    int startTime = 0;
    for (Playlist playlist : playlists) {
      for (Entry entry : playlist.getEntries()) {
        TitleEntry titleEntry = new TitleEntry(playlist, entry, startTime);
        if (titleEntry.getTitle() != null && titleEntry.getTitle().getType() == 1 && titleEntry.getTitle().getArtist() != null) {
          String artist = titleEntry.getTitle().getArtist().toLowerCase().trim();
          int featPos = artist.indexOf(" feat");
          if (featPos > 0) {
            artist = artist.substring(0, featPos).trim();
          }
          List<TitleEntry> entries = map.get(artist);
          if (entries == null) {
            entries = new ArrayList<TitleEntry>();
            map.put(artist, entries);
          }
          entries.add(titleEntry);
          startTime += titleEntry.getTitle().getLength();
        }
      }
    }

    return map;
  }

  private static class TitleEntry {
    private Playlist playlist;
    private Entry entry;
    private BasicTrack title;
    private int startTime;

    public TitleEntry(Playlist playlist, Entry entry, int startTime) {
      super();
      this.playlist = playlist;
      this.entry = entry;
      this.startTime = startTime;
      this.title = playlist.getTrackRegistry().getTrack(entry.getTrackId());
    }

    public Playlist getPlaylist() {
      return playlist;
    }

    public Entry getEntry() {
      return entry;
    }

    public BasicTrack getTitle() {
      return title;
    }

    public int getStartTime() {
      return startTime;
    }

  }

}
