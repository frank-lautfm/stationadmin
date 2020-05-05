/**
 * 
 */
package de.stationadmin.base.playlist;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.config.ClientConfiguration;
import de.stationadmin.base.config.ClientConfigurationSource;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.exporter.PlaylistBackupExporter;
import de.stationadmin.base.playlist.profile.AdTriggerCfg;
import de.stationadmin.base.playlist.profile.ArtistNormalizationCfg;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.profile.TrackRuleCfg;
import de.stationadmin.base.playlist.shuffle.PlaylistProfileType;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.playlist.validation.PlaylistValidationException.Reason;
import de.stationadmin.base.playlist.validation.PlaylistValidator;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.CurrentPlaylist;
import de.stationadmin.lfm.backend.ExtendedPlaylistHead;
import de.stationadmin.lfm.backend.PlaylistHead;
import de.stationadmin.lfm.backend.ResourceNotFoundException;
import de.stationadmin.lfm.backend.Track;
import de.stationadmin.lfm.backend.TrackRef;

/**
 * @author Frank
 * 
 */
public class PlaylistService extends AbstractBean implements Service, ClientConfigurationSource, PlaylistProfileRegistry {
  public static final int MAX_TRACKS = 10000;

  private static final Logger log = Logger.getLogger(PlaylistService.class);
  private static final Pattern shuffleKeyPattern = Pattern.compile("key.\\s*([\\w|_]+)", Pattern.MULTILINE | Pattern.DOTALL);

  public static final String SHUFFLE_CLASSIC = "basic";
  public static final String SHUFFLE_BUCKET = "bucket";
  public static final String SHUFFLE_STATIONADMIN = "StationAdmin";
  public static final String SHUFFLE_BLOCKSELECT = "BlockSelect";

  private SessionCtx ctx;
  private TrackRegistry trackRegistry;
  private PlaylistRegistry playlistRegistry;
  private String dir;
  private String dirArchive;
  private PlaylistValidator playlistValidator;
  private PlaylistModificationDetector playlistModificationDetector;
  private List<ShuffleScriptMeta> shuffleScripts = new ArrayList<>();

  private List<PlaylistProfile> profiles = new ArrayList<>();
  private Settings settings;

  /**
   * @param ctx
   * @param titleRegistry
   * @param playlistRegistry
   */
  public PlaylistService(SessionCtx ctx, TrackRegistry titleRegistry, PlaylistRegistry playlistRegistry, Settings settings) {
    super();
    this.ctx = ctx;
    this.trackRegistry = titleRegistry;
    this.playlistRegistry = playlistRegistry;
    this.dir = ctx.getStationDirectory() + "playlists" + File.separatorChar;
    this.dirArchive = dir + "archive" + File.separatorChar;
    new File(this.dir).mkdirs();
    new File(this.dirArchive).mkdirs();

    this.settings = settings;

    this.playlistModificationDetector = new PlaylistModificationDetector(ctx, this.playlistRegistry);
  }

  public void deletePlaylist(Playlist playlist) throws IOException {
    if (!playlist.getType().isDeleteSupported()) {
      throw new IllegalArgumentException("Playlists of type " + playlist.getType() + " cannot be deleted");
    }

    if (playlist.getType() == PlaylistType.ONLINE && playlist.getId() > -1) {
      File file = new File(this.dir + File.separatorChar + playlist.getId() + ".lfm");
      if (!file.delete()) {
        throw new IOException("Unable to delete " + file);
      }
      try {
        this.ctx.getServer().deletePlaylist(ctx.getStationId(), playlist.getId());
      } catch (ResourceNotFoundException e) {
        // playlist was already deleted - ignore silently
      }
    } else {
      File file = new File(this.dirArchive + File.separatorChar + playlist.getFileName() + ".lfm");
      if (file.exists()) {
        if (!file.delete()) {
          throw new IOException("Unable to delete " + file);
        }
      }
    }

    // remove all entries to unregister them from title registry
    List<Entry> entries = new ArrayList<Playlist.Entry>(playlist.getEntries());
    playlist.removeEntries(entries);
    playlist.commit();
    // unregister from playlist registry
    this.playlistRegistry.unregister(playlist);
  }

  public Playlist getCurrentPlaylist() throws IOException {
    CurrentPlaylist source = this.ctx.getServer().getCurrentPlaylist(this.ctx.getStationId());

    Playlist playlist = new Playlist(this.trackRegistry, PlaylistType.TEMPORARY);
    playlist.setName(source.getPlaylistInfo().getTitle());
    playlist.setRawData(source);

    for (Track t : source.getTracks()) {
      BasicTrack track = this.trackRegistry.getTrack(t.getId());
      if (track == null) {
        track = new BasicTrack();
        track.update(t);
      }
      playlist.addTrack(track);
    }

    return playlist;
  }

  /**
   * @return the playlistModificationDetector
   */
  public PlaylistModificationDetector getPlaylistModificationDetector() {
    return playlistModificationDetector;
  }

  /**
   * @return the playlistRegistry
   */
  public PlaylistRegistry getPlaylistRegistry() {
    return playlistRegistry;
  }

  /**
   * Gets the playlist validator that is used to check the playlist before it is
   * stored on laut.fm.
   * 
   * @return the playlistValidator
   */
  public PlaylistValidator getPlaylistValidator() {
    return playlistValidator;
  }

