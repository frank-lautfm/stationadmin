/**
 * 
 */
package de.stationadmin.gui.tasks.status;

import java.awt.BorderLayout;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 * 
 */
public class TaskExecutionResultPanel extends JPanel {
  private static final long serialVersionUID = -875129706181510245L;

  private TextProvider textProvider;
  private ValueModel taskHolder;

  public TaskExecutionResultPanel(TextProvider textProvider, ValueModel taskHolder) {
    super();
    this.textProvider = textProvider;
    this.taskHolder = taskHolder;
    this.init();
  }

  private void init() {
    this.setLayout(new BorderLayout());

    final JLabel lastExecution = new JLabel();
    JPanel head = new JPanel(new FormLayout("pref,5dlu,pref:grow", "2dlu,pref,2dlu"));
    head.add(new JLabel(this.textProvider.getString("tasks.status.lastExecution.name")), new CellConstraints(1, 2));
    head.add(lastExecution, new CellConstraints(3, 2));
    
    final SimpleDateFormat dateFormat = new SimpleDateFormat(this.textProvider.getString("timeFormat"));
    
    this.add(head, BorderLayout.NORTH);

    final TaskExecutionMessagesTableModel tableModel = new TaskExecutionMessagesTableModel(textProvider);
    JXTable table = new JXTable(tableModel);
    this.add(new JScrollPane(table), BorderLayout.CENTER);
    table.getColumn(0).setMaxWidth(30);
    table.getColumn(0).setCellRenderer(new MessageStatusRenderer());

    this.taskHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ScheduledTask task = (ScheduledTask) evt.getNewValue();
        tableModel.setTask(task);
        if(task != null) {
          if(task.getLastExecution() > 0) {
            Date date = new Date(task.getLastExecution());
            lastExecution.setText(dateFormat.format(date));
          }
          else {
            lastExecution.setText(textProvider.getString("tasks.status.lastExecution.never"));
          }
        }
        else {
          lastExecution.setText(null);
        }

      }
    });

  }
  
  

}
