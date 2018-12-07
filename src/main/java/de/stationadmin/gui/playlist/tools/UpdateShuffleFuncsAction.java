package de.stationadmin.gui.playlist.tools;

import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

public class UpdateShuffleFuncsAction extends ThreadedAction {
  private static final long serialVersionUID = -8350038457830542754L;
  private ClientContext ctx;
  private int numModified;

  public UpdateShuffleFuncsAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.updateshufflefunc"));
  }

  @Override
  protected boolean beforeExecution() {
    String msg = ctx.getString("action.updateshufflefunc.message");
    return JOptionPane.showConfirmDialog(AppUtils.getRootFrame(), msg, null, JOptionPane.YES_NO_OPTION) == JOptionPane.YES_OPTION;
  }

  @Override
  protected String getStatus() {
    return ctx.getString("action.updateshufflefunc.status");
  }

  @Override
  protected void onSuccess() {
    String msg = ctx.getString("action.updateshufflefunc.message.success", Integer.toString(numModified));
    JOptionPane.showMessageDialog(AppUtils.getRootFrame(), msg, null, JOptionPane.INFORMATION_MESSAGE);
  }

  @Override
  protected void performAction() throws Exception {
    numModified = ctx.getAdminClient().getPlaylistService().updateShuffleFunctions().size();
  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "action.updateshufflefunc.error"));
  }

}
