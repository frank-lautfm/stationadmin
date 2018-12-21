package de.stationadmin.base.playlist;

/**
 * Rule for populating a playlist automatically
 */
public class AutoFillRule {
  
  private boolean enabled = false;
  private int[] sourcePlaylists;
  private String[] sourceTags;
  private boolean includeAdTrigger = false;
  private boolean includeTrackRules = false;

  public boolean isEnabled() {
    return enabled;
  }

  public void setEnabled(boolean enabled) {
    this.enabled = enabled;
  }

  public int[] getSourcePlaylists() {
    return sourcePlaylists;
  }

  public void setSourcePlaylists(int[] sourcePlaylists) {
    this.sourcePlaylists = sourcePlaylists;
  }

  public String[] getSourceTags() {
    return sourceTags;
  }

  public void setSourceTags(String[] sourceTags) {
    this.sourceTags = sourceTags;
  }

  public boolean isIncludeAdTrigger() {
    return includeAdTrigger;
  }

  public void setIncludeAdTrigger(boolean includeAdTrigger) {
    this.includeAdTrigger = includeAdTrigger;
  }

  public boolean isIncludeTrackRules() {
    return includeTrackRules;
  }

  public void setIncludeTrackRules(boolean includeTrackRules) {
    this.includeTrackRules = includeTrackRules;
  }

}
