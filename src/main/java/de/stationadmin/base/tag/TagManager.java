/**
 * 
 */
package de.stationadmin.base.tag;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.loganalyzer.Play;
import de.stationadmin.base.loganalyzer.PlayFilter;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.Track;

/**
 * Manages title tags
 * 
 * @author Frank Korf
 */
public class TagManager extends AbstractBean implements Service, TagChecker {
  /** pseudo tag: used titles */
  public static final String USED_TITLES = "#USED#";
  /** pseudo tag: unused titles */
  public static final String UNUSED_TITLES = "#UNUSED#";

  private TrackService trackService;
  private TrackRegistry trackRegistry;
  private LogAnalyzerService logAnalyzer;
  private SessionCtx ctx;
  private Map<String, StaticTag> staticTags = new HashMap<String, StaticTag>();
  private Map<String, DynamicTag> dynamicTags = new HashMap<String, DynamicTag>();
  private Map<String, TagSet> tagSets = new HashMap<String, TagSet>();

  private String currentTagSetName;

  private String playsCacheKey;
  private Set<Integer> playsCache;
  private String playlistsCacheKey;
  private Set<Integer> playlistsCache;
  private Schedule schedule;
  private PlaylistRegistry playlistRegistry;

  public TagManager(SessionCtx ctx, TrackService titleService, PlaylistRegistry playlistRegistry, LogAnalyzerService logAnalyzer, Schedule schedule) {
    this.trackRegistry = titleService.getTrackRegistry();
    this.trackService = titleService;
    this.logAnalyzer = logAnalyzer;
    this.schedule = schedule;
    this.playlistRegistry = playlistRegistry;
    this.ctx = ctx;
  }

  /**
   * Adds a new static tag
   * 
   * @param tag
   * @throws IOException
   */
  public StaticTag addStaticTag(String tag) throws IOException {
    this.getTagFile(tag, true);
    return this.staticTags.get(tag.toLowerCase());
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  /**
   * Deletes the tag with the given name completely - all tagging information
   * gets lost
   * 
   * @param tag
   */
  public void deleteTag(String tag) throws IOException {
    TagFile tagFile = this.getTagFile(tag, false);
    boolean tracksDirty = false;
    if (tagFile != null) {
      int[] ids = tagFile.getIds();
      this.ctx.getServer().deleteTag(ctx.getStationId(), tag);
      tagFile.delete();
      this.staticTags.remove(tag.toLowerCase());
      this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());

      for (int id : ids) {
        RegisteredTrack track = this.trackRegistry.getTrack(id);
        if (track != null) {
          track.tagCountDec();
          if (track.getTagCnt() == 0 && track.isUnused()) {
            this.trackRegistry.remove(track.getId());
            tracksDirty = true;
          }
        }
      }

      if (tracksDirty) {
        this.trackService.saveTracks();
      }

    } else if (this.dynamicTags.containsKey(tag.toLowerCase())) {
      DynamicTag dtag = this.dynamicTags.get(tag.toLowerCase());
      dtag.delete();
      this.dynamicTags.remove(tag.toLowerCase());
      this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
    }
  }

  public void deleteTagSet(TagSet tagSet) throws IOException {
    if (tagSet.getFilename() != null) {
      new File(tagSet.getFilename()).delete();
    }
    this.tagSets.remove(tagSet.getRegisteredName().toLowerCase());
    this.firePropertyChange("tagSets", new ArrayList<TagSet>(0), this.getTagSets());

  }

  public List<DynamicTag> getDynamicTags() {
    return new ArrayList<DynamicTag>(this.dynamicTags.values());
  }

  /**
   * Gets the file names of the underlying .tag files
   * 
   * @return
   */
  public List<File> getFiles() {
    ArrayList<File> files = new ArrayList<File>();
    for (StaticTag tag : this.staticTags.values()) {
      files.add(new File(tag.getTagFile().getFilename()));
    }
    return files;
  }

  public List<StaticTag> getStaticTags() {
    return new ArrayList<StaticTag>(this.staticTags.values());
  }

  public Tag getTag(String name) {
    Tag tag = this.staticTags.get(name.toLowerCase());
    return tag != null ? tag : this.dynamicTags.get(name.toLowerCase());
  }

  private TagFile getTagFile(String tagname, boolean create) throws IOException {
    StaticTag tag = this.staticTags.get(tagname.toLowerCase());
    if (tag == null && create) {
      TagFile tagFile = new TagFile(this.ctx.getStationDirectory() + File.separatorChar + tagname.toLowerCase() + ".tag", tagname);
      this.staticTags.put(tagname.toLowerCase(), new StaticTag(tagFile));
      this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
      return tagFile;
    }
    return tag != null ? tag.getTagFile() : null;
  }

