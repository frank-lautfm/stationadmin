package de.stationadmin.base.playlist.scheduled;

import java.util.Map;
import java.util.UUID;

public class ScheduledItem {
  private String id = UUID.randomUUID().toString();
  private String name;
  private String tag;
  private boolean excludeTracksFromShuffle = true;
  private TrackType trackType = TrackType.Song;
  private int introJingleId;
  private TrackSelectionMode selection = TrackSelectionMode.Random;

  public String getId() {
    return id;
  }

  public int getIntroJingleId() {
    return introJingleId;
  }

  public String getName() {
    return name;
  }

  public TrackSelectionMode getSelection() {
    return selection;
  }

  public String getTag() {
    return tag;
  }

  public TrackType getTrackType() {
    return trackType;
  }

  public void setId(String id) {
    this.id = id;
  }

  public void setIntroJingleId(int intoJingleId) {
    this.introJingleId = intoJingleId;
  }

  public void setName(String name) {
    this.name = name;
  }

  public void setSelection(TrackSelectionMode selection) {
    this.selection = selection;
  }

  public void setTag(String tag) {
    this.tag = tag;
  }

  public void setTrackType(TrackType trackType) {
    this.trackType = trackType;
  }

  public String toString() {
    return name;
  }
  
  public void updateIn(Map<String, Object> map) {
    map.put("tag", this.tag);
    map.put("trackType", this.trackType.name().toLowerCase());
    map.put("selection", this.selection.name().toLowerCase());
    if(this.introJingleId > 0) {
      map.put("introJingleId", this.introJingleId);
    }
    else {
      map.remove("introJingleId");
    }
    if(this.excludeTracksFromShuffle) {
      map.put("exclude", true);
    }
    else {
      map.remove("exclude");
    }
  }

  public boolean isExcludeTracksFromShuffle() {
    return excludeTracksFromShuffle;
  }

  public void setExcludeTracksFromShuffle(boolean excludeTracksFromShuffle) {
    this.excludeTracksFromShuffle = excludeTracksFromShuffle;
  }
}
