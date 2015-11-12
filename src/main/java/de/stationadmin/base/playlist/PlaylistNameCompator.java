/**
 * 
 */
package de.stationadmin.base.playlist;

import java.util.Comparator;

import org.apache.commons.lang.StringUtils;

/**
 * Compares playlists by name
 * 
 * @author korf
 */
public class PlaylistNameCompator implements Comparator<Playlist> {

	@Override
	public int compare(Playlist o1, Playlist o2) {
		return StringUtils.trimToEmpty(o1.getName()).compareToIgnoreCase(StringUtils.trimToEmpty(o2.getName()));
	}

}
