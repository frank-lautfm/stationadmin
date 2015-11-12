/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class SubscriptionManagerDisplayAction extends AbstractAction {
  private static final long serialVersionUID = 2831696561059292353L;
  private ClientContext ctx;

  public SubscriptionManagerDisplayAction(ClientContext ctx) {
    this.putValue(Action.NAME, ctx.getTextProvider().getString("subscriptionmanager.action.open"));
    this.ctx = ctx;
  }
  
  /* (non-Javadoc)
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    SubscriptionManagerDlg dlg = new SubscriptionManagerDlg(ctx);
    dlg.setVisible(true);

  }

}
