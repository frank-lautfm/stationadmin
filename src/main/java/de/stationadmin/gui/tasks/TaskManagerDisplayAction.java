/**
 * 
 */
package de.stationadmin.gui.tasks;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;

import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class TaskManagerDisplayAction extends AbstractAction {
  private static final long serialVersionUID = 6158486566155653898L;
  private ClientContext ctx;
  
  public TaskManagerDisplayAction(ClientContext ctx) {
    super(ctx.getTextProvider().getString("tasks.editor.action.display.name"));
    this.ctx = ctx;
  }


  @Override
  public void actionPerformed(ActionEvent e) {
    TaskManager.getInstance(ctx).setVisible(true);
  }

}
