package de.stationadmin.base.playlist;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;

import com.thoughtworks.xstream.XStream;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.base.util.XStreamFactory;

/**
 * Playlist
 * 
 * @author Frank Korf
 */
public class Playlist extends AbstractBean {
  private static int nextGeneratedId = 1 << 20;

  private transient TrackRegistry trackRegistry;
  private transient boolean modified = false;
  private transient List<ChangeListener> changeListeners;

  private int id = -1;

  private String name;
  private String color;
  private transient String displayName;
  private String description;
  private boolean shuffle;
  private int length;
  private Date createdAt;
  private Date updatedAt;
  private ExtendedPlaylistData localData;
  private ArrayList<Entry> entries = new ArrayList<Entry>();
  private PlaylistType type = PlaylistType.ONLINE;

  private Object rawData;

  private String shuffleType;
  private Map<String, Object> shuffleOpts;

  private transient boolean metaDataModified = false;

  private transient ArrayList<Entry> workingEntries = new ArrayList<Entry>();
  private transient Map<Integer, Long> timestampMap = new HashMap<Integer, Long>();

  public Playlist(TrackRegistry titleRegistry) {
    this(titleRegistry, PlaylistType.ONLINE);
  }

  public Playlist(TrackRegistry titleRegistry, PlaylistType type) {
    super();
    this.trackRegistry = titleRegistry;
    this.type = type;
    if (type == PlaylistType.ONLINE) {
      this.id = -1;
    } else {
      this.id = generateId();
    }
  }

  /**
   * Checks if the given playlist id belongs to an online playlist.
   * 
   * @param id
   * @return
   */
  public static boolean isOnlinePlaylistId(int id) {
    return id < (1 << 20);
  }

  private static int generateId() {
    int next = nextGeneratedId;
    nextGeneratedId++;
    return next;
  }

  public void addChangeListener(ChangeListener changeListener) {
    if (this.changeListeners == null) {
      this.changeListeners = new ArrayList<ChangeListener>();
    }
    this.changeListeners.add(changeListener);
  }

  /**
   * Adds a title
   * 
   * @param title title
   */
  public void addTrack(BasicTrack title) {
    this.addTrack(title, null);
  }

  public void addTrack(BasicTrack title, Date addedAt) {
    Entry entry = null;

    if (this.workingEntries.size() == 0) {
      entry = this.createEntry(0, title, addedAt != null ? addedAt.getTime() : System.currentTimeMillis());
      this.workingEntries.add(entry);
    } else {
      Entry last = this.workingEntries.get(this.workingEntries.size() - 1);
      int start = last.getStart() + last.getTrack().getLength();
      entry = this.createEntry(start, title, addedAt != null ? addedAt.getTime() : System.currentTimeMillis());
      this.workingEntries.add(entry);
      this.setLength(start + title.getLength());
    }
    entry.track = this.register(title);
    this.fireChangeEvent(new ChangeEvent(this));
    this.setModified(true);
  }

  private Entry createEntry(int start, BasicTrack title, long addedAt) {
    RegisteredTrack regTitle = this.trackRegistry.getTrack(title.getId());
    Long timestamp = this.getTimestampMap().get(title.getId());
    return new Entry(start, title.getId(), regTitle != null ? regTitle : title, timestamp != null && timestamp.longValue() < addedAt ? timestamp : addedAt);
  }

  private BasicTrack register(BasicTrack track) {
    if (this.type != PlaylistType.TEMPORARY) {
      return this.trackRegistry.register(this, track);
    } else {
      return track;
    }
  }

  /**
   * Assigns Title objects to all entries. Necessary after loading 2.x playlists.
   */
  protected void resolveTitles() {
    for (Entry entry : this.entries) {
      if (entry.getTrack() == null) {
        entry.track = this.trackRegistry.getTrack(entry.getTrackId());
      }
    }
  }

  private void unregister(int titleId) {
    if (this.type != PlaylistType.TEMPORARY) {
      this.trackRegistry.unregister(this, titleId);
    }
  }

  /**
   * Commits all modifications. Next reset will fall back to the current state.
   */
  public void commit() {
    this.entries.clear();
    this.entries.addAll(this.workingEntries);
    this.trackRegistry.commit(this);
    this.timestampMap = null;
    this.setModified(false);
    this.metaDataModified = false;
  }

  public boolean containsTitle(int id) {
    for (Entry entry : this.entries) {
      if (entry.trackId == id) {
        return true;
      }
    }
    return false;
  }

