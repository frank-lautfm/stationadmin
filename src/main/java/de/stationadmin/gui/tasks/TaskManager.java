/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPopupMenu;
import javax.swing.JToolBar;

import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.PlaylistGenerateTask;
import de.stationadmin.base.playlist.PlaylistShuffleTask;
import de.stationadmin.base.schedule.PlaylistExchangeTask;
import de.stationadmin.base.schedule.ScheduleImportTask;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminFrame;
import de.stationadmin.gui.tasks.editor.ScheduledTaskEditorPanel;

/**
 * @author korf
 * 
 */
public class TaskManager extends StationAdminFrame {
  private static final long serialVersionUID = 4537837223052671400L;
  private ClientContext ctx;

  private static TaskManager instance;

  public static TaskManager getInstance(ClientContext ctx) {
    if (instance == null) {
      instance = new TaskManager(ctx);
    }
    return instance;
  }

  TaskManager(ClientContext ctx) throws HeadlessException {
    super(ctx, "taskManager");
    this.ctx = ctx;
    this.init();
    this.setDefaultCloseOperation(JFrame.HIDE_ON_CLOSE);
  }

  private void init() {
    FormLayout layout = new FormLayout("5dlu,pref:grow,5dlu", "5dlu,100dlu,5dlu,pref,10dlu,pref,5dlu");
    this.getContentPane().setLayout(layout);
    
    ScheduledTaskTablePanel taskViewer = new ScheduledTaskTablePanel(ctx.getTextProvider(), ctx.getAdminClient().getTaskExecutionService());
    this.getContentPane().add(taskViewer, new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    ScheduledTaskEditorPanel editor = new ScheduledTaskEditorPanel(ctx, taskViewer.getTaskHolder());
    this.getContentPane().add(editor, new CellConstraints(2, 6, CellConstraints.FILL, CellConstraints.FILL));

    
    final JButton newBtn = new JButton(ctx.getIcon("filenew.png"));
    final JPopupMenu newTaskPopup = new JPopupMenu();
    newTaskPopup.add(new AddTaskAction(ctx.getTextProvider(), PlaylistShuffleTask.class, taskViewer.getTaskHolder()));
    newTaskPopup.add(new AddTaskAction(ctx.getTextProvider(), PlaylistGenerateTask.class, taskViewer.getTaskHolder()));
    newTaskPopup.add(new AddTaskAction(ctx.getTextProvider(), PlaylistExchangeTask.class, taskViewer.getTaskHolder()));
    newTaskPopup.add(new AddTaskAction(ctx.getTextProvider(), ScheduleImportTask.class, taskViewer.getTaskHolder()));
    newBtn.addActionListener(new ActionListener() {
      
      @Override
      public void actionPerformed(ActionEvent e) {
        newTaskPopup.show(newBtn, 0, newBtn.getHeight());
      }
    });
    
    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    toolbar.add(newBtn);
    toolbar.addSeparator();
    toolbar.add(editor.getSaveAction());
    toolbar.add(new DeleteTaskAction(ctx.getTextProvider(), ctx.getAdminClient().getTaskExecutionService(), taskViewer.getTaskHolder()));
    this.getContentPane().add(toolbar,new CellConstraints(2, 4));

    

  }

}
