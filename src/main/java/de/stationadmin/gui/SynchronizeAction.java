/**
 * 
 */
package de.stationadmin.gui;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.Status;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * Retrieves the latest data (playlists etc) from the server
 * 
 * @author korf
 */
public class SynchronizeAction extends ThreadedAction {
  private static final long serialVersionUID = -3225640894960403848L;
  private TextProvider textProvider;
  private StationAdminClient adminClient;
  
  public SynchronizeAction(TextProvider textProvider, StationAdminClient adminClient) {
    super();
    this.textProvider = textProvider;
    this.adminClient = adminClient;
    this.putValue(Action.NAME, textProvider.getString("action.synchronize"));
  }

  private boolean checkPlaylists() {
    boolean hasUnsavedPlaylists = false;
    for (Playlist playlist : this.adminClient.getPlaylistService().getPlaylistRegistry().getAllPlaylists()) {
      if (playlist.isModified()) {
        hasUnsavedPlaylists = true;
      }
    }
    if (hasUnsavedPlaylists) {
      int response = JOptionPane.showConfirmDialog(AppUtils.getRootFrame(), textProvider.getString("action.synchronize.confirm.modifiedplaylists"), textProvider.getString("action.synchronize.confirm.title"), JOptionPane.YES_NO_OPTION);
      return (response == JOptionPane.YES_OPTION);
    }
    
    return true;
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
   */
  @Override
  protected String getStatus() {
    Status status = this.adminClient.getStatus();
    if (status != null) {
      return textProvider.getString("status." + status.getKey(), status.getParameters());
    } else {
      return textProvider.getString("action.synchronize.msg");
    }
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    adminClient.synchronize();

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "action.synchronize.error"));
  }

  /**
   * @return the textProvider
   */
  public TextProvider getTextProvider() {
    return textProvider;
  }

  /**
   * @return the adminClient
   */
  public StationAdminClient getAdminClient() {
    return adminClient;
  }

  @Override
  protected boolean beforeExecution() {
    return this.checkPlaylists();
  }

}
