/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.PlaylistShuffleTask;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.Task;
import de.stationadmin.base.tasks.WeekdayTrigger;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

public class AddTaskAction extends AbstractAction {
  private static final long serialVersionUID = 1156775040541350232L;
  private TextProvider textProvider;
  private ValueModel taskHolder;
  private Class<? extends Task> taskType;

  public AddTaskAction(TextProvider textProvider, Class<? extends Task> taskType, ValueModel taskHolder) {
    super(textProvider.getString("task.type." + taskType.getSimpleName()));
    this.taskHolder = taskHolder;
    this.taskType = taskType;
    this.textProvider = textProvider;
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    taskHolder.setValue(null);
    ScheduledTask task = new ScheduledTask();
    task.setTrigger(new WeekdayTrigger());
    task.setTriggerTolerance(60 * 6);
    task.setTask(new PlaylistShuffleTask());

    try {
      task.setTask(taskType.newInstance());

      taskHolder.setValue(task);
    } catch (Exception e) {
      JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "tasks.editor.action.create.msg.failed"));

    }
  }

}