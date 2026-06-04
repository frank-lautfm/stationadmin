package de.stationadmin.base;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.prefs.Preferences;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.codehaus.jackson.map.ObjectMapper;
import org.json.JSONException;

import com.thoughtworks.xstream.XStream;

import de.stationadmin.base.backup.BackupService;
import de.stationadmin.base.config.ClientConfigurationService;
import de.stationadmin.base.loganalyzer.LogAnalyzerService;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.statistics.StatisticsService;
import de.stationadmin.base.subscription.SubscriptionService;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.base.tools.StreamingServerResolver;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.util.XStreamFactory;
import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfm.backend.LiveAccessData;
import de.stationadmin.lfm.backend.LogEntry;
import de.stationadmin.lfm.backend.Station;
import de.stationadmin.lfmapi.LautfmService;
import de.stationadmin.streamlive.MP3Streamer;

/**
 * Main class for administrative access
 * 
 * @author Frank Korf
 */
public class StationAdminClient {
  private static final Logger log = LogManager.getLogger(StationAdminClient.class);

  public static long TIMESTAMP_RADIOADMIN_SWITCH = 1481806854618l;

  private SessionCtx sessionCtx;
  private TaskExecutionService taskExecutionService;
  private TrackService trackService;
  private PlaylistService playlistService;
  private SubscriptionService subscriptionService;
  private Schedule schedule;
  private StatisticsService statisticsService;
  private LogAnalyzerService logAnalyzerService;
  private ClientConfigurationService clientConfigService;
  private List<Service> services = new ArrayList<Service>();

  private MP3Streamer mp3Streamer;

  private Settings settings = new Settings();

  private TagManager tagManager;
  private BackupService backupService;

  public StationAdminClient(LautfmAdminService service, Station station) throws IOException, JSONException {
    Properties props = this.readStationAdminConfig();
    if (props.containsKey("proxy.host")) {
      System.setProperty("http.proxyHost", props.getProperty("proxy.host"));
      System.setProperty("http.proxyPort", props.getProperty("proxy.port"));
    }

    String sAdminDefaultDir = System.getProperty("defaultDir", "laut.fm/StationAdmin/");

    if (!(new File(System.getProperty("user.home") + File.separatorChar + sAdminDefaultDir)).exists()
        && new File(System.getProperty("user.home") + File.separatorChar + "laut.fm/beta").exists()) {
      File old = new File(System.getProperty("user.home") + File.separatorChar + "laut.fm/beta");
      old.renameTo(new File(System.getProperty("user.home") + File.separatorChar + sAdminDefaultDir));
    }

    String dataDirectory = props.getProperty("data.dir", System.getProperty("user.home") + File.separatorChar + sAdminDefaultDir);
    String settingsDirectory = props.getProperty("settings.dir", dataDirectory + station.getName().toLowerCase());

    this.sessionCtx = new SessionCtx(service, new LautfmService(), station.getId(), station.getName().toLowerCase(), dataDirectory, settingsDirectory);
    this.sessionCtx.setRole(Role.valueOf(station.getRole().toUpperCase()));

    this.taskExecutionService = new TaskExecutionService(this);

    TrackRegistry titleRegistry = new TrackRegistry();
    PlaylistRegistry playlistRegistry = new PlaylistRegistry();
    this.clientConfigService = new ClientConfigurationService(sessionCtx, this.settings);
    this.trackService = new TrackService(this.sessionCtx, titleRegistry, this.settings);
    this.playlistService = new PlaylistService(this.sessionCtx, titleRegistry, playlistRegistry, this.settings);
    this.schedule = new Schedule(sessionCtx, playlistRegistry);
    this.logAnalyzerService = new LogAnalyzerService(this.sessionCtx, this.trackService);
    this.tagManager = new TagManager(this.sessionCtx, this.trackService, this.playlistService.getPlaylistRegistry(), logAnalyzerService, this.schedule);
    this.statisticsService = new StatisticsService(this.sessionCtx, this.settings, this.logAnalyzerService);
    this.subscriptionService = new SubscriptionService(this.sessionCtx, titleRegistry);

    this.backupService = new BackupService(sessionCtx, this.playlistService, this.trackService, this.tagManager, this.schedule, this.taskExecutionService, this.settings);

    this.clientConfigService.register(this.playlistService);
    this.clientConfigService.register(this.tagManager);

    this.services.addAll(Arrays.asList(this.taskExecutionService, this.trackService, this.tagManager, this.playlistService, this.schedule, this.statisticsService,
        this.backupService, this.subscriptionService, this.logAnalyzerService, this.clientConfigService));

    this.loadSettings();

  }

  private Properties readStationAdminConfig() {
    Properties props = new Properties();
    try {
      InputStream confStream = this.getClass().getClassLoader().getResourceAsStream("stationadmin-default.conf");
      if (confStream != null) {
        props.load(confStream);
      }
    } catch (IOException e) {
      // forget it...
    }
    try {
      InputStream confStream = this.getClass().getClassLoader().getResourceAsStream("stationadmin.conf");
      if (confStream != null) {
        props.load(confStream);
      }
    } catch (IOException e) {
      // forget it...
    }
    return props;

  }

