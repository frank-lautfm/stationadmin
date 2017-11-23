package de.stationadmin.base.playlist.shuffle;

import java.io.Serializable;

public class TrackRuleGroup implements Serializable {
  private static final long serialVersionUID = -2153598585047049570L;
  private String name;
  private int minDistance = 0;

  public TrackRuleGroup() {
  }
  
  public TrackRuleGroup(String name, int minDistance) {
    this.name = name;
    this.minDistance = minDistance;
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
}
