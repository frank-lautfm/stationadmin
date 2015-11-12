/**
 * 
 */
package de.stationadmin.gui;

import de.stationadmin.base.StationAdminClient;

/**
 * @author Frank
 *
 */
public class PartialSynchronizeAction extends SynchronizeAction {
  private static final long serialVersionUID = 6329867489634501607L;
  private int[] playlistIds;

  /**
   * @param textProvider
   * @param adminClient
   */
  public PartialSynchronizeAction(TextProvider textProvider, StationAdminClient adminClient, int[] playlistIds) {
    super(textProvider, adminClient);
    this.playlistIds = playlistIds;
  }

  /* (non-Javadoc)
   * @see de.emjoy.stationadmin.gui.SynchronizeAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    if (playlistIds.length > 0) {
      this.getAdminClient().getPlaylistService().synchronize(playlistIds);
      this.getAdminClient().getPlaylistService().getPlaylistModificationDetector().markClean();
    }
  }

}
