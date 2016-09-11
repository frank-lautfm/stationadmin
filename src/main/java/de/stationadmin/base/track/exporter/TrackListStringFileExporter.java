/**
 * 
 */
package de.stationadmin.base.track.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;

import de.stationadmin.base.track.RegisteredTrack;

/**
 * @author korf
 *
 */
public abstract class TrackListStringFileExporter implements TrackListExporter {

  protected abstract String toString(RegisteredTrack title);

  protected abstract String getHeadLine();

  /**
   * Creates a string representation of the playlist
   * @param playlist
   * @return
   */
  public String toString(List<RegisteredTrack> titles) {
    StringBuilder buf = new StringBuilder();
    String head = this.getHeadLine();
    if (head != null) {
      buf.append(head);
      buf.append("\n");
    }
    for (RegisteredTrack title : titles) {
      buf.append(toString(title));
      buf.append("\n");
    }

    return buf.toString();
  }

  /**
   * Saves a playlist to a file
   * @param playlist
   * @param file
   * @throws IOException
   */
  public void toFile(List<RegisteredTrack> titles, File file) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    IOUtils.write(this.toString(titles), out);
    out.flush();
    out.close();
  }
  
}
