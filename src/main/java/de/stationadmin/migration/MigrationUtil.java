/**
 * 
 */
package de.stationadmin.migration;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;

import de.emjoy.stationadmin.base.Settings;
import de.emjoy.stationadmin.base.StationAdminClient;
import de.emjoy.stationadmin.base.playlist.Playlist;
import de.emjoy.stationadmin.base.playlist.Playlist.PlaylistType;
import de.emjoy.stationadmin.base.playlist.shuffle.TagWeight;
import de.emjoy.stationadmin.base.title.RegisteredTitle;
import de.emjoy.stationadmin.base.title.TitleAlias;
import de.emjoy.stationadmin.base.titletag.DynamicTitleTag;
import de.emjoy.stationadmin.base.titletag.StaticTitleTag;
import de.emjoy.stationadmin.raw.LautServerAccess;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.base.tag.DynamicTag;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.RegisteredTrack;

/**
 * @author korf
 * 
 */
public class MigrationUtil {
  private static final Logger log = Logger.getLogger(MigrationUtil.class);
  private String station;

  private de.stationadmin.base.StationAdminClient clientV4;
  private StationAdminClient clientV3;
  private Map<Integer, Integer> trackIdMap = new HashMap<Integer, Integer>();

  private MessageReceiver messageReceiver;
  private boolean reloadTrackmapping = false;

  /**
   * @param station
   */
  public MigrationUtil(de.stationadmin.base.StationAdminClient clientV4) {
    super();
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    this.clientV4 = clientV4;
    this.station = clientV4.getStation();
  }

  private void message(String msg) {
    if (this.messageReceiver != null) {
      this.messageReceiver.onMessage(msg);
    }
  }

  public void init() throws Exception {
    this.clientV3 = new StationAdminClient(new LautServerAccess(), this.station, null);
    this.clientV3.load();
    this.message("Version 3: " + this.clientV3.getTitleService().getTitleRegistry().getNumTitles() + " Titel, " + this.clientV3.getPlaylistService().getPlaylistRegistry().getAllPlaylists().size()
        + " Playlists");
    log.info(this.clientV3.getTitleService().getTitleRegistry().getNumTitles() + " tracks");
    log.info(this.clientV3.getPlaylistService().getPlaylistRegistry().getAllPlaylists().size() + " playlists");

    
    InputStream trackMappingStream = this.getClass().getClassLoader().getResourceAsStream("trackmapping");
    this.trackIdMap = readTrackMapping(trackMappingStream);
    
    /*
    String file = this.clientV4.getSessionCtx().getDataDirectory() + "/trackmapping";
    if (new File(file).exists() && !reloadTrackmapping) {
      log.info("read track mapping");
      this.trackIdMap = this.readTrackMapping(file);
    } else {
      this.downloadTrackMapping(file);
    }
    this.writeStationTrackMapping();
    */

  }
  
  public void checkTracks() {
    
    message("Überprüfe ob alle verwendeten Tracks im neuen Radioadmin vorhanden sind...");
    int cnt = 0;
    int found = 0;
    for(RegisteredTitle title : this.clientV3.getTitleService().getTitleRegistry().getAllTitles()) {
      if(title.getPlaylistIds().size() > 0) {
        cnt++;
        if(!this.trackIdMap.containsKey(title.getId())) {
          message("Fehlender Track: " + title.getId() + " " + title.getArtist() + " - " + title.getTitle());
          
        }
        else {
          found++;
        }
      }
    }
    message(found + " von " + cnt + " Tracks gefunden");
    
  }

  public boolean isTrackmappingDownloaded() {
    String file = this.clientV4.getSessionCtx().getDataDirectory() + "/trackmapping";
    return new File(file).exists();
  }

