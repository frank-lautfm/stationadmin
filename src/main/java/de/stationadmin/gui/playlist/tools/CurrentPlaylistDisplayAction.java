/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;

/**
 * Opens a {@link TempPlaylistWindow}
 *
 * @author Frank Korf
 *
 */
public class CurrentPlaylistDisplayAction extends AbstractAction {
  private static final long serialVersionUID = 3817995845010951965L;
  private ClientContext ctx;

  public CurrentPlaylistDisplayAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.currentplaylist"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    try {
      Playlist playlist = ctx.getAdminClient().getPlaylistService().getCurrentPlaylist();
      CurrentPlaylistWindow win = new CurrentPlaylistWindow(ctx, playlist);
      win.setVisible(true);
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.currentplaylist.error"));

    }
  }

}
