package de.stationadmin.base.playlist.profile;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import de.stationadmin.base.playlist.shuffle.AdTriggerEngine.AdJingleCollisionStrategy;
import de.stationadmin.base.track.TrackRegistry;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "AdTriggerCfg")
public class AdTriggerCfg {

  private int pos1 = -1;
  private int pos2 = -1;
  private int seperatorId = -1;
  private int triggerId = TrackRegistry.STANDARD_AD_TRIGGER_ID;
  private AdJingleCollisionStrategy jingleCollisionStrategy = AdJingleCollisionStrategy.KEEP_BOTH;

  public AdTriggerCfg() {

  }

  public AdTriggerCfg(AdTriggerCfg source) {
    this.pos1 = source.pos1;
    this.pos2 = source.pos2;
    this.seperatorId = source.seperatorId;
    this.triggerId = source.triggerId;
    this.jingleCollisionStrategy = source.jingleCollisionStrategy;
  }

  public int getPos1() {
    return pos1;
  }

  public void setPos1(int pos1) {
    this.pos1 = pos1;
  }

  public int getPos2() {
    return pos2;
  }

  public void setPos2(int pos2) {
    this.pos2 = pos2;
  }

  public int getSeperatorId() {
    return seperatorId;
  }

  public void setSeperatorId(int seperatorId) {
    this.seperatorId = seperatorId;
  }

  public int getTriggerId() {
    return triggerId;
  }

  public void setTriggerId(int triggerId) {
    this.triggerId = triggerId;
  }

  public AdJingleCollisionStrategy getJingleCollisionStrategy() {
    return jingleCollisionStrategy;
  }

  public void setJingleCollisionStrategy(AdJingleCollisionStrategy jingleCollisionStrategy) {
    this.jingleCollisionStrategy = jingleCollisionStrategy;
  }

}
