package de.stationadmin.base.track;

import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.util.AbstractBean;

/**
 * Registry for titles that are used in playlists
 * 
 * @author korf
 */
public class TrackRegistry extends AbstractBean {
  private Map<Integer, RegisteredTrack> tracks = Collections.synchronizedMap(new HashMap<Integer, RegisteredTrack>());
  private Map<String, String> sharedStrings = new HashMap<String, String>();
  
  private DetailedTrack standardAdTrigger;

  private boolean blockChangsEvts = false;
  private int numTracksBeforeBlock = -1;

  private Map<Integer, Integer> legacyIdMapping = new HashMap<Integer, Integer>();
  private boolean fullLegacyIdMappingLoaded = false;
  private Map<Integer, Integer> reverseLegacyIdMapping = new HashMap<Integer, Integer>();

  /**
   * Removes all entries
   */
  public void clear() {
    int oldNum = this.tracks.size();
    this.tracks.clear();
    this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
  }

  public void removeUnused() {
    int oldNum = this.tracks.size();
    Set<Integer> remove = new HashSet<Integer>();
    for (RegisteredTrack title : this.tracks.values()) {
      if (title.getTagCnt() == 0 && title.getPlaylistIds().size() == 0 && !title.isOwnTrack()) {
        remove.add(title.getId());
      }
    }
    for (Integer id : remove) {
      tracks.remove(id);
    }
    this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
  }

  public int getNumTracks() {
    return this.tracks.size();
  }

  /**
   * Registers a title for a playlist
   * 
   * @param playlist
   * @param title
   */
  public RegisteredTrack register(Playlist playlist, BasicTrack track) {
    if (!this.tracks.containsKey(track.getId())) {
      RegisteredTrack regTitle = this.assignSharedStrings(new RegisteredTrack(track));
      regTitle.addPlaylist(playlist.getId());
      int oldNum = this.tracks.size();
      this.tracks.put(track.getId(), regTitle);
      if (!this.blockChangsEvts) {
        this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
      }
      return regTitle;
    } else {
      RegisteredTrack regTrack = this.tracks.get(track.getId());
      regTrack.addPlaylist(playlist.getId());
      return regTrack;
    }
  }

  public void register(BasicTrack track) {
    if (!this.tracks.containsKey(track.getId())) {
      RegisteredTrack regTitle = this.assignSharedStrings(new RegisteredTrack(track));
      int oldNum = this.tracks.size();
      this.tracks.put(track.getId(), regTitle);
      if (!this.blockChangsEvts) {
        this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
      }
    }
  }

  /**
   * Registers an alias for a given title. Should be used rather than using {@link RegisteredTrack#addAlias(String, String)} directly because this version replaces the artist by a shared string.
   * 
   * @param titleId
   * @param artist
   * @param title
   */
  public void registerAlias(int titleId, String artist, String title) {
    RegisteredTrack t = this.tracks.get(titleId);
    if (t != null && StringUtils.isNotEmpty(artist) && StringUtils.isNotEmpty(title)) {
      t.addAlias(this.getSharedString(artist), title);
    }
  }

  public void registerOwnTrack(DetailedTrack title) {
    if (!this.tracks.containsKey(title.getId())) {
      RegisteredTrack regTitle = this.assignSharedStrings(new RegisteredTrack(title));
      regTitle.setOwnTrack(true);
      int oldNum = this.tracks.size();
      this.tracks.put(title.getId(), regTitle);
      if (!this.blockChangsEvts) {
        this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
      }
    } else {
      RegisteredTrack regTitle = this.tracks.get(title.getId());
      regTitle.setOwnTrack(true);
      regTitle.setUploadDate(title.getUploadDate());
      regTitle.setArtist(title.getArtist());
      regTitle.setTitle(title.getTitle());
      regTitle.setAlbum(title.getAlbum());
      regTitle.setGenre(title.getGenre());
      regTitle.setPrivateTrack(title.isPrivateTrack());
      regTitle.setYear(title.getYear());
      regTitle.setType(title.getType());
    }
  }

