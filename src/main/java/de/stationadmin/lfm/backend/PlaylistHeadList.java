/**
 * 
 */
package de.stationadmin.lfm.backend;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlaylistHeadList {
  
  private PlaylistHead[] playlists;

  public PlaylistHead[] getPlaylists() {
    return playlists;
  }

  public void setPlaylists(PlaylistHead[] playlists) {
    this.playlists = playlists;
  }


}
