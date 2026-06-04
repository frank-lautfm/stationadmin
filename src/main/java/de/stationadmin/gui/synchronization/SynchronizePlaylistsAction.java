/**
 * 
 */
package de.stationadmin.gui.synchronization;

import javax.swing.Action;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.TextProvider;

/**
 * @author korf
 *
 */
public class SynchronizePlaylistsAction extends AbstractSynchronizeAction {
  private static final long serialVersionUID = 3626156092420153333L;

  /**
   * @param textProvider
   * @param adminClient
   */
  public SynchronizePlaylistsAction(TextProvider textProvider, StationAdminClient adminClient) {
    super(textProvider, adminClient);
    this.putValue(Action.NAME, textProvider.getString("action.synchronize.playlists"));
  }

  @Override
  protected void performAction() throws Exception {
    adminClient.getPlaylistService().synchronize(true);
  }

}
