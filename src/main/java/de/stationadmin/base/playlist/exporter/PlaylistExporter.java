/**
 * 
 */
package de.stationadmin.base.playlist.exporter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;

/**
 * @author korf
 * 
 */
public abstract class PlaylistExporter {

  protected abstract String toString(Entry entry, Title title);

  protected abstract String getHeadLine();

  /**
   * Creates a string representation of the playlist
   * 
   * @param playlist
   * @return
   */
  public String toString(Playlist playlist) {
    StringBuilder buf = new StringBuilder();
    String headData = this.getHeadData(playlist);
    if (headData != null) {
      buf.append(headData);
    }
    String head = this.getHeadLine();
    if (head != null) {
      buf.append(head);
      buf.append("\n");
    }
    for (Entry entry : playlist.getEntries()) {
      Title title = entry.getTrack();
      if (!(title instanceof DetailedTrack)) {
        RegisteredTrack regTitle = playlist.getTrackRegistry().getTrack(entry.getTrackId());
        if (regTitle != null) {
          title = regTitle;
        }
      }
      if (title != null) {
        buf.append(toString(entry, title));
        buf.append("\n");
      }
    }

    return buf.toString();
  }

  /**
   * Saves a playlist to a file
   * 
   * @param playlist
   * @param file
   * @throws IOException
   */
  public void toFile(Playlist playlist, File file) throws IOException {
    FileOutputStream out = new FileOutputStream(file);
    IOUtils.write(this.toString(playlist), out, "UTF-8");
    out.flush();
    out.close();
  }

  protected String getHeadData(Playlist playlist) {
    return null;
  }

}
