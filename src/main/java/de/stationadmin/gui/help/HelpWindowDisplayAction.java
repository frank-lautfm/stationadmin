/**
 * 
 */
package de.stationadmin.gui.help;

import java.awt.event.ActionEvent;
import java.io.File;
import java.net.URL;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

/**
 * 
 * @author Frank Korf
 * 
 */
public class HelpWindowDisplayAction extends AbstractAction {
  private static final long serialVersionUID = -950619028306548082L;
  private ClientContext ctx;

  public HelpWindowDisplayAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.help"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    if (AppUtils.getDesktop() != null && AppUtils.getDesktop().isSupported(java.awt.Desktop.Action.OPEN)) {
      URL url = this.getClass().getClassLoader().getResource("index.html");
      if (url != null) {
        try {
          File file = new File(url.toURI());
          AppUtils.getDesktop().open(file);
          return;
        } catch (Exception e) {
        }
      }
    }
    // fallback
    HelpWindow browser = new HelpWindow(ctx);
    browser.setVisible(true);
  }

}
