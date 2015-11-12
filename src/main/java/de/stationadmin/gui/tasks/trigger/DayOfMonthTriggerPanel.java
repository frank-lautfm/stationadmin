/**
 * 
 */
package de.stationadmin.gui.tasks.trigger;

import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.text.NumberFormat;

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

import de.stationadmin.base.tasks.DayOfMonthTrigger;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link DayOfMonthTrigger}
 * 
 * @author korf
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class DayOfMonthTriggerPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = -2545406970817101938L;

  private TextProvider textProvider;
  private JCheckBox[] dayCbs = new JCheckBox[32];
  private JSpinner hourCmb, minuteCmb;
  private JComboBox toleranceCmb;

  public DayOfMonthTriggerPanel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
    this.init();

  }

  private void init() {

    JPanel dayPanel = new JPanel(new GridLayout(4, 7, 5, 5));
    {
      for (int i = 1; i <= 31; i++) {
        this.dayCbs[i] = new JCheckBox(Integer.toString(i) + ".");
        dayPanel.add(this.dayCbs[i]);
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
          float hours = (float) (minutes.intValue()) / 60;
          setText(fmt.format(hours));
          return cmp;
        }

      });
      tolerancePanel.add(toleranceCmb);
      tolerancePanel.add(new JLabel(" " + this.textProvider.getString("task.editor.trigger.property.within.hours")));
    }

    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.add(dayPanel, cc.xywh(1, 1, 3, 1));

    this.add(new JLabel("um"), cc.xy(1, 5));
    this.add(timePanel, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    this.add(new JLabel(textProvider.getString("task.editor.trigger.property.within")), cc.xy(1, 7));
    this.add(tolerancePanel, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.CENTER));

  }

  public void updateTask(ScheduledTask task) {
    if (task.getTrigger() instanceof DayOfMonthTrigger) {
      DayOfMonthTrigger trigger = (DayOfMonthTrigger) task.getTrigger();
      long days = 0;
      boolean all = true;
      for (int i = 1; i <= 31; i++) {
        if (this.dayCbs[i].isSelected()) {
          days |= 1l << i;
        } else {
          all = false;
        }
      }
      trigger.setDaysOfMonth(all ? 0 : days);
      trigger.setHour((Integer) this.hourCmb.getValue());
      trigger.setMinute((Integer) this.minuteCmb.getValue());

      task.setTriggerTolerance((Integer) this.toleranceCmb.getSelectedItem());
    }
  }

  public void updateView(ScheduledTask task) {
    DayOfMonthTrigger trigger;
    if (task.getTrigger() instanceof DayOfMonthTrigger) {
      trigger = (DayOfMonthTrigger) task.getTrigger();
    } else {
      trigger = new DayOfMonthTrigger();
    }

    for (int i = 1; i <= 31; i++) {
      boolean set = trigger.getDaysOfMonth() == 0 || (trigger.getDaysOfMonth() & (1l << i)) > 0;
      this.dayCbs[i].setSelected(set);
    }

    this.hourCmb.setValue(trigger.getHour());
    this.minuteCmb.setValue(trigger.getMinute());

    this.toleranceCmb.setSelectedItem(task.getTriggerTolerance());

  }

}
