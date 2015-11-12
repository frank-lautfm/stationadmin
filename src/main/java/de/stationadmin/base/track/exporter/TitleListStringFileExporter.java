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
public abstract class TitleListStringFileExporter implements TitleListExporter {

  protected abstract String toString(RegisteredTrack title, boolean full);

  protected abstract String getHeadLine(boolean full);

  /**
   * Creates a string representation of the playlist
   * @param playlist
   * @return
   */
  public String toString(List<RegisteredTrack> titles, boolean full) {
    StringBuilder buf = new StringBuilder();
    String head = this.getHeadLine(full);
    if (head != null) {
      buf.append(head);
      buf.append("\n");
    }
    for (RegisteredTrack title : titles) {
      buf.append(toString(title, full));
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
  public void toFile(List<RegisteredTrack> titles, File file, boolean full) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    IOUtils.write(this.toString(titles, full), out);
    out.flush();
    out.close();
  }
  
}
