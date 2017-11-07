/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import de.stationadmin.base.track.BasicTrack;

/**
 * @author korf
 * 
 */
public class TagLimitAdvice implements Advice {
  private Set<Integer> taggedTrackIds = new HashSet<Integer>();
  private int maxLengthTagged;
  private float maxFraction;

  public TagLimitAdvice(int[] trackIds, int maxLengthTagged, float maxFraction ) {
    this.maxLengthTagged = maxLengthTagged;
    this.maxFraction = maxFraction;
    for (int id : trackIds) {
      this.taggedTrackIds.add(id);
    }
  }

  TagLimitAdvice(TagLimitAdvice source, int targetLengh) {
    this.maxLengthTagged = (int)(targetLengh * source.maxFraction);
    this.maxFraction = source.maxFraction;
    this.taggedTrackIds = source.taggedTrackIds;
  }

  /**
   * @see de.stationadmin.base.playlist.shuffle.Advice#accept(java.util.List,
   *      de.stationadmin.base.track.BasicTrack)
   */
  @Override
  public boolean accept(List<BasicTrack> titles, BasicTrack candidate) {
    if (taggedTrackIds.contains(candidate.getId())) {
      int lenTagged = 0;

      for (int i = 0; i < titles.size(); i++) {
        if (taggedTrackIds.contains(titles.get(i).getId())) {
          lenTagged += titles.get(i).getLength();
        }
      }

      return lenTagged + candidate.getLength() <= maxLengthTagged;
    }
    return true;
  }

  /**
   * @see de.stationadmin.base.playlist.shuffle.Advice#toJSON()
   */
  @Override
  public String toJSON() throws JSONException {
    throw new UnsupportedOperationException();
  }

}
