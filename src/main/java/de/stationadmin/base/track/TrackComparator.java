/**
 * 
 */
package de.stationadmin.base.track;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

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
		int result = StringUtils.trimToEmpty(o1.getArtist()).compareToIgnoreCase(StringUtils.trimToEmpty(o2.getArtist()));
		if(result == 0) {
			result = o1.getTitle().compareToIgnoreCase(o2.getTitle());
		}
		return result;
	}

}
