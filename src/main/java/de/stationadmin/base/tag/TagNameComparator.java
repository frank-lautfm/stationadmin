/**
 * 
 */
package de.stationadmin.base.tag;

import java.util.Comparator;

/**
 * @author korf
 *
 */
public class TagNameComparator implements Comparator<Tag> {

  /* (non-Javadoc)
   * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
   */
  @Override
  public int compare(Tag o1, Tag o2) {
    return o1.getName().compareToIgnoreCase(o2.getName());
  }

}
