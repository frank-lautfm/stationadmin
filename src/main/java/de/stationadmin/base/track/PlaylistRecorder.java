/**
 * 
 */
package de.stationadmin.base.track;

import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;

import de.stationadmin.base.StationStatus;
import de.stationadmin.lfmapi.LautfmService;
import de.stationadmin.lfmapi.Song;

/**
 * Records the titles played on a station
 * 
 * @author Frank Korf
 */
public class PlaylistRecorder extends Thread {
  private static final Logger log = Logger.getLogger(PlaylistRecorder.class);

  private TrackRegistry titleRegistry;
  private LautfmService lfmService;
  private String station;
  private TrackCollector collector;
  private StationStatus stationStatus;
  private volatile boolean stopRequested = false;

  public PlaylistRecorder(TrackRegistry titleRegistry, LautfmService lfmService, String station, StationStatus status,
      TrackCollector collector) {
    super();
    this.lfmService = lfmService;
    this.titleRegistry = titleRegistry;
    this.station = station;
    this.collector = collector;
    this.stationStatus = status;
    this.setDaemon(true);
  }

  public PlaylistRecorder(String station, TrackCollector collector) {
    super();
    this.lfmService = new LautfmService();
    this.station = station;
    this.collector = collector;
    this.setDaemon(true);
  }

  public void run() {

    long timeDiff = 0;

    try {
      Date serverTime = this.lfmService.getTime();
      timeDiff = System.currentTimeMillis() - serverTime.getTime();
      log.info("time difference: " + timeDiff + " (Local: " + new Date() + " <=> Server: " + serverTime);
    } catch (Exception e) {
      log.warn("time synchronization failed", e);
    }

    boolean first = true;
    int failures = 0;
    Song lastTrack = null;
    while (!stopRequested) {
      try {
        Song curr = this.lfmService.getCurrentSong(this.station);
        if (this.stationStatus != null) {
          int currTrackId = curr.getId();
          
          String label = curr.getArtist() != null  ? curr.getArtist().getName() : "";
          if(StringUtils.isNotEmpty(curr.getTitle())) {
            label += " - " + curr.getTitle();
          }
          
          
          this.stationStatus.setCurrentTrackLive(curr.isLive());
          this.stationStatus.setCurrentTrackId(currTrackId);
          this.stationStatus.setCurrentTrackLabel(label);
          this.stationStatus.setCurrentTrackEndTime(curr.getEndsAt().getTime() + timeDiff);
        }
        if (!first) {
          if (lastTrack == null || !lastTrack.equals(curr)) {
            if (!curr.isLive() && this.titleRegistry != null && this.titleRegistry.getTrack(curr.getId()) != null) {
              collector.add(this.titleRegistry.getTrack(curr.getId()));
            } else {
              BasicTrack title = new BasicTrack();
              title.setId(curr.getId());
              title.setArtist(curr.getArtist().getName());
              title.setTitle(curr.getTitle());
              title.setLength(curr.getLength());
              collector.add(title);
            }
          }
          lastTrack = curr;
        }
        // else: We don't know when title started - avoid adding it
        first = false;
        long localEndAt = curr.getEndsAt().getTime() + timeDiff;
        int currResidual = (int) ((localEndAt - System.currentTimeMillis()) / 1000);
        if (currResidual <= 0) {
          // count failures and avoid polling every two seconds if no title
          // information is available
          failures++;
        } else {
          failures = 0;
        }
        try {
          int residual = Math.max(currResidual + 2, 7);
          int sleepTime = failures > 2 ? Math.min(1000 * failures, 30000) : residual * 1000;
          Thread.sleep(sleepTime);
        } catch (InterruptedException e) {
        }

      } catch (Exception e) {
        log.warn("failed to retrieve title", e);
        try {
          Thread.sleep(30000);
        } catch (InterruptedException ex) {
        }
      }
    }
  }

  public void requestStop() {
    this.stopRequested = true;
  }
}
