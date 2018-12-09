/**
 * 
 */
package de.stationadmin.gui.help;

import java.awt.event.ActionEvent;
import java.net.URI;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class HelpAction extends AbstractAction {
  private static final long serialVersionUID = -950619028306548082L;
  private ClientContext ctx;

  public HelpAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.help"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    if (AppUtils.getDesktop() != null) {
      try {
        AppUtils.getDesktop().browse(new URI("http://stationadmin.sourceforge.net/userguide/"));
        return;
      } catch (Exception e) {

      }
    }

    // fallback
    HelpWindow browser = new HelpWindow(ctx);
    browser.setVisible(true);
  }

}
