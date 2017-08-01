/**
 * 
 */
package de.stationadmin.base.schedule;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Random;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule.Entry;

/**
 * @author korf
 *
 */
public class ScheduleShuffler {
  public static final String TAG_USED = "#used";
  private PlaylistRegistry playlistRegistry;
  private Random random = new Random();

  private boolean slotLenghForPlaylistsRequired = false;
  private String playlistTag = TAG_USED;
  private int basePlaylistId;

  /**
   * 
   */
  public ScheduleShuffler(PlaylistRegistry playlistRegistry, int basePlaylistId) {
    this.playlistRegistry = playlistRegistry;
    this.basePlaylistId = basePlaylistId;
  }

  /**
   * @return the slotLenghForPlaylistsRequired
   */
  public boolean isSlotLenghForPlaylistsRequired() {
    return slotLenghForPlaylistsRequired;
  }

  /**
   * @param slotLenghForPlaylistsRequired
   *          the slotLenghForPlaylistsRequired to set
   */
  public void setSlotLenghForPlaylistsRequired(boolean slotLenghForPlaylistsRequired) {
    this.slotLenghForPlaylistsRequired = slotLenghForPlaylistsRequired;
  }

  private List<Playlist> getCandidatePlaylists(List<Schedule.Entry> entries) {
    List<Playlist> playlists = new ArrayList<Playlist>();

    if (playlistTag != null && playlistTag.equals(TAG_USED)) {
      HashSet<Integer> known = new HashSet<Integer>();
      for (Schedule.Entry e : entries) {
        Playlist p = this.playlistRegistry.getPlaylist(e.getPlaylistId());
        if (p != null && !known.contains(p.getId()) && p.getId() != basePlaylistId) {
          playlists.add(p);
          known.add(p.getId());
        }
      }
    } else {
      for (Playlist p : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
        if (p.getLength() >= 60 * 60 && (this.playlistTag == null || p.getTags().contains(this.playlistTag))) {
          playlists.add(p);
        }
      }
    }

    return playlists;

  }

  private List<EntryRef> getEntries(List<Schedule.Entry> entries) {
    List<EntryRef> list = new ArrayList<EntryRef>();
    for (int i = 0; i < entries.size(); i++) {
      Schedule.Entry entry = entries.get(i);
      if (entry.getPlaylistId() != basePlaylistId) {
        int slot = (entry.getWeekday().getRawDay() - 1) * 24 + entry.getHour();
        Schedule.Entry next = null;
        for (int j = i + 1; j < entries.size() && next == null; j++) {
          if (entries.get(j).getPlaylistId() != entry.getPlaylistId()) {
            next = entries.get(j);
          }
        }
        int nextSlot = next != null ? (next.getWeekday().getRawDay() - 1) * 24 + next.getHour() : 24 * 7;
        int length = nextSlot - slot;
        list.add(new EntryRef(entry, length * 60 * 60));
      }
    }

    Collections.sort(list);

    return list;

  }

  public List<Schedule.Entry> shuffle(List<Schedule.Entry> entries) {
    List<Playlist> playlists = this.getCandidatePlaylists(entries);
    if (playlists.size() == 0) {
      return entries;
    }

    // create copy of entries list - this is what will contain the new playlists
    List<Schedule.Entry> result = new ArrayList<Schedule.Entry>();
    for (Schedule.Entry e : entries) {
      result.add(new Schedule.Entry(e.getPlaylistId(), e.getWeekday(), e.getHour()));
    }
    List<EntryRef> entryRefs = this.getEntries(result);

    List<Playlist> availablePlaylists = new ArrayList<Playlist>(this.randomize(playlists));

    for (EntryRef e : entryRefs) {
      Playlist p = availablePlaylists.get(0);
      if (e.getEntry().getPlaylistId() == p.getId() || (this.slotLenghForPlaylistsRequired && p.getLength() < e.getLength())) {
        // try to find a better entry
        int i = 1;
        while (i < availablePlaylists.size()) {
          Playlist next = availablePlaylists.get(i);
          if (e.getEntry().getPlaylistId() != next.getId() && (!this.slotLenghForPlaylistsRequired || next.getLength() >= e.getLength())) {
            p = availablePlaylists.get(i);
            break;
          }
          i++;
        }
      }
      e.getEntry().setPlaylistId(p.getId());
      availablePlaylists.remove(p);
      if (availablePlaylists.size() == 0) {
        availablePlaylists = new ArrayList<Playlist>(this.randomize(playlists));
      }
    }

    return result;

  }

  private <T> List<T> randomize(Collection<T> list) {
    ArrayList<T> available = new ArrayList<T>(list);
    if (available.size() < 2) {
      return available;
    }

    ArrayList<T> randomizedList = new ArrayList<T>(list.size());

    while (available.size() > 1) {
      int idx = this.random.nextInt(available.size());
      randomizedList.add(available.remove(idx));
    }
    randomizedList.add(available.get(0));

    return randomizedList;
  }

  /**
   * @return the playlistTag
   */
  public String getPlaylistTag() {
    return playlistTag;
  }

  /**
   * @param playlistTag
   *          the playlistTag to set
   */
  public void setPlaylistTag(String playlistTag) {
    this.playlistTag = playlistTag;
  }

  private static class EntryRef implements Comparable<EntryRef> {
    Schedule.Entry entry;
    int length;

    EntryRef(Entry entry, int length) {
      super();
      this.entry = entry;
      this.length = length;
    }

    @Override
    public int compareTo(EntryRef o) {
      return -Integer.compare(this.length, o.length);
    }

    /**
     * @return the entry
     */
    public Schedule.Entry getEntry() {
      return entry;
    }

    /**
     * @return the length
     */
    public int getLength() {
      return length;
    }
  }
}