  public void initPlaylistModificationDetection() {
    this.ctx.getTimer().schedule(this.playlistModificationDetector.getCheckTask(), 1000, 1000 * 60 * 30);
  }

  protected void loadPlaylist(File file, PlaylistType type) throws IOException {
    try (FileInputStream stream = new FileInputStream(file)) {
      String content = IOUtils.toString(stream, "UTF-8");
      String name = FilenameUtils.getBaseName(file.getName());
      this.loadPlaylist(content, name, type);
    }
  }

  @SuppressWarnings("unchecked")
  protected void loadPlaylist(String content, String name, PlaylistType type) throws IOException {
    String[] lines = StringUtils.split(content, "\n");

    // read properties
    StringBuffer tsMapBuffer = new StringBuffer();
    int i = 0;
    List<String> properties = new ArrayList<String>();
    while (i < lines.length && lines[i].startsWith("# ")) {
      String line = lines[i].substring(2).trim();
      if (line.startsWith("tsmap")) {
        int p = line.indexOf('=');
        if (p > 0 && p < line.length() - 2) {
          tsMapBuffer.append(line.substring(p + 1).trim());
        }

      } else {
        properties.add(line);
      }
      i++;
    }

    // restore timestamp map
    Map<Integer, Long> timestampMap = null;
    if (tsMapBuffer.length() > 0) {
      try (ByteArrayInputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(tsMapBuffer.toString()))) {
        try (ObjectInputStream objIn = new ObjectInputStream(in)) {
          timestampMap = (Map<Integer, Long>) objIn.readObject();
        }
      } catch (Exception e) {
        log.error("unable to restore timestampes for playlist " + name, e);
      }
    }

    Playlist playlist = new Playlist(this.trackRegistry, type);
    playlist.setProperties(properties);
    playlist.setTimestampMap(timestampMap);

    Playlist previous = this.playlistRegistry.getPlaylist(playlist.getId());
    Map<Integer, Long> entryTimestamps = new HashMap<Integer, Long>();
    if (previous != null) {
      List<Entry> entries = new ArrayList<Playlist.Entry>(playlist.getEntries());
      for (Entry entry : entries) {
        entryTimestamps.put(entry.getTrackId(), entry.getTimestamp());
      }
      previous.removeEntries(entries);
      previous.commit();
      this.playlistRegistry.unregister(previous);
    }

    this.trackRegistry.setBlockChangsEvts(true);
    try {
      ExtendedTrackFormat format = new ExtendedTrackFormat();
      // read titles
      while (i < lines.length) {
        String line = lines[i].trim();
        if (line.length() > 0) {
          DetailedTrack track = format.fromString(line);
          if (track != null) {
            // TODO check track availability for archived playlists?
            playlist.addTrack(track);
          }
        }
        i++;
      }
      playlist.commit(); // marks playlist as clean
    } finally {
      this.trackRegistry.setBlockChangsEvts(false);
    }

    // restore timestamps
    for (Entry entry : playlist.getEntries()) {
      Long timestamp = entryTimestamps.get(entry.getTrackId());
      if (timestamp != null) {
        entry.setTimestamp(timestamp);
      }
    }

    if (playlist.getName() == null) {
      playlist.setName(name);
    }

