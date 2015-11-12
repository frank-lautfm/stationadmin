package de.stationadmin.base.playlist.shuffle;

import java.util.List;

import org.json.JSONException;

import de.stationadmin.base.track.Title;


/**
 * Advice for playlist generation
 * 
 * @author korf
 */
public interface Advice {

  /**
   * Check if the rule of the advice is matched when the candidate title
   * is added to the list of titles
   * 
   * @param titles
   * @param candidate
   * @return <code>true</code> if accepted
   */
  boolean accept(List<Title> titles, Title candidate);
  
  /**
   * Exports the advice to a JSON string
   * @return
   */
  String toJSON() throws JSONException;

}
