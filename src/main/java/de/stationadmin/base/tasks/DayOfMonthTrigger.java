/**
 * 
 */
package de.stationadmin.base.tasks;

import java.util.Calendar;

/**
 * Triggers the execution on certain days of the month
 * 
 * @author korf
 */
public class DayOfMonthTrigger extends Trigger {
  private long daysOfMonth = 2;
  private int hour;
  private int minute;

  @Override
  public long getNextExecutionTimeAfter(long baseTime) {
    if (this.daysOfMonth == 0) {
      return 0;
    }
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

  private boolean matches(Calendar cal) {
    boolean match = (this.daysOfMonth & (1l << cal.get(Calendar.DAY_OF_MONTH))) > 0;
    if(!match && ((1 << 31) & this.daysOfMonth) > 0 && cal.get(Calendar.DAY_OF_MONTH) >= 28) {
      // test if this is the last day of the month
      Calendar cal2 = Calendar.getInstance();
      cal2.setTimeInMillis(cal.getTimeInMillis());
      cal2.add(Calendar.DAY_OF_YEAR, 1);
      if(cal2.get(Calendar.MONTH) != cal.get(Calendar.MONTH)) {
        return true;
      }
    }
    return match;
  }

  /**
   * Gets the days of the month
   * @return days of the month with a bit set for each day that is accepted
   */
  public long getDaysOfMonth() {
    return daysOfMonth;
  }

  /**
   * Sets the days of the month
   * @param daysOfMonth days of the month with a bit set for each day that is accepted
   */
  public void setDaysOfMonth(long daysOfMonth) {
    this.daysOfMonth = daysOfMonth;
  }

  public int getHour() {
    return hour;
  }

  public void setHour(int hour) {
    this.hour = hour;
  }

  public int getMinute() {
    return minute;
  }

  public void setMinute(int minute) {
    this.minute = minute;
  }

}