  private Map<Integer, Integer> readTrackMapping(String file) throws IOException {
    FileInputStream fin = new FileInputStream(file);
    return readTrackMapping(fin);

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


  private void writeTrackMapping(Map<Integer, Integer> map, String file) throws IOException {
    FileOutputStream fout = new FileOutputStream(file);
    BufferedOutputStream bout = new BufferedOutputStream(fout, 4096);
    DataOutputStream out = new DataOutputStream(bout);
    out.writeInt(map.size());
    for (Entry<Integer, Integer> entry : map.entrySet()) {
      out.writeInt(entry.getKey());
      out.writeInt(entry.getValue());
    }
    out.close();

  }

  private void writeStationTrackMapping() throws IOException {
    String file = this.clientV4.getSessionCtx().getStationDirectory() + "/trackmapping";

    HashMap<Integer, Integer> stationTrackMap = new HashMap<Integer, Integer>();
    for (Entry<Integer, Integer> entry : this.trackIdMap.entrySet()) {
      if (this.clientV4.getTrackService().getTrackRegistry().getTrack(entry.getValue()) != null) {
        stationTrackMap.put(entry.getKey(), entry.getValue());
      }
    }

    this.writeTrackMapping(stationTrackMap, file);

  }

  private void downloadTrackMapping(String file) throws Exception {
    this.message("Download des Track-Id-Mappings - dies kann einige Minuten dauern");
    this.trackIdMap = this.clientV4.getSessionCtx().getServer().getTrackMappings();
    this.writeTrackMapping(this.trackIdMap, file);
  }

  public void migrateTags() throws Exception {
    this.message("Migriere Tags");
    for (StaticTitleTag stag : clientV3.getTitleTagManager().getStaticTags()) {
      this.message("Tag: " + stag.getName());
      log.info("tag " + stag.getName());
      int[] ids = clientV3.getTitleTagManager().getTitleIds(stag.getName());

      // delete existing tag to get rid of tagged tracks
      try {
        clientV4.getTagManager().deleteTag(stag.getName());
      } catch (Exception e) {

      }

      StaticTag stag4 = new StaticTag();
      stag4.setName(stag.getName());
      stag4.setGroup(stag.getGroup());

      clientV4.getTagManager().saveStaticTag(stag4);

      int[] newIds = new int[ids.length];
      int idx = 0;
      for (int id : ids) {
        Integer idv4 = this.trackIdMap.get(id);
        if (idv4 != null && this.clientV4.getTrackService().getTrackRegistry().getTrack(idv4) != null) {
          newIds[idx++] = idv4;
        }
      }

      if (idx < newIds.length) {
        int[] tmp = new int[idx];
        System.arraycopy(newIds, 0, tmp, 0, idx);
        newIds = tmp;
      }

      this.message("tagge " + idx + " Titel");
      log.info("tagging " + idx + " tracks");
      try {
        clientV4.getTagManager().tagTracks(stag.getName(), newIds);
      } catch (Exception e) {
        e.printStackTrace();
      }
    }

    for (DynamicTitleTag dtag : clientV3.getTitleTagManager().getDynamicTags()) {
      this.message("Tag: " + dtag.getName());
      DynamicTag dtag4 = new DynamicTag();
      dtag4.setAlbums(dtag.getAlbums());
      dtag4.setArtists(dtag.getArtists());
      dtag4.setGroup(dtag.getGroup());
      dtag4.setMaxLength(dtag.getMaxLength());
      dtag4.setMinLength(dtag.getMinLength());
      dtag4.setName(dtag.getName());
      dtag4.setPlayedWithin(dtag.getPlayedWithin());
      dtag4.setPlayedWithinMaxHour(dtag.getPlayedWithinMaxHour());
      dtag4.setPlayedWithinMinHour(dtag.getPlayedWithinMinHour());
      dtag4.setPlayedWithinPlaylist(dtag.getPlayedWithinPlaylist());
      dtag4.setPlaylistIds(dtag.getPlaylistIds());
      dtag4.setTags(dtag.getTags());
      dtag4.setTitles(dtag.getTitles());

      clientV4.getTagManager().saveDynamicTag(dtag4);
    }

  }

  public void migrateOnlinePlaylists() throws Exception {
    this.message("Migriere Playlist-Einstellungen");
    Map<String, de.stationadmin.base.playlist.Playlist> p4ByName = new HashMap<String, de.stationadmin.base.playlist.Playlist>();
    for (de.stationadmin.base.playlist.Playlist p4 : this.clientV4.getPlaylistService().getPlaylistRegistry().getPlaylists(de.stationadmin.base.playlist.Playlist.PlaylistType.ONLINE)) {
      p4ByName.put(p4.getName(), p4);
    }

    for (Playlist p3 : this.clientV3.getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      de.stationadmin.base.playlist.Playlist p4 = p4ByName.get(p3.getName());
      if (p4 != null) {
        log.info("migrate playlist settings for " + p3.getName());
        this.message(p3.getName());
        List<String> props = p3.getProperties();
        p4.setProperties(props, true);
        this.clientV4.getPlaylistService().savePlaylist(p4);
      }
    }
  }

  public void migrateArchivePlaylists() throws Exception {
    this.message("Migriere Archiv-Playlists");
    for (Playlist p3 : this.clientV3.getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ARCHIVED)) {
      this.message(p3.getName());
      log.info("migrate " + p3.getName());
      de.stationadmin.base.playlist.Playlist p4 = new de.stationadmin.base.playlist.Playlist(this.clientV4.getTrackService().getTrackRegistry(),
          de.stationadmin.base.playlist.Playlist.PlaylistType.ARCHIVED);
      p4.setProperties(p3.getProperties());

      for (de.emjoy.stationadmin.base.playlist.Playlist.Entry entry : p3.getEntries()) {
        Integer idv4 = this.trackIdMap.get(entry.getTitleId());
        if (idv4 != null) {
          BasicTrack t4 = this.clientV4.getTrackService().getTrack(idv4);
          if (t4 != null) {
            p4.addTrack(t4);
          } else {
            this.message("Felender Titel: " + entry.getTitle());
            log.warn("unable to find track " + entry.getTitle());
          }
        } else {
          this.message("Felender Titel: " + entry.getTitle());
          log.warn("unable to find track " + entry.getTitle());
        }
      }

      this.clientV4.getPlaylistService().savePlaylist(p4);
    }
  }

