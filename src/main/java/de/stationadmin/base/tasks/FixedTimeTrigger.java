/**
 * 
 */
package de.stationadmin.base.tasks;


/**
 * Triggers the execution at a given, fixed time. 
 * 
 * @author korf
 */
public class FixedTimeTrigger extends Trigger {
  private long time = System.currentTimeMillis();

  /**
   * @see de.stationadmin.base.tasks.Trigger#getNextExecutionTimeAfter(long)
   */
  @Override
  public long getNextExecutionTimeAfter(long baseTime) {
    return this.time > baseTime ? this.time : 0;
  }

  public long getTime() {
    return time;
  }

  public void setTime(long time) {
    this.time = time;
  }

}
