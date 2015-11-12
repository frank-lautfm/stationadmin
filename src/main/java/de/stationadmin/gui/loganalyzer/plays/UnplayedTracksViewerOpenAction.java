/**
 * 
 */
package de.stationadmin.gui.loganalyzer.plays;

import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.loganalyzer.LogAccessAction;

/**
 * @author korf
 * 
 */
public class UnplayedTracksViewerOpenAction extends LogAccessAction {
  private static final long serialVersionUID = 9086410550326478691L;

  public UnplayedTracksViewerOpenAction(ClientContext ctx) {
    super(ctx);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.unplayedtitleviewer.name"));

  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void performAction() {
    try {
      UnplayedTracksViewer win = new UnplayedTracksViewer(ctx);
      win.setVisible(true);
    } catch (Throwable t) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(t, "action.unplayedtitleviewer.msg.error"));
    }
  }

}
