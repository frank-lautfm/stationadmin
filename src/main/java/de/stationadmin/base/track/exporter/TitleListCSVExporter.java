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
public class TitleListCSVExporter extends TitleListStringFileExporter {

  @Override
  protected String toString(RegisteredTrack title, boolean full) {
    StringBuilder buf = new StringBuilder();
    buf.append(quote(title.getArtist()));
    buf.append(',');
    buf.append(quote(title.getTitle()));
    if (full) {
      buf.append(',');
      buf.append(quote(title.getAlbum()));
    }
    buf.append(',');
    buf.append(quote(TimeFormat.format(title.getLength(), false)));
    if (full) {
      buf.append(',');
      buf.append(quote(title.getGenre()));
      buf.append(',');
      buf.append(quote(title.getYear() > 0 ? Integer.toString(title.getYear()) : ""));
    }
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
   * @see de.stationadmin.base.track.exporter.TitleListStringFileExporter#getHeadLine(boolean)
   */
  @Override
  protected String getHeadLine(boolean full) {
    StringBuilder buf = new StringBuilder();
    buf.append(quote("Artist"));
    buf.append(',');
    buf.append(quote("Titel"));
    if (full) {
      buf.append(',');
      buf.append(quote("Album"));
    }
    buf.append(',');
    buf.append(quote("Länge"));
    if (full) {
      buf.append(',');
      buf.append(quote("Genre"));
      buf.append(',');
      buf.append(quote("Jahr"));
    }
    return buf.toString();
  }

}
