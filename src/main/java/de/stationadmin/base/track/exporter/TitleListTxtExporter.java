/**
 * 
 */
package de.stationadmin.base.track.exporter;

import de.stationadmin.base.track.RegisteredTrack;

/**
 * @author Frank
 *
 */
public class TitleListTxtExporter extends TitleListStringFileExporter {

  /* (non-Javadoc)
   * @see de.emjoy.stationadmin.base.title.exporter.TitleListExporter#toString(de.emjoy.stationadmin.base.title.RegisteredTitle, boolean)
   */
  @Override
  protected String toString(RegisteredTrack title, boolean full) {
    StringBuilder buf = new StringBuilder();
    buf.append(title.getArtist() + " - " + title.getTitle());
    if(full && title.getAlbum() != null) {
      buf.append(" (");
      buf.append(title.getAlbum());
      buf.append(")");
    }
    return buf.toString();
  }

  /* (non-Javadoc)
   * @see de.emjoy.stationadmin.base.title.exporter.TitleListExporter#getHeadLine(boolean)
   */
  @Override
  protected String getHeadLine(boolean full) {
    return null;
  }

}
