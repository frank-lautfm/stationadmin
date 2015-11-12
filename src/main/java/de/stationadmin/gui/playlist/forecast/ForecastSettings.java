/**
 * 
 */
package de.stationadmin.gui.playlist.forecast;

import java.util.Calendar;
import java.util.Date;

import com.jgoodies.binding.beans.Model;

import de.stationadmin.base.schedule.Schedule.Weekday;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ForecastSettings extends Model {
  private static final long serialVersionUID = 2060865994244937156L;
  
  private Weekday weekday;
  private int startHour;
  private int hours = 24;
  private int offset;
  
  public ForecastSettings() {
    this.weekday = Weekday.getWeekday(new Date());
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    this.startHour = cal.get(Calendar.HOUR_OF_DAY);
  }
  
  /**
   * @return the hours
   */
  public int getHours() {
    return hours;
  }

  /**
   * @return the offset
   */
  public int getOffset() {
    return offset;
  }

  /**
   * @return the startHour
   */
  public int getStartHour() {
    return startHour;
  }

  public Date getStartTime() {
    Calendar cal = Calendar.getInstance();
    cal.setTime(new Date());
    cal.set(Calendar.MINUTE, 0);
    cal.set(Calendar.SECOND, 0);
    while(cal.get(Calendar.HOUR_OF_DAY) != this.startHour || cal.get(Calendar.DAY_OF_WEEK) != weekday.getCalDay()) {
      cal.add(Calendar.HOUR_OF_DAY, 1);
    }
    return cal.getTime();
  }

  /**
   * @return the weekday
   */
  public Weekday getWeekday() {
    return weekday;
  }

  /**
   * @param hours the hours to set
   */
  public void setHours(int hours) {
    int old = this.hours;
    this.hours = hours;
    this.firePropertyChange("hours", old, hours);
  }

  /**
   * @param offset
   *          the offset to set
   */
  public void setOffset(int offset) {
    int old = this.offset;
    this.offset = offset;
    this.firePropertyChange("offset", old, offset);
  }

  /**
   * @param startHour the startHour to set
   */
  public void setStartHour(int startHour) {
    int old = this.startHour;
    this.startHour = startHour;
    this.firePropertyChange("startHour", old, startHour);
  }

  /**
   * @param weekday
   *          the weekday to set
   */
  public void setWeekday(Weekday weekday) {
    Weekday old = this.weekday;
    this.weekday = weekday;
    this.firePropertyChange("weekday", old, weekday);
  }

}
