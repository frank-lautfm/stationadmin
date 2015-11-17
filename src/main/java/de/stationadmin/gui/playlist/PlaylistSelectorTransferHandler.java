/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;

import javax.swing.JList;
import javax.swing.ListModel;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXList;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;

/**
 * Transfer handler for {@link PlaylistViewer}.
 * <p>
 * <ul>
 * <li>supports cut, copy and paste
 * <li>supports drag (copy of entry)
 * <li>supports drop of files and a string holding title representations
 * </ul>
 * 
 * @author Frank Korf
 */
class PlaylistSelectorTransferHandler extends TransferHandler {
  private static final long serialVersionUID = -4695810613414149078L;
  private TextProvider textProvider;
  private StationAdminClient client;
  private ClientContext ctx;
  private ListModel model;

  public PlaylistSelectorTransferHandler(ClientContext ctx, JXList list) {
    super();
    this.textProvider = ctx.getTextProvider();
    this.client = ctx.getAdminClient();
    this.ctx = ctx;
    this.model = list.getModel();
  }

  public boolean canImport(TransferSupport support) {
    if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      return true;
    }

    return false;
  }

  public boolean importData(TransferSupport support) {
    // if we can't handle the import, say so
    if (!canImport(support)) {
      return false;
    }

    int index = 0;
    if (support.isDrop()) {
      // fetch the drop location
      JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
      index = dl.getIndex();
    }

    boolean ok = false;
    try {
      for (DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.equals(DataFlavor.stringFlavor)) {
          String string = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
          ok = this.handleString(index, string);
          break;
        }
      }
    } catch (UnsupportedFlavorException e) {
      return false;
    } catch (IOException e) {
      return false;
    }

    return ok;
  }

  private boolean handleString(int row, String string) {
    if (row >= 0 && row < this.model.getSize()) {
      Playlist playlist = (Playlist) this.model.getElementAt(row);
      TrackImportHandler handler = new TrackImportHandler(client.getTrackService(), client.getTagManager(), playlist, playlist.getEntries().size());
      handler.add(string);

      try {
        handler.resolveTags();
        handler.resolveTitlesLocal();
        if (handler.isEverythingResolved()) {
          // if everything can be resolved without server requests handle this
          // silently
          handler.addTracksToPlaylist();
        } else {
          TrackImportDlg dlg = new TrackImportDlg(ctx, handler);
          dlg.setVisible(true);
          dlg.startTitleResolve();
        }
      } catch (Exception e) {
        JXErrorPane.showDialog(null, this.textProvider.createErrorInfo(e, "playlist.transfer.error.import"));

        return false;
      }

      return true;

    } else {
      return false;
    }

  }

}
