/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 *
 * @author Frank Korf
 *
 */
public class MultiPlaylistShuffleDisplayAction extends AbstractAction {

  private ClientContext ctx;
  
  public MultiPlaylistShuffleDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.multishuffle"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    MultiPlaylistShuffleDlg dlg = new MultiPlaylistShuffleDlg(ctx);
    dlg.setVisible(true);
  }

}
