/**
 * 
 */
package de.stationadmin.base.playlist.exporter;

import java.io.ByteArrayOutputStream;
import java.io.ObjectOutputStream;
import java.util.List;
import java.util.Map;

import javax.xml.bind.DatatypeConverter;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.format.ExtendedTrackFormat;

/**
 * Exports the playlist in a format that is easy to reimport.
 * <p>
 * Each line consists of 3 values, separated by tabs: title id, artist and title
 * name
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistBackupExporter extends PlaylistExporter {
  public static final String EXTENSION = "lfm";
  private ExtendedTrackFormat format = new ExtendedTrackFormat(true);

  /**
   * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#getHeadLine()
   */
  @Override
  protected String getHeadLine() {
    return null;
  }

  /**
   * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#toString(de.stationadmin.base.playlist.Playlist.Entry,
   *      de.stationadmin.base.track.Title)
   */
  @Override
  protected String toString(Entry entry, Title title) {
    return this.format.toString(title);
  }

  /**
   * @see de.stationadmin.base.playlist.exporter.PlaylistExporter#getHeadData(de.stationadmin.base.playlist.Playlist)
   */
  @Override
  protected String getHeadData(Playlist playlist) {
    List<String> properties = playlist.getProperties();
    StringBuilder buf = new StringBuilder();
    for (String line : properties) {
      buf.append("# ");
      buf.append(line);
      buf.append('\n');
    }

    try {
      Map<Integer, Long> tsMap = playlist.getTimestampMap();
      ByteArrayOutputStream out = new ByteArrayOutputStream(1024);
      ObjectOutputStream objOut = new ObjectOutputStream(out);
      objOut.writeObject(tsMap);
      objOut.close();
      byte[] data = out.toByteArray();
      String data64 = DatatypeConverter.printBase64Binary(data);
      int offset = 0;
      do {
        String line = offset + 80 < data64.length() ? data64.substring(offset, offset + 80) : data64.substring(offset);
        buf.append("# tsmap = " + line);
        buf.append('\n');
        offset += 80;
      } while (offset < data64.length());

    } catch (Exception e) {
      e.printStackTrace();
    }

    return buf.toString();
  }

}
