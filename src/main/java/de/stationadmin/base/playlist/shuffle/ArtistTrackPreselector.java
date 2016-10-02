/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.List;

import de.stationadmin.base.track.BasicTrack;


/**
 * @author korf
 *
 */
public interface ArtistTrackPreselector {

  /**
   * Preselects <code>max</code> tracks
   * @param tracks list of available tracks
   * @param max maximum number of tracks to select
   * @return preselected list of tracks
   */
  List<BasicTrack> preselect(List<BasicTrack> tracks, int max);
}
