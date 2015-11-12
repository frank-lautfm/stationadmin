/**
 * 
 */
package de.stationadmin.base.track;


/**
 * Interface for checking search results against some criteria
 *
 * @author Frank Korf
 */
public interface TrackMatcher {
  
  /**
   * Checks if the given search result matches the condition
   * @param result
   * @return
   */
  boolean matches(DetailedTrack result);

}
