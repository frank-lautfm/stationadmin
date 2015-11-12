/**
 * 
 */
package de.stationadmin.gui.loganalyzer.dailysummary;

import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.loganalyzer.LogAccessAction;

/**
 * @author korf
 * 
 */
public class DailySummariesOpenAction extends LogAccessAction {
  private static final long serialVersionUID = 9086410550326478691L;

  public DailySummariesOpenAction(ClientContext ctx) {
    super(ctx);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.dailysummaries.name"));

  }

  @Override
  public void performAction() {
    try {
      DailySummariesDlg win = new DailySummariesDlg(ctx);
      win.setVisible(true);
    } catch (Throwable t) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(t, "action.dailysummaries.msg.error"));
    }

  }

}
