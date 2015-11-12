/**
 * 
 */
package de.stationadmin.gui.playlist.forecast;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ForecastDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -6309588214323645173L;
  private ClientContext ctx;

  public ForecastDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME,
        ctx.getTextProvider().getString("action.forecast"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    try {
      ForecastDlg dlg = new ForecastDlg(ctx);
      dlg.setVisible(true);
      
    } catch (Throwable t) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.getTextProvider()
          .createErrorInfo(t, "action.forecast.failed"));
    }
  }

}