  protected void setOwnTracks(List<RegisteredTrack> ownTitles) {
    if (tracks.size() > 0) {
      throw new IllegalStateException();
    }
    int oldNum = this.tracks.size();
    for (RegisteredTrack title : ownTitles) {
      this.tracks.put(title.getId(), title);
    }
    if (!this.blockChangsEvts) {
      this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
    }
  }

  public boolean isRegisteredAsOwnTrack(int titleId) {
    return this.tracks.containsKey(titleId) && this.tracks.get(titleId).isOwnTrack();
  }

  /**
   * Removes the ownTitle flag from all registered titles and removes those titles from the registry that are not assigned to any playlist
   */
  public void resetOwnTitles() {
    Collection<RegisteredTrack> titles = new ArrayList<RegisteredTrack>(this.tracks.values());
    for (RegisteredTrack title : titles) {
      if (title.isOwnTrack()) {
        title.setOwnTrack(false);
        if (title.isUnused()) {
          this.tracks.remove(title.getId());
        }
      }
    }

  }

  public void unregister(Playlist playlist, int titleId) {
    if (this.tracks.containsKey(titleId)) {
      RegisteredTrack regTitle = this.tracks.get(titleId);
      regTitle.removePlaylist(playlist.getId());
    }
  }

  private void remove(RegisteredTrack title) {
    int oldNum = this.tracks.size();
    this.tracks.remove(title.getId());
    if (!this.blockChangsEvts) {
      this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
    }
  }

  public void remove(int id) {
    int oldNum = this.tracks.size();
    this.tracks.remove(id);
    if (!this.blockChangsEvts) {
      this.getPcs().firePropertyChange("numTracks", oldNum, this.tracks.size());
    }
  }

  public void commit(Playlist playlist) {
    ArrayList<RegisteredTrack> titles = new ArrayList<RegisteredTrack>(this.tracks.values());
    for (RegisteredTrack title : titles) {
      title.commitPlaylist(playlist.getId());
      if (title.isUnused()) {
        this.remove(title);
      }
    }
  }

  public void reset(Playlist playlist) {
    ArrayList<RegisteredTrack> titles = new ArrayList<RegisteredTrack>(this.tracks.values());
    for (RegisteredTrack title : titles) {
      title.resetPlaylist(playlist.getId());
      if (title.isUnused()) {
        this.remove(title);
      }
    }
  }

  /**
   * Adds a registered title - should only be used for import of persisted data
   * <p>
   * Notice that this method does not fire property change events for numTracks - this would slow down the GUI application too much. Use {@link #firenumTracksEvent()} after all titles have been added.
   * 
   * @param title
   */
  public void add(DetailedTrack title) {
    RegisteredTrack rtitle = title instanceof RegisteredTrack ? (RegisteredTrack) title : new RegisteredTrack(title);
    this.tracks.put(title.getId(), this.assignSharedStrings(rtitle));
  }

  private String getSharedString(String str) {
    String result;
    if (str != null) {
      result = this.sharedStrings.get(str);
      if (result == null) {
        this.sharedStrings.put(result, result);
        return str;
      } else {
        return result;
      }
    } else {
      return str;
    }
  }

  /**
   * Replaces artist / album / genre by shared strings to reduce memory usage
   * 
   * @param title
   * @return
   */
  private RegisteredTrack assignSharedStrings(RegisteredTrack title) {
    if (this.sharedStrings == null) {
      this.sharedStrings = new HashMap<String, String>();
    }
    title.setArtist(this.getSharedString(title.getArtist()));
    title.setAlbum(this.getSharedString(title.getAlbum()));
    title.setGenre(this.getSharedString(title.getGenre()));
    return title;
  }

  protected void fireNumTracksEvent() {
    this.firePropertyChange("numTracks", 0, this.tracks.size());
  }

