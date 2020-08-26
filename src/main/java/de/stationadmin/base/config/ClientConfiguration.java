package de.stationadmin.base.config;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import de.stationadmin.base.Version;
import de.stationadmin.base.playlist.PlaylistClientCfgData;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.scheduled.ScheduledItem;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ClientCfg")
public class ClientConfiguration {

  private String version = Version.MAJOR + "." + Version.MINOR;
  private List<String> dynamicTags = new ArrayList<>();
  private Map<String, String> tagGroups = new HashMap<>();
  private List<PlaylistClientCfgData> playlistData = new ArrayList<>();
  private List<PlaylistProfile> playlistProfiles = new ArrayList<>();
  private List<ScheduledItem> scheduledPlaylistItems = new ArrayList<>();
  private Date timmestamp = new Date();

  public List<String> getDynamicTags() {
    return dynamicTags;
  }

  public List<PlaylistClientCfgData> getPlaylistData() {
    return playlistData;
  }

  public Map<String, String> getTagGroups() {
    return tagGroups;
  }

  public Date getTimmestamp() {
    return timmestamp;
  }

  public String getVersion() {
    return version;
  }

  public void setDynamicTags(List<String> dynamicTags) {
    this.dynamicTags = dynamicTags;
  }

  public void setPlaylistData(List<PlaylistClientCfgData> playlistData) {
    this.playlistData = playlistData;
  }

  public void setTagGroups(Map<String, String> tagFolders) {
    this.tagGroups = tagFolders;
  }

  public void setTimmestamp(Date timmestamp) {
    this.timmestamp = timmestamp;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<PlaylistProfile> getPlaylistProfiles() {
    return playlistProfiles;
  }

  public void setPlaylistProfiles(List<PlaylistProfile> playlistProfiles) {
    this.playlistProfiles = playlistProfiles;
  }

  public List<ScheduledItem> getScheduledPlaylistItems() {
    return scheduledPlaylistItems;
  }

  public void setScheduledPlaylistItems(List<ScheduledItem> scheduledPlaylistItems) {
    this.scheduledPlaylistItems = scheduledPlaylistItems;
  }

}
