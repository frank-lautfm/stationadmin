/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.track.Title;
import de.stationadmin.gui.ClientContext;

/**
 * Opens a dialog to distribute titles to selected playlists
 * 
 * @author Frank Korf
 */
public class DistributeTracksAction extends AbstractAction {
  private static final long serialVersionUID = 6360083656552138387L;
  private ClientContext ctx;
  private List<Title> titles;

  public DistributeTracksAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.distributeTitles"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (titles != null && this.titles.size() > 0) {
      DistributeTracksDlg dlg = new DistributeTracksDlg(ctx, titles);
      dlg.setVisible(true);
    }

  }

  /**
   * @return the titles
   */
  public List<Title> getTitles() {
    return titles;
  }

  /**
   * @param titles
   *          the titles to set
   */
  public void setTitles(List<Title> titles) {
    this.titles = titles;
    this.setEnabled(this.titles != null && this.titles.size() > 0);
  }

}
