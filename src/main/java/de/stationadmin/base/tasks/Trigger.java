/**
 * 
 */
package de.stationadmin.base.tasks;


/**
 * @author korf
 * 
 */
public abstract class Trigger {

  public boolean isDue(long lastExecutionTime, int tolerance) {
    long toleranceMs = tolerance > 0 ? tolerance * 1000 * 60 : 60000;
    long baseTime = System.currentTimeMillis() - toleranceMs;
    long next = this.getNextExecutionTimeAfter(baseTime);
    boolean due = next > lastExecutionTime && next <= System.currentTimeMillis();
    return due;
  }

  public abstract long getNextExecutionTimeAfter(long baseTime);
}
