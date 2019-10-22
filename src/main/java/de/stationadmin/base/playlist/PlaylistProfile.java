package de.stationadmin.base.playlist;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.base.playlist.shuffle.PlaylistProfileType;
import de.stationadmin.base.playlist.shuffle.TagWeight;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine.JingleCollisionStratagy;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;

public class PlaylistProfile {
  private String id = UUID.randomUUID().toString();
  private String name;
  private PlaylistProfileType type = PlaylistProfileType.StationAdminShuffle;

  private int jingleInterval = 0;
  private String jingleOrder = "shuffle_repeat";
  private boolean protectFirstJingle;
  private WordDistributionStrategy wordDistributionStrategy;
  private List<String> artistNormalizerSeparators;
  private Map<String, String> artistAliases;

  private AdTriggerProfile adTriggerProfile = new AdTriggerProfile();

  private JingleCollisionStratagy trackRuleJingleCollisionStrategy = JingleCollisionStratagy.KEEP_BOTH;
  private MultiMatchSelection trackRuleGroupCollisionStrategy = MultiMatchSelection.ALL;
  private List<TrackRuleGroup> trackRuleGroups;
  private List<TrackRule> trackRules;
  
  private List<TagWeight> tagWeights;

  public PlaylistProfile() {
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getJingleInterval() {
    return jingleInterval;
  }

  public void setJingleInterval(int jingleInterval) {
    this.jingleInterval = jingleInterval;
  }

  public String getJingleOrder() {
    return jingleOrder;
  }

  public void setJingleOrder(String jingleOrder) {
    this.jingleOrder = jingleOrder;
  }

  public boolean isProtectFirstJingle() {
    return protectFirstJingle;
  }

  public void setProtectFirstJingle(boolean protectFirstJingle) {
    this.protectFirstJingle = protectFirstJingle;
  }

  public WordDistributionStrategy getWordDistributionStrategy() {
    return wordDistributionStrategy;
  }

  public void setWordDistributionStrategy(WordDistributionStrategy wordDistributionStrategy) {
    this.wordDistributionStrategy = wordDistributionStrategy;
  }

  public List<String> getArtistNormalizerSeparators() {
    return artistNormalizerSeparators;
  }

  public void setArtistNormalizerSeparators(List<String> artistNormalizerSeparators) {
    this.artistNormalizerSeparators = artistNormalizerSeparators;
  }

  public Map<String, String> getArtistAliases() {
    return artistAliases;
  }

  public void setArtistAliases(Map<String, String> artistAliases) {
    this.artistAliases = artistAliases;
  }

  public AdTriggerProfile getAdTriggerProfile() {
    return adTriggerProfile;
  }

  public void setAdTriggerProfile(AdTriggerProfile adTriggerProfile) {
    this.adTriggerProfile = adTriggerProfile;
  }

  public JingleCollisionStratagy getTrackRuleJingleCollisionStrategy() {
    return trackRuleJingleCollisionStrategy;
  }

  public void setTrackRuleJingleCollisionStrategy(JingleCollisionStratagy trackRuleJingleCollisionStrategy) {
    this.trackRuleJingleCollisionStrategy = trackRuleJingleCollisionStrategy;
  }

  public MultiMatchSelection getTrackRuleGroupCollisionStrategy() {
    return trackRuleGroupCollisionStrategy;
  }

  public void setTrackRuleGroupCollisionStrategy(MultiMatchSelection trackRuleGroupCollisionStrategy) {
    this.trackRuleGroupCollisionStrategy = trackRuleGroupCollisionStrategy;
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

  public PlaylistProfileType getType() {
    return type;
  }

  public void setType(PlaylistProfileType type) {
    this.type = type;
  }

  public List<TagWeight> getTagWeights() {
    return tagWeights;
  }

  public void setTagWeights(List<TagWeight> tagWeights) {
    this.tagWeights = tagWeights;
  }

}
