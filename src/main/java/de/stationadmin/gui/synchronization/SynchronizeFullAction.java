/**
 * 
 */
package de.stationadmin.gui.synchronization;

import javax.swing.Action;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.gui.TextProvider;

/**
 * Retrieves the latest data (playlists etc) from the server
 * 
 * @author korf
 */
public class SynchronizeFullAction extends AbstractSynchronizeAction {
  private static final long serialVersionUID = -3225640894960403848L;
  
  public SynchronizeFullAction(TextProvider textProvider, StationAdminClient adminClient) {
    super(textProvider, adminClient);
    this.putValue(Action.NAME, textProvider.getString("action.synchronize.all"));
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    adminClient.synchronize();

  }

  @Override
  protected boolean beforeExecution() {
    return this.checkPlaylists();
  }

}
