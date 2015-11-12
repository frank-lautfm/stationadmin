/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang.StringUtils;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.schedule.PlaylistExchangeTask;
import de.stationadmin.base.schedule.ScheduleImportTask;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;
import de.stationadmin.gui.util.AppUtils;

/**
 * Editor panel for {@link PlaylistExchangeTask}
 * 
 * @author korf
 * 
 */
public class ScheduleImportTaskPanel extends JPanel implements ScheduledTaskEditorComponent {
  private static final long serialVersionUID = 1319217057035869783L;
  private TextProvider textProvider;

  private JTextField taskNameTf;
  private JTextField filenameTf;

  public ScheduleImportTaskPanel(TextProvider textProvider) {
    super();
    this.textProvider = textProvider;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("pref,5dlu,pref:grow", "pref,5dlu,pref,5dlu,pref"));
    CellConstraints cc = new CellConstraints();

    this.taskNameTf = new JTextField(15);
    this.add(new JLabel(this.textProvider.getString("task.name")), cc.xy(1, 1));
    this.add(taskNameTf, cc.xy(3, 1));

    JLabel desc = new JLabel(this.textProvider.getString("task.schedule.import.editor.property.filename"));
    this.add(desc, cc.xywh(1, 3, 3, 1));

    JPanel filenamePanel = new JPanel(new FormLayout("pref:grow,2dlu,pref", "pref"));
    this.filenameTf = new JTextField(15);
    filenamePanel.add(this.filenameTf, cc.xy(1, 1));

    JButton selectBtn = new JButton("...");
    selectBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        JFileChooser fileChooser = new JFileChooser();
        if (StringUtils.isNotEmpty(filenameTf.getText())) {
          fileChooser.setSelectedFile(new File(filenameTf.getText()));
        }
        fileChooser.setFileFilter(new FileNameExtensionFilter("Sendeplan", "sched"));
        if (fileChooser.showOpenDialog(AppUtils.getRootFrame()) == JFileChooser.APPROVE_OPTION) {
          filenameTf.setText(fileChooser.getSelectedFile().getAbsolutePath());
        }
      }
    });
    filenamePanel.add(selectBtn, cc.xy(3, 1));
    this.add(filenamePanel, cc.xywh(1, 5, 3, 1));

  }

  @Override
  public void updateView(ScheduledTask task) {
    if (task.getTask() instanceof ScheduleImportTask) {
      ScheduleImportTask importTask = (ScheduleImportTask) task.getTask();
      this.filenameTf.setText(StringUtils.trimToEmpty(importTask.getFilename()));
      this.taskNameTf.setText(importTask.getName());
    }

  }

  @Override
  public void updateTask(ScheduledTask task) {
    if (task.getTask() instanceof ScheduleImportTask) {
      ScheduleImportTask importTask = (ScheduleImportTask) task.getTask();
      importTask.setFilename(filenameTf.getText());
      importTask.setName(StringUtils.trimToEmpty(taskNameTf.getText()));
    }

  }
}
