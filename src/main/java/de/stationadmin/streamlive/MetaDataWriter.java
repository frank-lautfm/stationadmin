/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;


/**
 * @author korf
 * 
 */
public class MetaDataWriter {
  private static final Logger log = Logger.getLogger(MetaDataWriter.class);
  private List<Long> times = new ArrayList<Long>();
  private List<String> songs = new ArrayList<String>();
  private IcecastServerConnector ice;
  private volatile boolean abort = false;
  
  private volatile String currentSong;

  public MetaDataWriter(File source, IcecastServerConnector ice) throws IOException {
    this.ice = ice;
    this.readList(source);
  }

  @SuppressWarnings("unchecked")
  private void readList(File file) throws IOException {
    FileInputStream in = new FileInputStream(file);
    List<String> lines = IOUtils.readLines(in);
    for (String line : lines) {
      line = line.trim();
      if (line.length() > 0) {
        String[] parts = StringUtils.split(line, " \t", 2);
        if (parts.length == 2) {
          long time = parseTime(parts[0]);
          times.add(time);
          songs.add(parts[1]);
        }
      }
    }
    in.close();
  }

  /**
   * Writes the meta data
   * 
   * @param startTime
   *          time at which the stream started
   */
  public void write(long startTime) {
    int idx = 0;
    while (idx < times.size() && !abort) {
      long next = startTime + times.get(idx);
      long diff = next - System.currentTimeMillis();
      if (next > 0) {
        try {
          Thread.sleep(diff);
        } catch (Exception e) {
        }
      }
      if (!abort) {
        try {
          ice.updateSong(songs.get(idx));
          this.currentSong = songs.get(idx);
        } catch (IOException e) {
          log.warn("unable to update live meta data", e);
        }
        idx++;
      }
    }
  }

  public void abort() {
    this.abort = true;
  }

  public static long parseTime(String str) {
    String[] parts = StringUtils.split(str, ":");
    if (parts.length > 3) {
      throw new NumberFormatException();
    }
    int hours = 0;
    int idx = 0;
    if (parts.length == 3) {
      hours = Integer.parseInt(parts[idx++]);
    }
    int minutes = Integer.parseInt(parts[idx++]);
    int seconds = Integer.parseInt(parts[idx++]);

    return (hours * 60 * 60 + minutes * 60 + seconds) * 1000;

  }

  /**
   * @return the abort
   */
  public boolean isAbort() {
    return abort;
  }

  /**
   * @return the currentSong
   */
  public String getCurrentSong() {
    return currentSong;
  }

  /**
   * @return the times
   */
  public List<Long> getTimes() {
    return times;
  }

  /**
   * @return the songs
   */
  public List<String> getSongs() {
    return songs;
  }

}
