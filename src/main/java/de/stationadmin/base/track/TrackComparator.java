/**
 * 
 */
package de.stationadmin.base.track;

import java.util.Comparator;

/**
 * Comparator for comparing titles by artist / name
 * 
 * @author korf
 */
public class TrackComparator implements Comparator<Title> {

	/**
	 * @see java.util.Comparator#compare(java.lang.Object, java.lang.Object)
	 */
	@Override
	public int compare(Title o1, Title o2) {
		int result = o1.getArtist().compareToIgnoreCase(o2.getArtist());
		if(result == 0) {
			result = o1.getTitle().compareToIgnoreCase(o2.getTitle());
		}
		return result;
	}

}
