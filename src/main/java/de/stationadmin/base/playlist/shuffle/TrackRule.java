package de.stationadmin.base.playlist.shuffle;

import java.io.Serializable;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "TrackRule")
public class TrackRule implements Serializable {
  private static final long serialVersionUID = -6689217810843399901L;
  
  private String groupName;
  private int trackId;
  private TrackPosition position = TrackPosition.BEFORE;
  private FilterType filterType = FilterType.TAG;
  private String filter;
  private int minDistance = 0;
  
  public TrackRule() {
    
  }

  public TrackRule(String groupName, int trackId, TrackPosition postion, FilterType bindingType, String bindTo, int minDistance) {
    super();
    this.groupName = groupName;
    this.trackId = trackId;
    this.position = postion;
    this.filterType = bindingType;
    this.filter = bindTo;
    this.minDistance = minDistance;
  }

  public TrackRule(TrackRule rule) {
    this.groupName = rule.groupName;
    this.trackId = rule.trackId;
    this.position = rule.position;
    this.filterType = rule.filterType;
    this.filter = rule.filter;
    this.minDistance = rule.minDistance;
  }

  public FilterType getFilterType() {
    return filterType;
  }

  public String getFilter() {
    return filter;
  }

  public String getGroupName() {
    return groupName;
  }

  public TrackPosition getPosition() {
    return position;
  }

  public int getTrackId() {
    return trackId;
  }

  public int getMinDistance() {
    return minDistance;
  }

  public void setMinDistance(int minDistance) {
    this.minDistance = minDistance;
  }

  public void setFilterType(FilterType bindingType) {
    this.filterType = bindingType;
  }

  public void setFilter(String bindTo) {
    this.filter = bindTo;
  }

  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public void setPosition(TrackPosition postion) {
    this.position = postion;
  }

  public void setTrackId(int trackId) {
    this.trackId = trackId;
  }

  public enum TrackPosition {
    BEFORE,
    AFTER
  }
  public enum FilterType {
    TAG,
    ARTIST,
    ALBUM,
    ARTIST_TITLE
  }

}

