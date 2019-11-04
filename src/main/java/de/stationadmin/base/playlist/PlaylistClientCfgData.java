package de.stationadmin.base.playlist;

/**
 * Data transfer object for playlist data stored in client configuration on
 * server
 */
public class PlaylistClientCfgData {
  private int id;
  private String profileId;
  private String[] tags;
  private String comment;
  private AutoFillRule autoFillRule;

  public PlaylistClientCfgData() {
  }

  public AutoFillRule getAutoFillRule() {
    return autoFillRule;
  }

  public int getId() {
    return id;
  }

  public String getComment() {
    return comment;
  }

  public String[] getTags() {
    return tags;
  }

  public void setAutoFillRule(AutoFillRule autoFillRule) {
    this.autoFillRule = autoFillRule;
  }

  public void setId(int id) {
    this.id = id;
  }

  public void setComment(String note) {
    this.comment = note;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

  public String getProfileId() {
    return profileId;
  }

  public void setProfileId(String profileId) {
    this.profileId = profileId;
  }

}
