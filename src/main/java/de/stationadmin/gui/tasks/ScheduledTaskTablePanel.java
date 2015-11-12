/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.BorderLayout;
import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.tasks.DayOfMonthTrigger;
import de.stationadmin.base.tasks.FixedTimeTrigger;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.base.tasks.WeekdayTrigger;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskTableModel.Column;

/**
 * @author korf
 * 
 */
public class ScheduledTaskTablePanel extends JPanel {
  private static final long serialVersionUID = 5068113045926275929L;

  private TextProvider textProvider;
  private TaskExecutionService taskService;
  private ValueHolder taskHolder = new ValueHolder(null, true);

  public ScheduledTaskTablePanel(TextProvider textProvider, TaskExecutionService taskService) {
    super();
    this.textProvider = textProvider;
    this.taskService = taskService;
    this.init();
  }

  private void init() {
    this.setLayout(new BorderLayout());

    ScheduledTaskTableModel tableModel = new ScheduledTaskTableModel(this.taskService, this.textProvider);
    final JXTable table = new JXTable(tableModel);
    table.getColumn(Column.TRIGGER.ordinal()).setCellRenderer(new TriggerRenderer());
    table.getColumn(Column.LAST_EXECUTION.ordinal()).setCellRenderer(new DateRenderer());
    table.getColumn(Column.LAST_EXECUTION.ordinal()).setMaxWidth(120);
    table.getColumn(Column.STATUS.ordinal()).setMaxWidth(30);
    table.getColumn(Column.STATUS.ordinal()).setCellRenderer(new ScheduledTaskStatusRenderer());
    table.setSortOrder(Column.LAST_EXECUTION.ordinal(), SortOrder.DESCENDING);

    table.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        int row = table.getSelectedRow() > -1 ? table.convertRowIndexToModel(table.getSelectedRow()) : -1;
        ScheduledTask task = null;
        if (row >= 0 && row < taskService.getScheduledTasks().size()) {
          task = taskService.getScheduledTasks().get(row);
        }
        taskHolder.setValue(task);
      }
    });

    this.taskHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null) {
          ScheduledTask task = (ScheduledTask) evt.getNewValue();
          int selected = table.getSelectedRow() > -1 ? table.convertRowIndexToModel(table.getSelectedRow()) : -1;
          if (selected < 0 || task != taskService.getScheduledTasks().get(selected)) {
            table.getSelectionModel().clearSelection();
          }
        } else {
          table.getSelectionModel().clearSelection();
        }

      }
    });

    this.add(new JScrollPane(table), BorderLayout.CENTER);
  }

  private class DateRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -7019007025334982649L;

    private SimpleDateFormat fmt = new SimpleDateFormat("dd.MM HH:mm");

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component cmp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof Date) {
        setText(fmt.format((Date) value));
      }
      return cmp;
    }
  }

  private class TriggerRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -7019007025334982649L;
    int[] weekdays = { 2, 3, 4, 5, 6, 7, 1 };
    String[] weekdaynames = { "", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };
    NumberFormat intFmt = NumberFormat.getIntegerInstance();
    SimpleDateFormat dateFormat;

    TriggerRenderer() {
      intFmt.setMinimumIntegerDigits(2);
      dateFormat = new SimpleDateFormat(textProvider.getString("timeFormat"));
    }

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component cmp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);

      if (value instanceof WeekdayTrigger) {
        WeekdayTrigger t = (WeekdayTrigger) value;

        StringBuffer buf = new StringBuffer();
        if (t.getWeekdays() > 0) {
          for (int day : weekdays) {
            if ((t.getWeekdays() & (1 << day)) > 0) {
              String name = textProvider.getString("weekday." + weekdaynames[day] + ".short");
              if (buf.length() > 0) {
                buf.append(",");
              }
              buf.append(name);
            }
          }
        } else {
          String mon = textProvider.getString("weekday." + weekdaynames[2] + ".short");
          String sun = textProvider.getString("weekday." + weekdaynames[1] + ".short");
          buf.append(mon + "-" + sun);

        }
        buf.append(' ');
        buf.append(intFmt.format(t.getHour()));
        buf.append(':');
        buf.append(intFmt.format(t.getMinute()));

        setText(buf.toString());
      }
      if (value instanceof DayOfMonthTrigger) {
        DayOfMonthTrigger t = (DayOfMonthTrigger) value;

        StringBuffer buf = new StringBuffer();
        for (int i = 1; i <= 31; i++) {
          if ((t.getDaysOfMonth() & (1 << i)) > 0) {
            if (buf.length() > 0) {
              buf.append(",");
            }
            buf.append(i + ".");
          }
        }
        buf.append(' ');
        buf.append(intFmt.format(t.getHour()));
        buf.append(':');
        buf.append(intFmt.format(t.getMinute()));

        setText(buf.toString());

      }

      if (value instanceof FixedTimeTrigger) {
        setText(dateFormat.format(new Date(((FixedTimeTrigger) value).getTime())));
      }

      return cmp;
    }

  }

  public ValueHolder getTaskHolder() {
    return taskHolder;
  }

}
