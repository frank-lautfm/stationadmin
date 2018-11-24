/**
 * 
 */
package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "Playlist")
public class Playlist extends ExtendedPlaylistHead {
  private TrackRef[] entries;

  public TrackRef[] getEntries() {
    return entries;
  }

  public void setEntries(TrackRef[] tracks) {
    this.entries = tracks;
  }

}