  private void ensureLocalDataExists() {
    if (this.localData == null) {
      this.localData = new ExtendedPlaylistData(this.id);
    }
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof Playlist && (this.type == PlaylistType.ONLINE ? ((Playlist) obj).getId() == this.id : this == obj);
  }

  protected void fireChangeEvent(ChangeEvent evt) {
    if (this.changeListeners != null) {
      for (ChangeListener listener : this.changeListeners) {
        listener.stateChanged(evt);
      }
    }
  }

  /**
   * @return the color
   */
  public String getColor() {
    return color;
  }

  /**
   * Gets the description
   * 
   * @return
   */
  public String getDescription() {
    return description;
  }

  /**
   * Gets the name with HTML entities esacped
   * 
   * @return
   */
  public String getDisplayName() {
    if (this.displayName == null && this.name != null) {
      this.displayName = this.name;
      this.displayName = this.displayName.replaceAll("&amp;", "&");
    }
    return this.displayName;
  }

  /**
   * Gets the current entries of the playlist
   * 
   * @return
   */
  public List<Entry> getEntries() {
    return Collections.unmodifiableList(this.workingEntries);
  }

  public Entry getEntry(int num) {
    return this.workingEntries.get(num);
  }

  public String[] getGenerateAdvices() {
    return this.localData != null ? this.localData.getGenerateAdvices() : null;
  }

  public int getGenerateLength() {
    return this.localData != null ? this.localData.getGenerateLength() : 0;
  }

  public String getGeneratePushTag() {
    return this.localData != null ? this.localData.getGeneratePushTag() : null;
  }

  public String getGenerateTags() {
    return this.localData != null ? this.localData.getGenerateTags() : null;
  }

  /**
   * Gets the id of the playlist
   * 
   * @return
   */
  public int getId() {
    return id;
  }

  /**
   * Gets the length of the playlist in seconds
   * 
   * @return
   */
  public int getLength() {
    return length;
  }

  /**
   * @return the localData
   */
  protected ExtendedPlaylistData getLocalData() {
    return localData;
  }

  /**
   * Gets the name of the playlist
   * 
   * @return
   */
  public String getName() {
    return name;
  }

  /**
   * Gets the number of different artists used in this playlist
   * 
   * @return
   */
  public int getNumDifferentArtists() {
    HashSet<String> artists = new HashSet<String>();
    for (Entry entry : this.workingEntries) {
      BasicTrack track = this.trackRegistry.getTrack(entry.getTrackId());
      if (track != null && track.getArtist() != null) {
        artists.add(track.getArtist().toLowerCase().trim());
      }
    }
    return artists.size();
  }

