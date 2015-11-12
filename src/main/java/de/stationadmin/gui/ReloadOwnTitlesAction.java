/**
 * 
 */
package de.stationadmin.gui;

import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.track.TrackService;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * Retrieves the latest data (playlists etc) from the server
 * 
 * @author korf
 */
public class ReloadOwnTitlesAction extends ThreadedAction {
  private static final long serialVersionUID = -3225640894960403848L;
  private TextProvider textProvider;
  private TrackService titleService;

  public ReloadOwnTitlesAction(TextProvider textProvider, TrackService titleService) {
    super();
    this.textProvider = textProvider;
    this.titleService = titleService;
    this.putValue(Action.NAME, this.textProvider.getString("action.reloadOwnTitles"));

  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
   */
  @Override
  protected String getStatus() {
    return this.textProvider.getString("status.updateOwnTitles");
  }

  /**
   * @see de.stationadmin.gui.util.ThreadedAction#performAction()
   */
  @Override
  protected void performAction() throws Exception {
    this.titleService.reloadOwnTitles();

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(null, this.textProvider.createErrorInfo(e, "action.synchronize.error"));
  }

}
