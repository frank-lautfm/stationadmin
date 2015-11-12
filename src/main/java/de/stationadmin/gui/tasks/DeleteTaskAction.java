/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.util.AppUtils;

public class DeleteTaskAction extends AbstractAction {
  private static final long serialVersionUID = 191371583298483504L;
  private ValueModel taskHolder;
  private TextProvider textProvider;
  private TaskExecutionService taskService;

  public DeleteTaskAction(TextProvider textProvider, TaskExecutionService taskService, ValueModel taskHolder) {
    this.putValue(Action.SMALL_ICON, AppUtils.getIcon("delete.png"));
    this.setEnabled(false);
    this.taskHolder = taskHolder;
    this.taskService = taskService;
    this.textProvider = textProvider;
    taskHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() != null);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    ScheduledTask task = (ScheduledTask) taskHolder.getValue();
    if (task != null) {
      try {
        taskService.deleteScheduledTask(task.getId());
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "tasks.editor.action.delete.msg.failed"));
      }
    }

  }

}