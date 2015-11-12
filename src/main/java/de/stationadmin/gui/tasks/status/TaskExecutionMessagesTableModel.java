/**
 * 
 */
package de.stationadmin.gui.tasks.status;

import javax.swing.table.AbstractTableModel;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.TaskExecutionMessage;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class TaskExecutionMessagesTableModel extends AbstractTableModel {
  private static final long serialVersionUID = 9034996032701032498L;

  private TextProvider textProvider;
  private ScheduledTask task;
  
  public TaskExecutionMessagesTableModel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
  }

  @Override
  public int getColumnCount() {
    return 2;
  }

  @Override
  public int getRowCount() {
    return this.task != null && this.task.getLastResult() != null ? this.task.getLastResult().getMessages().size() : 0;
  }

  public ScheduledTask getTask() {
    return task;
  }

  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    TaskExecutionMessage msg = this.task.getLastResult().getMessages().get(rowIndex);
    if(columnIndex == 0) {
      return msg.isError();
    }
    else {
      return this.textProvider.getString(msg.getKey(), msg.getParameters());
    }
    
  }

  public void setTask(ScheduledTask task) {
    this.task = task;
    this.fireTableDataChanged();
  }

  @Override
  public String getColumnName(int column) {
    return column == 1 ? this.textProvider.getString("tasks.status.column.msg") : "";
  }

  @Override
  public Class<?> getColumnClass(int columnIndex) {
    return columnIndex == 0 ? Boolean.class : String.class;
  }

}
