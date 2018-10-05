/**
 * (c) 2000 - 2010 Brainware, Inc
 */
package de.stationadmin.base.backup;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TimerTask;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;
import java.util.zip.ZipOutputStream;

import javax.xml.bind.DatatypeConverter;

import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.exporter.PlaylistBackupExporter;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.schedule.Schedule;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.base.track.TrackService;

/**
 * @author korf
 * 
 */
public class BackupService implements Service {
  private static final Logger log = Logger.getLogger(BackupService.class);

  private static final String PATH_PLAYLIST_ARCHIVE = "playlists/archive/";
  private static final String PATH_PLAYLIST_ONLINE = "playlists/";

  private SessionCtx sessionCtx;
  private PlaylistService playlistService;
  private TrackService trackService;
  private TagManager trackManager;
  private PlaylistRegistry playlistRegistry;
  private Schedule schedule;
  private TaskExecutionService taskService;

  private String backupDirectory;
  private BackupFrequency backupFrequency = BackupFrequency.NEVER;

  /**
   * @param playlistService
   * @param titleService
   * @param titleTagManager
   * @param schedule
   */
  public BackupService(SessionCtx sessionCtx, PlaylistService playlistService, TrackService titleService, TagManager titleTagManager, Schedule schedule,
      TaskExecutionService taskService, Settings settings) {
    super();
    this.sessionCtx = sessionCtx;
    this.playlistService = playlistService;
    this.trackService = titleService;
    this.trackManager = titleTagManager;
    this.schedule = schedule;
    this.taskService = taskService;
    this.playlistRegistry = playlistService.getPlaylistRegistry();

    this.backupDirectory = settings.getBackupDirectory();
    settings.addPropertyChangeListener("backupDirectory", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null) {
          backupDirectory = (String) evt.getNewValue();
        }
      }
    });
    if (this.backupDirectory == null) {
      this.backupDirectory = sessionCtx.getStationDirectory() + "backup" + File.separatorChar;
    }

    this.backupFrequency = BackupFrequency.values()[settings.getBackupFrequency()];
    settings.addPropertyChangeListener("backupFrequency", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        Integer value = (Integer) evt.getNewValue();
        if (value != null) {
          backupFrequency = BackupFrequency.values()[value.intValue()];
        }
      }
    });

  }

  /**
   * Checks for which of the currently registered playlist a backup is available
   * in the given backup file
   * 
   * @param file
   * @return
   * @throws IOException
   */
  public Map<Integer, Boolean> checkPlaylistBackupAvailability(File file) throws IOException {
    HashMap<Integer, Boolean> availability = new HashMap<Integer, Boolean>();
    ZipFile zip = new ZipFile(file);
    for (Playlist playlist : this.playlistRegistry.getAllPlaylists()) {
      availability.put(playlist.getId(), zip.getEntry(this.getFilename(playlist)) != null);
    }
    zip.close();
    return availability;
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
  }

  public void createBackup(File file) throws IOException {
    try (FileOutputStream out = new FileOutputStream(file)) {

      try (ZipOutputStream zip = new ZipOutputStream(out)) {
        // create playlist backups
        PlaylistBackupExporter exporter = new PlaylistBackupExporter();
        for (Playlist playlist : this.playlistRegistry.getAllPlaylists()) {
          String playlistStr = exporter.toString(playlist);
          ZipEntry entry = new ZipEntry(getFilename(playlist));
          zip.putNextEntry(entry);
          zip.write(playlistStr.getBytes("UTF-8"));
        }

        // create tag backups
        for (File tagFile : this.trackManager.getFiles()) {
          ZipEntry entry = new ZipEntry("tags/" + tagFile.getName());
          zip.putNextEntry(entry);
          FileInputStream in = new FileInputStream(tagFile);
          byte[] data = IOUtils.toByteArray(in);
          in.close();
          zip.write(data);
        }

        // create schedule backup
        {
          if (schedule.getEntries().size() > 1) {
            ZipEntry entry = new ZipEntry("schedule/schedule.xml");
            zip.putNextEntry(entry);
            ByteArrayOutputStream scheduleOut = new ByteArrayOutputStream();
            schedule.save(scheduleOut);
            zip.write(scheduleOut.toByteArray());
          }
        }

        // create task backup
        {
          for (File taskFile : this.taskService.getTaskFiles()) {
            ZipEntry entry = new ZipEntry("tasks/" + taskFile.getName());
            zip.putNextEntry(entry);
            FileInputStream in = new FileInputStream(taskFile);
            byte[] data = IOUtils.toByteArray(in);
            in.close();
            zip.write(data);
          }

        }
      }
    }
  }

  public List<String> getArchivePlaylists(File file) throws IOException {
    List<String> names = new ArrayList<String>();
    ;
    try (ZipFile zip = new ZipFile(file)) {
      Enumeration<? extends ZipEntry> entries = zip.entries();
      while (entries.hasMoreElements()) {
        ZipEntry entry = entries.nextElement();
        if (entry.getName().startsWith(PATH_PLAYLIST_ARCHIVE)) {
          String name = entry.getName().substring(PATH_PLAYLIST_ARCHIVE.length());
          name = FilenameUtils.getBaseName(name);
          names.add(name);
        }
      }
      return names;
    }
  }

  public Map<String, String> getAvailableTags(File file) throws IOException {
    ZipFile zip = new ZipFile(file);
    Enumeration<? extends ZipEntry> entries = zip.entries();
    HashMap<String, String> availableTags = new HashMap<String, String>();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (entry.getName().startsWith("tags") && entry.getName().length() > 6) {
        String filename = entry.getName();
        DataInputStream in = new DataInputStream(zip.getInputStream(entry));
        String name = in.readUTF();
        if (name == null || name.length() == 0) {
          in = new DataInputStream(zip.getInputStream(entry));
          in.read(new byte[4]);
          name = in.readUTF();
        }
        availableTags.put(name, filename);
      }
    }
    zip.close();

    return availableTags;
  }

  public List<ScheduledTask> getAvailableTasks(File file) throws IOException {
    ZipFile zip = new ZipFile(file);
    Enumeration<? extends ZipEntry> entries = zip.entries();
    List<ScheduledTask> availableTasks = new ArrayList<ScheduledTask>();
    while (entries.hasMoreElements()) {
      ZipEntry entry = entries.nextElement();
      if (entry.getName().startsWith("tasks") && entry.getName().length() > 6) {
        try {
          ScheduledTask task = this.taskService.load(zip.getInputStream(entry));
          availableTasks.add(task);
        } catch (IOException e) {
        }
      }
    }
    zip.close();

    return availableTasks;

  }

  /**
   * @return the backupDirectory
   */
  public String getBackupDirectory() {
    return backupDirectory;
  }

  private String getFilename(Playlist playlist) {
    StringBuilder buf = new StringBuilder();
    for (char c : playlist.getName().toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
        buf.append(c);
      } else {
        buf.append('_');
      }
    }

    String dir = playlist.getType() == PlaylistType.ARCHIVED ? PATH_PLAYLIST_ARCHIVE : PATH_PLAYLIST_ONLINE;
    return dir + buf.toString() + ".lfm";
  }

  public boolean isScheduleAvailable(File file) throws IOException {
    ZipFile zip = new ZipFile(file);
    ZipEntry entry = zip.getEntry("schedule/schedule.xml");
    zip.close();
    return entry != null;
  }

  /**
   * @see de.stationadmin.base.Service#load()
   */
  @Override
  public void load() throws IOException {
  }

  public boolean restoreArchivePlaylist(File file, String name) throws IOException, PlaylistValidationException {
    try (ZipFile zip = new ZipFile(file)) {
      ZipEntry entry = zip.getEntry(PATH_PLAYLIST_ARCHIVE + name + ".lfm");
      if (entry != null) {
        String content = IOUtils.toString(zip.getInputStream(entry), "UTF-8");
        this.playlistService.importArchivedPlaylist(name + ".lfm", content);
      }

      return false;
    }
  }

  /**
   * Restores an online playlist from the backup file
   * 
   * @param file
   * @param playlistId @return.
   * @throws IOException
   * @throws JSONException
   * @throws PlaylistValidationException
   */
  public boolean restorePlaylist(File file, int playlistId) throws IOException, JSONException, PlaylistValidationException {
    return restorePlaylist(file, playlistId, false);
  }

  public boolean restorePlaylist(File file, int playlistId, boolean legacy) throws IOException, JSONException, PlaylistValidationException {
    try (ZipFile zip = new ZipFile(file)) {
      Playlist playlist = this.playlistRegistry.getPlaylist(playlistId);
      if (playlist != null) {
        ZipEntry entry = zip.getEntry(this.getFilename(playlist));
        if (entry != null) {
          playlist.removeEntries(new ArrayList<Entry>(playlist.getEntries()));
          String playlistStr = IOUtils.toString(zip.getInputStream(entry), "UTF-8");

          this.restorePlaylistLocalData(playlist, playlistStr);

          TrackImportHandler importHandler = new TrackImportHandler(this.trackService, this.trackManager, playlist, 0);
          importHandler.add(playlistStr);
          importHandler.resolveTags();
          importHandler.addTracksToPlaylist(legacy);
          this.playlistService.savePlaylist(playlist);

          return true;
        }
      }
      return false;
    }
  }

  @SuppressWarnings("unchecked")
  private boolean restorePlaylistLocalData(Playlist playlist, String playlistStr) {
    String[] lines = StringUtils.split(playlistStr, "\n\r");
    if (playlistStr.contains("<playlistcfg")) {
      // < Version 2.3
      StringBuilder xmlBuf = new StringBuilder();
      for (int i = 0; i < lines.length && lines[i].startsWith("# "); i++) {
        xmlBuf.append(lines[i].substring(2));
        xmlBuf.append('\n');
      }
      try {
        playlist.setLocalDataFromXML(xmlBuf.toString());
        return true;
      } catch (Exception e) {
        log.error("unable to restore local data for " + playlist.getName(), e);
        return false;
      }
    } else {
      List<String> properties = new ArrayList<String>();
      StringBuffer tsMapBuffer = new StringBuffer();
      for (int i = 0; i < lines.length && lines[i].startsWith("# "); i++) {
        if (!lines[i].startsWith("# id")) {
          if (lines[i].startsWith("# tsmap")) {
            int p = lines[i].indexOf('=');
            if (p > 0 && p < lines[i].length() - 2) {
              tsMapBuffer.append(lines[i].substring(p + 1).trim());
            }
          } else {
            properties.add(lines[i].substring(2));
          }
        }
      }

      // restore timestamp map
      Map<Integer, Long> timestampMap = null;
      if (tsMapBuffer.length() > 0) {
        try (ByteArrayInputStream in = new ByteArrayInputStream(DatatypeConverter.parseBase64Binary(tsMapBuffer.toString()))) {
          try (ObjectInputStream objIn = new ObjectInputStream(in)) {
            timestampMap = (Map<Integer, Long>) objIn.readObject();
          }
        } catch (Exception e) {
          log.error("unable to restore timestampes for playlist " + playlist.getName(), e);
        }
      }

      playlist.setTimestampMap(timestampMap);
      playlist.setProperties(properties);
      return true;
    }
  }

  public void restoreSchedule(File file) throws IOException, JSONException {
    ZipFile zip = new ZipFile(file);
    try {
      ZipEntry entry = zip.getEntry("schedule/schedule.xml");
      if (entry != null) {
        InputStream in = zip.getInputStream(entry);
        this.schedule.load(in);
        this.schedule.submitToServer();
        this.schedule.save();
      }
    } finally {
      zip.close();
    }

  }

  public void restoreTag(File file, String tagname, String filename) throws IOException {
    ZipFile zip = new ZipFile(file);
    ZipEntry entry = zip.getEntry(filename);
    Tag tag = this.trackManager.getTag(tagname);
    if (tag == null) {
      tag = this.trackManager.addStaticTag(tagname);
    }
    ((StaticTag) tag).writeRaw(zip.getInputStream(entry));
    this.trackManager.updateTagOnServer(tagname);
    zip.close();
  }

  public void restoreTask(ScheduledTask task) throws IOException {
    this.taskService.configureScheduledTask(task);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.startBackupCreationTask();

  }

  public void startBackupCreationTask() {
    this.sessionCtx.getTimer().schedule(new BackupAutocreationTask(), 1000 * 30, 1000 * 60 * 30);
  }

  /**
   * @see de.stationadmin.base.Service#synchronize()
   */
  @Override
  public void synchronize() throws IOException {
    // nothing to do
  }

  private class BackupAutocreationTask extends TimerTask {
    private String prefix;
    private long latestBackup = 0; // time of newest backup file

    BackupAutocreationTask() {
      this.prefix = sessionCtx.getStation() + "-";
      this.findLatestBackup();

    }

    private void findLatestBackup() {
      File dir = new File(backupDirectory);
      File[] files = dir.listFiles();
      if (files != null) {
        for (File file : files) {
          if (file.getName().startsWith(prefix) && file.getName().endsWith(".zip")) {
            if (file.lastModified() > this.latestBackup) {
              latestBackup = file.lastModified();
            }
          }
        }
      }
    }

    /**
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      if (backupFrequency != BackupFrequency.NEVER) {
        log.debug("check if backup is due");
        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(System.currentTimeMillis());
        switch (backupFrequency) {
        case DAILY:
          cal.add(Calendar.DAY_OF_MONTH, -1);
          break;
        case WEEKLY:
          cal.add(Calendar.WEEK_OF_YEAR, -1);
          break;
        case MONTHLY:
          cal.add(Calendar.MONTH, -1);
          break;
        }

        long threshold = cal.getTimeInMillis();
        if (this.latestBackup < threshold) {
          String filename = prefix + new SimpleDateFormat("yyyy-MM-dd").format(new Date()) + ".zip";
          File file = new File(FilenameUtils.concat(backupDirectory, filename));
          try {
            createBackup(file);
          } catch (Exception e) {
            log.error("creation of backup failed", e);
          }
        }
      }
    }
  }

}
