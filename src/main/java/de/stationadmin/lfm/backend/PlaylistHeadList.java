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
  
  private ExtendedPlaylistHead[] playlists;

  public ExtendedPlaylistHead[] getPlaylists() {
    return playlists;
  }

  public void setPlaylists(ExtendedPlaylistHead[] playlists) {
    this.playlists = playlists;
  }


}
