/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.event.ActionEvent;
import java.io.IOException;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;

/**
 * Opens the Upload dialog
 * @author Frank Korf
 * 
 */
public class UploadAction extends AbstractAction {
  private static final long serialVersionUID = -7896999632352707953L;
  private ClientContext ctx;

  public UploadAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.setEnabled(false);
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.upload.name"));
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    try {
      if (ctx.getAdminClient().isUploadAvailable()) {
        this.ctx.getUploadWindowManager().getUploadWindow().setVisible(true);
      } else {
        JOptionPane.showMessageDialog(ctx.getRootWindow(), ctx.getTextProvider().getString("action.upload.msg.unavailable"), null, JOptionPane.WARNING_MESSAGE);
      }
    } catch (IOException e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.getTextProvider().createErrorInfo(e, "action.upload.msg.checkfailed"));
    }
  }

}