  public void close() {
    for (Service service : this.services) {
      service.close();
    }
    // this.sessionCtx.getServer().logout();
  }

  /**
   * Gets the playlist registry
   * 
   * @return playlist registry
   */
  @Deprecated
  public PlaylistRegistry getPlaylistRegistry() {
    return playlistService.getPlaylistRegistry();
  }

  public void autoSynchronize() throws IOException, JSONException {
    if (this.settings.getAutoSynchronisation() == null) {
      return;
    }
    switch (this.settings.getAutoSynchronisation()) {
    case FULL:
      this.synchronize();
      break;
    case MODIFIED_PLAYLISTS:
      int[] modified = this.playlistService.getPlaylistModificationDetector().detectModifiedPlaylists();
      if (modified != null && modified.length > 0) {
        this.playlistService.synchronize(modified);
        this.clientConfigService.synchronize();
      }
      break;
    case NONE:
      break;

    }
  }

  public void initBackgroundTasks() {
    for (Service service : this.services) {
      service.initBackgroundTasks();
    }
  }

  /**
   * @return the playlistService
   */
  public PlaylistService getPlaylistService() {
    return playlistService;
  }

  /**
   * Gets the playlist schedule for the station
   * 
   * @return schedule
   */
  public Schedule getSchedule() {
    return schedule;
  }

  /**
   * @return the sessionCtx
   */
  public SessionCtx getSessionCtx() {
    return sessionCtx;
  }

  /**
   * Gets the configuraiton settings
   * 
   * @return settings
   */
  public Settings getSettings() {
    return settings;
  }

  /**
   * Gets the name of the station
   * 
   * @return
   */
  public String getStation() {
    return sessionCtx.getStation();
  }

  public int getStationId() {
    return sessionCtx.getStationId();
  }

  /**
   * Gets the station status. This includes data like current listeners, rank,
   * current title or current playlist
   * 
   * @return station status
   */
  public StationStatus getStationStatus() {
    return this.sessionCtx.getStationStatus();
  }

  /**
   * Gets the streaming server on which this station is running
   * 
   * @return streaming server
   */
  public String getStreamingServer() {
    return StreamingServerResolver.getStreamingServer(this.sessionCtx.getStation());
  }

  /**
   * @return the statisticsService
   */
  public StatisticsService getStatisticsService() {
    return statisticsService;
  }

  // /**
  // * Gets the URL that is used by {@link #isUpToDate()} to check if this
  // version
  // * of Station Admin is up to date
  // *
  // * @return URL for update check
  // */
  // public String getUpdateCheckURL() {
  // return updateCheckURL;
  // }

  public Status getStatus() {
    return this.sessionCtx.getStatus();
  }

  /**
   * Gets the title registry
   * 
   * @return title registry
   */
  @Deprecated
  public TrackRegistry getTitleRegistry() {
    return this.trackService.getTrackRegistry();
  }

  /**
   * @return the titleService
   */
  public TrackService getTrackService() {
    return trackService;
  }

  /**
   * @return the titleTagManager
   */
  public TagManager getTagManager() {
    return tagManager;
  }

  private XStream getXStream() {
    XStream xstream = XStreamFactory.newXStream();
    xstream.alias("playlist", Playlist.class);
    xstream.alias("entry", Entry.class);
    xstream.alias("title", RegisteredTrack.class);
    xstream.alias("settings", Settings.class);
    xstream.alias("scheduledShow", Schedule.Entry.class);
    return xstream;
  }

  /**
   * Checks if the radio is started
   * 
   * @return
   * @throws IOException
   */
  public boolean isRadioStarted() throws IOException {
    return this.sessionCtx.getServer().isRunning(this.sessionCtx.getStationId());
  }

  public LiveAccount getLiveAccount() throws IOException {
    LiveAccessData data = this.sessionCtx.getServer().getLiveAccessData(this.sessionCtx.getStationId());
    if (data != null && data.getPassword() != null) {
      LiveAccount account = new LiveAccount();
      account.setPort(data.getPort());
      account.setServer(data.getServer());
      account.setUser(data.getUser());
      account.setPassword(data.getPassword());
      return account;
    }
    return null;
  }

  public LogEntry[] getLogs(int days) throws IOException {
    return this.sessionCtx.getServer().getLogs(this.sessionCtx.getStationId(), days);
  }

  public boolean isLiveEnabled() throws IOException {
    return this.sessionCtx.isLiveEnabled();
  }

  public boolean isUploadAvailable() throws IOException {
    return true; // FIXME this.sessionCtx.getServer().isUploadAvailable();
  }

