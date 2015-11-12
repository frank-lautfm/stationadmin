/**
 * 
 */
package de.stationadmin.lfm.backend;

import org.apache.commons.lang.ArrayUtils;
import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class TrackList {
  
  private Track[] tracks;
  @JsonProperty("_paging")
  private Paging paging;

  public Track[] getTracks() {
    return tracks;
  }

  public void setTracks(Track[] tracks) {
    this.tracks = tracks;
  }

  public Paging getPaging() {
    return paging;
  }

  public void setPaging(Paging paging) {
    this.paging = paging;
  }

  @Override
  public String toString() {
    return "page " + paging.getCurrentPage() + " of " + paging.getTotalPages() + " - " + ArrayUtils.toString(tracks);
  }

}
