/**
 * 
 */
package de.stationadmin.gui.playlist.archive;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;

public class ArchiveDialogOpenAction extends AbstractAction implements PropertyChangeListener {
  private static final long serialVersionUID = -6462922216248612645L;
  private ValueModel playlistHolder;
  private ClientContext ctx;

  public ArchiveDialogOpenAction(ClientContext ctx, ValueModel playlistHolder) {
    this.putValue(Action.SMALL_ICON, AppUtils.getIcon("archive.png"));
    this.putValue(Action.SHORT_DESCRIPTION, ctx.getTextProvider().getString("action.playlist.archive.tooltip"));
    this.playlistHolder = playlistHolder;
    this.ctx = ctx;
    setEnabled(false);
    playlistHolder.addValueChangeListener(this);
  }

  @Override
  public void actionPerformed(ActionEvent e) {
    Playlist playlist = (Playlist) this.playlistHolder.getValue();
    if (playlist != null) {
      ArchiveDialog dlg = new ArchiveDialog(ctx.getTextProvider(), ctx.getAdminClient().getPlaylistService(), playlist);
      dlg.setModal(true);
      dlg.setVisible(true);
    }

  }

  @Override
  public void propertyChange(PropertyChangeEvent evt) {
    setEnabled(evt.getNewValue() != null);
  }

}