package de.stationadmin.base.playlist.shuffle;

import java.util.List;

import de.stationadmin.base.track.BasicTrack;

public interface PlaylistEnhancer {

  /**
   * Checks if a track should be excluded from the core playlist created by shuffle / generate
   * @param track
   * @return
   */
  boolean excludeFromCorePlaylist(BasicTrack track);

  /**
   * Add or remove tracks
   * @param tracks
   */
  List<BasicTrack> process(List<BasicTrack> tracks, boolean protectFirstJingle);
  
  /**
   * Reset internal state
   */
  void reset();
}
