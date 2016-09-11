/**
 * 
 */
package de.stationadmin.base.track.exporter;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.TimeFormat;

/**
 * @author Frank
 *
 */
public class TrackListCSVExporter extends TrackListStringFileExporter {

  @Override
  protected String toString(RegisteredTrack title) {
    StringBuilder buf = new StringBuilder();
    buf.append(quote(title.getArtist()));
    buf.append(',');
    buf.append(quote(title.getTitle()));
    buf.append(',');
    buf.append(quote(title.getAlbum()));
    buf.append(',');
    buf.append(quote(TimeFormat.format(title.getLength(), false)));
    buf.append(',');
    buf.append(quote(title.getGenre()));
    buf.append(',');
    buf.append(quote(title.getYear() > 0 ? Integer.toString(title.getYear()) : ""));
    return buf.toString();
  }

  private String quote(String str) {
    if (str != null) {
      str = StringUtils.replace(str, "\"", "\"\"");
    } else {
      str = "";
    }
    return "\"" + str + "\"";
  }

  /**
   * @see de.stationadmin.base.track.exporter.TrackListStringFileExporter#getHeadLine(boolean)
   */
  @Override
  protected String getHeadLine() {
    StringBuilder buf = new StringBuilder();
    buf.append(quote("Artist"));
    buf.append(',');
    buf.append(quote("Titel"));
    buf.append(',');
    buf.append(quote("Album"));
    buf.append(',');
    buf.append(quote("Länge"));
    buf.append(',');
    buf.append(quote("Genre"));
    buf.append(',');
    buf.append(quote("Jahr"));
    return buf.toString();
  }

}
