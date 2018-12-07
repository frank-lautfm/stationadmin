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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import com.thoughtworks.xstream.XStream;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.exporter.PlaylistBackupExporter;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.playlist.validation.PlaylistValidationException.Reason;
import de.stationadmin.base.playlist.validation.PlaylistValidator;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.util.XStreamFactory;
import de.stationadmin.lfm.backend.CurrentPlaylist;
import de.stationadmin.lfm.backend.ExtendedPlaylistHead;
import de.stationadmin.lfm.backend.PlaylistHead;
import de.stationadmin.lfm.backend.Track;
import de.stationadmin.lfm.backend.TrackRef;

/**
 * @author Frank
 * 
 */
public class PlaylistService implements Service {
  private static final Logger log = Logger.getLogger(PlaylistService.class);
  private static final Pattern shuffleKeyPattern = Pattern.compile("key.\\s*([\\w|_]+)", Pattern.MULTILINE | Pattern.DOTALL);

  public static final String SHUFFLE_CLASSIC = "basic_v1";
  public static final String SHUFFLE_BUCKET = "bucket_v1_1";
  public static final String SHUFFLE_STATIONADMIN = "StationAdmin_v1";

  private SessionCtx ctx;
  private TrackRegistry trackRegistry;
  private PlaylistRegistry playlistRegistry;
  private String dir;
  private String dirArchive;
  private PlaylistValidator playlistValidator;
  private PlaylistModificationDetector playlistModificationDetector;

  /**
   * @param ctx
   * @param titleRegistry
   * @param playlistRegistry
   */
  public PlaylistService(SessionCtx ctx, TrackRegistry titleRegistry, PlaylistRegistry playlistRegistry) {
    super();
    this.ctx = ctx;
    this.trackRegistry = titleRegistry;
    this.playlistRegistry = playlistRegistry;
    this.dir = ctx.getStationDirectory() + "playlists" + File.separatorChar;
    this.dirArchive = dir + "archive" + File.separatorChar;
    new File(this.dir).mkdirs();
    new File(this.dirArchive).mkdirs();

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
      this.ctx.getServer().deletePlaylist(ctx.getStationId(), playlist.getId());
    }

