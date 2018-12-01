package de.stationadmin.gui.playlist.tools;

import java.awt.event.ActionEvent;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

public class UpdateShuffleFuncsAction extends AbstractAction {
  private static final long serialVersionUID = -8350038457830542754L;
  private ClientContext ctx;

  public UpdateShuffleFuncsAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.updateshufflefunc"));
  }

  @Override
  public void actionPerformed(ActionEvent evt) {

    String msg = ctx.getString("action.updateshufflefunc.message");
    if (JOptionPane.showConfirmDialog(AppUtils.getRootFrame(), msg, null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION) {
      try {
        ctx.getAdminClient().getPlaylistService().updateShuffleFunctions();
      } catch (Exception e) {
        JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "action.updateshufflefunc.error"));
      }
    }

  }

}
