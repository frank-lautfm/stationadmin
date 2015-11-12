/**
 * 
 */
package de.stationadmin.base.util;

import java.io.IOException;
import java.util.Map.Entry;

import de.stationadmin.base.Settings;
import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.shuffle.DefaultArtistNormalizer;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.playlist.shuffle.WeightedTrackPreselector;


/**
 * @author korf
 * 
 */
public class PlaylistGeneratorFactory {

  public static PlaylistGenerator createGenerator(StationAdminClient client) {
    Settings settings = client.getSettings();

    PlaylistGenerator generator = new PlaylistGenerator(client.getTagManager(), client.getTrackService()
        .getTrackRegistry());
    generator.setProtectFirstJingle(settings.isShuffleProtectFirstJingle());
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

    return generator;

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
    shuffler.setJingleInterval(client.getSettings().getShuffleJingleInterval());
    shuffler.setWordDistribution(client.getSettings().getShuffleWordDistributionStrategy());
    shuffler.setArtistNormalizer(createNormalizer(client.getSettings()));
    return shuffler;
    
  }

}