  public List<Tag> getTagObjects() {
    List<Tag> tags = new ArrayList<Tag>(this.staticTags.values());
    tags.addAll(this.dynamicTags.values());
    Collections.sort(tags);
    return tags;
  }

  /**
   * Gets a list of used tags
   * 
   * @return
   */
  public List<String> getTags() {
    ArrayList<String> tags = new ArrayList<String>();
    for (Tag tag : this.staticTags.values()) {
      tags.add(tag.getName());
    }
    for (Tag tag : this.dynamicTags.values()) {
      tags.add(tag.getName());
    }
    Collections.sort(tags);
    return tags;
  }

  /**
   * Gets all groups that are refered in the tags
   * 
   * @return
   */
  public List<String> getGroups() {
    return this.getGroups(false);
  }

  /**
   * Gets all groups that are refered in the tags
   * 
   * @return
   */
  public List<String> getGroups(boolean staticOnly) {
    ArrayList<String> groups = new ArrayList<String>();
    for (Tag tag : this.getTagObjects()) {
      if (tag.getGroup() != null && !groups.contains(tag.getGroup())) {
        if (!staticOnly || tag instanceof StaticTag) {
          groups.add(tag.getGroup());
        }
      }
    }
    Collections.sort(groups);
    return groups;
  }

  /**
   * Gets the list of available tag sets
   * 
   * @return
   */
  public List<TagSet> getTagSets() {
    return new ArrayList<TagSet>(this.tagSets.values());
  }

  public TagSet getCurrentTagSet() {
    return this.currentTagSetName != null ? this.tagSets.get(this.currentTagSetName.toLowerCase()) : null;
  }

