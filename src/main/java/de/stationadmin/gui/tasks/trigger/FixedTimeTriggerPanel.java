/**
 * 
 */
package de.stationadmin.gui.tasks.trigger;

import java.awt.Component;
import java.awt.FlowLayout;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.Date;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JSpinner;
import javax.swing.SpinnerNumberModel;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.toedter.calendar.JDateChooser;

import de.stationadmin.base.tasks.FixedTimeTrigger;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;

/**
 * Editor panel for {@link FixedTimeTrigger}
 * 
 * @author korf
 * 
 */
@SuppressWarnings({ "rawtypes", "unchecked" })
public class FixedTimeTriggerPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 5948619134611914227L;
  private TextProvider textProvider;
  private JDateChooser dateChooser;
  private JSpinner hourSpinner, minuteSpinner;
  private JComboBox toleranceCmb;

  public FixedTimeTriggerPanel(TextProvider textProvider) {
    this.textProvider = textProvider;
    this.init();

  }

  private void init() {
    this.setLayout(new FormLayout("max(30dlu;pref),5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.dateChooser = new JDateChooser();
    this.dateChooser.setLocale(this.textProvider.getLocale());
    this.dateChooser.setDateFormatString(this.textProvider.getString("dateFormat"));
    this.add(new JLabel(this.textProvider.getString("task.editor.trigger.property.day")), cc.xy(1, 1));
    this.add(this.dateChooser, cc.xy(3, 1));

    JPanel timePanel = new JPanel(new FlowLayout());
    {
      SpinnerNumberModel hourModel = new SpinnerNumberModel(8, 0, 23, 1);
      this.hourSpinner = new JSpinner(hourModel);

      SpinnerNumberModel minuteModel = new SpinnerNumberModel(0, 0, 58, 5);
      this.minuteSpinner = new JSpinner(minuteModel);

      timePanel.add(this.hourSpinner);
      timePanel.add(new JLabel(":"));
      timePanel.add(this.minuteSpinner);
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

    this.add(new JLabel(textProvider.getString("task.editor.trigger.property.time")), cc.xy(1, 5));
    this.add(timePanel, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    this.add(new JLabel(textProvider.getString("task.editor.trigger.property.within")), cc.xy(1, 7));
    this.add(tolerancePanel, cc.xy(3, 7, CellConstraints.LEFT, CellConstraints.CENTER));

  }

  /**
   * @see de.stationadmin.gui.tasks.ScheduledTaskEditorComponent#updateView(de.stationadmin.base.tasks.ScheduledTask)
   */
  @Override
  public void updateView(ScheduledTask task) {
    FixedTimeTrigger trigger;
    if (task.getTrigger() instanceof FixedTimeTrigger) {
      trigger = (FixedTimeTrigger) task.getTrigger();
    } else {
      trigger = new FixedTimeTrigger();
    }
    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(trigger.getTime());
    this.dateChooser.setDate(new Date(trigger.getTime()));

    this.hourSpinner.setValue(cal.get(Calendar.HOUR_OF_DAY));
    this.minuteSpinner.setValue(cal.get(Calendar.MINUTE));
  }

  /**
   * @see de.stationadmin.gui.tasks.ScheduledTaskEditorComponent#updateTask(de.stationadmin.base.tasks.ScheduledTask)
   */
  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTrigger() instanceof FixedTimeTrigger) {
      FixedTimeTrigger trigger = (FixedTimeTrigger) task.getTrigger();

      Calendar cal = Calendar.getInstance();
      if (this.dateChooser.getDate() != null) {
        cal.setTime(this.dateChooser.getDate());
      }

      cal.set(Calendar.HOUR_OF_DAY, (Integer) this.hourSpinner.getValue());
      cal.set(Calendar.MINUTE, (Integer) this.minuteSpinner.getValue());
      cal.set(Calendar.SECOND, 0);
      cal.set(Calendar.MILLISECOND, 0);
      trigger.setTime(cal.getTimeInMillis());

      task.setTriggerTolerance((Integer) this.toleranceCmb.getSelectedItem());
    }
  }

}
