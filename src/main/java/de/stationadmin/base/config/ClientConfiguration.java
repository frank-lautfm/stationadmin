package de.stationadmin.base.config;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import de.stationadmin.base.Version;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ClientCfg")
public class ClientConfiguration {

  private String version = Version.MAJOR + "." + Version.MINOR;
  private List<TrackRuleGroup> trackRuleGroups = new ArrayList<TrackRuleGroup>();
  private List<TrackRule> trackRules = new ArrayList<TrackRule>();
  private List<String> dynamicTags = new ArrayList<>();

  public String getVersion() {
    return version;
  }

  public void setVersion(String version) {
    this.version = version;
  }

  public List<TrackRuleGroup> getTrackRuleGroups() {
    return trackRuleGroups;
  }

  public void setTrackRuleGroups(List<TrackRuleGroup> trackRuleGroups) {
    this.trackRuleGroups = trackRuleGroups;
  }

  public List<TrackRule> getTrackRules() {
    return trackRules;
  }

  public void setTrackRules(List<TrackRule> trackRules) {
    this.trackRules = trackRules;
  }

  public List<String> getDynamicTags() {
    return dynamicTags;
  }

  public void setDynamicTags(List<String> dynamicTags) {
    this.dynamicTags = dynamicTags;
  }

}
