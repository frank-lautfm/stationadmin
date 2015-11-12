/**
 * 
 */
package de.stationadmin.gui;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import de.stationadmin.base.playlist.Playlist;

/**
 * Performs a logout and closes the application
 * 
 * @author korf
 */
public class ExitAction extends AbstractAction {
  private static final long serialVersionUID = -8665099289417946051L;
  private ClientContext ctx;

  public ExitAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.exit"));
  }
  
  private boolean checkPlaylists() {
    boolean hasUnsavedPlaylists = false;
    for (Playlist playlist : this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getAllPlaylists()) {
      if (playlist.isModified()) {
        hasUnsavedPlaylists = true;
      }
    }
    if (hasUnsavedPlaylists) {
      TextProvider textProvider = this.ctx.getTextProvider();
      int response = JOptionPane.showConfirmDialog(ctx.getRootWindow(), textProvider.getString("action.exit.confirm.modifiedplaylists"), textProvider.getString("action.exit.confirm.title"), JOptionPane.YES_NO_OPTION);
      return (response == JOptionPane.YES_OPTION);
    }
    
    return true;
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    boolean exitOk = this.checkPlaylists();

    if (exitOk) {
      this.ctx.getAdminClient().close();
      System.exit(0);
    }
  }

}