  public void migrateSettings() throws Exception {
    this.message("Migriere Einstellungen");
    de.stationadmin.base.Settings settings4 = this.clientV4.getSettings();
    Settings settings = this.clientV3.getSettings();

    // settings4.setStatisticsLogFile(settings.getStatisticsLogFile());
    settings4.setStatisticsRefreshInterval(settings.getStatisticsRefreshInterval());
    // settings4.setTitleLogFile(settings.getTitleLogFile());
    settings4.setShuffleJingleInterval(settings.getShuffleJingleInterval());
    settings4.setShuffleProtectFirstJingle(settings.isShuffleProtectFirstJingle());
    settings4.setShuffleWordDistributionStrategy(WordDistributionStrategy.valueOf(settings.getShuffleWordDistributionStrategy().name()));
    settings4.setGenerateMinRandomValue(settings.getGenerateMinRandomValue());
    settings4.setGenerateArtistPreselectLimits(settings.getGenerateArtistPreselectLimits());
    settings4.setGenerateArtistPreselectTagWeights(settings.getGenerateArtistPreselectTagWeights());
    settings4.setAutoUpdateCheckEnabled(settings.isAutoUpdateCheckEnabled());
    settings4.setMp3ExplorerMaxFiles(settings.getMp3ExplorerMaxFiles());
    settings4.setLogTitleWithListeners(settings.isLogTitleWithListeners());
    settings4.setLogRank(settings.isLogRank());
    settings4.setMp3Player(settings.getMp3Player());
    settings4.setMp3Root(settings.getMp3Root());
    settings4.setBackupDirectory(settings.getBackupDirectory());
    settings4.setBackupFrequency(settings.getBackupFrequency());
    settings4.setLogAutodownloadPermitted(settings.isLogAutodownloadPermitted());
    settings4.setLogDownloadPermitted(settings.isLogDownloadPermitted());
    settings4.setArtistNormalizerAliases(settings.getArtistNormalizerAliases());
    settings4.setArtistNormalizerSeperators(settings.getArtistNormalizerSeperators());

    if (settings.getGenerateGlobalTagWeights() != null) {
      List<de.stationadmin.base.playlist.shuffle.TagWeight> w4 = new ArrayList<de.stationadmin.base.playlist.shuffle.TagWeight>();
      for (TagWeight tagWeight3 : settings.getGenerateGlobalTagWeights()) {
        de.stationadmin.base.playlist.shuffle.TagWeight t4 = new de.stationadmin.base.playlist.shuffle.TagWeight(tagWeight3.getTag(), tagWeight3.getWeight(), tagWeight3.getMaxFraction());
        w4.add(t4);
      }
      settings4.setGenerateGlobalTagWeights(w4);
    }

    this.clientV4.saveSettings();

  }

