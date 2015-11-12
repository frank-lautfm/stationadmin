/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class PlaylistSchedules implements Serializable {
  private static final long serialVersionUID = 3733665071315630178L;
  private Playlist playlist;
  private AirTime[] airTimes;

  /**
   * @param json
   */
  public PlaylistSchedules(JSONObject json) {
    this.playlist = new Playlist(json);
    try {
      JSONArray jsonAirtimes = json.getJSONArray("airtimes");
      this.airTimes = new AirTime[jsonAirtimes.length()];
      for (int i = 0; i < jsonAirtimes.length(); i++) {
        this.airTimes[i] = new AirTime(jsonAirtimes.getJSONObject(i));
      }
    } catch (JSONException e) {

    }
  }

  /**
   * @return the playlist
   */
  public Playlist getPlaylist() {
    return playlist;
  }

  /**
   * @return the airTimes
   */
  public AirTime[] getAirTimes() {
    return airTimes;
  }
  
  public String toString() {
    return this.playlist + " " + this.airTimes;
  }

}
