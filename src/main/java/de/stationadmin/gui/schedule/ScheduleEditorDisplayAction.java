/**
 * 
 */
package de.stationadmin.gui.schedule;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.Role;
import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class ScheduleEditorDisplayAction extends AbstractAction {
  private static final long serialVersionUID = 2052538593061399256L;
  private ClientContext ctx;

  /**
   * @param ctx
   */
  public ScheduleEditorDisplayAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.editschedule"));
    this.setEnabled(ctx.getAdminClient().getSessionCtx().getRole() != Role.DJ);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    try {
      ScheduleEditor editor = new ScheduleEditor(ctx);
      editor.setVisible(true);
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), this.ctx.createErrorInfo(e, "action.error.msg"));
    }
  }

}
