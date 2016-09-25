/**
 * 
 */
package de.stationadmin.gui.synchronization;

import javax.swing.Action;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank
 *
 */
public class SynchronizePlaylistsModifiedAction extends SynchronizeFullAction {
  private static final long serialVersionUID = 6329867489634501607L;
  private int[] playlistIds;

  /**
   * @param textProvider
   * @param adminClient
   */
  public SynchronizePlaylistsModifiedAction(TextProvider textProvider, StationAdminClient adminClient, int[] playlistIds) {
    super(textProvider, adminClient);
    this.putValue(Action.NAME, textProvider.getString("action.synchronize.playlists.modified"));
    this.playlistIds = playlistIds;
  }

  public SynchronizePlaylistsModifiedAction(TextProvider textProvider, StationAdminClient adminClient) {
    this(textProvider, adminClient, null);
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.gui.SynchronizeAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    if (this.playlistIds == null) {
      this.playlistIds = adminClient.getPlaylistService().getPlaylistModificationDetector().detectModifiedPlaylists();
    }
    if (playlistIds != null && playlistIds.length > 0) {
      this.getAdminClient().getPlaylistService().synchronize(playlistIds);
      this.getAdminClient().getPlaylistService().getPlaylistModificationDetector().markClean();
    }
  }

}