  public List<String> getProperties() {
    List<String> properties = this.localData != null ? this.localData.export() : new ArrayList<String>();
    if (properties.size() == 0 && this.id >= 0) {
      properties.add("id = " + this.id);
    }
    properties.add("name = " + this.name);
    properties.add("type = " + this.type.name());
    if (StringUtils.isNotEmpty(this.description)) {
      properties.add("description = " + this.description);
    }
    properties.add("length = " + Integer.toString(this.length));
    if (StringUtils.isNotEmpty(this.color)) {
      properties.add("color = " + this.color);
    }
    properties.add("shuffle = " + Boolean.toString(this.shuffle));
    properties.add("createdAt = " + (this.createdAt != null ? this.createdAt.getTime() : new Date().getTime()));
    properties.add("updatedAt = " + (this.updatedAt != null ? this.updatedAt.getTime() : new Date().getTime()));
    AutoFillRule autoFillRule = this.localData != null ? this.localData.getAutoFillRule() : null;
    if (this.shuffleOpts != null || (autoFillRule != null && autoFillRule.isEnabled())) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        if (this.shuffleOpts != null) {
          properties.add("shuffleOpts = " + mapper.writeValueAsString(this.shuffleOpts));
        }
        if (autoFillRule != null && autoFillRule.isEnabled()) {
          properties.add("autoFill = " + mapper.writeValueAsString(autoFillRule));
        }
      } catch (Exception e) {
      }
    }
    if (this.shuffleType != null) {
      properties.add("shuffleType = " + shuffleType);

    }

    return properties;
  }

  public Set<String> getTags() {
    return this.localData != null ? new HashSet<String>(this.localData.getTags()) : new HashSet<String>();
  }

  public TrackRegistry getTrackRegistry() {
    return trackRegistry;
  }

  /**
   * @return the type
   */
  public PlaylistType getType() {
    return type != null ? this.type : PlaylistType.ONLINE;
  }

  @Override
  public int hashCode() {
    return this.id;
  }

  public int indexOf(int titleId) {
    return this.indexOf(titleId, 0);
  }

  public int indexOf(int titleId, int minIdx) {
    for (int i = minIdx; i < this.workingEntries.size(); i++) {
      if (this.workingEntries.get(i).getTrackId() == titleId) {
        return i;
      }
    }
    return -1;
  }

  /**
   * Inserts a title at the given position
   * 
   * @param position
   * @param title
   */
  public void insertTrack(int position, BasicTrack track) {
    if (position >= this.workingEntries.size()) {
      this.addTrack(track);
      return;
    }
    Entry entry = null;
    if (position == 0) {
      entry = this.createEntry(0, track, System.currentTimeMillis());
      this.workingEntries.add(0, entry);
    } else {
      Entry last = this.workingEntries.get(position - 1);
      int start = last.getStart() + last.getTrack().getLength();
      entry = this.createEntry(start, track, System.currentTimeMillis());
      this.workingEntries.add(position, entry);
    }
    entry.track = this.register(track);
    this.updateStartTimes(position);
    this.setLength(this.length + track.getLength());
    this.fireChangeEvent(new ChangeEvent(this));
    this.setModified(true);
  }

  public void insertTracks(int position, List<BasicTrack> tracks) {
    if (position >= this.workingEntries.size()) {
      for (BasicTrack track : tracks) {
        this.addTrack(track);
      }
      return;
    }
    int trackPos = position;
    int additionalLength = 0;
    for (BasicTrack track : tracks) {
      Entry entry = null;
      if (trackPos == 0) {
        entry = this.createEntry(0, track, System.currentTimeMillis());
        this.workingEntries.add(0, entry);
      } else {
        int start;
        if (trackPos < this.workingEntries.size()) {
          start = this.workingEntries.get(trackPos).getStart();
        } else {
          Entry last = this.workingEntries.get(this.workingEntries.size() - 1);
          start = last.getStart() + last.getTrack().getLength();
        }
        entry = this.createEntry(start, track, System.currentTimeMillis());
        this.workingEntries.add(trackPos, entry);
      }
      entry.track = this.register(track);
      additionalLength += track.getLength();
      trackPos++;
    }
    this.updateStartTimes(position);
    this.setLength(this.length + additionalLength);
    this.fireChangeEvent(new ChangeEvent(this));
    this.setModified(true);
  }

  public boolean isGenerate() {
    return StringUtils.isNotEmpty(this.getGenerateTags()) && this.getGenerateLength() > 0;
  }

  public String getComment() {
    return this.localData != null ? this.localData.getComment() : null;
  }

  public boolean isGenerateTagsAll() {
    return this.localData != null ? this.localData.isGenerateTagsAll() : false;
  }

  public boolean isGenerateMinimizeArtistRepeats() {
    return this.localData != null ? this.localData.isGenerateMinimizeArtistRepeats() : true;
  }

  public int getGenerateTitleRepeatLevel() {
    return this.localData != null ? this.localData.getGenerateTitleRepeatLevel() : -1;
  }

  public int getGenerateMaxArtistTitles() {
    return this.localData != null ? this.localData.getGenerateMaxArtistTitles() : 3;
  }

  public boolean isLocalShuffleAllowed() {
    return this.localData != null ? this.localData.isShuffleAllowed() : false;
  }

  public int getGenerateNewsInterval() {
    return this.localData != null ? this.localData.getNewsInterval() : 0;
  }

  public boolean getGenerateFirstJingleAfterNews() {
    return this.localData != null ? this.localData.isFirstJingleAfterNews() : false;
  }

  /**
   * Checks if the playlist was modified locally and not saved yet
   * 
   * @return <code>true</code> if modified
   */
  public boolean isModified() {
    return modified;
  }

  /**
   * Checks if the playlist is in shuffle mode
   * 
   * @return
   */
  public boolean isShuffle() {
    return shuffle;
  }

  /**
   * Checks if this playlist is tagged with the given tag
   * 
   * @param tag
   * @return <code>true</code> if tagged with given tag
   */
  public boolean isTaggedWith(String tag) {
    return this.localData != null ? this.localData.getTags().contains(tag) : false;
  }

  public void removeChangeListener(ChangeListener changeListener) {
    if (this.changeListeners != null) {
      this.changeListeners.remove(changeListener);
    }
  }

  private boolean isUsed(int titleId) {
    for (Entry entry : this.workingEntries) {
      if (entry.getTrackId() == titleId) {
        return true;
      }
    }
    return false;
  }

  public void removeEntries(List<Entry> entries) {
    int length = 0;
    int minIdx = 0;
    for (Entry entry : entries) {

      int idx = this.workingEntries.indexOf(entry);
      if (idx > -1) {
        minIdx = Math.min(idx, minIdx);
        this.workingEntries.remove(idx);
        BasicTrack track = entry.getTrack();
        length += track != null ? track.getLength() : 0;
        if (!isUsed(entry.getTrackId())) {
          this.unregister(entry.getTrackId());
        }
      }
    }
    if (length > 0) {
      this.updateStartTimes(minIdx - 1);
      this.setLength(this.length - length);
      this.fireChangeEvent(new ChangeEvent(this));
      this.setModified(true);
    }
  }

  public void removeEntry(Entry entry) {
    int idx = this.workingEntries.indexOf(entry);
    if (idx > -1) {
      this.workingEntries.remove(idx);
      BasicTrack title = this.trackRegistry.getTrack(entry.getTrackId());
      if (!isUsed(entry.getTrackId())) {
        this.unregister(entry.getTrackId());
      }
      this.setLength(this.length - title.getLength());
      this.updateStartTimes(idx - 1);
      this.fireChangeEvent(new ChangeEvent(this));
      this.setModified(true);
    }
  }

  public void removeTrack(int trackId) {
    ArrayList<Entry> entries = new ArrayList<Entry>(this.workingEntries);
    for (Entry entry : entries) {
      if (entry.getTrackId() == trackId) {
        this.removeEntry(entry);
      }
    }
  }

  /**
   * Resets all changes
   */
  public void reset() {
    this.workingEntries = new ArrayList<Entry>(this.entries);
    this.updateLength();
    if (modified) {
      this.trackRegistry.reset(this);
      this.setModified(false);
    }
    this.fireChangeEvent(new ChangeEvent(this));
  }

  /**
   * @param color the color to set
   */
  public void setColor(String color) {
    if (!StringUtils.equals(this.color, color)) {
      metaDataModified = true;
    }
    this.color = color;
  }

  public void setDescription(String description) {
    String old = this.description;
    this.description = description;
    if (!StringUtils.equals(old, description)) {
      metaDataModified = true;
    }
    getPcs().firePropertyChange("description", old, description);
  }

  public void setComment(String comment) {
    this.ensureLocalDataExists();
    String old = this.localData.getComment();
    this.localData.setComment(comment);
    this.firePropertyChange("comment", old, comment);
  }

  public void setGenerateLength(int hours) {
    this.ensureLocalDataExists();
    int old = this.localData.getGenerateLength();
    this.localData.setGenerateLength(hours);
    if (old != hours) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateLength", old, hours);
  }

  public void setGeneratePushTag(String tag) {
    this.ensureLocalDataExists();
    String old = this.localData.getGeneratePushTag();
    this.localData.setGeneratePushTag(tag);
    if (!StringUtils.equals(old, tag)) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generatePushTag", old, tag);
  }

  public void setGenerateTags(String titleTags) {
    this.ensureLocalDataExists();
    String old = this.localData.getGenerateTags();
    this.localData.setGenerateTags(titleTags);
    if (!StringUtils.equals(old, titleTags)) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateTags", old, titleTags);
  }

  public void setGenerateTagsAll(boolean all) {
    this.ensureLocalDataExists();
    boolean old = this.localData.isGenerateTagsAll();
    this.localData.setGenerateTagsAll(all);
    if (old != all) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateTagsAll", old, all);
  }

  public void setGenerateNewsInterval(int time) {
    this.ensureLocalDataExists();
    int old = this.localData.getNewsInterval();
    this.localData.setNewsInterval(time);
    if (old != time) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateNewsInterval", old, time);
  }

  public void setGenerateFirstJingleAfterNews(boolean firstJingleAfterNews) {
    this.ensureLocalDataExists();
    boolean old = this.localData.isFirstJingleAfterNews();
    this.localData.setFirstJingleAfterNews(firstJingleAfterNews);
    if (old != firstJingleAfterNews) {
      this.generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateFirstJingleAfterNews", old, firstJingleAfterNews);
  }

  /**
   * Configures if / which titles can be repeated when generating a playlist.
   * 
   * @param level -1 = no repeats (default), 0 = any title, 1 - 3 corrosponds to
   *        the push factor
   */
  public void setGenerateTitleRepeatLevel(int level) {
    this.ensureLocalDataExists();
    int old = this.localData.getGenerateTitleRepeatLevel();
    this.localData.setGenerateTitleRepeatLevel(level);
    if (old != level) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateTitleRepeatLevel", old, level);
  }

  public void setGenerateMaxArtistTitles(int max) {
    this.ensureLocalDataExists();
    int old = this.localData.getGenerateMaxArtistTitles();
    this.localData.setGenerateMaxArtistTitles(max);
    if (old != max) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateMaxArtistTitles", old, max);
  }

  /**
   * Sets advices for the generation of playlists
   * 
   * @param advices
   */
  public void setGenerateAdvices(String[] advices) {
    this.ensureLocalDataExists();
    String[] old = this.localData.getGenerateAdvices();
    this.localData.setGenerateAdvices(advices);
    this.generateSettingsToShuffleOpts();
    this.firePropertyChange("generateAdvices", old, advices);
  }

  /**
   * Specifies whether or not repeating of artists shall be minimized. If repeats
   * are minimized the generator will select titles from all other artists before
   * repeating an artist. If repeats are not minimized it will only select one
   * hour of music from other artists before an artist is repeated.
   * 
   * @param minimize <code>true</code> to minimize artist repeats (default)
   */
  public void setGenerateMinimizeArtistRepeats(boolean minimize) {
    this.ensureLocalDataExists();
    boolean old = this.localData.isGenerateMinimizeArtistRepeats();
    this.localData.setGenerateMinimizeArtistRepeats(minimize);
    if (old != minimize) {
      generateSettingsToShuffleOpts();
    }
    this.firePropertyChange("generateMinimizeArtistRepeats", old, minimize);
  }

  public void setId(int id) {
    this.id = id;
    if (this.localData != null && this.localData.getId() != id) {
      // id may have changed if we are working on a new playlist
      this.localData.setId(id);
    }
  }

  public void setLength(int length) {
    int old = this.length;
    this.length = length;
    getPcs().firePropertyChange("length", old, length);
  }

  /**
   * @param localData the localData to set
   */
  protected void setLocalData(ExtendedPlaylistData localData) {
    this.localData = localData;
  }

  public void setLocalDataFromXML(String xml) {
    XStream xstream = XStreamFactory.newXStream();
    xstream.alias("playlistcfg", ExtendedPlaylistData.class);
    this.localData = (ExtendedPlaylistData) xstream.fromXML(xml);
  }

  public void setLocalShuffleAllowed(boolean shuffleAllowed) {
    this.ensureLocalDataExists();
    boolean old = this.isLocalShuffleAllowed();
    this.localData.setShuffleAllowed(shuffleAllowed);
    this.firePropertyChange("localShuffleAllowed", old, shuffleAllowed);
    if (!shuffle && !shuffleAllowed) {
      localData.getAutoFillRule().setEnabled(false);
    }
  }

  /**
   * Marks the playlist as modified / unmodified
   * 
   * @param modified
   */
  public void setModified(boolean modified) {
    boolean old = this.modified;
    this.modified = modified;
    this.firePropertyChange("modified", old, modified);
  }

  public void setName(String name) {
    String old = this.name;
    this.name = name;
    if (!StringUtils.equals(old, name)) {
      metaDataModified = true;
    }
    this.displayName = null; // enforce reset
    getPcs().firePropertyChange("name", old, name);
  }

  public void setProperties(List<String> properties) {
    this.setProperties(properties, false);
  }

  @SuppressWarnings("unchecked")
  public void setProperties(List<String> properties, boolean preserveId) {
    Map<String, String> map = new HashMap<String, String>();
    for (String property : properties) {
      String[] data = StringUtils.split(property, "=", 2);
      if (data.length == 2) {
        map.put(data[0].trim(), data[1].trim());
      }
    }
    if (map.containsKey("id")) {
      int id = Integer.parseInt(map.get("id"));
      if (id < 1 << 20 && !preserveId) {
        this.id = id;
      } else {
        map.put("id", Integer.toString(this.id));
      }
    } else {
      map.put("id", Integer.toString(this.id));
    }
    this.localData = ExtendedPlaylistData.create(map);
    this.name = map.get("name");
    this.description = map.get("description");
    this.color = map.get("color");
    this.createdAt = map.containsKey("createdAt") ? new Date(Long.parseLong(map.get("createdAt"))) : null;
    this.updatedAt = map.containsKey("updatedAt") ? new Date(Long.parseLong(map.get("updatedAt"))) : null;
    if (map.containsKey("shuffle")) {
      this.shuffle = map.get("shuffle").equalsIgnoreCase("true");
    }
    if (map.containsKey("length")) {
      try {
        this.length = Integer.parseInt(map.get("length"));
      } catch (NumberFormatException e) {
      }
    }
    if (map.containsKey("type")) {
      this.type = PlaylistType.valueOf(map.get("type"));
    }
    if (map.containsKey("shuffleOpts")) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        Map<Object, Object> rawOpts = mapper.readValue(map.get("shuffleOpts"), Map.class);
        this.shuffleOpts = new HashMap<>();
        for (java.util.Map.Entry<Object, Object> entry : rawOpts.entrySet()) {
          shuffleOpts.put(entry.getKey().toString(), entry.getValue());
        }
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (map.containsKey("autoFill")) {
      try {
        ObjectMapper mapper = new ObjectMapper();
        this.localData.setAutoFillRule(mapper.readValue(map.get("autoFill"), AutoFillRule.class));
      } catch (Exception e) {
        e.printStackTrace();
      }
    }
    if (map.containsKey("shuffleType")) {
      this.shuffleType = map.get("shuffleType");
    }
  }

  public void setShuffle(boolean shuffle) {
    boolean old = this.shuffle;
    this.shuffle = shuffle;
    if (old != shuffle) {
      this.metaDataModified = true;
    }
    if (!shuffle && !isLocalShuffleAllowed()) {
      if (localData != null) {
        // turn auto fill rule off
        localData.getAutoFillRule().setEnabled(false);
      }
      // else: no auto fill rule yet => can't be enabled
    }
    getPcs().firePropertyChange("shuffle", old, shuffle);
  }

  public void setTags(Set<String> tags) {
    this.ensureLocalDataExists();
    Set<String> old = this.getTags();
    this.localData.setTags(tags);
    this.firePropertyChange("tags", old, tags);
  }

  public boolean isGvlCheck() {
    return this.localData != null ? localData.isGvlCheck() : true;
  }

  public void setGvlCheck(boolean gvlCheck) {
    this.ensureLocalDataExists();
    boolean old = this.localData.isGvlCheck();
    this.localData.setGvlCheck(gvlCheck);
    this.firePropertyChange("gvlCheck", old, gvlCheck);
  }

  public void setTrackRegistry(TrackRegistry titleRegistry) {
    this.trackRegistry = titleRegistry;
  }

  /**
   * Replaces the current playlist entries with the given titles
   * 
   * @param titles new titles
   */
  public void setTracks(List<BasicTrack> titles) {
    this.unregisterFromTitleRegistry(entries);
    int startTime = 0;
    this.workingEntries.clear();
    for (BasicTrack title : titles) {
      Entry entry = this.createEntry(startTime, title, System.currentTimeMillis());
      this.workingEntries.add(entry);
      if (this.type != PlaylistType.TEMPORARY) {
        this.trackRegistry.register(this, title);
      }
      startTime += title.getLength();
    }
    this.setLength(startTime);
    this.fireChangeEvent(new ChangeEvent(this));
    this.setModified(true);
  }

  /**
   * Sorts the entries by artist and title
   */
  public void sortByArtist() {
    Collections.sort(this.workingEntries, new Comparator<Entry>() {

      @Override
      public int compare(Entry o1, Entry o2) {
        BasicTrack t1 = trackRegistry.getTrack(o1.getTrackId());
        BasicTrack t2 = trackRegistry.getTrack(o2.getTrackId());

        int result = t1.getArtist().compareToIgnoreCase(t2.getArtist());
        if (result == 0) {
          result = t1.getTitle().compareToIgnoreCase(t2.getTitle());
        }

        return result;
      }

    });
    this.updateStartTimes(-1);
    this.setModified(true);
    this.fireChangeEvent(new ChangeEvent(this));

  }

  @Override
  public String toString() {
    return this.getDisplayName();
  }

  private void unregisterFromTitleRegistry(List<Entry> entries) {
    for (Entry entry : entries) {
      this.trackRegistry.unregister(this, entry.getTrackId());
    }
  }

  void updateLength() {
    if (this.workingEntries.size() > 0) {
      int len = 0;
      for (Entry entry : this.workingEntries) {
        BasicTrack title = this.trackRegistry.getTrack(entry.getTrackId());
        if (title != null) {
          len += title.getLength();
        }
      }
      this.setLength(len);
    }

  }

  void updateStartTimes(int first) {
    if (this.workingEntries.size() > 0) {
      Entry current = null;
      BasicTrack currentTitle = null;

      if (first >= 0) {
        current = this.workingEntries.get(first);
        currentTitle = this.trackRegistry.getTrack(current.getTrackId());
      } else {
        current = new Entry(0, 0, new BasicTrack());
        currentTitle = current.getTrack();
      }
      for (int i = first + 1; i < this.workingEntries.size(); i++) {
        int startTime = current.getStart() + currentTitle.getLength();
        current = this.workingEntries.get(i);
        current.setStart(startTime);
        currentTitle = this.trackRegistry.getTrack(current.getTrackId());
      }
    }
  }

  /**
   * Playlist entry
   */
  public static class Entry {
    private int start;
    private int trackId;
    private BasicTrack track;
    private long timestamp;

    public Entry(int start, int titleId, BasicTrack title) {
      this(start, titleId, title, System.currentTimeMillis());
    }

    public Entry(int start, int titleId, BasicTrack title, long added) {
      this.start = start;
      this.trackId = titleId;
      this.track = title;
      this.timestamp = added;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      return obj instanceof Entry && ((Entry) obj).trackId == this.trackId && ((Entry) obj).start == this.start;
    }

    public int getStart() {
      return start;
    }

    public int getTrackId() {
      return trackId;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return this.trackId ^ this.start;
    }

    public void setStart(int start) {
      this.start = start;
    }

    /**
     * @return the title
     */
    public BasicTrack getTrack() {
      return track;
    }

    public long getTimestamp() {
      return timestamp;
    }

    public void setTimestamp(long timestamp) {
      this.timestamp = timestamp;
    }
  }

  public enum PlaylistType {
    ONLINE(true, true, true), ARCHIVED(true, false, true), TEMPORARY(false, false, false);
    private boolean saveToDiskSupported;
    private boolean saveToServerSupported;
    private boolean deleteSupported;

    /**
     * @param saveToDiskSupported
     * @param saveToServerSupported
     * @param deleteSupported
     */
    private PlaylistType(boolean saveToDiskSupported, boolean saveToServerSupported, boolean deleteSupported) {
      this.saveToDiskSupported = saveToDiskSupported;
      this.saveToServerSupported = saveToServerSupported;
      this.deleteSupported = deleteSupported;
    }

    /**
     * @return the saveToDiskSupported
     */
    public boolean isSaveToDiskSupported() {
      return saveToDiskSupported;
    }

    /**
     * @return the saveToServerSupported
     */
    public boolean isSaveToServerSupported() {
      return saveToServerSupported;
    }

    /**
     * @return the deleteSupported
     */
    public boolean isDeleteSupported() {
      return deleteSupported;
    }
  }

  public Map<Integer, Long> getTimestampMap() {
    if (this.timestampMap == null) {
      this.timestampMap = new HashMap<Integer, Long>();
      for (Entry entry : this.entries) {
        Long previous = this.timestampMap.put(entry.getTrackId(), entry.getTimestamp());
        if (previous != null && previous.longValue() < entry.getTimestamp()) {
          // if multiple entries exist for the same title use the timestamp of
          // the oldest entry
          this.timestampMap.put(entry.getTrackId(), previous);
        }
      }
    }
    return timestampMap;
  }

  public void setTimestampMap(Map<Integer, Long> timestampMap) {
    this.timestampMap = timestampMap;
  }

  public Date getCreatedAt() {
    return createdAt;
  }

  public void setCreatedAt(Date createdAt) {
    Date old = this.createdAt;
    this.createdAt = createdAt;
    this.firePropertyChange("createdAt", old, createdAt);
  }

  public Date getUpdatedAt() {
    return updatedAt;
  }

  public void setUpdatedAt(Date updatedAt) {
    Date old = this.updatedAt;
    this.updatedAt = updatedAt;
    this.firePropertyChange("updatedAt", old, updatedAt);
  }

  public boolean isMetaDataModified() {
    return metaDataModified;
  }

  public Map<String, Object> getShuffleOpts() {
    return shuffleOpts;
  }

  private int getShuffleIntSetting(String key) {
    return this.shuffleOpts.containsKey(key) ? (Integer) shuffleOpts.get(key) : 0;
  }

  private boolean getShuffleBooleanSetting(String key) {
    return this.shuffleOpts.containsKey(key) ? (Boolean) shuffleOpts.get(key) : false;
  }

  public void setShuffleOpts(Map<String, Object> shuffleOpts) {
    if (this.shuffleOpts == null || !this.shuffleOpts.equals(shuffleOpts)) {
      this.shuffleOpts = shuffleOpts;
      this.metaDataModified = true;
      if (shuffleOpts != null && shuffleOpts.containsKey("type") && shuffleOpts.get("type").equals("generate")) {
        shuffleOptsToGenerateSettings();
        localData.setGenerateTags((String) this.shuffleOpts.get("tags"));
        localData.setGeneratePushTag((String) this.shuffleOpts.get("pushTags"));
        localData.setGenerateLength(getShuffleIntSetting("length"));
        localData.setGenerateMaxArtistTitles(getShuffleIntSetting("maxArtistTracks"));
        localData.setGenerateTitleRepeatLevel(getShuffleIntSetting("trackRepeatLevel"));
        localData.setNewsInterval(getShuffleIntSetting("newsInterval"));
        localData.setFirstJingleAfterNews(getShuffleBooleanSetting("firstJingleAfterNews"));
        localData.setGenerateMinimizeArtistRepeats(getShuffleBooleanSetting("minimizeArtistRepeats"));
        localData.setGenerateTagsAll(getShuffleBooleanSetting("tagsAll"));
        int i = 0;
        ArrayList<String> advices = new ArrayList<>();
        while (shuffleOpts.containsKey("advice." + i)) {
          advices.add((String) shuffleOpts.get("advice." + i));
          i++;
        }
        localData.setGenerateAdvices(advices.toArray(new String[advices.size()]));
      }
    }
  }

  private void shuffleOptsToGenerateSettings() {
    ensureLocalDataExists();

  }

  private void generateSettingsToShuffleOpts() {
    if (!shuffle && localData != null && localData.getGenerateTags() != null) {
      HashMap<String, Object> opts = new HashMap<>();
      opts.put("type", "generate");
      opts.put("tags", localData.getGenerateTags());
      opts.put("pushTags", localData.getGeneratePushTag() != null ? localData.getGeneratePushTag() : "");
      opts.put("length", localData.getGenerateLength());
      opts.put("maxArtistTracks", localData.getGenerateMaxArtistTitles());
      opts.put("trackRepeatLevel", localData.getGenerateTitleRepeatLevel());
      opts.put("newsInterval", localData.getNewsInterval());
      opts.put("firstJingleAfterNews", localData.isFirstJingleAfterNews());
      opts.put("minimizeArtistRepeats", localData.isGenerateMinimizeArtistRepeats());
      opts.put("tagsAll", localData.isGenerateTagsAll());
      if (localData.getGenerateAdvices() != null) {
        for (int i = 0; i < localData.getGenerateAdvices().length; i++) {
          opts.put("advice." + i, localData.getGenerateAdvices()[i]);
        }
      }

      this.shuffleOpts = opts;
      this.metaDataModified = true;
    }
  }

  public boolean isShuffleTypeSet() {
    return this.shuffleType != null;
  }

  public String getShuffleType() {
    return shuffleType != null ? shuffleType : "basic_v1";
  }

  public void setShuffleType(String shuffleType) {
    if (!org.apache.commons.lang3.StringUtils.equals(shuffleType, this.shuffleType)) {
      String old = this.shuffleType;
      this.shuffleType = shuffleType;
      this.metaDataModified = true;
      this.firePropertyChange("shuffleType", old, shuffleType);
    }
  }

  public Object getRawData() {
    return rawData;
  }

  public void setRawData(Object rawData) {
    this.rawData = rawData;
  }

  public AutoFillRule getAutoFillRule() {
    this.ensureLocalDataExists();
    return this.localData.getAutoFillRule();
  }

  public void setAutoFillRule(AutoFillRule autoFillRule) {
    this.ensureLocalDataExists();
    AutoFillRule old = this.localData.getAutoFillRule();
    this.localData.setAutoFillRule(autoFillRule);
    this.firePropertyChange("autoFillRule", old, autoFillRule);
  }
}
