/**
 * 
 */
package de.stationadmin.base.tasks;

import java.util.UUID;

/**
 * @author korf
 * 
 */
public class ScheduledTask {
  private String id;
  private Trigger trigger;
  private int triggerTolerance;
  private Task task;
  private TaskExecutionResult lastResult;
  private long lastExecution;

  public ScheduledTask() {
    this.id = UUID.randomUUID().toString();
  }
  
  public Task getTask() {
    return task;
  }

  public void setTask(Task task) {
    this.task = task;
  }

  public long getLastExecution() {
    return lastExecution;
  }

  protected void setLastExecution(long lastExecution) {
    this.lastExecution = lastExecution;
  }

  public Trigger getTrigger() {
    return trigger;
  }

  public void setTrigger(Trigger trigger) {
    this.trigger = trigger;
  }

  public int getTriggerTolerance() {
    return triggerTolerance;
  }

  public void setTriggerTolerance(int triggerTolerance) {
    this.triggerTolerance = triggerTolerance;
  }

  public boolean isDue() {
    return this.trigger.isDue(this.lastExecution, this.triggerTolerance);
  }

  public String getId() {
    return id;
  }

  @Override
  public int hashCode() {
    return this.id.hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof ScheduledTask && ((ScheduledTask)obj).id.equals(this.id);
  }

  public TaskExecutionResult getLastResult() {
    return lastResult;
  }

  protected void setLastResult(TaskExecutionResult lastResult) {
    this.lastResult = lastResult;
  }

}
