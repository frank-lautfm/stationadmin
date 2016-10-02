/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.HashSet;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;

import de.stationadmin.base.track.BasicTrack;

/**
 * @author korf
 * 
 */
public class TitleNameLimitAdvice implements Advice {

  private int numTitles = 3;

  public TitleNameLimitAdvice(int numTitles) {
    super();
    this.numTitles = numTitles;
  }

  public TitleNameLimitAdvice(JSONObject json) throws JSONException {
    int type = json.getInt("type");
    if (type != 2) {
      throw new IllegalArgumentException("Wrong type");
    }
    this.numTitles = json.getInt("numTitles");
  }

  /**
   * @see de.stationadmin.base.playlist.shuffle.Advice#accept(java.util.List,
   *      de.stationadmin.base.track.BasicTrack)
   */
  @Override
  public boolean accept(List<BasicTrack> titles, BasicTrack candidate) {
    HashSet<String> used = new HashSet<String>();

    for (int i = titles.size() - 1; i >= 0 && i >= titles.size() - this.numTitles; i--) {
      String name = titles.get(i).getTitle().toLowerCase();
      String normalizedName = name.replaceAll("\\W", "");
      used.add(normalizedName);
    }
    
    String normalizedName = candidate.getTitle().toLowerCase().replaceAll("\\W", "");

    return !used.contains(normalizedName);
  }

  /**
   * @see de.stationadmin.base.playlist.shuffle.Advice#toJSON()
   */
  @Override
  public String toJSON() throws JSONException {
    JSONObject obj = new JSONObject();
    obj.put("type", 2);
    obj.put("numTitles", this.numTitles);
    return obj.toString();
  }

  public int getNumTitles() {
    return numTitles;
  }

}
