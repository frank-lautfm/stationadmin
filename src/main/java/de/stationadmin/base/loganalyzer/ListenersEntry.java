/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.Date;

/**
 * Recorded listener number
 */
public class ListenersEntry {
  private long time;
  private int listeners;
  
  public ListenersEntry(Date time, int listeners) {
    super();
    this.time = time.getTime();
    this.listeners = listeners;
  }

  public Date getTime() {
    return new Date(time);
  }

  public int getListeners() {
    return listeners;
  }

}
