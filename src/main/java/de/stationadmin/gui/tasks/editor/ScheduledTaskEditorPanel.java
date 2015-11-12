/**
 * 
 */
package de.stationadmin.gui.tasks.editor;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.HashMap;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.JPanel;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistGenerateTask;
import de.stationadmin.base.playlist.PlaylistShuffleTask;
import de.stationadmin.base.schedule.PlaylistExchangeTask;
import de.stationadmin.base.schedule.ScheduleImportTask;
import de.stationadmin.base.tasks.ScheduledTask;
import de.stationadmin.base.tasks.Task;
import de.stationadmin.base.tasks.TaskExecutionService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.tasks.ScheduledTaskEditorComponent;
import de.stationadmin.gui.tasks.status.TaskExecutionResultPanel;
import de.stationadmin.gui.tasks.trigger.TriggerPanel;
import de.stationadmin.gui.util.AppUtils;

/**
 * @author korf
 * 
 */
public class ScheduledTaskEditorPanel extends JPanel {
  private static final long serialVersionUID = -7919623611754227290L;

  private TextProvider textProvider;
  private TriggerPanel triggerPanel;
  private ValueModel selection;

  private Map<Class<? extends Task>, JPanel> taskEditorPanels = new HashMap<Class<? extends Task>, JPanel>();
  private JPanel currentTaskEditorPanel;

  private JPanel triggerContainer, editorContainer, lastExecutionContainer;

  private SaveAction saveAction;

  public ScheduledTaskEditorPanel(ClientContext ctx, ValueModel selection) {
    this.textProvider = ctx.getTextProvider();
    this.selection = selection;
    this.init(ctx);
    setVisible(false);
  }

  private void init(ClientContext ctx) {
    this.taskEditorPanels.put(PlaylistShuffleTask.class, new PlaylistShuffleTaskPanel(
        ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(), ctx.getTextProvider()));
    this.taskEditorPanels.put(PlaylistGenerateTask.class, new PlaylistGenerateTaskPanel(ctx.getAdminClient().getPlaylistService()
        .getPlaylistRegistry(), ctx.getTextProvider()));
    this.taskEditorPanels.put(PlaylistExchangeTask.class, new PlaylistExchangeTaskPanel(ctx.getAdminClient().getPlaylistService()
        .getPlaylistRegistry(), ctx.getTextProvider()));
    this.taskEditorPanels.put(ScheduleImportTask.class, new ScheduleImportTaskPanel(ctx.getTextProvider()));

    this.saveAction = new SaveAction(ctx.getAdminClient().getTaskExecutionService());

    this.editorContainer = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.editorContainer.setBorder(BorderFactory.createTitledBorder(textProvider.getString("tasks.editor.section.task")));
    this.triggerContainer = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    this.triggerContainer.setBorder(BorderFactory.createTitledBorder(textProvider.getString("tasks.editor.section.trigger")));
    this.lastExecutionContainer = new JPanel(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,60dlu:grow,5dlu"));
    this.lastExecutionContainer.setBorder(BorderFactory.createTitledBorder(textProvider.getString("tasks.editor.section.status")));

    FormLayout layout = new FormLayout("pref:grow,5dlu,pref:grow", "pref,5dlu,pref:grow");
    this.setLayout(layout);
    CellConstraints cc = new CellConstraints();
    this.add(this.editorContainer, new CellConstraints(1, 1, CellConstraints.FILL, CellConstraints.FILL));
    this.add(this.triggerContainer, new CellConstraints(3, 1, CellConstraints.FILL, CellConstraints.FILL));
    this.add(this.lastExecutionContainer, new CellConstraints(1, 3, 3, 1, CellConstraints.FILL, CellConstraints.FILL));
    layout.setColumnGroups(new int[][] { { 1, 3 } });

    this.triggerPanel = new TriggerPanel(this.textProvider);
    this.triggerContainer.add(this.triggerPanel, cc.xy(2, 2, CellConstraints.LEFT, CellConstraints.TOP));

    TaskExecutionResultPanel resultPanel = new TaskExecutionResultPanel(textProvider, selection);
    this.lastExecutionContainer.add(resultPanel, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ScheduledTask task = (ScheduledTask) evt.getNewValue();

        if (task != null) {
          JPanel taskEditorPanel = task.getTask() != null ? taskEditorPanels.get(task.getTask().getClass()) : null;
          if (taskEditorPanel != null) {
            ((ScheduledTaskEditorComponent) taskEditorPanel).updateView(task);
          }
          if (taskEditorPanel != currentTaskEditorPanel) {
            if (currentTaskEditorPanel != null) {
              editorContainer.remove(currentTaskEditorPanel);
            }
            if (taskEditorPanel != null) {
              editorContainer.add(taskEditorPanel, new CellConstraints(2, 2, CellConstraints.LEFT, CellConstraints.TOP));
            }
            currentTaskEditorPanel = taskEditorPanel;
          }

          triggerPanel.updateView(task);

          validate();
          repaint();
        }
        setVisible(evt.getNewValue() != null);
        saveAction.setEnabled(evt.getNewValue() != null);
      }
    });
  }

  private void updateTask(ScheduledTask task) {
    ((ScheduledTaskEditorComponent) this.triggerPanel).updateTask(task);
    if (this.currentTaskEditorPanel != null) {
      ((ScheduledTaskEditorComponent) this.currentTaskEditorPanel).updateTask(task);
    }
  }

  private class SaveAction extends AbstractAction {
    private static final long serialVersionUID = -245909195010318687L;
    private TaskExecutionService taskService;

    SaveAction(TaskExecutionService taskService) {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("save.png"));
      this.taskService = taskService;
      setEnabled(false);
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      ScheduledTask task = (ScheduledTask) selection.getValue();
      if (task != null) {
        updateTask(task);
      }
      try {
        this.taskService.configureScheduledTask(task);
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), textProvider.createErrorInfo(e, "tasks.editor.action.save.msg.failed"));
      }

    }

  }

  public SaveAction getSaveAction() {
    return saveAction;
  }
}
