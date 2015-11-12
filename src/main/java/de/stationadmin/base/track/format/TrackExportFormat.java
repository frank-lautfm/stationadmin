/**
 * 
 */
package de.stationadmin.base.track.format;

import de.stationadmin.base.track.Title;

/**
 * A <code>TitleExportFormat</code> transforms titles to a string representation
 * and the string representation back to a title.
 *
 * @author Frank Korf
 */
public interface TrackExportFormat {
  
  /**
   * Creates a string representation of the title
   * @param title 
   * @return
   */
  String toString(Title title);
  
  /**
   * Creates a title based on the string representation
   * @param str
   * @return
   */
  Title fromString(String str);
  
  /**
   * Checks if the given string has a supported format
   * @param str
   * @return <code>true</code> if supported
   */
  boolean supports(String str);

}
