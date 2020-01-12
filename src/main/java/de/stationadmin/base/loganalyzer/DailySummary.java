/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.io.Serializable;
import java.util.Date;

/**
 * @author korf
 * 
 */
public class DailySummary implements Serializable {
  private static final long serialVersionUID = -5234164922460587710L;

  private Date day;
  private int listeners;
  private int uniqs;
  private int duration;
  private int avgListeningTime;
  private boolean estimated = false;
  
  public DailySummary(Date day, int duration) {
    super();
    this.day = day;
    this.duration = duration;
    this.estimated = true;
  }
  
  public DailySummary(Date day, int listeners, int duration, int avgListeningTime, int uniqs) {
    super();
    this.day = day;
    this.listeners = listeners;
    this.duration = duration;
    this.avgListeningTime = avgListeningTime;
    this.estimated = false;
    this.uniqs = uniqs;
  }

  public int getAvgListeningTime() {
    return avgListeningTime;
  }

  public int getDuration() {
    return duration;
  }

  public int getListeners() {
    return listeners;
  }

  public boolean isEstimated() {
    return estimated;
  }
  public Date getDay() {
    return day;
  }
  public int getUniqs() {
    return uniqs;
  }
  public void setUniqs(int uniqs) {
    this.uniqs = uniqs;
  }

}
