/**
 * 
 */
package de.stationadmin.gui.tasks.trigger;

import java.awt.Component;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JList;
import javax.swing.JPanel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tasks.DayOfMonthTrigger;
import de.stationadmin.base.tasks.FixedTimeTrigger;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.WeekdayTrigger;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * @author korf
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class TriggerPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = -1470320669932304798L;

  private TextProvider textProvider;

  private JPanel weekdayPanel, dayOfMonthPanel, fixedTimePanel, current;
  private JComboBox triggerTypeCmb;

  public TriggerPanel(TextProvider textProvider) {
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.weekdayPanel = new WeekdayTriggerPanel(this.textProvider);
    this.dayOfMonthPanel = new DayOfMonthTriggerPanel(this.textProvider);
    this.fixedTimePanel = new FixedTimeTriggerPanel(textProvider);

    triggerTypeCmb = new JComboBox(TriggerType.values());
    triggerTypeCmb.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = -2246237413234454084L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if(value instanceof TriggerType) {
          setText(textProvider.getString("task.editor.trigger.type." + ((TriggerType)value).name().toLowerCase()));
        }
        return cmp;
      }

    });

    this.setLayout(new FormLayout("pref:grow", "pref,5dlu,pref:grow"));
    this.add(triggerTypeCmb, new CellConstraints(1, 1));

    triggerTypeCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        replaceTriggerEditor();
      }
    });

  }

  private void replaceTriggerEditor() {
    JPanel next = null;
    if (triggerTypeCmb.getSelectedItem() == TriggerType.WEEKDAY) {
      next = weekdayPanel;
    }
    if (triggerTypeCmb.getSelectedItem() == TriggerType.DAY_OF_MONTH) {
      next = dayOfMonthPanel;
    }
    if (triggerTypeCmb.getSelectedItem() == TriggerType.FIXED) {
      next = fixedTimePanel;
    }
    if (next != current) {
      if (current != null) {
        remove(current);
      }
      if (next != null) {
        add(next, new CellConstraints(1, 3, CellConstraints.LEFT, CellConstraints.FILL));
      }
      current = next;
      getRootPane().validate();
      getRootPane().repaint();
    }
  }

  enum TriggerType {
    WEEKDAY, DAY_OF_MONTH, FIXED
  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTrigger() instanceof WeekdayTrigger) {
      triggerTypeCmb.setSelectedItem(TriggerType.WEEKDAY);
    } else if (task.getTrigger() instanceof DayOfMonthTrigger) {
      triggerTypeCmb.setSelectedItem(TriggerType.DAY_OF_MONTH);
    } else {
      triggerTypeCmb.setSelectedItem(TriggerType.FIXED);
    }
    ((ScheduledTaskEditorComponent) this.weekdayPanel).updateView(task);
    ((ScheduledTaskEditorComponent) this.dayOfMonthPanel).updateView(task);
    ((ScheduledTaskEditorComponent) this.fixedTimePanel).updateView(task);
    replaceTriggerEditor();
  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (triggerTypeCmb.getSelectedItem().equals(TriggerType.WEEKDAY)) {
      if (!(task.getTrigger() instanceof WeekdayTrigger)) {
        task.setTrigger(new WeekdayTrigger());
      }
      ((ScheduledTaskEditorComponent) this.weekdayPanel).updateTask(task);
    } else if (triggerTypeCmb.getSelectedItem().equals(TriggerType.DAY_OF_MONTH)) {
      if (!(task.getTrigger() instanceof DayOfMonthTrigger)) {
        task.setTrigger(new DayOfMonthTrigger());
      }
      ((ScheduledTaskEditorComponent) this.dayOfMonthPanel).updateTask(task);
    } else {
      if (!(task.getTrigger() instanceof FixedTimeTrigger)) {
        task.setTrigger(new FixedTimeTrigger());
      }
      ((ScheduledTaskEditorComponent) this.fixedTimePanel).updateTask(task);
    }
  }

}
