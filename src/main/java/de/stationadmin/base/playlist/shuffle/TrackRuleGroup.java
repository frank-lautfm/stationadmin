package de.stationadmin.base.playlist.shuffle;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "TrackRuleGroup")
public class TrackRuleGroup implements Serializable {
  private static final long serialVersionUID = -2153598585047049570L;
  private String name;
  private MultiMatchSelection multiMatchSelection = MultiMatchSelection.ALL;
  private int minDistance = 0;

  public TrackRuleGroup() {
  }
  
  public TrackRuleGroup(String name, int minDistance) {
    this.name = name;
    this.minDistance = minDistance;
  }
  
  public TrackRuleGroup(String name, MultiMatchSelection multiMatch, int minDistance) {
    this.name = name;
    this.multiMatchSelection = multiMatch;
    this.minDistance = minDistance;
  }

  public TrackRuleGroup(TrackRuleGroup source) {
    this.name = source.name;
    this.multiMatchSelection = source.multiMatchSelection;
    this.minDistance = source.minDistance;
  }
  
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public int getMinDistance() {
    return minDistance;
  }

  public void setMinDistance(int minDistance) {
    this.minDistance = minDistance;
  }

  @Override
  public String toString() {
    return name;
  }

  public MultiMatchSelection getMultiMatchSelection() {
    return multiMatchSelection != null ? multiMatchSelection : MultiMatchSelection.ALL;
  }

  public void setMultiMatchSelection(MultiMatchSelection multiMatchSelection) {
    this.multiMatchSelection = multiMatchSelection;
  }
  
  
  public enum MultiMatchSelection {
    ALL, FIRST, RANDOM
  }

}
