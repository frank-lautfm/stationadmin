/**
 * 
 */
package de.stationadmin.base.track;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.lfm.backend.ProgressListener;
import de.stationadmin.lfm.backend.Track;
import de.stationadmin.lfm.backend.TrackList;
import de.stationadmin.lfmapi.Song;

/**
 * @author Frank
 * 
 */
public class TrackService implements Service {
  private static final Logger log = Logger.getLogger(TrackService.class);
  public static final String FILENAME_TITLES = "tracks.tsv";
  public static final String FILENAME_ALIASES = "aliases.tsv";
  private ExtendedTrackFormat fmt = new ExtendedTrackFormat(true);
  private SessionCtx ctx;
  private TrackRegistry trackRegistry;
  private PlaylistRecorder playlistRecorder;
  private TrackHistory trackHistory;

  /**
   * @param ctx
   * @param titleRegistry
   */
  public TrackService(SessionCtx ctx, TrackRegistry titleRegistry, Settings settings) {
    super();
    this.ctx = ctx;
    this.trackRegistry = titleRegistry;
    this.trackHistory = new TrackHistory();
    this.ctx.getStationStatus().addPropertyChangeListener("currentListeners", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        Integer listeners = (Integer) evt.getNewValue();
        if (listeners != null) {
          trackHistory.setCurrentListeners(listeners);
        }
      }
    });
    this.initSettingsObserver(settings);

  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
    if (this.playlistRecorder != null) {
      this.playlistRecorder.requestStop();
      this.playlistRecorder.interrupt();
    }
  }

  public boolean deleteTrack(int trackId) throws IOException {
    RegisteredTrack track = this.trackRegistry.getTrack(trackId);
    if (track != null && track.isOwnTrack()) {
      this.ctx.getServer().deleteTrack(ctx.getStationId(), trackId);
      this.trackRegistry.remove(trackId);
      this.saveTracks();
      return true;
    } else {
      return false;
    }
  }

  public void deleteTracks(int[] trackIds) throws IOException {
    boolean modified = false;
    for (int trackId : trackIds) {
      RegisteredTrack track = this.trackRegistry.getTrack(trackId);
      if (track != null && track.isOwnTrack()) {
        this.ctx.getServer().deleteTrack(ctx.getStationId(), trackId);
        modified = true;
        this.trackRegistry.remove(trackId);
      }
    }
    if (modified) {
      this.saveTracks();
    }
  }

  /**
   * Searchs a title that matches the given {@link TrackMatcher}. If multiple
   * results exist, only the first one is returned
   * 
   * @param text
   *          text to search for
   * @param ownTracks
   *          <code>true</code> to search only in own tracks
   * @param matcher
   *          matcher that decides wich titles are really accepted
   * @return first matching title or <code>null</code> if no title matches
   * @throws IOException
   * @throws JSONException
   */
  public DetailedTrack find(TrackQuery query, TrackMatcher matcher) throws IOException, JSONException {
    // List<DetailedTitle> titles = this.findAll(text, ownTracks, matcher,
    // true);
    // if (titles.size() > 0) {
    // return titles.get(0);
    // } else {
    // return null;
    // }
    return null;
  }

  public SearchResultSet find(TrackQuery query) throws IOException, JSONException {
    TrackList list = ctx.getServer().getTracks(ctx.getStationId(), query.getPage(), query.asFilterMap(), query.getOrderBy(),
        query.isOrderAscending());
    ArrayList<DetailedTrack> titles = new ArrayList<DetailedTrack>();
    for (Track track : list.getTracks()) {
      titles.add(new DetailedTrack(track));
    }
    return new SearchResultSet(list.getPaging().getTotalEntries(), list.getPaging().getCurrentPage(), list.getPaging().getTotalPages(), titles);

  }

  /**
   * @return the playlistRecorder
   */
  public PlaylistRecorder getPlaylistRecorder() {
    return playlistRecorder;
  }

  public String getSnippetURL(int trackId) throws IOException {
    return this.ctx.getServer().getTrackPrelistenUrl(ctx.getStationId(), trackId);
  }

  /**
   * @return the titleHistory
   */
  public TrackHistory getTrackHistory() {
    return trackHistory;
  }

  /**
   * @return the titleRegistry
   */
  public TrackRegistry getTrackRegistry() {
    return trackRegistry;
  }

  /**
   * Initializes the rcordings of playlists
   */
  public void initPlaylistRecording() {
    if (this.playlistRecorder == null) {
      try {
        this.ctx.updateStatus("initPlaylistRecording");
        // fetch history from server
        try {
          log.info("retrieve recent titles from laut.fm");
          Song[] songs = this.ctx.getLfmAPI().getLastSongs(this.ctx.getStation());

          for (Song played : songs) {
            Title title = new Title();
            title.setTitle(played.getTitle());
            title.setArtist(played.getArtist().getName());
            this.trackHistory.add(played.getStartedAt(), title);
          }
        } catch (Exception e) {
          log.error("failed to retrieve title history from laut.fm", e);
        }

        // init recorder
        log.info("init playlist recorder");
        this.playlistRecorder = new PlaylistRecorder(this.trackRegistry, this.ctx.getLfmAPI(), this.ctx.getStation(), this.ctx.getStationStatus(),
            this.trackHistory);
        this.playlistRecorder.start();
      } finally {
        this.ctx.updateStatus(null);
      }
    }
  }

  private void initSettingsObserver(Settings settings) {
    settings.addPropertyChangeListener("titleLogFile", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        trackHistory.setLogFile(StringUtils.trimToNull((String) evt.getNewValue()));
      }

    });

    settings.addPropertyChangeListener("logTitleWithListeners", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        trackHistory.setLogCurrentListeners((Boolean) evt.getNewValue());
      }

    });

  }

  /**
   * Checks if a title is used by the own or any other station
   * 
   * @param titleId
   * @return
   * @throws IOException
   */
  public boolean isTrackUsed(int titleId) throws IOException {
    RegisteredTrack title = this.trackRegistry.getTrack(titleId);
    if (title != null && title.getPlaylistIds().size() > 1) {
      return true;
    }
    return false;
  }

  private void loadLegacyIds() throws IOException {
    // TODO remove in later version
    File file = new File(this.ctx.getStationDirectory() + "/trackmapping");
    if (file.exists()) {
      FileInputStream fin = new FileInputStream(file);
      BufferedInputStream bin = new BufferedInputStream(fin);
      DataInputStream in = new DataInputStream(bin);

      int cnt = in.readInt();
      for (int i = 0; i < cnt; i++) {
        this.trackRegistry.registerLegacyId(in.readInt(), in.readInt());
      }
      in.close();
    }
  }

  public void load() throws IOException {
    this.ctx.updateStatus("loadTitles");
    this.trackRegistry.clear();
    this.trackRegistry.setBlockChangsEvts(true);
    try {
      this.loadTracks();
    } finally {
      this.trackRegistry.setBlockChangsEvts(false);
    }
    if (this.getTrackRegistry().getNumTracks() > 0) {
      this.loadAliases();
    }
    this.loadLegacyIds();
    this.ctx.updateStatus(null);
  }

  @SuppressWarnings("unchecked")
  public void loadAliases() throws IOException {
    log.info("load title aliases");
    File file = new File(this.ctx.getStationDirectory() + FILENAME_ALIASES);
    if (file.exists()) {
      FileInputStream in = new FileInputStream(file);
      try {
        List<String> lines = (List<String>) IOUtils.readLines(in, "UTF-8");
        for (String line : lines) {
          String[] parts = StringUtils.split(line, "\t");
          if (parts.length >= 3) {
            try {
              int id = Integer.parseInt(parts[0]);
              this.trackRegistry.registerAlias(id, parts[1], parts[2]);
            } catch (Exception e) {
              log.error("unable to read title alias from " + line, e);
            }
          }
        }
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }

  @SuppressWarnings("unchecked")
  public void loadTracks() throws IOException {
    log.info("load titles");
    File file = new File(this.ctx.getStationDirectory() + FILENAME_TITLES);
    if (file.exists()) {
      FileInputStream in = new FileInputStream(file);
      try {
        List<String> lines = (List<String>) IOUtils.readLines(in, "UTF-8");
        for (String line : lines) {
          Title t = fmt.fromString(line);
          if (t instanceof DetailedTrack) {
            this.trackRegistry.add((DetailedTrack) t);
          }
        }
        this.trackRegistry.fireNumTracksEvent();
      } finally {
        IOUtils.closeQuietly(in);
      }
    }
  }

  /**
   * Completely reloads all own titles. This should be used for initial loading
   * and if files have been removed with another instance / via radioadmin
   * 
   * @throws IOException
   * @throws JSONException
   */
  public void reloadOwnTitles() throws IOException, JSONException {
    log.info("reload own titles");

    for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
      if (title.isOwnTrack() && title.getPlaylistIds().size() == 0) {
        this.trackRegistry.remove(title.getId());
      }
    }

    Map<String, String> filter = new HashMap<String, String>();
    filter.put("own", "true");
    int page = 1;

    boolean requestMore = true;
    do {
      TrackList list = this.ctx.getServer().getTracks(ctx.getStationId(), page, filter, "created_at", false);
      for (Track track : list.getTracks()) {
        if (this.trackRegistry.getTrack(track.getId()) == null) {
          this.trackRegistry.registerOwnTrack(new DetailedTrack(track));
        }
      }
      requestMore = list.getPaging().getCurrentPage() < list.getPaging().getTotalPages();
      page++;
    } while (requestMore);

    this.saveTracks();
  }

  /**
   * Saves all title aliases to a file
   * 
   * @throws IOException
   */
  public void saveAliases() throws IOException {
    log.info("save title aliases");
    FileOutputStream out = new FileOutputStream(ctx.getStationDirectory() + FILENAME_ALIASES);
    OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");

    try {
      for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
        if (title.getAliases() != null) {
          for (TrackAlias alias : title.getAliases()) {
            String str = Integer.toString(title.getId()) + "\t" + alias.getArtist() + "\t" + alias.getTitle();
            writer.write(str + "\n");
          }
        }
      }
    } finally {
      writer.flush();
      IOUtils.closeQuietly(out);
    }

  }

  /**
   * Saves all own titles to a file
   * 
   * @throws IOException
   */
  public void saveTracks() throws IOException {
    log.info("save tracks");
    FileOutputStream out = new FileOutputStream(ctx.getStationDirectory() + FILENAME_TITLES);
    OutputStreamWriter writer = new OutputStreamWriter(out, "UTF-8");

    try {
      for (RegisteredTrack title : this.trackRegistry.getAllTracks()) {
        writer.write(fmt.toString(title) + "\n");
      }
    } finally {
      writer.flush();
      IOUtils.closeQuietly(out);
    }
  }

  public List<DetailedTrack> findAll(TrackQuery query, TrackMatcher matcher, boolean onlyFirst) throws IOException {
    int page = 0;
    TrackList list = null;
    ArrayList<DetailedTrack> titles = new ArrayList<DetailedTrack>();
    do {
      page++;
      list = this.ctx.getServer().getTracks(ctx.getStationId(), page, query.asFilterMap(), query.getOrderBy(), query.isOrderAscending());
      for (Track track : list.getTracks()) {
        DetailedTrack title = new DetailedTrack(track);
        if (matcher.matches(title)) {
          titles.add(title);
          if (onlyFirst) {
            break;
          }
        }
      }
    } while (list.getPaging().getCurrentPage() < list.getPaging().getTotalPages() && (!onlyFirst || titles.size() == 0));
    return titles;
  }

  /**
   * Finds titles that match the given artist / title
   * 
   * @param artist
   * @param title
   * @return
   * @throws IOException
   * @throws JSONException
   */
  public List<DetailedTrack> search(final String artist, final String title) throws IOException, JSONException {
    TrackQuery query = new TrackQuery();
    query.setArtist(artist);
    query.setTitle(title);
    return this.findAll(query, new TrackMatcher() {

      @Override
      public boolean matches(DetailedTrack result) {
        return Title.isArtistEqual(artist, result.getArtist()) && title.equalsIgnoreCase(result.getTitle());
      }

    }, false);

  }

  public DetailedTrack getTrack(int titleId) throws IOException {
    DetailedTrack title = this.trackRegistry.getTrack(titleId);
    if (title == null) {
      Track track = this.ctx.getServer().getTrack(ctx.getStationId(), titleId);
      if (track != null) {
        title = new DetailedTrack(track);
      }
    }

    return title;
  }

  public void synchronize() throws IOException {
    this.ctx.updateStatus("updateOwnTitles");
    try {
      this.updateOwnTitles();
    } catch (JSONException e) {
      log.error("unable to update own titles", e);
    }
    this.saveTracks();
    this.saveAliases();
  }

  public void updateOwnTitles() throws IOException, JSONException {

    Map<String, String> filter = new HashMap<String, String>();
    filter.put("own", "true");
    int page = 1;

    boolean requestMore = true;
    do {
      TrackList list = this.ctx.getServer().getTracks(ctx.getStationId(), page, filter, "created_at", false);
      boolean hasUnknown = false;
      for (Track track : list.getTracks()) {
        if (this.trackRegistry.getTrack(track.getId()) == null) {
          hasUnknown = true;
          this.trackRegistry.registerOwnTrack(new DetailedTrack(track));
        }
      }
      requestMore = hasUnknown && list.getPaging().getCurrentPage() <= list.getPaging().getTotalPages();
      page++;
    } while (requestMore);

  }

  /**
   * Uploads an mp3 file to the laut.fm server
   * 
   * @param file
   *          file to upload
   * @param progressListener
   *          progress listener - may be <code>null</code>
   * @return <code>true</code> on success
   * @throws IOException
   */
  public boolean upload(File file, ProgressListener progressListener) throws IOException {
    return false; // this.ctx.getServer().upload(file, progressListener);
  }

  /**
   * Updates a title on the server
   * 
   * @param title
   * @return
   * @throws IOException
   */
  public boolean updateTrack(DetailedTrack title) throws IOException {
    this.ctx.getServer().updateTrack(this.ctx.getStationId(), title.asLfmAPITrack());
    this.saveTracks();
    return true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.initPlaylistRecording();
  }

}
