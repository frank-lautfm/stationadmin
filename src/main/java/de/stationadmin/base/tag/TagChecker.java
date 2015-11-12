/**
 * 
 */
package de.stationadmin.base.tag;

import java.io.IOException;

/**
 * @author korf
 *
 */
public interface TagChecker {
  
  /**
   * Gets the ids of all titles that have been tagged with the given tag
   * @param tagname name of the tag
   * @return ids of tagged titles
   * @throws IOException
   */
  int[] getTrackIds(String tagname) throws IOException;

  /**
   * Checks if a given title is tagged with the requested tag
   * @param tag name of the tag
   * @param titleId id of the title to check
   * @return
   * @throws IOException
   */
  boolean isTagged(String tag, int titleId) throws IOException;

}