    File file = new File(this.dirArchive + File.separatorChar + playlist.getName() + ".lfm");
    if (file.exists()) {
      if (!file.delete()) {
        throw new IOException("Unable to delete " + file);
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
    this.playlistRegistry.clear();
    this.loadPlaylists(PlaylistType.ONLINE);
    this.loadPlaylists(PlaylistType.ARCHIVED);
    if (this.getPlaylistRegistry().getNumPlaylists() == 0) {
      this.loadPlaylistsLegacy();
    }
  }

  @SuppressWarnings("unchecked")
  private void loadPlaylistsLegacy() throws IOException {
    XStream xstream = XStreamFactory.newXStream();
    xstream.alias("playlist", Playlist.class);
    xstream.alias("entry", Entry.class);

    File file = new File(this.ctx.getStationDirectory() + "playlists.xml");
    if (file.exists()) {
      FileInputStream playlistStream = new FileInputStream(file);
      List<Playlist> playlists = (List<Playlist>) xstream.fromXML(playlistStream);
      playlistStream.close();

      for (Playlist playlist : playlists) {
        playlist.setTrackRegistry(this.trackRegistry);
        playlist.reset(); // marks playlist as clean
        playlist.resolveTitles();
        this.playlistRegistry.register(playlist);
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
    this.savePlaylistAs(playlist, playlist.getType() == PlaylistType.ONLINE ? Integer.toString(playlist.getId()) : playlist.getName());
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
      if (playlist.isShuffle() && playlist.getShuffleType() != null) {
        String shuffleFunc = playlist.getShuffleType();
        if (shuffleFunc.startsWith("StationAdmin")) {
          try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("shuffle/" + shuffleFunc + ".js")) {
            shuffleFunc = IOUtils.toString(stream, "UTF-8");
          }
        }

        this.ctx.getServer().setPlaylistShuffleFunction(ctx.getStationId(), playlist.getId(), shuffleFunc);
      }
    }

    if (playlist.isModified()) {
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
  
  public List<Playlist> updateShuffleFunctions() throws IOException {
    List<Playlist> updatedPlaylists = new ArrayList<>();
    for(Playlist playlist : this.playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if(playlist.isShuffle()) {
        String shuffleFunc = playlist.getShuffleType();
        if (shuffleFunc.startsWith("StationAdmin")) {
          if(shuffleFunc.contains("dev")) {
            shuffleFunc = "StationAdmin_v1";
          }
          try (InputStream stream = this.getClass().getClassLoader().getResourceAsStream("shuffle/" + shuffleFunc + ".js")) {
            shuffleFunc = IOUtils.toString(stream, "UTF-8");
          }
        }
        this.ctx.getServer().setPlaylistShuffleFunction(ctx.getStationId(), playlist.getId(), shuffleFunc);
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
      this.initOnlinePlaylist(playlist, playlistInfo);
      playlist.setTimestampMap(timestampMaps.get(playlist.getId()));
      this.playlistRegistry.register(playlist);

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
          missing[missingIdx++] = trackRefs[i].getTrackId();
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
        this.initOnlinePlaylist(playlist, playlistInfo);
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

  private static String getShuffleType(ExtendedPlaylistHead head) {
    if (head.getShuffleFunction() != null) {
      Matcher matcher = shuffleKeyPattern.matcher(head.getShuffleFunction());
      if (matcher.find()) {
        String match = matcher.group(1);
        // limit to supported types
        if (match.startsWith("basic") || match.startsWith("bucket") || match.startsWith("StationAdmin")) {
          return match;
        }
      }
    }
    return null;
  }

  private static void updateMetaData(ExtendedPlaylistHead head, Playlist playlist) {
    playlist.setColor(head.getColor());
    playlist.setCreatedAt(head.getCreatedAt());
    playlist.setDescription(head.getDescription());
    playlist.setName(head.getTitle());
    playlist.setUpdatedAt(head.getUpdatedAt());
    playlist.setShuffleOpts(head.getShuffleOpts());
    playlist.setShuffleType(getShuffleType(head));
  }

  private void initOnlinePlaylist(Playlist playlist, ExtendedPlaylistHead playlistInfo) {
    playlist.setId(playlistInfo.getId());
    playlist.setName(playlistInfo.getTitle());
    playlist.setDescription(playlistInfo.getDescription());
    // playlist.setLength(playlistInfo.getLength());
    playlist.setShuffle(playlistInfo.isShuffled());
    playlist.setColor(playlistInfo.getColor());
    playlist.setCreatedAt(playlistInfo.getCreatedAt());
    playlist.setUpdatedAt(playlist.getUpdatedAt());
    playlist.setShuffleOpts(playlistInfo.getShuffleOpts());
    playlist.setShuffleType(getShuffleType(playlistInfo));
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
   * Patches the global shuffle options of all playlists that are shuffled on
   * server with Station Admin shuffle
   * 
   * @param settings
   * @throws IOException
   */
  public void updateGlobalShuffleOpts(Settings settings) throws IOException {
    for (Playlist pl : playlistRegistry.getPlaylists(PlaylistType.ONLINE)) {
      if (pl.isShuffle() && pl.getShuffleType().equals(SHUFFLE_STATIONADMIN)) {
        Map<String, Object> opts = pl.getShuffleOpts();
        boolean modified = pl.isMetaDataModified();
        if (opts == null) {
          opts = new HashMap<>();
          pl.setShuffleOpts(opts);
        }
        updateGlobalShuffleOpts(opts, settings);
        if (!modified) {
          this.savePlaylistAs(pl, Integer.toString(pl.getId()));
          this.ctx.getServer().updatePlaylistShuffleOpts(ctx.getStationId(), pl.getId(), opts);
        }
      }
    }

  }

  public static void updateGlobalShuffleOpts(Map<String, Object> opts, Settings settings) {

    // jingles
    opts.put("preserveAllJingles", settings.isShuffleProtectAllJingles() ? 1 : 0);
    if (!settings.isShuffleProtectAllJingles()) {
      opts.put("jingleInterval", settings.getShuffleJingleInterval());
      opts.put("jingleOrder", "shuffle_repeat"); // TODO replace by setting if available
      opts.put("protectFirstJingle", settings.isShuffleProtectFirstJingle() ? 1 : 0);
    } else {
      opts.remove("jingleInterval");
      opts.remove("jingleOrder");
      opts.remove("protectFirstJingle");
    }

    // words
    switch (settings.getShuffleWordDistributionStrategy()) {
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
    if (settings.getArtistNormalizerSeperators() != null && settings.getArtistNormalizerSeperators().size() > 0) {
      opts.put("artistSeparators", settings.getArtistNormalizerSeperators());
    } else {
      opts.remove("artistSeparators");
    }
    if (settings.getArtistNormalizerAliases() != null && settings.getArtistNormalizerAliases().size() > 0) {
      opts.put("artistAliases", settings.getArtistNormalizerAliases());
    } else {
      opts.remove("artistAliases");
    }

    // ad triggers
    if (settings.getAdTriggerPosition1() > -1) {
      opts.put("adTrigger", settings.getAdTriggerId());
      if (settings.getAdSeparatorId() > 0) {
        opts.put("adSeparator", settings.getAdSeparatorId());
      } else {
        opts.remove("adSeparator");
      }
      opts.put("adPositions", new int[] { settings.getAdTriggerPosition1(), settings.getAdTriggerPosition2() });
      switch (settings.getAdJingleCollisionStrategy()) {
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

  }

}