    log.info("register playlist " + playlist.getName() + " / " + playlist.getId());
    this.playlistRegistry.register(playlist);
  }

  public void importArchivedPlaylist(String filename, String content) throws IOException {
    File file = new File(this.dirArchive + filename);
    try (FileOutputStream out = new FileOutputStream(file)) {
      IOUtils.write(content, out, "UTF-8");
    }
    this.loadPlaylist(file, PlaylistType.ARCHIVED);
  }

  public void loadPlaylists(PlaylistType type) throws IOException {
    File[] files = new File(type == PlaylistType.ONLINE ? this.dir : this.dirArchive).listFiles();
    if (files != null) {
      for (File file : files) {
        if (file.getName().endsWith(".lfm")) {
          this.loadPlaylist(file, type);
        }
      }
    }
  }

  public void load() throws IOException {
    this.ctx.updateStatus("loadPlaylists");

    this.loadShuffleScripts();
    this.playlistRegistry.clear();
    this.loadPlaylists(PlaylistType.ONLINE);
    this.loadPlaylists(PlaylistType.ARCHIVED);
    this.loadProfilesFromFile();
    this.checkIntegrity();
  }

  private void checkIntegrity() {
    for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.getProfileId() == null || (playlist.getProfileId() != null && this.getProfile(playlist.getProfileId()) == null)) {
        // illegal reference
        if (playlist.isShuffle()) {
          if (playlist.getShuffleType() != null && playlist.getShuffleType().equals(SHUFFLE_STATIONADMIN)) {
            this.autoAssignProfile(playlist, PlaylistProfileType.StationAdminShuffle);
          }
        } else {
          if (playlist.isGenerate()) {
            this.autoAssignProfile(playlist, PlaylistProfileType.Generate);
          } else if (playlist.isLocalShuffleAllowed()) {
            this.autoAssignProfile(playlist, PlaylistProfileType.LocalShuffle);
          }
        }
      }
    }
  }

  private void autoAssignProfile(Playlist playlist, PlaylistProfileType type) {
    for (PlaylistProfile profile : this.profiles) {
      if (profile.getType().equals(type)) {
        playlist.setProfileId(profile.getId());
        try {
          log.info("no or illegal profile reference - changed profile to " + profile.getName() + " for " + playlist.getName());
          this.savePlaylistAs(playlist, Integer.toString(playlist.getId()));
        } catch (Exception e) {
        }

        break;
      }
    }
  }

  private void loadShuffleScripts() throws IOException {
    this.loadShuffleScripts("shufflescripts.json");
    this.loadShuffleScripts("shufflescripts-custom.json");
    this.loadShuffleScripts("shufflescripts-" + ctx.getStation() + ".json");
  }

  private void loadShuffleScripts(String fileName) throws IOException {
    if (this.getClass().getClassLoader().getResource(fileName) != null) {
      try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream(fileName)) {
        ObjectMapper mapper = new ObjectMapper();
        ShuffleScriptMeta[] scripts = mapper.readValue(stream, ShuffleScriptMeta[].class);
        this.shuffleScripts.addAll(Arrays.asList(scripts));
      }
    }
  }

  private void restoreBackups() {
    File[] files = new File(this.dir).listFiles();
    for (File file : files) {
      if (file.getName().endsWith(".bak")) {
        File original = new File(file.getParent() + File.separatorChar + FilenameUtils.getBaseName(file.getName()) + ".lfm");
        file.renameTo(original);
      }
    }

  }

  /**
   * Saves all playlists to disk
   * 
   * @throws IOException
   */
  public void saveOnlinePlaylists() throws IOException {
    this.ctx.updateStatus("savePlaylists");

    File[] files = new File(this.dir).listFiles();
    if (files != null) {
      // delete old backups
      for (File file : files) {
        if (file.getName().endsWith(".bak")) {
          file.delete();
        }
      }
      // rename current files
      for (File file : files) {
        if (file.getName().endsWith(".lfm")) {
          File backup = new File(file.getParent() + File.separatorChar + FilenameUtils.getBaseName(file.getName()) + ".bak");
          file.renameTo(backup);
        }
      }
    }

    try {
      for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
        try {
          this.savePlaylistAs(playlist, playlist.getId() > -1 ? Integer.toString(playlist.getId()) : playlist.getName());
        } catch (IOException e) {
          log.error("error while trying to save playlist " + playlist.getName(), e);
          this.restoreBackups();
          throw e;
        }
      }
    } finally {
      this.ctx.updateStatus(null);
    }
  }

  /**
   * Saves a playlist to disk
   * 
   * @param playlist
   * @throws IOException
   */
  public void savePlaylist(Playlist playlist) throws IOException, PlaylistValidationException {
    if (!playlist.getType().isSaveToDiskSupported()) {
      throw new IllegalArgumentException("playlists of type " + playlist.getType() + " cannot be saved");
    }
    if ((playlist.getId() == -1 || playlist.isModified() || playlist.isMetaDataModified()) && playlist.getType().isSaveToServerSupported()) {
      this.savePlaylistToServer(playlist);
    }
    this.savePlaylistAs(playlist, playlist.getType() == PlaylistType.ONLINE ? Integer.toString(playlist.getId()) : playlist.getFileName());
  }

  /**
   * Saves a playlist to disk, using the given name
   * 
   * @param playlist
   * @param name
   * @throws IOException
   */
  private void savePlaylistAs(Playlist playlist, String name) throws IOException {
    if (!name.endsWith(".lfm")) {
      name += ".lfm";
    }
    playlist.commit();
    PlaylistBackupExporter exporter = new PlaylistBackupExporter();
    String dir = playlist.getType() == PlaylistType.ONLINE ? this.dir : this.dirArchive;
    exporter.toFile(playlist, new File(dir + name));
  }

  /**
   * Stores the given playlist on the laut.fm server
   * 
   * @param playlist
   * @throws PlaylistValidationException if the playlist didn't pass the assigned
   *         {@link PlaylistValidator}
   * @throws IOException
   * @throws JSONException
   */
  private void savePlaylistToServer(Playlist playlist) throws PlaylistValidationException, IOException {
    if (playlist.getType() != PlaylistType.ONLINE) {
      throw new IllegalArgumentException("Only playlists of type ONLINE can be saved to the server");
    }

    if (StringUtils.isEmpty(playlist.getName())) {
      throw new PlaylistValidationException(Reason.MISSING_NAME);
    }

    if (playlist.isMetaDataModified() || playlist.getId() == -1) {
      PlaylistHead head = new PlaylistHead();
      head.setColor(playlist.getColor() != null ? playlist.getColor() : "#FFFFFF");
      head.setDescription(playlist.getDescription());
      head.setTitle(playlist.getName());
      head.setShuffled(playlist.isShuffle());
      head.setShuffleOpts(playlist.getShuffleOpts());
      if (!playlist.isShuffle() && StringUtils.isNotEmpty(playlist.getGenerateTags())) {
        head.setShuffleOpts(playlist.getGenerateSettingsAsMap());
      }

      ShuffleScriptMeta scriptMeta = getShuffleScriptMeta(shuffleScripts, playlist.getShuffleType());
      if (playlist.isShuffle()) {
        head.setAutomationAlgorithm(scriptMeta != null ? scriptMeta.getAutomationAlgorithm() : "simple_shuffle");
      }

      if (playlist.getId() > -1) {
        head.setId(playlist.getId());
        head = this.ctx.getServer().updatePlaylist(ctx.getStationId(), head);
        playlist.setUpdatedAt(head.getUpdatedAt());
      } else {
        head = this.ctx.getServer().createPlaylist(ctx.getStationId(), head);
        playlist.setId(head.getId());
        playlist.setCreatedAt(head.getCreatedAt());
        playlist.setUpdatedAt(head.getUpdatedAt());
        playlistRegistry.register(playlist);
      }
      if (playlist.isShuffle() && scriptMeta != null) {
        updateShuffleFunc(playlist, false);
      }
    }

    if (playlist.isModified()) {
      if (playlist.getEntries().size() > MAX_TRACKS) {
        throw new PlaylistValidationException(Reason.MAX_TRACKS);
      }
      if (playlist.getLength() < 60 * 60) {
        // FIXME throw new PlaylistValidationException(Reason.MIN_LENGTH);
      }
      List<Entry> entries = playlist.getEntries();
      int[] ids = new int[entries.size()];
      for (int i = 0; i < entries.size(); i++) {
        ids[i] = entries.get(i).getTrackId();
      }
      de.stationadmin.lfm.backend.Playlist rawPlaylist = this.ctx.getServer().setPlaylistTracks(ctx.getStationId(), playlist.getId(), ids);
      playlist.setUpdatedAt(rawPlaylist.getUpdatedAt());
    }
    playlist.commit();

  }

  private boolean updateShuffleFunc(Playlist playlist, boolean updateShufleAlgorithm) throws IOException {
    if (playlist.isShuffle() && playlist.getShuffleType() != null) {
      String shuffleType = playlist.getShuffleType();
      ShuffleScriptMeta scriptMeta = getShuffleScriptMeta(shuffleScripts, shuffleType);
      if (scriptMeta != null && StringUtils.isEmpty(scriptMeta.getAutomationAlgorithm())) {
        String shuffleFunc = scriptMeta.getDefaultVersion();
        if (scriptMeta.getFile() != null) {
          try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("shuffle/" + scriptMeta.getFile())) {
            shuffleFunc = IOUtils.toString(stream, "UTF-8");
          }
        }
        if (shuffleFunc != null) {
          this.ctx.getServer().setPlaylistShuffleFunction(ctx.getStationId(), playlist.getId(), shuffleFunc);
          return true;
        }
      } else {
        this.ctx.getServer().setPlaylistShuffleFunction(ctx.getStationId(), playlist.getId(), "");
        if (updateShufleAlgorithm && !StringUtils.isEmpty(scriptMeta.getAutomationAlgorithm())) {
          this.ctx.getServer().setAutomationAlgorithm(ctx.getStationId(), playlist.getId(), scriptMeta.getAutomationAlgorithm());
          return true;
        }
      }
    }
    return false;

  }

  public List<Playlist> updateShuffleFunctions() throws IOException {
    List<Playlist> updatedPlaylists = new ArrayList<>();
    for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (updateShuffleFunc(playlist, true)) {
        updatedPlaylists.add(playlist);
      }
    }
    return updatedPlaylists;
  }

  /**
   * @param playlistValidator the playlistValidator to set
   */
  public void setPlaylistValidator(PlaylistValidator playlistValidator) {
    this.playlistValidator = playlistValidator;
  }

  /**
   * Refetches and updates all online playlists
   */
  public void synchronize() throws IOException {
    Map<Integer, Map<Integer, Long>> timestampMaps = new HashMap<Integer, Map<Integer, Long>>();
    for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      timestampMaps.put(playlist.getId(), playlist.getTimestampMap());
    }
    this.playlistRegistry.clear();

    this.ctx.updateStatus("getAllPlaylists");
    List<ExtendedPlaylistHead> playlistInfos = this.ctx.getServer().getPlaylists(this.ctx.getStationId());

    Set<Integer> refreshed = new HashSet<Integer>(); // ids of tracks that have been updated in track registry
    for (ExtendedPlaylistHead playlistInfo : playlistInfos) {

      // prepare basic information
      Playlist playlist = new Playlist(this.trackRegistry, PlaylistType.ONLINE);
      this.initOnlinePlaylist(playlist, playlistInfo, false);
      playlist.setTimestampMap(timestampMaps.get(playlist.getId()));
      this.playlistRegistry.register(playlist);
      // defer setting of shuffleOpts as it might create local data too early
      // otherwise
      playlist.setShuffleOpts(playlistInfo.getShuffleOpts(), true);
      playlist.setShuffleType(getShuffleType(playlistInfo));

      // add tracks to playlist
      try {
        this.ctx.updateStatus("getPlaylist", playlist.getDisplayName());
        de.stationadmin.lfm.backend.Playlist pl = ctx.getServer().getPlaylist(ctx.getStationId(), playlistInfo.getId());

        this.loadPlaylistTracks(playlist, pl.getEntries(), refreshed);
        updateMetaData(pl, playlist);
      } catch (Exception e) {
        log.error("error while loading titles for playlist " + playlistInfo.getTitle(), e);
      }
    }

    this.saveOnlinePlaylists();
    this.playlistModificationDetector.markClean();

    this.loadPlaylists(PlaylistType.ARCHIVED);
    this.playlistModificationDetector.markClean();
  }

  private void loadPlaylistTracks(Playlist playlist, TrackRef[] trackRefs, Set<Integer> refreshed) throws IOException {
    if (trackRefs != null) {
      BasicTrack[] tracks = new BasicTrack[trackRefs.length];
      int[] missing = new int[trackRefs.length];
      int missingIdx = 0;
      for (int i = 0; i < trackRefs.length; i++) {
        tracks[i] = this.trackRegistry.getTrack(trackRefs[i].getTrackId());
        if (tracks[i] == null) {
          if (trackRefs[i].getTrackId() == TrackRegistry.STANDARD_AD_TRIGGER_ID) {
            tracks[i] = this.trackRegistry.getStandardAdTrigger();
          } else {
            missing[missingIdx++] = trackRefs[i].getTrackId();
          }
        }
      }

      if (missingIdx > 0) {
        for (Track track : ctx.getServer().getTracks(ctx.getStationId(), missing)) {
          RegisteredTrack regTrack = this.trackRegistry.getTrack(track.getId());
          if (regTrack == null) {
            this.trackRegistry.add(new RegisteredTrack(track));
            refreshed.add(track.getId());
          } else if (!refreshed.contains(track.getId())) {
            regTrack.update(track);
            refreshed.add(track.getId());
          }
        }
      }

      for (int i = 0; i < trackRefs.length; i++) {
        if (tracks[i] == null) {
          tracks[i] = this.trackRegistry.getTrack(trackRefs[i].getTrackId());
        }
        if (tracks[i] != null) {
          playlist.addTrack(tracks[i], trackRefs[i].getAddedAt());
        }
      }

    }
    playlist.setModified(false);
    playlist.commit();

  }

  public void synchronize(int... playlistIds) throws IOException {
    this.ctx.checkSession();
    HashSet<Integer> ids = new HashSet<Integer>();
    for (int id : playlistIds) {
      ids.add(id);
    }

    this.ctx.updateStatus("getAllPlaylists");
    List<ExtendedPlaylistHead> playlistInfos = this.ctx.getServer().getPlaylists(ctx.getStationId());

    Set<Integer> refreshed = new HashSet<Integer>(); // tracks that have been updated in track registry
    for (ExtendedPlaylistHead playlistInfo : playlistInfos) {
      if (ids.contains(playlistInfo.getId())) {

        // prepare basic information
        Playlist playlist = this.playlistRegistry.getPlaylist(playlistInfo.getId());
        if (playlist == null) {
          playlist = new Playlist(this.trackRegistry, PlaylistType.ONLINE);
        } else {
          // remove all old titles - will be filled with new ones
          playlist.removeEntries(new ArrayList<Playlist.Entry>(playlist.getEntries()));
        }
        this.initOnlinePlaylist(playlist, playlistInfo, true);
        this.playlistRegistry.register(playlist);

        // add titles to playlist
        this.ctx.updateStatus("getPlaylist", playlist.getDisplayName());

        de.stationadmin.lfm.backend.Playlist pl = ctx.getServer().getPlaylist(ctx.getStationId(), playlist.getId());
        this.loadPlaylistTracks(playlist, pl.getEntries(), refreshed);
        updateMetaData(pl, playlist);
        this.savePlaylistAs(playlist, Integer.toString(playlist.getId()));

      }
    }
    this.playlistModificationDetector.markClean();
    this.trackRegistry.removeUnused();

  }

  private String getShuffleType(ExtendedPlaylistHead head) {
    if (!StringUtils.isEmpty(head.getAutomationAlgorithm())) {
      ShuffleScriptMeta meta = getShuffleScriptMeta(shuffleScripts, head.getAutomationAlgorithm());
      if (meta != null) {
        return meta.getKey();
      } else {
        // unknown algorithm - just use it
        head.getAutomationAlgorithm();
      }
    } else if (head.getShuffleFunction() != null) {
      Matcher matcher = shuffleKeyPattern.matcher(head.getShuffleFunction());
      if (matcher.find()) {
        String match = matcher.group(1);
        // limit to supported types
        if (getShuffleScriptMeta(shuffleScripts, match) != null) {
          return match;
        }
      }
    } else if (head.getShuffleOpts() != null) {
      if (head.getShuffleOpts().containsKey("jingleInterval")) {
        return SHUFFLE_STATIONADMIN;
      } else if (head.getShuffleOpts().containsKey("pattern")) {
        return SHUFFLE_CLASSIC;
      } else if (head.getShuffleOpts().containsKey("iterationStepHours")) {
        return SHUFFLE_BLOCKSELECT;
      }
    }
    return head.isShuffled() ? SHUFFLE_CLASSIC : null;
  }

  /**
   * Gets the associated shuffle script meta data for the given type
   * 
   * @param shuffleType shuffle type as set in key line
   * @return associated meta data or <code>null</code> if type is not known
   */
  public static ShuffleScriptMeta getShuffleScriptMeta(List<ShuffleScriptMeta> shuffleScripts, String shuffleType) {
    if (shuffleType == null) {
      return null;
    }
    shuffleType = shuffleType.toLowerCase();
    ShuffleScriptMeta best = null;
    for (ShuffleScriptMeta script : shuffleScripts) {
      if (shuffleType.equals(script.getKey().toLowerCase()) || shuffleType.equals(script.getAutomationAlgorithm())) {
        return script;
      } else if (shuffleType.startsWith(script.getKey().toLowerCase() + "_")) {
        best = script;
      }
    }
    return best;
  }

  private void updateMetaData(ExtendedPlaylistHead head, Playlist playlist) {
    playlist.setColor(head.getColor());
    playlist.setCreatedAt(head.getCreatedAt());
    playlist.setDescription(head.getDescription());
    playlist.setName(head.getTitle());
    playlist.setUpdatedAt(head.getUpdatedAt());
    playlist.setShuffleOpts(head.getShuffleOpts(), true);
    playlist.setShuffleType(getShuffleType(head));
  }

  private void initOnlinePlaylist(Playlist playlist, ExtendedPlaylistHead playlistInfo, boolean withOpts) {
    playlist.setId(playlistInfo.getId());
    playlist.setName(playlistInfo.getTitle());
    playlist.setDescription(playlistInfo.getDescription());
    playlist.setShuffle(playlistInfo.isShuffled());
    playlist.setColor(playlistInfo.getColor());
    playlist.setCreatedAt(playlistInfo.getCreatedAt());
    playlist.setUpdatedAt(playlist.getUpdatedAt());
    if (withOpts) {
      playlist.setShuffleOpts(playlistInfo.getShuffleOpts(), true);
      playlist.setShuffleType(getShuffleType(playlistInfo));
    }
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.initPlaylistModificationDetection();
  }

  /**
   * Updates shuffle opts in affected playlists after profile changed
   * 
   * @param profileId
   */
  public void updateProfileOpts(String profileId) throws PlaylistValidationException, IOException {
    PlaylistProfile profile = getProfile(profileId);
    if (profile == null) {
      return;
    }
    HashSet<String> profileIds = new HashSet<>();
    profileIds.add(profileId);
    // check for profiles referencing the changed one
    for (PlaylistProfile prof : getProfiles()) {
      if (profileId.equals(prof.getArtistNormalizationFromProfile()) || profileId.equals(prof.getTrackRuleFromProfile())) {
        profileIds.add(prof.getId());
      }
    }
    for (Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (playlist.isShuffle() && playlist.getProfileId() != null && profileId.contains(playlist.getProfileId())) {
        Map<String, Object> opts = playlist.getShuffleOpts();
        boolean modified = playlist.isMetaDataModified();
        if (opts == null) {
          opts = new HashMap<>();
          playlist.setShuffleOpts(opts);
        }
        assignProfileOpts(opts, playlist.getProfileId());
        if (!modified) {
          this.savePlaylistAs(playlist, Integer.toString(playlist.getId()));
          this.ctx.getServer().updatePlaylistShuffleOpts(ctx.getStationId(), playlist.getId(), opts);
        }
      }
    }
  }

  /**
   * Assigns profile options to the given opts map.
   * 
   * @param opts map to add settings to
   * @param profileId id of the profile to read optios from
   */
  public void assignProfileOpts(Map<String, Object> opts, String profileId) {
    if (opts == null) {
      return;
    }

    PlaylistProfile main = getProfile(profileId);
    if (main == null || main.getType() == PlaylistProfileType.Generate) {
      opts.remove("jingleInterval");
      opts.remove("jingleOrder");
      opts.remove("protectFirstJingle");
      opts.remove("preserveAllJingles");
      opts.remove("wordDistribution");
      opts.remove("artistSeparators");
      opts.remove("artistAliases");
      opts.remove("adTrigger");
      opts.remove("adSeparator");
      opts.remove("adPositions");
      opts.remove("adJingleCollisionStrategy");
      opts.remove("trackRuleJingleCollisionStrategy");
      opts.remove("trackRuleGroupCollisionStrategy");
      opts.remove("trackRuleGroups");
      opts.remove("trackRules");
      return;
    }

    PlaylistProfile trackRulesProfile = main.getTrackRuleFromProfile() == null || main.getTrackRuleFromProfile().equals(main.getId()) ? main
        : getProfile(main.getTrackRuleFromProfile());
    PlaylistProfile artistNormProfile = main.getArtistNormalizationFromProfile() == null || main.getArtistNormalizationFromProfile().equals(main.getId()) ? main
        : getProfile(main.getArtistNormalizationFromProfile());
    ArtistNormalizationCfg artistNorm = artistNormProfile != null ? artistNormProfile.getArtistNormalization() : new ArtistNormalizationCfg();

    // jingles
    opts.put("preserveAllJingles", main.isProtectAllJingles() ? 1 : 0);
    if (!main.isProtectAllJingles()) {
      opts.put("jingleInterval", main.getJingleInterval());
      opts.put("jingleOrder", "shuffle_repeat"); // TODO replace by setting if available
      opts.put("protectFirstJingle", main.isProtectFirstJingle() ? 1 : 0);
    } else {
      opts.remove("jingleInterval");
      opts.remove("jingleOrder");
      opts.remove("protectFirstJingle");
    }

    // words
    switch (main.getWordDistributionStrategy()) {
    case RANDOM:
      opts.put("wordDistribution", "random");
      break;
    case PROTECT:
      opts.put("wordDistribution", "preserve");
      break;
    case PREDECESSOR_COUPLING:
      opts.put("wordDistribution", "link_previous");
      break;
    case SUCCESSOR_COUPLING:
      opts.put("wordDistribution", "link_next");
      break;
    }

    // artist alias
    if (artistNorm.getSeparators() != null && artistNorm.getSeparators().size() > 0) {
      opts.put("artistSeparators", artistNorm.getSeparators());
    } else {
      opts.remove("artistSeparators");
    }
    if (artistNorm.getAliases() != null && artistNorm.getAliases().size() > 0) {
      opts.put("artistAliases", artistNorm.getAliases());
    } else {
      opts.remove("artistAliases");
    }

    // ad triggers
    AdTriggerCfg adTrigger = main.getAdTrigger();
    if (adTrigger.getPos1() > -1) {
      opts.put("adTrigger", adTrigger.getTriggerId());
      if (adTrigger.getSeperatorId() > 0) {
        opts.put("adSeparator", adTrigger.getSeperatorId());
      } else {
        opts.remove("adSeparator");
      }
      opts.put("adPositions", new int[] { adTrigger.getPos1(), adTrigger.getPos2() });
      switch (adTrigger.getJingleCollisionStrategy()) {
      case KEEP_BOTH:
        opts.put("adJingleCollisionStrategy", "keep_both");
        break;
      case MOVE_ADTRIGGER:
        opts.put("adJingleCollisionStrategy", "move_adtrigger");
        break;
      case REMOVE_JINGLE:
        opts.put("adJingleCollisionStrategy", "remove_jingle");
        break;
      }

    } else {
      opts.remove("adTrigger");
      opts.remove("adSeparator");
      opts.remove("adPositions");
      opts.remove("adJingleCollisionStrategy");
    }

    // track rules
    TrackRuleCfg trackRuleCfg = trackRulesProfile != null && trackRulesProfile.getTrackRules() != null ? trackRulesProfile.getTrackRules() : new TrackRuleCfg();
    if (trackRuleCfg.getRules() != null && trackRuleCfg.getRules().size() > 0) {
      opts.put("trackRuleJingleCollisionStrategy", trackRuleCfg.getJingleCollisionStrategy().name().toLowerCase());
      opts.put("trackRuleGroupCollisionStrategy", trackRuleCfg.getGroupCollisionStrategy().name().toLowerCase());

      HashMap<String, HashMap<String, Object>> groups = new HashMap<>();
      for (TrackRuleGroup group : trackRuleCfg.getGroups()) {
        HashMap<String, Object> groupOpts = new HashMap<>();
        groupOpts.put("minDistance", group.getMinDistance());
        groupOpts.put("multiMatchSelection", group.getMultiMatchSelection().name().toLowerCase());
        groups.put(group.getName(), groupOpts);
      }
      opts.put("trackRuleGroups", groups);

      ArrayList<HashMap<String, Object>> rules = new ArrayList<>();
      for (TrackRule rule : trackRuleCfg.getRules()) {
        HashMap<String, Object> ruleOpts = new HashMap<>();
        ruleOpts.put("groupName", rule.getGroupName());
        ruleOpts.put("trackId", rule.getTrackId());
        ruleOpts.put("filter", rule.getFilter());
        ruleOpts.put("filterType", rule.getFilterType().name().toLowerCase());
        ruleOpts.put("position", rule.getPosition().name().toLowerCase());
        ruleOpts.put("minDistance", rule.getMinDistance());
        rules.add(ruleOpts);
      }

      opts.put("trackRules", rules);
    } else {
      opts.remove("trackRuleJingleCollisionStrategy");
      opts.remove("trackRuleGroupCollisionStrategy");
      opts.remove("trackRuleGroups");
      opts.remove("trackRules");
    }

  }

  public List<ShuffleScriptMeta> getShuffleScripts() {
    return shuffleScripts;
  }

  @Override
  public void applyClientConfiguration(ClientConfiguration cfg) {

    if (cfg.getPlaylistProfiles() != null && cfg.getPlaylistProfiles().size() > 0) {
      this.profiles = cfg.getPlaylistProfiles();
      try {
        this.saveProfilesToFile();
      } catch (Exception e) {
        log.info("unable to update playlist profiles from client configuration", e);
      }
    }

    HashMap<Integer, PlaylistClientCfgData> map = new HashMap<>();
    cfg.getPlaylistData().forEach(c -> map.put(c.getId(), c));
    if (map.size() == 0) {
      // no data available - leave it as it is
      return;
    }
    for (Playlist pl : playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      PlaylistClientCfgData data = map.get(pl.getId());
      if (data != null) {
        pl.setAutoFillRule(data.getAutoFillRule());
        pl.setProfileId(data.getProfileId());
        pl.setComment(data.getComment());
        pl.setTags(new HashSet<>(Arrays.asList(data.getTags())));
      } else {
        // no data from server available - leave it as it is
      }
      if (!pl.isModified()) {
        try {
          this.savePlaylistAs(pl, pl.getType() == PlaylistType.ONLINE ? Integer.toString(pl.getId()) : pl.getName());
        } catch (Exception e) {
          log.info("unable to update playlist meta data from client configuration", e);
        }
      }
    }
  }

  @Override
  public void collectClientConfiguration(ClientConfiguration cfg) {
    cfg.setPlaylistProfiles(this.profiles);
    List<PlaylistClientCfgData> list = new ArrayList<>();
    for (Playlist pl : playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if ((pl.getAutoFillRule() != null && pl.getAutoFillRule().isConfigured()) || org.apache.commons.lang3.StringUtils.isNotEmpty(pl.getProfileId())
          || org.apache.commons.lang3.StringUtils.isNotEmpty(pl.getComment()) || pl.getTags().size() > 0) {
        PlaylistClientCfgData data = new PlaylistClientCfgData();
        data.setId(pl.getId());
        if (pl.getAutoFillRule() != null && pl.getAutoFillRule().isEnabled()) {
          data.setAutoFillRule(pl.getAutoFillRule());
        }
        data.setComment(pl.getComment());
        data.setProfileId(pl.getProfileId());
        data.setTags(pl.getTags().toArray(new String[pl.getTags().size()]));
        list.add(data);
      }
    }
    cfg.setPlaylistData(list);

  }

  public List<PlaylistProfile> getProfiles() {
    return this.profiles;
  }

  public PlaylistProfile getProfile(String id) {
    if (id != null) {
      for (PlaylistProfile p : profiles) {
        if (p.getId().equals(id)) {
          return p;
        }
      }
    }
    return null;
  }

  public void addProfile(PlaylistProfile profile) {
    List<PlaylistProfile> old = new ArrayList<>(this.profiles);
    this.profiles.add(profile);
    this.firePropertyChange("profiles", old, profiles);
  }

  public void removeProfile(String id) {
    List<PlaylistProfile> old = new ArrayList<>(this.profiles);
    for (int i = 0; i < profiles.size(); i++) {
      if (profiles.get(i).getId().equals(id)) {
        profiles.remove(i);
        this.firePropertyChange("profiles", old, profiles);
        return;
      }
    }
  }

  public void saveProfiles() throws IOException {

    saveProfilesToFile();
  }

  private List<PlaylistProfile> convertJsonToProfiles(String json) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    PlaylistProfile[] profiles = mapper.readValue(json, PlaylistProfile[].class);
    return new ArrayList<>(Arrays.asList(profiles));
  }

  private String convertProfilesToJson() throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    PlaylistProfile[] profiles = this.profiles.toArray(new PlaylistProfile[this.profiles.size()]);
    return mapper.writeValueAsString(profiles);
  }

  private void loadProfilesFromFile() throws IOException {
    String file = ctx.getStationDirectory() + "playlistprofiles.json";
    if (new File(file).exists()) {
      this.profiles = convertJsonToProfiles(org.apache.commons.io.FileUtils.readFileToString(new File(file), Charset.forName("UTF-8")));
    } else {
      this.migrateGlobalSettingsToProfiles();
    }
  }

  public void reloadProfiles() throws IOException {
    String file = ctx.getStationDirectory() + "playlistprofiles.json";
    if (new File(file).exists()) {
      this.profiles = convertJsonToProfiles(org.apache.commons.io.FileUtils.readFileToString(new File(file), Charset.forName("UTF-8")));
    }
  }

  private void migrateGlobalSettingsToProfiles() throws IOException {
    PlaylistProfile generate = new PlaylistProfile(PlaylistProfileType.Generate, "Generieren", settings);
    PlaylistProfile shuffleLocal = new PlaylistProfile(PlaylistProfileType.LocalShuffle, "Shuffeln (lokal)", settings);
    PlaylistProfile shuffleServer = new PlaylistProfile(PlaylistProfileType.StationAdminShuffle, "Shuffeln - Station Admin", settings);

    this.profiles.add(generate);
    this.profiles.add(shuffleLocal);
    this.profiles.add(shuffleServer);

    this.saveProfilesToFile();

    for (Playlist pl : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (pl.getGenerateTags() != null) {
        pl.setProfileId(generate.getId());
        System.out.println(pl.getName() + " => " + generate.getName());
        this.savePlaylistAs(pl, Integer.toString(pl.getId()));
      } else if (pl.isShuffle()) {
        ShuffleScriptMeta scriptMeta = getShuffleScriptMeta(shuffleScripts, pl.getShuffleType());
        if (scriptMeta != null && scriptMeta.isSupportsGlobalOpts()) {
          pl.setProfileId(shuffleServer.getId());
          System.out.println(pl.getName() + " => " + shuffleServer.getName());
          this.savePlaylistAs(pl, Integer.toString(pl.getId()));
        }
      } else if (pl.isLocalShuffleAllowed()) {
        pl.setProfileId(shuffleLocal.getId());
        System.out.println(pl.getName() + " => " + shuffleLocal.getName());
        this.savePlaylistAs(pl, Integer.toString(pl.getId()));
      }
    }

  }

  private void saveProfilesToFile() throws IOException {
    String file = ctx.getStationDirectory() + "playlistprofiles.json";
    FileUtils.write(new File(file), convertProfilesToJson(), Charset.forName("UTF-8"));
  }

  public String getPlaylistJson(int playlistId) throws IOException {
    return this.ctx.getServer().getPlaylistJson(this.ctx.getStationId(), playlistId);
  }

}
