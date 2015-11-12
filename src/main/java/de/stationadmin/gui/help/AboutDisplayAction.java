/**
 * 
 */
package de.stationadmin.gui.help;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class AboutDisplayAction extends AbstractAction {
  private ClientContext ctx;
  
  public AboutDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.about"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    AboutDlg dlg = new AboutDlg(ctx);
    dlg.setVisible(true);

  }

}
