package de.stationadmin.base.playlist.shuffle;

import de.stationadmin.base.track.BasicTrack;

public abstract class AbstractPlaylistEnhancer implements PlaylistEnhancer {

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    return false;
  }

  @Override
  public void reset() {
  }

}
