/**
 * 
 */
package de.stationadmin.base.util;

import java.util.ArrayList;
import java.util.List;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.AdTriggerEngine;
import de.stationadmin.base.playlist.shuffle.NewsEngine;
import de.stationadmin.base.playlist.shuffle.PlaylistEnhancer;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.playlist.shuffle.PlaylistsEnhancerGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine;

/**
 * @author korf
 * 
 */
public class PlaylistGeneratorFactory {

  public static PlaylistGenerator createGenerator(StationAdminClient client) {
    PlaylistGenerator generator = new PlaylistGenerator(client.getTagManager(), client.getTrackService().getTrackRegistry(), client.getPlaylistService());

    List<PlaylistEnhancer> playlistEnhancers = new ArrayList<PlaylistEnhancer>();
    playlistEnhancers.add(new TrackRuleEngine(client.getTrackService().getTrackRegistry(), client.getTagManager()));

    playlistEnhancers.add(createNewsEngine(client, false));
    playlistEnhancers.add(new AdTriggerEngine(client.getTrackService().getTrackRegistry()));
    generator.setPlaylistEnhancer(getPlaylistEnhancer(playlistEnhancers));

    return generator;

  }

  private static PlaylistEnhancer getPlaylistEnhancer(List<PlaylistEnhancer> playlistEnhancers) {
    if (playlistEnhancers.size() == 0) {
      return null;
    } else if (playlistEnhancers.size() == 1) {
      return playlistEnhancers.get(0);
    } else {
      PlaylistsEnhancerGroup group = new PlaylistsEnhancerGroup();
      for (PlaylistEnhancer enhancer : playlistEnhancers) {
        group.add(enhancer);
      }
      return group;
    }
  }

  public static AdTriggerEngine createAdTriggerEngine(StationAdminClient client, PlaylistProfile profile) {
    AdTriggerEngine engine = new AdTriggerEngine(client.getTrackService().getTrackRegistry(), profile != null ? profile.getAdTrigger() : null);
    return engine;
  }

  public static NewsEngine createNewsEngine(StationAdminClient client, boolean shuffleMode) {
    return new NewsEngine(client.getTrackService(), shuffleMode);
  }

  public static PlaylistShuffler createShuffler(StationAdminClient client) {
    PlaylistShuffler shuffler = new PlaylistShuffler(client.getPlaylistService());

    List<PlaylistEnhancer> playlistEnhancers = new ArrayList<PlaylistEnhancer>();
    playlistEnhancers.add(new TrackRuleEngine(client.getTrackService().getTrackRegistry(), client.getTagManager()));

    playlistEnhancers.add(createNewsEngine(client, true));

    playlistEnhancers.add(new AdTriggerEngine(client.getTrackService().getTrackRegistry()));
    shuffler.setPlaylistEnhancer(getPlaylistEnhancer(playlistEnhancers));
    return shuffler;

  }

}
