/**
 * 
 */
package de.stationadmin.gui.loganalyzer.monthly;

import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.loganalyzer.LogAccessAction;

/**
 * @author korf
 * 
 */
public class MonthlySummariesOpenAction extends LogAccessAction {
  private static final long serialVersionUID = 9086410550326478691L;

  public MonthlySummariesOpenAction(ClientContext ctx) {
    super(ctx);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.monthlysummaries.name"));

  }

  @Override
  public void performAction() {
    try {
      MonthlySummaryDlg win = new MonthlySummaryDlg(ctx, ctx.getAdminClient().getLogAnalyzerService().getMonthlySummary());
      win.setVisible(true);
    } catch (Throwable t) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(t, "action.dailysummaries.msg.error"));
    }

  }

}