  /**
   * Gets a title by its id
   * 
   * @param id
   * @return
   */
  public RegisteredTrack getTrack(int id) {
    return this.tracks.get(id);
  }

  /**
   * Gets all registered titles
   * 
   * @return titles
   */
  public List<RegisteredTrack> getAllTracks() {
    return new ArrayList<RegisteredTrack>(this.tracks.values());
  }

  public List<RegisteredTrack> search(String artist, String title) {
    ArrayList<RegisteredTrack> result = new ArrayList<RegisteredTrack>();
    for (RegisteredTrack t : this.tracks.values()) {
      if (t.matches(artist, title)) {
        result.add(t);
      } else if (t.getAliases() != null) {
        for (TrackAlias alias : t.getAliases()) {
          if (alias.matches(artist, title)) {
            result.add(t);
          }
        }
      }
    }
    return result;
  }

  public List<RegisteredTrack> search(String query) {
    query = query.toLowerCase();
    ArrayList<RegisteredTrack> result = new ArrayList<RegisteredTrack>();
    for (RegisteredTrack t : this.tracks.values()) {
      if (t.getArtist().toLowerCase().contains(query) || t.getTitle().toLowerCase().contains(query)) {
        result.add(t);
      }

    }
    return result;
  }

  public boolean isBlockChangsEvts() {
    return blockChangsEvts;
  }

  public void setBlockChangsEvts(boolean blockChangsEvts) {
    this.blockChangsEvts = blockChangsEvts;
    if (blockChangsEvts) {
      this.numTracksBeforeBlock = this.tracks.size();
    } else {
      this.firePropertyChange("numTracks", this.numTracksBeforeBlock, this.tracks.size());
    }
  }

  public void registerLegacyId(int legacyId, int id) {
    this.legacyIdMapping.put(legacyId, id);
    this.reverseLegacyIdMapping.put(id, legacyId);
  }

  private Map<Integer, Integer> readTrackMapping(InputStream stream) throws IOException {
    Map<Integer, Integer> map;

    BufferedInputStream bin = new BufferedInputStream(stream);
    DataInputStream in = new DataInputStream(bin);

    int cnt = in.readInt();
    map = new HashMap<Integer, Integer>(cnt);
    for (int i = 0; i < cnt; i++) {
      map.put(in.readInt(), in.readInt());
    }

    in.close();

    return map;

  }

  public Integer getLegacyId(int id) {
    return this.reverseLegacyIdMapping.get(id);
  }

  public Integer convertLegacyId(int legacyId) {
    Integer id = this.legacyIdMapping.get(legacyId);
    if (id == null && !fullLegacyIdMappingLoaded) {
      fullLegacyIdMappingLoaded = true;
      try {
        InputStream trackMappingStream = this.getClass().getClassLoader().getResourceAsStream("trackmapping");
        this.legacyIdMapping = readTrackMapping(trackMappingStream);
        id = this.legacyIdMapping.get(legacyId);
      } catch (Exception e) {
      }
    }

    return id;
  }

  public RegisteredTrack getByLegacyId(int legacyId) {
    Integer id = this.legacyIdMapping.get(legacyId);
    if (id != null) {
      return this.tracks.get(id);
    } else {
      return null;
    }
  }

  public DetailedTrack getStandardAdTrigger() {
    if(this.standardAdTrigger == null) {
      this.standardAdTrigger = this.getTrack(0);
      if(this.standardAdTrigger == null) {
        this.standardAdTrigger = new DetailedTrack();
        this.standardAdTrigger.setType(DetailedTrack.TYPE_JINGLE);
        this.standardAdTrigger.setArtist("START_AD_BREAK");
        this.standardAdTrigger.setTitle("START_AD_BREAK");
        this.standardAdTrigger.setAlbum("ad");
        this.standardAdTrigger.setLength(1);
        this.standardAdTrigger.setYear(2017);;
      }
    }
    return standardAdTrigger;
  }
}
