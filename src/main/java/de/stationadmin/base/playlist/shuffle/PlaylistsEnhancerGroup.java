package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.track.BasicTrack;

public class PlaylistsEnhancerGroup implements PlaylistEnhancer {

  private List<PlaylistEnhancer> playlistEnhancers = new ArrayList<PlaylistEnhancer>();

  public void add(PlaylistEnhancer enhancer) {
    this.playlistEnhancers.add(enhancer);
  }

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    for (int i = 0; i < playlistEnhancers.size(); i++) {
      if (playlistEnhancers.get(i).excludeFromCorePlaylist(track)) {
        return true;
      }
    }
    return false;
  }

  @Override
  public List<BasicTrack> process(Playlist playlist, List<BasicTrack> tracks, boolean protectFirstJingle) {
    for (int i = 0; i < playlistEnhancers.size(); i++) {
      tracks = playlistEnhancers.get(i).process(playlist, tracks, protectFirstJingle);
    }
    return tracks;
  }

  @Override
  public void reset() {
    for (int i = 0; i < playlistEnhancers.size(); i++) {
      playlistEnhancers.get(i).reset();
    }
  }

  public List<PlaylistEnhancer> getPlaylistEnhancers() {
    return Collections.unmodifiableList(playlistEnhancers);
  }

  @Override
  public void initialize(PlaylistProfile profile) {
    for (int i = 0; i < playlistEnhancers.size(); i++) {
      playlistEnhancers.get(i).initialize(profile);
    }
    
  }

}
