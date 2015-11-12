/**
 * 
 */
package de.stationadmin.gui.tasks.trigger;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;
import java.util.Calendar;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.WeekdayTrigger;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link WeekdayTrigger}
 * 
 * @author korf
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class WeekdayTriggerPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = -2545406970817101938L;
  private static int[] weekdays = { 2, 3, 4, 5, 6, 7, 1 };
  private static String[] weekdaynames = { "", "sunday", "monday", "tuesday", "wednesday", "thursday", "friday", "saturday" };

  private TextProvider textProvider;
  private JCheckBox[] weekdayCbs = new JCheckBox[8];
  private JCheckBox[] weekCbs = new JCheckBox[6];
  private JSpinner hourCmb, minuteCmb;
  private JComboBox toleranceCmb;

  public WeekdayTriggerPanel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;

    this.init();
  }

  private void init() {

    JPanel weekPanel = new JPanel(new GridLayout(1, 6, 5, 5));
    {
      for (int i = 1; i <= 5; i++) {
        this.weekCbs[i] = new JCheckBox(Integer.toString(i) + ".");
        weekPanel.add(this.weekCbs[i]);
      }
    }

    JPanel weekdayPanel = new JPanel(new GridLayout(2, 4, 5, 5));
    {
      for (int wday : weekdays) {
        this.weekdayCbs[wday] = new JCheckBox(this.textProvider.getString("weekday." + weekdaynames[wday] + ".short"));
        weekdayPanel.add(this.weekdayCbs[wday]);
      }
    }

    JPanel timePanel = new JPanel(new FlowLayout());
    {
      SpinnerNumberModel hourModel = new SpinnerNumberModel(8, 0, 23, 1);
      this.hourCmb = new JSpinner(hourModel);

      SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 58, 5);
      this.minuteCmb = new JSpinner(minuteModel);

      timePanel.add(this.hourCmb);
      timePanel.add(new JLabel(":"));
      timePanel.add(this.minuteCmb);
    }

    JPanel tolerancePanel = new JPanel(new FlowLayout());
    {
      final NumberFormat fmt = NumberFormat.getInstance(textProvider.getLocale());
      fmt.setMinimumFractionDigits(1);
      fmt.setGroupingUsed(false);
      Integer[] minutes = { 0, 30, 60, 90, 120, 180, 240, 300, 360, 720, 1440 };
      this.toleranceCmb = new JComboBox(minutes);
      this.toleranceCmb.setRenderer(new DefaultListCellRenderer() {
        private static final long serialVersionUID = -7796279404882984799L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          Integer minutes = (Integer) value;
          float hours = minutes != null ? (float) (minutes.intValue()) / 60 : 0f;
          setText(fmt.format(hours));
          return cmp;
        }

      });
      tolerancePanel.add(toleranceCmb);
      tolerancePanel.add(new JLabel(" " + this.textProvider.getString("task.editor.trigger.property.within.hours")));
    }

    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.add(new JLabel(this.textProvider.getString("task.editor.trigger.property.weekdays")), cc.xy(1, 1, CellConstraints.LEFT, CellConstraints.TOP));
    this.add(weekdayPanel, cc.xy(3, 1, CellConstraints.LEFT, CellConstraints.TOP));

    this.add(new JLabel(this.textProvider.getString("task.editor.trigger.property.weeks")), cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.TOP));
    this.add(weekPanel, cc.xy(3, 3, CellConstraints.LEFT, CellConstraints.CENTER));

    this.add(new JLabel(textProvider.getString("task.editor.trigger.property.time")), cc.xy(1, 5));
    this.add(timePanel, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    this.add(new JLabel(textProvider.getString("task.editor.trigger.property.within")), cc.xy(1, 7));
    this.add(tolerancePanel, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.CENTER));

  }

  public void updateTask(ScheduledTask task) {
    if (task.getTrigger() instanceof WeekdayTrigger) {
      WeekdayTrigger trigger = (WeekdayTrigger) task.getTrigger();
      int weeks = 0;
      boolean all = true;
      for (int i = 1; i <= 5; i++) {
        if (this.weekCbs[i].isSelected()) {
          weeks |= 1 << i;
        } else {
          all = false;
        }
      }
      trigger.setWeeks(all ? 0 : weeks);

      all = true;
      int weekdays = 0;
      for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
        if (this.weekdayCbs[i].isSelected()) {
          weekdays |= 1 << i;
        } else {
          all = false;
        }
      }
      trigger.setWeekdays(all ? 0 : weekdays);

      trigger.setHour((Integer) this.hourCmb.getValue());
      trigger.setMinute((Integer) this.minuteCmb.getValue());

      task.setTriggerTolerance((Integer) this.toleranceCmb.getSelectedItem());
    }

  }

  public void updateView(ScheduledTask task) {
    WeekdayTrigger trigger;
    if (task.getTrigger() instanceof WeekdayTrigger) {
      trigger = (WeekdayTrigger) task.getTrigger();
    } else {
      trigger = new WeekdayTrigger();
    }

    for (int i = 1; i <= 5; i++) {
      boolean set = trigger.getWeeks() == 0 || (trigger.getWeeks() & (1 << i)) > 0;
      this.weekCbs[i].setSelected(set);
    }
    for (int i = Calendar.SUNDAY; i <= Calendar.SATURDAY; i++) {
      boolean set = trigger.getWeekdays() == 0 || (trigger.getWeekdays() & (1 << i)) > 0;
      this.weekdayCbs[i].setSelected(set);
    }

    this.hourCmb.setValue(trigger.getHour());
    this.minuteCmb.setValue(trigger.getMinute());

    this.toleranceCmb.setSelectedItem(task.getTriggerTolerance());

  }

}
