/**
 * 
 */
package de.stationadmin.base.tasks;

import java.util.Calendar;

/**
 * Triggers the execution at given weekdays. Optionally only the x-th weekday of
 * a month can be selected.
 * 
 * @author korf
 */
public class WeekdayTrigger extends Trigger {
  private int weekdays;
  private int hour;
  private int minute;
  private int weeks;

  public int getHour() {
    return hour;
  }

  public int getMinute() {
    return minute;
  }

  @Override
  public long getNextExecutionTimeAfter(long baseTime) {
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(baseTime);

    boolean todayIsCandiate = this.hour > cal.get(Calendar.HOUR_OF_DAY)
        || (this.hour == cal.get(Calendar.HOUR_OF_DAY) && this.minute >= cal.get(Calendar.MINUTE));
    cal.set(Calendar.HOUR_OF_DAY, this.hour);
    cal.set(Calendar.MINUTE, this.minute);
    cal.set(Calendar.SECOND, 0);
    cal.set(Calendar.MILLISECOND, 0);

    if (!todayIsCandiate) {
      cal.add(Calendar.DAY_OF_YEAR, 1);
    }

    while (!matches(cal)) {
      cal.add(Calendar.DAY_OF_YEAR, 1);
    }

    return cal.getTimeInMillis();
  }

  public int getWeekdays() {
    return weekdays;
  }

  public int getWeeks() {
    return weeks;
  }

  private boolean matches(Calendar cal) {
    // System.out.println(cal.getTime() + " " + cal.get(Calendar.DAY_OF_WEEK));
    boolean matchingWeekday = this.weekdays == 0 || (1 << cal.get(Calendar.DAY_OF_WEEK) & this.weekdays) > 0;
    if (matchingWeekday && this.weeks > 0) {
      Calendar cal2 = Calendar.getInstance();
      cal2.setTimeInMillis(cal.getTimeInMillis());
      int week = 0;
      do {
        week++;
        cal2.add(Calendar.DAY_OF_YEAR, -7);
      } while (cal.get(Calendar.MONTH) == cal2.get(Calendar.MONTH));

      return (this.weeks & 1 << week) > 0;
    }

    return matchingWeekday;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

  public void setWeekdays(int weekdays) {
    this.weekdays = weekdays;
  }

  public void setWeeks(int weeks) {
    this.weeks = weeks;
  }

}
