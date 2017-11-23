package de.stationadmin.base.playlist.shuffle;

import java.util.List;

import de.stationadmin.base.track.BasicTrack;

public interface PlaylistEnhancer {

  /**
   * Add or remove tracks
   * @param tracks
   */
  List<BasicTrack> process(List<BasicTrack> tracks);
  
  /**
   * Reset internal state
   */
  void reset();
}
