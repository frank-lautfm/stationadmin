/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.json.JSONException;

import de.stationadmin.base.track.Title;

/**
 * @author korf
 * 
 */
public class TagLimitAdvice implements Advice {
  private Set<Integer> taggedTitleIds = new HashSet<Integer>();
  private int maxLengthTagged;

  public TagLimitAdvice(int[] titleIds, int maxLengthTagged) {
    this.maxLengthTagged = maxLengthTagged;
    for (int id : titleIds) {
      this.taggedTitleIds.add(id);
    }
  }

  /**
   * @see de.stationadmin.base.playlist.shuffle.Advice#accept(java.util.List,
   *      de.stationadmin.base.track.Title)
   */
  @Override
  public boolean accept(List<Title> titles, Title candidate) {
    if (taggedTitleIds.contains(candidate.getId())) {
      int lenTagged = 0;

      for (int i = 0; i < titles.size(); i++) {
        if (taggedTitleIds.contains(titles.get(i).getId())) {
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
