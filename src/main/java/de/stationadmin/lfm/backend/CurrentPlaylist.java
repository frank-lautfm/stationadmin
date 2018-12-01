package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "CurrentPlaylist")
public class CurrentPlaylist {
  @JsonProperty("playlist_info")
  private CurrentPlaylistInfo playlistInfo;
  private Track[] tracks;
  

  public CurrentPlaylist() {
  }


  public Track[] getTracks() {
    return tracks;
  }

  public void setTracks(Track[] tracks) {
    this.tracks = tracks;
  }


  public CurrentPlaylistInfo getPlaylistInfo() {
    return playlistInfo;
  }


  public void setPlaylistInfo(CurrentPlaylistInfo playlistInfo) {
    this.playlistInfo = playlistInfo;
  }

}