  public void migrateTasks() throws Exception {
    this.message("Migriere Aufgaben");
    new File(this.clientV4.getSessionCtx().getStationDirectory() + "/tasks/").mkdirs();
    for (File file : this.clientV3.getTaskExecutionService().getTaskFiles()) {
      log.info("convert task " + file.getName());
      String str = FileUtils.readFileToString(file, "UTF-8");
      str = str.replaceAll("de.emjoy.stationadmin.base.tasks", "de.stationadmin.base.tasks");
      File file4 = new File(this.clientV4.getSessionCtx().getStationDirectory() + "/tasks/" + file.getName());
      FileUtils.writeStringToFile(file4, str, "UTF-8");
    }
    this.clientV4.getTaskExecutionService().load();
  }

  public void migrateTrackAliases() throws Exception {
    this.message("Migriere Titelalias");
    for (RegisteredTitle t : this.clientV3.getTitleService().getTitleRegistry().getAllTitles()) {
      Integer idv4 = this.trackIdMap.get(t.getId());
      if (t.getAliases() != null && t.getAliases().size() > 0 && idv4 != null) {
        RegisteredTrack t4 = this.clientV4.getTrackService().getTrackRegistry().getTrack(idv4);
        if (t4 != null) {
          for (TitleAlias alias : t.getAliases()) {
            this.clientV4.getTrackService().getTrackRegistry().registerAlias(t4.getId(), alias.getArtist(), alias.getTitle());
          }
        }
      }
    }
    this.clientV4.getTrackService().saveAliases();
  }

  public void migrateLogs() throws Exception {
    File logDir3 = new File(this.clientV3.getSessionCtx().getStationDirectory() + "log");
    File logDir4 = new File(this.clientV4.getSessionCtx().getStationDirectory() + "log");
    if (logDir3.exists()) {
      File[] files = logDir3.listFiles();
      if (files != null) {
        this.message("Migriere Logs");
        logDir4.mkdirs();

        for (File file : files) {
          File fileV4 = new File(logDir4.getAbsolutePath() + File.separatorChar + file.getName());
          if (!fileV4.exists() || file.length() > fileV4.length()) {
            try {
              FileUtils.copyFile(file, fileV4, true);
            } catch (Exception e) {
            }
          }
        }

      }

    }

  }

  /**
   * @return the messageReceiver
   */
  public MessageReceiver getMessageReceiver() {
    return messageReceiver;
  }

  /**
   * @param messageReceiver
   *          the messageReceiver to set
   */
  public void setMessageReceiver(MessageReceiver messageReceiver) {
    this.messageReceiver = messageReceiver;
  }

  /**
   * @return the reloadTrackmapping
   */
  public boolean isReloadTrackmapping() {
    return reloadTrackmapping;
  }

  /**
   * @param reloadTrackmapping
   *          the reloadTrackmapping to set
   */
  public void setReloadTrackmapping(boolean reloadTrackmapping) {
    this.reloadTrackmapping = reloadTrackmapping;
  }

}
