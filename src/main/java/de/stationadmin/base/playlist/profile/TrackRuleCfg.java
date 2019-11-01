package de.stationadmin.base.playlist.profile;

import java.util.ArrayList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine.JingleCollisionStratagy;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "TrackRuleCfg")
public class TrackRuleCfg {

  private JingleCollisionStratagy jingleCollisionStrategy = JingleCollisionStratagy.KEEP_BOTH;
  private MultiMatchSelection groupCollisionStrategy = MultiMatchSelection.ALL;
  private List<TrackRuleGroup> groups;
  private List<TrackRule> rules;

  public TrackRuleCfg() {
  }

  public TrackRuleCfg(TrackRuleCfg source) {
    this.jingleCollisionStrategy = source.jingleCollisionStrategy;
    this.groupCollisionStrategy = source.groupCollisionStrategy;
    if (source.groups != null) {
      this.groups = new ArrayList<>();
      source.groups.forEach(g -> groups.add(new TrackRuleGroup(g)));
    }
    if (source.rules != null) {
      this.rules = new ArrayList<>();
      source.rules.forEach(r -> rules.add(new TrackRule(r)));
    }
  }

  public JingleCollisionStratagy getJingleCollisionStrategy() {
    return jingleCollisionStrategy;
  }

  public void setJingleCollisionStrategy(JingleCollisionStratagy jingleCollisionStrategy) {
    this.jingleCollisionStrategy = jingleCollisionStrategy;
  }

  public MultiMatchSelection getGroupCollisionStrategy() {
    return groupCollisionStrategy;
  }

  public void setGroupCollisionStrategy(MultiMatchSelection groupCollisionStrategy) {
    this.groupCollisionStrategy = groupCollisionStrategy;
  }

  public List<TrackRuleGroup> getGroups() {
    return groups;
  }

  public void setGroups(List<TrackRuleGroup> groups) {
    this.groups = groups;
  }

  public List<TrackRule> getRules() {
    return rules;
  }

  public void setRules(List<TrackRule> rules) {
    this.rules = rules;
  }

}
