/**
 * 
 */
package de.stationadmin.gui.tasks;

import de.stationadmin.base.tasks.ScheduledTask;

/**
 * @author korf
 *
 */
public interface ScheduledTaskEditorComponent {

  /**
   * Updates the UI with the values from the given task
   * @param task
   */
  void updateView(ScheduledTask task);
  
  /**
   * Updates the given task with the values from the UI
   * @param task
   */
  void updateTask(ScheduledTask task);
}
