/**
 * 
 */
package de.stationadmin.base.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import de.stationadmin.base.Settings;
import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.shuffle.AdTriggerEngine;
import de.stationadmin.base.playlist.shuffle.DefaultArtistNormalizer;
import de.stationadmin.base.playlist.shuffle.PlaylistEnhancer;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.playlist.shuffle.PlaylistsEnhancerGroup;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine;
import de.stationadmin.base.playlist.shuffle.TrackRuleEngine.JingleCollisionStratagy;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;
import de.stationadmin.base.playlist.shuffle.WeightedTrackPreselector;

/**
 * @author korf
 * 
 */
public class PlaylistGeneratorFactory {

  public static PlaylistGenerator createGenerator(StationAdminClient client) {
    Settings settings = client.getSettings();

    PlaylistGenerator generator = new PlaylistGenerator(client.getTagManager(), client.getTrackService().getTrackRegistry());
    generator.setProtectFirstJingle(settings.isShuffleProtectFirstJingle());
    generator.setProtectAllJingles(settings.isShuffleProtectAllJingles());
    generator.setJingleInterval(settings.getShuffleJingleInterval());
    generator.setMinRandomValue(settings.getGenerateMinRandomValue());
    generator.setWordDistribution(settings.getShuffleWordDistributionStrategy());

    // global tag weights
    if (settings.getGenerateGlobalTagWeights() != null) {
      generator.setGlobalWeightTags(settings.getGenerateGlobalTagWeights());
    }

    // artist normalizer
    generator.setArtistNormalizer(createNormalizer(settings));

    // artist preselector
    Integer defaultLimit = settings.getGenerateArtistPreselectLimits() != null ? settings.getGenerateArtistPreselectLimits().get("*") : null;
    if (defaultLimit != null) {
      generator.setMaxArtistTitles(defaultLimit);
      WeightedTrackPreselector preselector = new WeightedTrackPreselector(client.getTagManager());
      if (settings.getGenerateArtistPreselectLimits() != null) {
        for (Entry<String, Integer> entry : settings.getGenerateArtistPreselectLimits().entrySet()) {
          preselector.setArtistMax(entry.getKey(), entry.getValue());
        }
      }
      if (settings.getGenerateArtistPreselectTagWeights() != null) {
        for (Entry<String, Integer> entry : settings.getGenerateArtistPreselectTagWeights().entrySet()) {
          try {
            preselector.setWeight(entry.getKey(), entry.getValue());
          } catch (IOException e) {
            e.printStackTrace();
          }
        }
      }

      generator.setArtistTrackPreselector(preselector);
    }

    List<PlaylistEnhancer> playlistEnhancers = new ArrayList<PlaylistEnhancer>();
    TrackRuleEngine trackRuleEngine = createTrackRuleEngine(client);
    if (trackRuleEngine != null) {
      // generator.setPlaylistEnhancer(trackRuleEngine);
      playlistEnhancers.add(trackRuleEngine);
    }

    if (client.getSettings().getAdTriggerPosition1() > -1) {
      playlistEnhancers.add(createAdTriggerEngine(client));
    }
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

  public static AdTriggerEngine createAdTriggerEngine(StationAdminClient client) {
    Settings settings = client.getSettings();
    AdTriggerEngine engine = new AdTriggerEngine(client.getTrackService().getTrackRegistry());
    engine.setAdSeparatorId(settings.getAdSeparatorId());
    engine.setAdTriggerId(settings.getAdTriggerId());
    engine.setJingleCollisionStrategy(settings.getAdJingleCollisionStrategy());
    engine.setPosition1(settings.getAdTriggerPosition1());
    engine.setPosition2(settings.getAdTriggerPosition2());

    return engine;
  }

  private static TrackRuleEngine createTrackRuleEngine(StationAdminClient client) {
    Settings settings = client.getSettings();
    TrackRuleEngine engine = null;
    if (settings.getTrackRules() != null && settings.getTrackRules().size() > 0) {
      engine = new TrackRuleEngine(client.getTrackService().getTrackRegistry(), client.getTagManager());

      if (settings.getTrackRuleGroups() != null) {
        for (TrackRuleGroup group : settings.getTrackRuleGroups()) {
          engine.register(group);
        }
      }
      for (TrackRule rule : settings.getTrackRules()) {
        engine.register(rule);
      }

      engine.setJingleCollisionStrategy(
          settings.getTrackRuleJingleCollsisionStrategy() != null ? settings.getTrackRuleJingleCollsisionStrategy() : JingleCollisionStratagy.KEEP_BOTH);
      engine.setGroupCollisionStrategy(settings.getTrackRuleGroupCollisionStrategy() != null ? settings.getTrackRuleGroupCollisionStrategy() : MultiMatchSelection.ALL);
    }
    return engine;

  }

  private static DefaultArtistNormalizer createNormalizer(Settings settings) {
    DefaultArtistNormalizer normalizer = new DefaultArtistNormalizer(settings.getArtistNormalizerSeperators());
    if (settings.getArtistNormalizerAliases() != null) {
      for (Entry<String, String> entry : settings.getArtistNormalizerAliases().entrySet()) {
        normalizer.addAlias(entry.getKey(), entry.getValue());
      }
    }
    return normalizer;

  }

  public static PlaylistShuffler createShuffler(StationAdminClient client) {
    PlaylistShuffler shuffler = new PlaylistShuffler();
    shuffler.setProtectFirstJingle(client.getSettings().isShuffleProtectFirstJingle());
    shuffler.setProtectAllJingles(client.getSettings().isShuffleProtectAllJingles());
    shuffler.setJingleInterval(client.getSettings().getShuffleJingleInterval());
    shuffler.setWordDistribution(client.getSettings().getShuffleWordDistributionStrategy());
    shuffler.setArtistNormalizer(createNormalizer(client.getSettings()));

    List<PlaylistEnhancer> playlistEnhancers = new ArrayList<PlaylistEnhancer>();
    TrackRuleEngine trackRuleEngine = createTrackRuleEngine(client);
    if (trackRuleEngine != null) {
      playlistEnhancers.add(trackRuleEngine);
    }

    if (client.getSettings().getAdTriggerPosition1() > -1) {
      playlistEnhancers.add(createAdTriggerEngine(client));
    }
    shuffler.setPlaylistEnhancer(getPlaylistEnhancer(playlistEnhancers));
    return shuffler;

  }

}
