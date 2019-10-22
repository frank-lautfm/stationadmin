/**
 * 
 */
package de.stationadmin.base;

import de.stationadmin.base.util.AbstractBean;

/**
 * Status information of the station
 * 
 * @author Frank Korf
 */
public class StationStatus extends AbstractBean {
  private int rank;
  private int currentListeners;
  private int peakListeners;
  private int peakRank = 0;
  private int listenersToday;
  private int listenersYesterday;
  private int avgListeningTimeToday;
  private int avgListeningTimeYesterday;
  private int durationToday;
  private int durationTodayCalculated;
  private int durationYesterday;
  private int currentTrackId;
  private boolean currentTrackLive;
  private String currentTrackLabel;
  
  public String getCurrentTrackLabel() {
    return currentTrackLabel;
  }

  private long currentTrackEndTime;
  private int currentPlaylistId;

  public int getAvgListeningTimeToday() {
    return avgListeningTimeToday;
  }

  public int getCurrentListeners() {
    return currentListeners;
  }

  public int getCurrentPlaylistId() {
    return currentPlaylistId;
  }

  public int getCurrentTrackId() {
    return currentTrackId;
  }

  public int getListenersToday() {
    return listenersToday;
  }

  public int getRank() {
    return rank;
  }

  public void setAvgListeningTimeToday(int avgListeningTime) {
    int old = this.avgListeningTimeToday;
    this.avgListeningTimeToday = avgListeningTime;
    this.firePropertyChange("avgListeningTimeToday", old, avgListeningTime);
  }

  public void setCurrentListeners(int currentListeners) {
    this.currentListeners = currentListeners;
    this.firePropertyChange("currentListeners", -1, currentListeners); // force sending of event
    if (this.currentListeners > this.peakListeners) {
      this.setPeakListeners(this.currentListeners);
    }
  }

  public void setCurrentPlaylistId(int currentPlaylistId) {
    int old = this.currentPlaylistId;
    this.currentPlaylistId = currentPlaylistId;
    this.firePropertyChange("currentPlaylistId", old, currentPlaylistId);
  }

  public void setCurrentTrackId(int currentTitleId) {
    int old = this.currentTrackId;
    this.currentTrackId = currentTitleId;
    this.firePropertyChange("currentTrackId", old, currentTitleId);
  }

  public void setCurrentTrackLabel(String currentTrackLabel) {
    String old = this.currentTrackLabel;
    this.currentTrackLabel = currentTrackLabel;
    this.firePropertyChange("currentTrackLabel", old, currentTrackLabel);
  }

  public void setListenersToday(int listenersPerDay) {
    int old = this.listenersToday;
    this.listenersToday = listenersPerDay;
    this.firePropertyChange("listenersToday", old, listenersPerDay);
  }

  public void setRank(int rank) {
    int old = this.rank;
    this.rank = rank;
    this.firePropertyChange("rank", old, rank);
    if(this.peakRank == 0 || rank < this.peakRank) {
      this.setPeakRank(rank);
    }
  }

  /**
   * @return the currentTitleEndTime
   */
  public long getCurrentTrackEndTime() {
    return currentTrackEndTime;
  }

  /**
   * @param currentTitleEndTime the currentTitleEndTime to set
   */
  public void setCurrentTrackEndTime(long currentTitleEndTime) {
    this.currentTrackEndTime = currentTitleEndTime;
  }

  public int getPeakListeners() {
    return peakListeners;
  }

  private void setPeakListeners(int peakListeners) {
    int old = this.peakListeners;
    this.peakListeners = peakListeners;
    this.firePropertyChange("peakListeners", old, peakListeners);
  }

  /**
   * @return the listenersYesterday
   */
  public int getListenersYesterday() {
    return listenersYesterday;
  }

  /**
   * @param listenersYesterday the listenersYesterday to set
   */
  public void setListenersYesterday(int listenersYesterday) {
    int old = this.listenersYesterday;
    this.listenersYesterday = listenersYesterday;
    this.firePropertyChange("listenersYesterday", old, listenersYesterday);
  }

  /**
   * @return the avgListeningTimeYesterday
   */
  public int getAvgListeningTimeYesterday() {
    return avgListeningTimeYesterday;
  }

  /**
   * @param avgListeningTimeYesterday the avgListeningTimeYesterday to set
   */
  public void setAvgListeningTimeYesterday(int avgListeningTimeYesterday) {
    int old = this.avgListeningTimeYesterday;
    this.avgListeningTimeYesterday = avgListeningTimeYesterday;
    this.firePropertyChange("avgListeningTimeYesterday", old, avgListeningTimeYesterday);
  }

  /**
   * @return the durationToday
   */
  public int getDurationToday() {
    return durationToday;
  }

  /**
   * @param durationToday the durationToday to set
   */
  public void setDurationToday(int durationToday) {
    int old = this.durationToday;
    this.durationToday = durationToday;
    this.firePropertyChange("durationToday", old, durationToday);
  }

  /**
   * @return the durationYesterday
   */
  public int getDurationYesterday() {
    return durationYesterday;
  }

  /**
   * @param durationYesterday the durationYesterday to set
   */
  public void setDurationYesterday(int durationYesterday) {
    int old = this.durationYesterday;
    this.durationYesterday = durationYesterday;
    this.firePropertyChange("durationYesterday", old, durationYesterday);
  }

  /**
   * @return the peakRank
   */
  public int getPeakRank() {
    return peakRank;
  }

  /**
   * @param peakRank the peakRank to set
   */
  public void setPeakRank(int peakRank) {
    int old = this.peakRank;
    this.peakRank = peakRank;
    this.firePropertyChange("peakRank", old, peakRank);
  }

  public int getDurationTodayCalculated() {
    return durationTodayCalculated;
  }

  public void setDurationTodayCalculated(int durationTodayCalculated) {
    int old = this.durationTodayCalculated;
    this.durationTodayCalculated = durationTodayCalculated;
    this.firePropertyChange("durationTodayCalculated", old, durationTodayCalculated);
  }

  public boolean isCurrentTrackLive() {
    return currentTrackLive;
  }

  public void setCurrentTrackLive(boolean currentTrackLive) {
    boolean old = this.currentTrackLive;
    this.currentTrackLive = currentTrackLive;
    this.firePropertyChange("currentTrackLive", old, currentTrackLive);
  }

}
