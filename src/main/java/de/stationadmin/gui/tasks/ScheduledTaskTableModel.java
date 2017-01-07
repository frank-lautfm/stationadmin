/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.base.tasks.Trigger;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class ScheduledTaskTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -8250197233358022155L;
  private List<ScheduledTask> tasks = new ArrayList<ScheduledTask>();
  private TextProvider textProvider;

  public ScheduledTaskTableModel(TaskExecutionService taskService, TextProvider textProvider) {
    this.textProvider = textProvider;
    this.tasks = taskService.getScheduledTasks();
    taskService.addPropertyChangeListener("scheduledTasks", new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        tasks = (List<ScheduledTask>) evt.getNewValue();
        fireTableDataChanged();
      }
    });

  }

  @Override
  public int getRowCount() {
    return this.tasks.size();
  }

  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    if (rowIndex >= this.tasks.size()) {
      return null;
    }
    ScheduledTask task = this.tasks.get(rowIndex);
    Column col = Column.values()[columnIndex];
    switch (col) {
    case TYPE:
      return task.getTask().getName() != null ? task.getTask().getName() : this.textProvider.getString("task.type." + task.getTask().getClass().getSimpleName());
    case TRIGGER:
      return task.getTrigger();
    case LAST_EXECUTION:
      return task.getLastExecution() > 0 ? new Date(task.getLastExecution()) : null;
    case STATUS:
      return task.getLastResult() != null ? task.getLastResult().isSucceeded() : null;
    }
    return null;
  }

  public enum Column {
    TYPE, TRIGGER, LAST_EXECUTION, STATUS
  }

  @Override
  public String getColumnName(int column) {
    return textProvider.getString("tasks.editor.table.column." + Column.values()[column].name().toLowerCase());
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case LAST_EXECUTION:
      return Date.class;
    case TRIGGER:
      return Trigger.class;
    case STATUS:
      return Boolean.class;
    default:
      return String.class;
    }
  }

}
