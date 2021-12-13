/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.tag.TagChecker;
import de.stationadmin.base.track.BasicTrack;

/**
 * Preselects tracks by using random values in combination with weights assigned
 * to tags.
 * 
 * @author korf
 */
public class WeightedTrackPreselector implements ArtistTrackPreselector {
  private static final Logger log = LogManager.getLogger(WeightedTrackPreselector.class);
  private Random random = new Random();
  private TagChecker tagManager;
  private Map<Integer, Integer> trackWeights = new HashMap<Integer, Integer>();
  private Map<String, Integer> artistLimits = new HashMap<String, Integer>();

  /**
   * @param tagManager
   * @param weights
   */
  public WeightedTrackPreselector(TagChecker tagManager) {
    super();
    this.tagManager = tagManager;
  }

  /**
   * Sets a weight for a tag
   * 
   * @param tag
   * @param weight
   *          value between -3 and 3
   * @throws IOException
   */
  public void setWeight(String tag, int weight) throws IOException {
    int[] ids = this.tagManager.getTrackIds(tag);
    if (ids != null && weight != 0 && weight >= -3 && weight <= 3) {
      Integer w = weight;
      for (int id : ids) {
        this.trackWeights.put(id, w);
      }
    }
  }

  public void setArtistMax(String artist, int maxTracks) {
    this.artistLimits.put(artist, maxTracks);
  }

  @Override
  public List<BasicTrack> preselect(List<BasicTrack> tracks, int max) {
    if (tracks.size() == 0) {
      return tracks;
    }
    Integer artistMax = this.artistLimits.get(tracks.get(0).getArtist());
    if (artistMax != null) {
      max = artistMax;
      log.debug("max " + max + " tracks for " + tracks.get(0).getArtist());
    } 

    if (tracks.size() > max) {
      log.debug("preselect " + max + " tracks for " + tracks.get(0).getArtist());

      List<TrackRef> refs = new ArrayList<WeightedTrackPreselector.TrackRef>();
      for (BasicTrack track : tracks) {
        TrackRef ref = new TrackRef();
        ref.track = track;
        ref.score = 100 + this.random.nextInt(500);
        Integer w = this.trackWeights.get(track.getId());
        if (w != null) {
          int weight = w;
          if (weight > 0) {
            float p = ((float) 4 - weight) / 4;
            ref.score = (int) (ref.score * p);
          } else if (weight < 0) {
            float p = 1 + (float) (-weight) / 4;
            ref.score = (int) (ref.score * p);
          }
        }
        refs.add(ref);
      }

      Collections.sort(refs);

      tracks = new ArrayList<BasicTrack>();
      for (int i = 0; i < refs.size() && i < max; i++) {
        tracks.add(refs.get(i).track);
      }
    }
    return tracks;
  }

  public int getWeight(BasicTrack track) {
    Integer w = this.trackWeights.get(track.getId());
    return w != null ? w.intValue() : 0;
  }

  protected static class TrackRef implements Comparable<TrackRef> {
    BasicTrack track;
    int score;

    @Override
    public int compareTo(TrackRef o) {
      return Integer.compare(this.score, o.score);
    }
  }

}
