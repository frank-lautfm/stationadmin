/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * Opens a {@link TempPlaylistWindow}
 *
 * @author Frank Korf
 *
 */
public class TempPlaylistDisplayAction extends AbstractAction {

  private ClientContext ctx;
  
  public TempPlaylistDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.tempplaylist"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    TempPlaylistWindow win = new TempPlaylistWindow(ctx);
    win.setVisible(true);
  }

}
