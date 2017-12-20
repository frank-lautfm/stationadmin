/**
 * 
 */
package de.stationadmin.streamlive;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

/**
 * @author korf
 * 
 */
public class MetaDataWriter {
  private static final Logger log = Logger.getLogger(MetaDataWriter.class);
  private ExecutorService executor = Executors.newSingleThreadExecutor();
  private List<Long> times = new ArrayList<Long>();
  private List<String> songs = new ArrayList<String>();
  private IcecastServerConnector ice;
  private volatile boolean abort = false;

  private volatile String currentSong;

  private int idx = 0;
  private long next;
  
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


  public void onStartBroadcasting() {
    this.idx = 0;
    this.next = idx < times.size() ? times.get(idx) : Long.MAX_VALUE;
  }

  public void onTimeChange(double time) {
    if (time >= next && !abort) {
      this.currentSong = songs.size() > idx ? songs.get(idx) : "";
      this.executor.submit(new SongUpdate(currentSong));
      this.idx++;
      this.next = this.idx < this.times.size() ? this.times.get(this.idx) : Long.MAX_VALUE;
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

  private class SongUpdate implements Runnable {
    private String song;

    SongUpdate(String song) {
      this.song = song;
    }

    public void run() {
      try {
        ice.updateSong(song);
      } catch (IOException e) {
        log.warn("unable to update live meta data", e);
      }

    }
  }
}
