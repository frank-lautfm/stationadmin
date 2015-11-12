/**
 * 
 */
package de.stationadmin.base.playlist.exporter;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.format.ArtistTracknameFormat;
import de.stationadmin.base.track.format.TrackExportFormat;

/**
 * Exports a playlist as text
 * 
 * @author Frank Korf
 */
public class PlaylistTxtExporter extends PlaylistExporter {
  private TrackExportFormat format = new ArtistTracknameFormat();

	/**
	 * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#getHeadLine()
	 */
	@Override
	protected String getHeadLine() {
		return null;
	}

	/**
	 * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#toString(de.stationadmin.base.playlist.Playlist.Entry, de.stationadmin.base.track.Title)
	 */
	@Override
	protected String toString(Entry entry, Title title) {
	  return this.format.toString(title);
	}

}
