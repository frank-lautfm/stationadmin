/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.track.TrackService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 *
 * @author Frank Korf
 *
 */
public class TracksDeleteAction extends AbstractAction {

  private TextProvider textProvider;
  private TrackService titleService;
  private int[] titleIds;

  public TracksDeleteAction(TextProvider textProvider, TrackService titleService) {
    super();
    this.textProvider = textProvider;
    this.titleService = titleService;
    this.putValue(Action.NAME, textProvider.getString("action.title.delete"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if(this.isEnabled()) {
      TracksDeleteDlg dlg = new TracksDeleteDlg(this.textProvider, this.titleService, this.titleIds);
      dlg.setVisible(true);
    }

  }

  /**
   * @return the titleIds
   */
  protected int[] getTitleIds() {
    return titleIds;
  }

  /**
   * @param titleIds the titleIds to set
   */
  protected void setTitleIds(int[] titleIds) {
    this.titleIds = titleIds;
    this.setEnabled(titleIds != null && titleIds.length > 0);
  }

}
