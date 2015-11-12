/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.List;

import de.stationadmin.base.util.AbstractBean;

/**
 * Tool class for calculating average values for daily summary statistics
 * 
 * @author korf
 */
public class DailySummaryStatistics extends AbstractBean {
  private int duration;
  private int listeners;
  private int avgListeningTime;

  public int getAvgListeningTime() {
    return avgListeningTime;
  }

  public int getDuration() {
    return duration;
  }

  public int getListeners() {
    return listeners;
  }

  public void setAvgListeningTime(int avgListeningTime) {
    int old = this.avgListeningTime;
    this.avgListeningTime = avgListeningTime;
    this.firePropertyChange("avgListeningTime", old, avgListeningTime);
  }

  public void setDuration(int duration) {
    int old = this.duration;
    this.duration = duration;
    this.firePropertyChange("duration", old, duration);
  }

  public void setListeners(int listeners) {
    int old = this.listeners;
    this.listeners = listeners;
    this.firePropertyChange("listeners", old, listeners);
  }

  public void update(List<DailySummary> entries) {
    int sumDuration = 0;
    int sumListeners = 0;
    int sumAvg = 0;
    int cntListeners = 0;

    for (DailySummary summary : entries) {
      sumDuration += summary.getDuration();
      if (!summary.isEstimated()) {
        cntListeners++;
        sumListeners += summary.getListeners();
        sumAvg += summary.getAvgListeningTime();
      }
    }

    if (cntListeners > 0) {
      this.setAvgListeningTime(sumAvg / cntListeners);
      this.setListeners(sumListeners / cntListeners);
    }
    else {
      this.setAvgListeningTime(0);
      this.setListeners(0);
    }
    if(entries.size() > 0) {
      this.setDuration(sumDuration / entries.size());
    }
    else {
      this.setDuration(0);
    }

  }
}
