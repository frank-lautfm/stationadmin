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
 * Opens a dialog to copy titles to a playlist
 * 
 * @author Frank Korf
 */
public class CopyTracksAction extends AbstractAction {
  private static final long serialVersionUID = 8980357896169561556L;
  private ClientContext ctx;
  private List<Title> titles;

  public CopyTracksAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.copyTitles"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (titles != null && this.titles.size() > 0) {
      CopyTracksDlg dlg = new CopyTracksDlg(ctx, titles);
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
