/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JDialog;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.SwingTools;

/**
 * Opens the dialog for creating a new playlist
 * 
 * @author korf
 */
public class PlaylistJsonAction extends AbstractAction {
  private static final long serialVersionUID = 4856891819065912632L;
  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistJsonAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.json"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ONLINE);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist pl = (Playlist) playlistHolder.getValue();

    try {
      String json = ctx.getAdminClient().getPlaylistService().getPlaylistJson(pl.getId());
      
      JDialog dlg = new JDialog();
      dlg.setTitle(pl.getName() + " - Rohdaten vom Server");
      JTextArea ta = new JTextArea(100, 100);
      ta.setEditable(false);
      ta.setText(json);
      ta.setCaretPosition(0);
      dlg.getContentPane().setLayout(new BorderLayout());
      dlg.getContentPane().add(new JScrollPane(ta));
      dlg.setSize(600, 700);
      SwingTools.centerOnScreen(dlg);
      dlg.setVisible(true);
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.error.generic"));

    }

  }

}