  public int[] getTrackIds(String tagname) throws IOException {
    TagFile tagFile = this.getTagFile(tagname, false);
    if (tagFile != null) {
      return tagFile.getIds();
    } else {
      DynamicTag tag = this.dynamicTags.get(tagname.toLowerCase());
      if (tag != null) {
        Set<Integer> plays = null;
        Set<Integer> playlists = null;
        Set<Integer> tags = null;
        if (tag.getPlayedWithin() > 0) {
          plays = this.getPlaysWithin(tag.getPlayedWithin(), tag.getPlayedWithinMinHour(), tag.getPlayedWithinMaxHour(),
              tag.getPlayedWithinPlaylist());
        }
        if (tag.getPlaylistIds() != null) {
          playlists = this.getTracksOfPlaylists(tag.getPlaylistIds());
        }
        if (tag.getTags() != null) {
          tags = this.getTracksOfTags(tag.getTags());
        }

        ArrayList<Integer> ids = new ArrayList<Integer>();
        for (BasicTrack title : this.trackRegistry.getAllTracks()) {
          if (tag.contains(title) && (plays == null || plays.contains(title.getId())) && (playlists == null || playlists.contains(title.getId()))
              && (tags == null || tags.contains(title.getId()))) {
            ids.add(title.getId());
          }
        }
        return toArray(ids);
      } else {
        if (tagname.equals(USED_TITLES)) {
          ArrayList<Integer> ids = new ArrayList<Integer>();
          for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
            if (title.getPlaylistIds().size() > 0) {
              ids.add(title.getId());
            }
          }
          return toArray(ids);
        }
        if (tagname.equals(UNUSED_TITLES)) {
          ArrayList<Integer> ids = new ArrayList<Integer>();
          for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
            if (title.getPlaylistIds().size() == 0) {
              ids.add(title.getId());
            }
          }
          return toArray(ids);
        }
      }

    }
    return null;
  }

  /**
   * Gets a bit set with the title ids that match the given tag set
   * 
   * @param tagSet
   * @return
   * @throws IOException
   */
  public BitSet getTrackIds(TagSet tagSet) throws IOException {
    BitSet trackIds = new BitSet();

    // mark everything that is included
    if (tagSet.getIncludeTags() != null && tagSet.getIncludeTags().length > 0) {
      for (String tagName : tagSet.getIncludeTags()) {
        int[] ids = this.getTrackIds(tagName);
        if (ids != null) {
          for (int id : ids) {
            trackIds.set(id);
          }
        }
      }
    } else {
      for (BasicTrack t : this.trackRegistry.getAllTracks()) {
        trackIds.set(t.getId());
      }
    }

    // mark everything that is excluded
    if (tagSet.getExcludeTags() != null) {
      for (String tagName : tagSet.getExcludeTags()) {
        int[] ids = this.getTrackIds(tagName);
        if (ids != null) {
          for (int id : ids) {
            trackIds.clear(id);
          }
        }
      }
    }

    return trackIds;
  }

  private static int[] toArray(List<Integer> ids) {
    int[] idArray = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      idArray[i] = ids.get(i);
    }
    Arrays.sort(idArray);
    return idArray;
  }

  private Set<Integer> getPlaysWithin(int hours, int minHour, int maxHour, int playlistId) throws IOException {
    String key = new SimpleDateFormat("HH:mm").format(new Date()) + "-" + hours;
    if (StringUtils.equals(this.playsCacheKey, key)) {
      return this.playsCache;
    }

    Set<Integer> set = new HashSet<Integer>();
    int ms = hours * 60 * 60 * 1000;
    long minTime = System.currentTimeMillis() - ms;

    PlayFilter filter = null;

    Playlist playlist = playlistId > -1 ? this.playlistRegistry.getPlaylist(playlistId) : null;
    if (playlist != null) {
      filter = new PlayFilter();
      filter.setSchedule(schedule);
      filter.setPlaylist(playlist);
    }

    Calendar cal = Calendar.getInstance();
    for (Play play : this.logAnalyzer.getPlaysBetween(new Date(minTime), new Date())) {
      boolean accept = true;
      cal.setTime(play.getStartTime());
      accept = accept && (minHour == -1 || cal.get(Calendar.HOUR_OF_DAY) >= minHour);
      accept = accept && (maxHour == -1 || cal.get(Calendar.HOUR_OF_DAY) < maxHour);
      accept = accept && (filter == null || filter.accept(play));

      if (accept) {
        set.add(play.getTrack().getId());
      }
    }
    playsCacheKey = key;
    playsCache = set;
    return set;

  }

  private Set<Integer> getTracksOfTags(String[] tags) throws IOException {
    Set<Integer> set = new HashSet<Integer>();
    for (String tag : tags) {
      TagFile tagFile = this.getTagFile(tag, false);
      if (tagFile != null) {
        int[] ids = tagFile.getIds();
        for (int id : ids) {
          set.add(id);
        }
      }
    }
    return set;
  }

  private Set<Integer> getTracksOfPlaylists(int[] ids) {
    if (this.playlistsCacheKey != null && playlistsCacheKey.equals(ArrayUtils.toString(ids))) {
      return this.playlistsCache;
    }
    Set<Integer> trackIds = new HashSet<Integer>();
    for (int id : ids) {
      Playlist playlist = this.playlistRegistry.getPlaylist(id);
      if (playlist != null) {
        for (Playlist.Entry entry : playlist.getEntries()) {
          trackIds.add(entry.getTrackId());
        }
      }
    }

    this.playlistsCache = trackIds;
    this.playlistsCacheKey = ArrayUtils.toString(ids);

    return trackIds;
  }

  /**
   * Checks if the title with the given id is tagged with the given tag
   * 
   * @param tag
   * @param titleId
   * @return
   * @throws IOException
   */
  public boolean isTagged(String tag, int titleId) throws IOException {
    TagFile tagFile = this.getTagFile(tag, false);
    if (tagFile != null) {
      return tagFile.isTagged(titleId);
    } else {
      DynamicTag dtag = this.dynamicTags.get(tag.toLowerCase());
      if (dtag != null) {
        boolean match = dtag.contains(this.trackRegistry.getTrack(titleId));
        if (match && dtag.getPlayedWithin() > 0) {
          match = this
              .getPlaysWithin(dtag.getPlayedWithin(), dtag.getPlayedWithinMinHour(), dtag.getPlayedWithinMaxHour(), dtag.getPlayedWithinPlaylist())
              .contains(titleId);
        }
        if (match && dtag.getPlaylistIds() != null) {
          match = this.getTracksOfPlaylists(dtag.getPlaylistIds()).contains(titleId);
        }
        if (match && dtag.getTags() != null) {
          match = this.getTracksOfTags(dtag.getTags()).contains(titleId);
        }

        return match;
      }
    }
    return false;
  }

  public void load() {
    this.ctx.updateStatus("loadTags");
    // read existing tags
    File fdir = new File(ctx.getStationDirectory());
    File[] files = fdir.listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.getAbsolutePath().endsWith(".tag")) {
          try {
            TagFile tagFile = new TagFile(file.getAbsolutePath());
            this.staticTags.put(tagFile.getTagname().toLowerCase(), new StaticTag(tagFile));
            for (int id : tagFile.getIds()) {
              RegisteredTrack t = this.trackRegistry.getTrack(id);
              if (t != null) {
                t.tagCountInc();
              }
            }
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (file.getAbsolutePath().endsWith(".dtag")) {
          try {
            DynamicTag tag = new DynamicTag();
            tag.setFilename(file.getAbsolutePath());
            tag.load();
            tag.setRegisteredName(tag.getName());
            this.dynamicTags.put(tag.getName().toLowerCase(), tag);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
        if (file.getAbsolutePath().endsWith(".tagset")) {
          try {
            TagSet tag = new TagSet();
            tag.setFilename(file.getAbsolutePath());
            tag.load();
            tag.setRegisteredName(tag.getName());
            this.tagSets.put(tag.getName().toLowerCase(), tag);
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }
    }
    this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
    this.firePropertyChange("tagSets", new ArrayList<String>(0), this.getTagSets());
  }

  public void saveDynamicTag(DynamicTag tag) throws IOException {
    if (tag.getFilename() == null) {
      String name = tag.getName().toLowerCase() + ".dtag";
      tag.setFilename(FilenameUtils.concat(this.ctx.getStationDirectory(), name));
    }
    tag.save();
    if (tag.getRegisteredName() != null) {
      this.dynamicTags.remove(tag.getRegisteredName().toLowerCase());
    }
    tag.setRegisteredName(tag.getName());
    this.dynamicTags.put(tag.getRegisteredName().toLowerCase(), tag);
    this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
  }

  public void saveTagSet(TagSet tagSet) throws IOException {
    if (tagSet.getFilename() == null) {
      String name = tagSet.getName().toLowerCase() + ".tagset";
      tagSet.setFilename(FilenameUtils.concat(this.ctx.getStationDirectory(), name));
    }
    tagSet.save();
    if (tagSet.getRegisteredName() != null) {
      this.tagSets.remove(tagSet.getRegisteredName().toLowerCase());
    }
    tagSet.setRegisteredName(tagSet.getName());
    this.tagSets.put(tagSet.getRegisteredName().toLowerCase(), tagSet);
    this.firePropertyChange("tagSets", new ArrayList<String>(0), this.getTagSets());
  }

  public void saveStaticTag(StaticTag tag) throws IOException {
    if (tag.getTagFile() == null) {
      TagFile tagFile = this.getTagFile(tag.getName(), true);
      tag.setTagFile(tagFile);
      if (!StringUtils.equals(tagFile.getGroup(), tag.getGroup())) {
        tagFile.setGroup(tag.getGroup());
        tag.getTagFile().writeHeader();
      }
      this.staticTags.put(tag.getName().toLowerCase(), tag);
    } else if (!tag.getTagFile().getTagname().equals(tag.getName())) {
      this.staticTags.remove(tag.getTagFile().getTagname().toLowerCase());
      tag.getTagFile().setTagname(tag.getName());
      if (!StringUtils.equals(tag.getTagFile().getGroup(), tag.getGroup())) {
        tag.getTagFile().setGroup(tag.getGroup());
        tag.getTagFile().writeHeader();
      }
      this.staticTags.put(tag.getName().toLowerCase(), tag);
      this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
    } else if (!StringUtils.equals(tag.getTagFile().getGroup(), tag.getGroup())) {
      tag.getTagFile().setGroup(tag.getGroup());
      tag.getTagFile().writeHeader();
      // fire a pseudo event to enforce a rebuild of the tag menu
      this.firePropertyChange("tags", new ArrayList<String>(0), this.getTags());
    }
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
    // TODO Auto-generated method stub

  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#synchronize()
   */
  @Override
  public void synchronize() throws IOException {
    Set<String> oldTags = new HashSet<String>(this.staticTags.keySet());

    for (String tag : ctx.getServer().getTags(ctx.getStationId())) {
      this.ctx.updateStatus("getTag", tag);
      
      
      oldTags.remove(tag.toLowerCase());

      int[] ids = this.ctx.getServer().getTaggedTracks(ctx.getStationId(), tag);
      Tag tagObj = this.getTag(tag);
      StaticTag sTag = null;
      if (tagObj == null) {
        sTag = new StaticTag();
        sTag.setName(tag);
        this.saveStaticTag(sTag);
      } else if (tagObj instanceof StaticTag) {
        sTag = (StaticTag) tagObj;
      }
      if (sTag != null) {

        // unregister tag in all old tags
        for (int id : getTagFile(tag, true).getIds()) {
          RegisteredTrack t = this.trackRegistry.getTrack(id);
          if (t != null) {
            t.tagCountDec();
          }
        }

        int[] missing = new int[ids.length];
        int missingIdx = 0;
        this.getTagFile(tag, true).set(ids);
        for (int id : ids) {
          RegisteredTrack t = this.trackRegistry.getTrack(id);
          if (t != null) {
            // register tag
            t.tagCountInc();
          } else {
            // mark as missing - will be loaded below
            missing[missingIdx++] = id;
          }
        }

        if (missingIdx > 0) {
          // retrieve missing tracks
          for (Track track : this.ctx.getServer().getTracks(ctx.getStationId(), missing)) {
            if (trackRegistry.getTrack(track.getId()) == null) {
              RegisteredTrack t = new RegisteredTrack(track);
              t.tagCountInc();
              this.trackRegistry.add(t);
            }
          }
        }

      }
    }

    // delete tags that do not exist anymore
    for (String tag : oldTags) {
      this.deleteTag(tag);
    }

  }

  /**
   * Tags the titles with the given ids with the given tag
   * 
   * @param tag
   * @param trackIds
   * @throws IOException
   */
  public void tagTracks(String tag, int... trackIds) throws IOException {
    Map<Integer, Track> tracks = this.ctx.getServer().tagTracks(ctx.getStationId(), tag, trackIds);
    trackIds = this.getTagFile(tag, true).tag(trackIds);
    this.updateRegisteredTracks(tracks, trackIds);
  }
  
  private void updateRegisteredTracks(Map<Integer, Track> tracks, int[] trackIds) throws IOException {
    boolean trackDirty = false;
    for (int id : trackIds) {
      RegisteredTrack t = this.trackRegistry.getTrack(id);
      if (t != null) {
        t.tagCountInc();
        t.update(tracks.get(id));
      } else if (tracks.containsKey(id)) {
        RegisteredTrack title = new RegisteredTrack(tracks.get(id));
        title.tagCountInc();
        this.trackRegistry.register(title);
        trackDirty = true;
      }
    }
    if (trackDirty) {
      this.trackService.saveTracks();
    }
    
  }
  
  /**
   * Tags tracks on server based on the content of the local file - used during backup
   * @param tag
   * @throws IOException
   */
  public void updateTagOnServer(String tag) throws IOException {
    this.ctx.getServer().deleteTag(ctx.getStationId(), tag);
    int[] trackIds = this.getTagFile(tag, true).getIds();
    Map<Integer, Track> tracks = this.ctx.getServer().tagTracks(ctx.getStationId(), tag, trackIds);
    this.updateRegisteredTracks(tracks, trackIds);
  }

  /**
   * Untags the titles with the given ids with the given tag
   * 
   * @param tag
   * @param trackIds
   * @throws IOException
   */
  public void untagTracks(String tag, int... trackIds) throws IOException {
    TagFile tagFile = this.getTagFile(tag, false);
    boolean tracksDirtry = false;
    if (tagFile != null) {
      this.ctx.getServer().untagTracks(ctx.getStationId(), tag, trackIds);
      for (int id : tagFile.untag(trackIds)) {
        RegisteredTrack t = this.trackRegistry.getTrack(id);
        if (t != null) {
          t.tagCountDec();
          if (t.getTagCnt() == 0 && t.isUnused()) {
            this.trackRegistry.remove(t.getId());
            tracksDirtry = true;
          }
        }
      }
    }
    if (tracksDirtry) {
      this.trackService.saveTracks();
    }

  }

  /**
   * Should be called if tracks are deleted - removes the tracks from the static tag file
   * @param trackIds
   * @throws IOException
   */
  public void onTracksDelete(int... trackIds) throws IOException {
    for (StaticTag tag : this.staticTags.values()) {
      TagFile file = tag.getTagFile();

      // pre-check if tag really contains any of the track ids to avoid
      // unnecessary untag operation
      boolean modify = false;
      for (int i = 0; i < trackIds.length && !modify; i++) {
        if (file.isTagged(trackIds[i])) {
          modify = true;
        }
      }

      if (modify) {
        // at least one id contained - do untag in file
        file.untag(trackIds);
      }

    }

  }

  public String getCurrentTagSetName() {
    return currentTagSetName;
  }

  public void setCurrentTagSetName(String currentTitleTagSetName) {
    this.currentTagSetName = currentTitleTagSetName;
  }

}
