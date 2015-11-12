/**
 * 
 */
package de.stationadmin.base.playlist.validation;

import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;

/**
 * A <code>PlaylistValidator</code> checks a playlist for rule violations
 *
 * @author Frank Korf
 *
 */
public interface PlaylistValidator {
  
  /**
   * Validates the given playlist
   * @param playlist playlist that is validated
   * @param violations list that is filled with violating entries - may be <code>null</code>
   * @return <code>true</code> if constraint violations exist
   */
  boolean validate(Playlist playlist, List<Entry> violations);

}
