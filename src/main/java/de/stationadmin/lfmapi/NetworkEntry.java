/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class NetworkEntry implements Serializable {
  private static final long serialVersionUID = -1955711348171519244L;

  private Station station;
  private String comment;

  public NetworkEntry(JSONObject json) {
    try {
      this.station = new Station(json.getJSONObject("station"));
    } catch (JSONException e) {
    }
    this.comment = JSONUtil.getString(json, "comment");
  }

  /**
   * @return the station
   */
  public Station getStation() {
    return station;
  }

  /**
   * @return the comment
   */
  public String getComment() {
    return comment;
  }

}
