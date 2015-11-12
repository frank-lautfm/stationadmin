/**
 * 
 */
package de.stationadmin.base.tag;

/**
 *
 * @author Frank Korf
 *
 */
public interface Tag extends Comparable<Tag> {
  
  /**
   * Gets the name of the group the tag belongs to
   * @return group name or <code>null</code> if tag does not belong to any group
   */
  String getGroup();
  
  /**
   * Gets the name of this tag
   * @return tag name
   */
  String getName();

}
