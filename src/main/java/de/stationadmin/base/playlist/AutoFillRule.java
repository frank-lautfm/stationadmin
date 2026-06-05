package de.stationadmin.base.playlist;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Rule for populating a playlist automatically
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "AutoFillRule")
public class AutoFillRule {

  private boolean enabled = false;
  private int[] sourcePlaylists;
  private boolean duplicatesFromPlaylists = false;
  private String[] sourceTags;
  private boolean includeAdTrigger = false;
  private boolean includeTrackRules = false;
  private boolean includeNews = false;
  private NewsTrackOption newsTrack = NewsTrackOption.NEWS_WITH_WEATHER;

  public boolean isEnabled() {
    return enabled;
  }

  public boolean isConfigured() {
    return (sourcePlaylists != null && sourcePlaylists.length > 0) || (sourceTags != null && sourceTags.length > 0);
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

  public boolean isIncludeNews() {
    return includeNews;
  }

  public void setIncludeNews(boolean includeNews) {
    this.includeNews = includeNews;
  }

	public boolean isDuplicatesFromPlaylists() {
		return duplicatesFromPlaylists;
	}

	public void setDuplicatesFromPlaylists(boolean duplicatesFromPlaylists) {
		this.duplicatesFromPlaylists = duplicatesFromPlaylists;
	}

	 public NewsTrackOption getNewsTrack() {
	   return newsTrack != null ? newsTrack : NewsTrackOption.NEWS_WITH_WEATHER;
	 }

	 public void setNewsTrack(NewsTrackOption newsTrack) {
	   this.newsTrack = newsTrack;
	 }

}
