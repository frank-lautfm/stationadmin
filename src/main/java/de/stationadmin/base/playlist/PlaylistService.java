/**
 * 
 */
package de.stationadmin.base.playlist;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import com.thoughtworks.xstream.XStream;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.exporter.PlaylistBackupExporter;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.playlist.validation.PlaylistValidationException.Reason;
import de.stationadmin.base.playlist.validation.PlaylistValidator;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.util.XStreamFactory;
import de.stationadmin.lfm.backend.PlaylistHead;
import de.stationadmin.lfm.backend.Track;
import de.stationadmin.lfm.backend.TrackRef;

/**
 * @author Frank
 * 
 */
public class PlaylistService implements Service {
  private static final Logger log = Logger.getLogger(PlaylistService.class);
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
    
    if(playlist.getType() == PlaylistType.ONLINE && playlist.getId() > -1) {
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
    FileInputStream stream = new FileInputStream(file);
    String content = IOUtils.toString(stream, "UTF-8");
    IOUtils.closeQuietly(stream);
    String name = FilenameUtils.getBaseName(file.getName());
    this.loadPlaylist(content, name, type);
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
      try {
        ByteArrayInputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(tsMapBuffer.toString()));
        ObjectInputStream objIn = new ObjectInputStream(in);
        timestampMap = (Map<Integer, Long>) objIn.readObject();
        IOUtils.closeQuietly(objIn);
        IOUtils.closeQuietly(in);
      } catch (Exception e) {
        e.printStackTrace();
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
    FileOutputStream out = new FileOutputStream(file);
    IOUtils.write(content, out);
    out.close();

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
    try {
      if (ctx.isLiveEnabled()) {
        Playlist live = new Playlist(this.trackRegistry, PlaylistType.TEMPORARY);
        live.setColor("#FF0000");
        live.setName("Live");
        live.setId(PlaylistRegistry.LIVE_PLAYLIST_ID);
        this.playlistRegistry.setLivePlaylist(live);
      }
    } catch (Exception e) {

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
   * @throws PlaylistValidationException
   *           if the playlist didn't pass the assigned
   *           {@link PlaylistValidator}
   * @throws IOException
   * @throws JSONException
   */
  private void savePlaylistToServer(Playlist playlist) throws PlaylistValidationException, IOException {
    if (playlist.getType() != PlaylistType.ONLINE) {
      throw new IllegalArgumentException("Only playlists of type ONLINE can be saved to the server");
    }
    
    if(StringUtils.isEmpty(playlist.getName())) {
      throw new PlaylistValidationException(Reason.MISSING_NAME);
    }

    if (playlist.isMetaDataModified() || playlist.getId() == -1) {
      PlaylistHead head = new PlaylistHead();
      head.setColor(playlist.getColor() != null ? playlist.getColor() : "#FFFFFF");
      head.setDescription(playlist.getDescription());
      head.setTitle(playlist.getName());
      head.setShuffled(playlist.isShuffle());
      if (playlist.getId() > -1) {
        head.setId(playlist.getId());
        this.ctx.getServer().updatePlaylist(ctx.getStationId(), head);
        playlist.setUpdatedAt(head.getUpdatedAt());
      }
      else {
        head = this.ctx.getServer().createPlaylist(ctx.getStationId(), head);
        playlist.setId(head.getId());
        playlist.setCreatedAt(head.getCreatedAt());
        playlist.setUpdatedAt(head.getUpdatedAt());
        playlistRegistry.register(playlist);
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

  /**
   * @param playlistValidator
   *          the playlistValidator to set
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
    List<PlaylistHead> playlistInfos = this.ctx.getServer().getPlaylists(this.ctx.getStationId());

    Set<Integer> refreshed = new HashSet<Integer>(); // ids of tracks that have been updated in track registry
    for (PlaylistHead playlistInfo : playlistInfos) {

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
      Title[] tracks = new Title[trackRefs.length];
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
          }
          else if(!refreshed.contains(track.getId())){
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
    List<PlaylistHead> playlistInfos = this.ctx.getServer().getPlaylists(ctx.getStationId());

    Set<Integer> refreshed = new HashSet<Integer>(); // tracks that have been updated in track registry
    for (PlaylistHead playlistInfo : playlistInfos) {
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
  
  private static void updateMetaData(PlaylistHead head, Playlist playlist) {
    playlist.setColor(head.getColor());
    playlist.setCreatedAt(head.getCreatedAt());
    playlist.setDescription(head.getDescription());
    playlist.setName(head.getTitle());
    playlist.setUpdatedAt(head.getUpdatedAt());
  }

  private void initOnlinePlaylist(Playlist playlist, PlaylistHead playlistInfo) {
    playlist.setId(playlistInfo.getId());
    playlist.setName(playlistInfo.getTitle());
    playlist.setDescription(playlistInfo.getDescription());
    // playlist.setLength(playlistInfo.getLength());
    playlist.setShuffle(playlistInfo.isShuffled());
    playlist.setColor(playlistInfo.getColor());
    playlist.setCreatedAt(playlistInfo.getCreatedAt());
    playlist.setUpdatedAt(playlist.getUpdatedAt());
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

}
