package de.stationadmin.gui.playlist.profile;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ThreadedAction;

public class ProfileOptsUpdateAction extends ThreadedAction {
  private static final long serialVersionUID = -2523268914961067876L;
  private String profileId;
  private ClientContext ctx;

  public ProfileOptsUpdateAction(ClientContext ctx, String profileId) {
    super(ctx);
    this.ctx = ctx;
    this.profileId = profileId;
  }

  @Override
  protected String getStatus() {
    return ctx.getString("shuffleopts.update.status");
  }

  @Override
  protected void performAction() throws Exception {
    this.ctx.getAdminClient().getPlaylistService().updateProfileOpts(profileId);

  }

  @Override
  protected void showError(Exception e) {
    JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "shuffleopts.update.error"));
  }

}
