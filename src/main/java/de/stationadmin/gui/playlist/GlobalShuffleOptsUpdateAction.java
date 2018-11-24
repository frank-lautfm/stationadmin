package de.stationadmin.gui.playlist;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

public class GlobalShuffleOptsUpdateAction extends ThreadedAction {
  private static final long serialVersionUID = -6633153627539525093L;
  private ClientContext ctx;

  public GlobalShuffleOptsUpdateAction(ClientContext ctx) {
    this.ctx = ctx;
  }

  @Override
  protected String getStatus() {
    return ctx.getString("shuffleopts.update.status");
  }

  @Override
  protected void performAction() throws Exception {
    this.ctx.getAdminClient().getPlaylistService().updateGlobalShuffleOpts(this.ctx.getAdminClient().getSettings());

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "shuffleopts.update.error"));

  }

}
