/**
 * 
 */
package de.stationadmin.gui.live;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 * 
 */
public class MP3StreamerOpenAction extends AbstractAction {
  private static final long serialVersionUID = 2960260368502865424L;
  private ClientContext ctx;

  public MP3StreamerOpenAction(ClientContext ctx) {
    this.putValue(Action.NAME, "MP3-Datei 'live' senden");
    this.ctx = ctx;
    try {
      this.setEnabled(ctx.getAdminClient().isLiveEnabled());
    } catch (Exception e) {
      this.setEnabled(false);
    }
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    MP3StreamerDlg dlg = new MP3StreamerDlg(ctx);
    dlg.setVisible(true);
  }

}
