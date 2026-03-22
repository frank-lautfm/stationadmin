/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Map;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.codehaus.jackson.map.ObjectMapper;
import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;

/**
 * Copies the shuffle_opts of the selected playlist as a JSON string to the clipboard.
 *
 * @author korf
 */
public class PlaylistShuffleOptsCopyAction extends AbstractAction {
  private static final long serialVersionUID = 3819204567123456789L;
  private ClientContext ctx;
  private ValueModel playlistHolder;

  /**
   * @param ctx
   * @param playlistHolder
   */
  public PlaylistShuffleOptsCopyAction(ClientContext ctx, ValueModel playlistHolder) {
    super();
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playlist.shuffleopts.copy"));
    this.ctx = ctx;
    this.playlistHolder = playlistHolder;
    this.setEnabled(false);
    playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        setEnabled(evt.getNewValue() instanceof Playlist
            && ((Playlist) evt.getNewValue()).getType() == PlaylistType.ONLINE);
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent evt) {
    Playlist pl = (Playlist) playlistHolder.getValue();

    try {
      Map<String, Object> shuffleOpts = pl.getShuffleOpts();
      String json;
      if (shuffleOpts == null || shuffleOpts.isEmpty()) {
        json = "{}";
      } else {
        ObjectMapper mapper = new ObjectMapper();
        json = mapper.writerWithDefaultPrettyPrinter().writeValueAsString(shuffleOpts);
      }
      StringSelection str = new StringSelection(json);
      java.awt.Toolkit.getDefaultToolkit().getSystemClipboard().setContents(str, str);
      JOptionPane.showMessageDialog(ctx.getRootWindow(),
      		ctx.getTextProvider().getString("action.playlist.shuffleopts.copy.success"),
          ctx.getTextProvider().getString("action.playlist.shuffleopts.copy"), JOptionPane.INFORMATION_MESSAGE);
    } catch (Exception e) {
      JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.error.generic"));
    }
  }

}
