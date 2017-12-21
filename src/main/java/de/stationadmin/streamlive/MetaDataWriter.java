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
    if (source != null) {
      this.readList(source);
    }
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

  public boolean addAdTriggers(String defaultTrackInfo, int position1, int position2) {
    if (position1 > position2) {
      int tmp = position1;
      position1 = position2;
      position2 = tmp;
    }

    boolean inserted = false;
    if (this.times.size() == 0) {

      // just fill in default song infos
      this.times.add(0l);
      this.songs.add(defaultTrackInfo);

      for (int i = 0; i < 10; i++) {
        long p1 = (i * 60 * 60 + position1 * 60) * 1000;
        long p2 = (i * 60 * 60 + position2 * 60) * 1000;
        this.times.add(p1);
        this.songs.add("START_AD_BREAK - START_AD_BREAK");
        this.times.add(p1 + 1000);
        this.songs.add(defaultTrackInfo);
        this.times.add(p2);
        this.songs.add("START_AD_BREAK - START_AD_BREAK");
        this.times.add(p2 + 1000);
        this.songs.add(defaultTrackInfo);
      }
      inserted = true;

    } else {

      int adCnt = 0;
      int nextAdPosition = (position1 * 60 * 1000);

      List<Long> newTimes = new ArrayList<Long>();
      List<String> newSongs = new ArrayList<String>();
      for (int i = 0; i < times.size(); i++) {
        long nextTime = times.get(i);
        if (nextTime > nextAdPosition) {
          newTimes.add(nextTime);
          newSongs.add("START_AD_BREAK - START_AD_BREAK");
          nextTime += 1000;

          adCnt++;
          int nextAdBase = adCnt % 2 == 0 ? position1 : position2;
          nextAdPosition = ((nextAdBase * 60) + (adCnt / 2) * 60 * 60) * 1000;
          inserted = true;
        }

        newTimes.add(nextTime);
        newSongs.add(songs.get(i));
      }

      this.times = newTimes;
      this.songs = newSongs;
    }

    // for (int i = 0; i < times.size(); i++) {
    // System.out.println(TimeFormat.format((int) (times.get(i) / 1000), true) + " "
    // + songs.get(i));
    // }

    return inserted;
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
