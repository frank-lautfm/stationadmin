/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.gui.ClientContext;

/**
 * Action for opening the {@link PlaylistTitleSearchDlg}
 *
 * @author Frank Korf
 */
public class PlaylistTitleSearchOpenAction extends AbstractAction {
  private static final long serialVersionUID = 8481902647321186951L;
  private ClientContext ctx;

  public PlaylistTitleSearchOpenAction(ClientContext ctx) {
    super();
    this.putValue(Action.NAME, ctx.getString("action.playlisttitlesearch"));
    this.ctx = ctx;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    PlaylistTitleSearchDlg viewer = new PlaylistTitleSearchDlg(ctx);
    viewer.setVisible(true);
  }

}
