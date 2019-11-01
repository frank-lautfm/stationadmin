package de.stationadmin.base.playlist.profile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import de.stationadmin.base.Settings;
import de.stationadmin.base.playlist.shuffle.PlaylistProfileType;
import de.stationadmin.base.playlist.shuffle.TagWeight;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "PlaylistProfile")
public class PlaylistProfile {
  private String id = UUID.randomUUID().toString();
  private String name;
  private PlaylistProfileType type = PlaylistProfileType.StationAdminShuffle;

  private int jingleInterval = 0;
  private String jingleOrder = "shuffle_repeat";
  private boolean protectFirstJingle;
  private boolean protectAllJingles;
  private WordDistributionStrategy wordDistributionStrategy;

  private ArtistNormalizationCfg artistNormalization = new ArtistNormalizationCfg();
  private AdTriggerCfg adTrigger = new AdTriggerCfg();
  private TrackRuleCfg trackRules = new TrackRuleCfg();
  private GenerateCfg generate;

  private List<TagWeight> tagWeights;

  public PlaylistProfile() {
  }

  public PlaylistProfile(PlaylistProfile source) {
    this.name = source.name;
    this.type = source.type;
    this.jingleInterval = source.jingleInterval;
    this.jingleOrder = source.jingleOrder;
    this.protectFirstJingle = source.protectFirstJingle;
    this.wordDistributionStrategy = source.wordDistributionStrategy;
    this.artistNormalization = new ArtistNormalizationCfg(source.artistNormalization);
    this.adTrigger = new AdTriggerCfg(source.adTrigger);
    this.trackRules = new TrackRuleCfg(source.trackRules);
    if(source.generate != null) {
      this.generate = new GenerateCfg(source.generate);
    }
  }

  public PlaylistProfile(PlaylistProfileType type, String name, Settings settings) {
    this.name = name;
    this.type = type;

    this.jingleInterval = settings.getShuffleJingleInterval();
    this.protectFirstJingle = settings.isShuffleProtectFirstJingle();
    this.protectAllJingles = settings.isShuffleProtectAllJingles();

    this.adTrigger.setPos1(settings.getAdTriggerPosition1());
    this.adTrigger.setPos2(settings.getAdTriggerPosition2());
    this.adTrigger.setTriggerId(settings.getAdTriggerId());
    this.adTrigger.setSeperatorId(settings.getAdSeparatorId());
    this.adTrigger.setJingleCollisionStrategy(settings.getAdJingleCollisionStrategy());

    this.artistNormalization.setAliases(new HashMap<String, String>(settings.getArtistNormalizerAliases()));
    this.artistNormalization.setSeparators(new ArrayList<>(settings.getArtistNormalizerSeperators()));

    this.trackRules.setGroupCollisionStrategy(settings.getTrackRuleGroupCollisionStrategy());
    this.trackRules.setJingleCollisionStrategy(settings.getTrackRuleJingleCollsisionStrategy());
    if (settings.getTrackRuleGroups() != null) {
      this.trackRules.setGroups(new ArrayList<>(settings.getTrackRuleGroups()));
    }
    if (settings.getTrackRules() != null) {
      this.trackRules.setRules(new ArrayList<>(settings.getTrackRules()));
    }

    if (type == PlaylistProfileType.Generate) {
      this.tagWeights = new ArrayList<>(settings.getGenerateGlobalTagWeights());
      this.generate = new GenerateCfg();
      this.generate.setMinRandomValue(settings.getGenerateMinRandomValue());
      this.generate.setArtistPreselectLimits(settings.getGenerateArtistPreselectLimits());
      this.generate.setArtistPreselectWeights(settings.getGenerateArtistPreselectTagWeights());
    }

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

  public AdTriggerCfg getAdTrigger() {
    return adTrigger;
  }

  public void setAdTrigger(AdTriggerCfg adTriggerProfile) {
    this.adTrigger = adTriggerProfile;
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

  public TrackRuleCfg getTrackRules() {
    return trackRules;
  }

  public void setTrackRules(TrackRuleCfg trackRules) {
    this.trackRules = trackRules;
  }

  public ArtistNormalizationCfg getArtistNormalization() {
    return artistNormalization;
  }

  public void setArtistNormalization(ArtistNormalizationCfg artistNormalization) {
    this.artistNormalization = artistNormalization;
  }

  public boolean isProtectAllJingles() {
    return protectAllJingles;
  }

  public void setProtectAllJingles(boolean protectAllJingles) {
    this.protectAllJingles = protectAllJingles;
  }

  public GenerateCfg getGenerate() {
    return generate;
  }

  public void setGenerate(GenerateCfg generate) {
    this.generate = generate;
  }

}
