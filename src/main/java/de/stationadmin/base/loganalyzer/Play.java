/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.Date;

import de.stationadmin.base.track.BasicTrack;

/**
 * Represents a single play of a title in the past
 * 
 */
public class Play {
  private long startTime;
  private BasicTrack track;

  public Play(Date startTime, BasicTrack track) {
    super();
    this.startTime = startTime.getTime();
    this.track = track;
  }

  public Date getStartTime() {
    return new Date(startTime);
  }

  public BasicTrack getTrack() {
    return track;
  }

  @Override
  public String toString() {
    return new Date(this.startTime) + " " + this.track;
  }

}
