/**
 * 
 */
package de.stationadmin.gui.logs;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class LogViewerDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -8448781028467753977L;
  private ClientContext ctx;

  /**
   * @param ctx
   */
  public LogViewerDisplayAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("logviewer.action.name"));
  }

  @Override
  public void actionPerformed(ActionEvent e) {

    LogViewer viewer = new LogViewer(ctx);
    viewer.setVisible(true);

  }

}
