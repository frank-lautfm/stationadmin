/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.io.Serializable;

/**
 * Contains average listener statistics for one or multiple hours of a day, based on a larger
 * time span
 * 
 * @author korf
 * 
 */
public class ListenersAvgEntry implements Serializable {
  private static final long serialVersionUID = 7917298532398325999L;

  private int startHour;
  private int endHour;
  private int listeners;
  private int fraction;
  
  public ListenersAvgEntry(int startHour, int endHour, int listeners, int fraction) {
    super();
    this.startHour = startHour;
    this.endHour = endHour;
    this.listeners = listeners;
    this.fraction = fraction;
  }

  /**
   * Gets the last hour this entry covers
   * @return hour as value between 0 and 23
   */
  public int getEndHour() {
    return endHour;
  }

  /**
   * Gets the fraction the listeners in the covered time period have among all listeners of a day
   * @return fraction in percent, as value between 0 and 100
   */
  public int getFraction() {
    return fraction;
  }

  /**
   * Gets the average number of listeners during the covered hours
   * @return
   */
  public int getListeners() {
    return listeners;
  }

  /**
   * Gets the first hour this entry covers
   * @return hour as value between 0 and 23
   */
  public int getStartHour() {
    return startHour;
  }

  protected void setFraction(int fraction) {
    this.fraction = fraction;
  }

}
