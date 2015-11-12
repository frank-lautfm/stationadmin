/**
 * 
 */
package de.stationadmin.base.playlist.exporter;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.util.TimeFormat;

/**
 * Tool class for exporting playlists as CSV
 * 
 * @author Frank Korf
 */
public class PlaylistCSVExporter extends PlaylistExporter {

	/**
	 * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#getHeadLine()
	 */
	@Override
	protected String getHeadLine() {
		return quote("Start Time") + "," + quote("Artist") + "," + quote("Title");
	}

	@Override
	protected String toString(Entry entry, Title title) {
		StringBuffer buf = new StringBuffer(100);
		buf.append(quote(TimeFormat.format(entry.getStart(), true)));
		buf.append(',');
		buf.append(quote(title.getArtist()));
		buf.append(',');
		buf.append(quote(title.getTitle()));
		buf.append(',');
		
		return buf.toString();
	}
	
	private String quote(String str) {
		str = StringUtils.replace(str, "\"", "\"\"");
		return "\"" + str + "\"";
	}


}