  /**
   * Loads all persisted data from disk
   * 
   * @throws IOException
   */
  public void load() throws IOException {
    try {
      for (Service service : this.services) {
        service.load();
      }
      this.updateStatus(null);
    } catch (IOException e) {
      log.error("error while loading data", e);
      throw e;
    }
  }

  public int updateVersionInfo() {
    int previous = Preferences.userRoot().getInt("stationadmin." + sessionCtx.getStation() + ".version", 0);
    if (previous != Version.NUMBER) {
      Preferences.userRoot().putInt("stationadmin." + sessionCtx.getStation() + ".version", Version.NUMBER);
    }
    return previous;
  }

  private void loadSettings() {
    try {
      File settingsFile = new File(this.sessionCtx.getSettingsDirectory() + File.separatorChar + "settings.json");
      if (settingsFile.exists()) {
        ObjectMapper mapper = new ObjectMapper();
        Settings settings = mapper.readValue(settingsFile, Settings.class);
        this.settings.copyFrom(settings);
      } else {
        if(!loadSettingsLegacy()) {
          // create a new file
          saveSettings();
        }
      }
    } catch (IOException e) {
      log.error("failed to load settings", e);
    }

  }

  private boolean loadSettingsLegacy() {
    try {
      File settingsFile = new File(this.sessionCtx.getSettingsDirectory() + File.separatorChar + "settings.xml");
      if (new File(this.sessionCtx.getDataDirectory() + "settings.xml").lastModified() > settingsFile.lastModified()) {
        settingsFile = new File(this.sessionCtx.getDataDirectory() + "settings.xml");
      }
      if (settingsFile.exists()) {
        XStream xstream = this.getXStream();
        FileInputStream settingsStream = new FileInputStream(settingsFile);
        Settings settings = (Settings) xstream.fromXML(settingsStream);
        settingsStream.close();
        if (settings.getBackupDirectory() == null) {
          settings.setBackupDirectory(this.backupService.getBackupDirectory());
        }
        this.settings.copyFrom(settings);
        this.settings.setSaveClientSettings(false);
        saveSettings();
        return true;
      }
    } catch (IOException e) {
      log.error("failed to load settings", e);
    }
    return false;

  }

  /**
   * Persists settings
   * 
   * @throws IOException
   */
  public void saveSettings() throws IOException {
    try {
      log.info("save settings");
      String dir = this.sessionCtx.getSettingsDirectory();
      new File(dir).mkdirs();
      ObjectMapper mapper = new ObjectMapper();
      mapper.writeValue(new File(this.sessionCtx.getSettingsDirectory() + File.separatorChar + "settings.json"), this.settings);
    } catch (IOException e) {
      log.error("error while saving setting", e);
      throw e;
    }

  }

  /**
   * Starts the radio
   * 
   * @return
   * @throws IOException
   */
  public boolean startRadio() throws IOException {
    this.sessionCtx.getServer().start(this.sessionCtx.getStationId());
    return true;
  }

  /**
   * Synchronizes against the laut.fm server.
   * <p>
   * This includes:
   * <ul>
   * <li>retrieving the latest playlists
   * <li>retrieving the latest version of the schedule
   * <li>persisting this data in the local data directory
   * </ul>
   * 
   * @throws IOException
   * @throws JSONException
   */
  public void synchronize() throws IOException, JSONException {
    log.info("synchronize");
    this.sessionCtx.checkSession();
    try {
      this.trackService.synchronize();
      this.playlistService.synchronize(true);
      this.tagManager.synchronize();
      this.schedule.synchronize();
      this.trackService.getTrackRegistry().removeUnused();
      this.trackService.saveTracks();
      this.clientConfigService.synchronize();
    } catch (IOException e) {
      log.error("error during synchronization", e);
      throw e;
    } finally {
      this.updateStatus(null);
    }
  }

  void updateStatus(String key, String... parameters) {
    if (key != null) {
      this.sessionCtx.setStatus(new Status(key, parameters));
    } else {
      this.sessionCtx.setStatus(null);
    }
  }

  /**
   * @return the backupService
   */
  public BackupService getBackupService() {
    return backupService;
  }

  public SubscriptionService getSubscriptionService() {
    return subscriptionService;
  }

  public TaskExecutionService getTaskExecutionService() {
    return taskExecutionService;
  }

  public LogAnalyzerService getLogAnalyzerService() {
    return logAnalyzerService;
  }

  /**
   * @return the mp3Streamer
   */
  public MP3Streamer getMp3Streamer() {
    return mp3Streamer;
  }

  /**
   * @param mp3Streamer the mp3Streamer to set
   */
  public void setMp3Streamer(MP3Streamer mp3Streamer) {
    this.mp3Streamer = mp3Streamer;
  }

  public void registerErrorHandler(ErrorHandler errorHandler) {
    this.sessionCtx.setErrorHandler(errorHandler);
  }

  public ClientConfigurationService getClientConfigService() {
    return clientConfigService;
  }

}
