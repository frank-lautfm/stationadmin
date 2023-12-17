/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.JTable;
import javax.swing.TransferHandler;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.track.format.ExtendedTrackFormat.TrackDetailLevel;
import de.stationadmin.gui.ClientContext;

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
class PlaylistTableTransferHandler extends TransferHandler {
  private static final long serialVersionUID = -4695810613414149078L;
  private ExtendedTrackFormat exportFormat = new ExtendedTrackFormat(TrackDetailLevel.ENHANCED);
  private ClientContext ctx;
  private JXTable table;
  private PlaylistTableModel model;
  private boolean readOnly;

  public PlaylistTableTransferHandler(ClientContext ctx, JXTable table, boolean readOnly) {
    super();
    this.readOnly = readOnly;
    this.ctx = ctx;
    this.table = table;
    this.model = (PlaylistTableModel) table.getModel();
  }

  public boolean canImport(TransferSupport support) {
    if (model.getPlaylist() == null || readOnly) {
      return false;
    }
    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      return true;
    }
    if (support.isDataFlavorSupported(DataFlavor.stringFlavor)) {
      return true;
    }

    return false;
  }

  @SuppressWarnings("unchecked")
  public boolean importData(TransferSupport support) {
    // if we can't handle the import, say so
    if (!canImport(support)) {
      return false;
    }

    int row = 0;
    if (support.isDrop()) {
      // fetch the drop location
      JTable.DropLocation dl = (JTable.DropLocation) support.getDropLocation();
      row = dl.getRow();
    } else {
      row = table.getSelectedRow();
      if (row == -1) {
        row = table.getModel().getRowCount();
      }
    }

    boolean ok = false;
    try {
      for (DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
          List<File> fileList = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
          ok = this.handleFileList(row, fileList);
          break;
        }
        if (flavor.equals(DataFlavor.stringFlavor)) {
          String string = (String) support.getTransferable().getTransferData(DataFlavor.stringFlavor);
          ok = this.handleString(row, string);
          break;
        }
      }
    } catch (UnsupportedFlavorException e) {
      return false;
    } catch (IOException e) {
      return false;
    }

    Rectangle rect = table.getCellRect(row, 0, false);
    if (rect != null) {
      table.scrollRectToVisible(rect);
    }

    return ok;
  }

  private boolean handleString(int row, String string) {
    TrackImportHandler handler = new TrackImportHandler(ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager(), model.getPlaylist(), row);
    handler.add(string);
    
    try {
      handler.resolveTags();
      handler.resolveTracksLocal();
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
      JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "playlist.transfer.error.import"));

      return false;
    }

    return true;
    
  }

  private boolean handleFileList(int row, List<File> files) {
    TrackImportHandler handler = new TrackImportHandler(ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager(), model.getPlaylist(), row);
    for (File file : files) {
      try {
        handler.add(file);
      } catch (IOException e) {
        JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "playlist.transfer.error.file"));
        return false;
      }
    }

    try {
      handler.resolveTags();
      handler.resolveTracksLocal();
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
      JXErrorPane.showDialog(null, ctx.getTextProvider().createErrorInfo(e, "playlist.transfer.error.import"));

      return false;
    }

    return true;
  }

  /**
   * @see javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent,
   *      java.awt.datatransfer.Clipboard, int)
   */
  @Override
  public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
    String string = this.getSelectionAsString(action);
    if (string != null) {
      StringSelection stringSelection = new StringSelection(string);
      clip.setContents(stringSelection, stringSelection);

    }
  }

  private String getSelectionAsString(int action) {
    int[] rows = table.getSelectedRows();
    if (rows.length > 0) {
      List<Entry> entriesToRemove = new ArrayList<Entry>();
      StringBuffer buf = new StringBuffer();
      for (int i = 0; i < rows.length; i++) {
        int row = table.convertRowIndexToModel(rows[i]);
        BasicTrack track = model.getTitleAt(row);
        if (track != null) {
          buf.append(exportFormat.toString(track));
          buf.append('\n');

          if (action == TransferHandler.MOVE) {
            entriesToRemove.add(model.getEntryAt(row));
          }
        }
      }

      if (entriesToRemove.size() > 0) {
        model.getPlaylist().removeEntries(entriesToRemove);
      }

      return buf.toString();
    }

    return null;

  }

  /**
   * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
   */
  @Override
  protected Transferable createTransferable(JComponent c) {
    String string = this.getSelectionAsString(MOVE);
    if (string != null) {
      return new StringSelection(string);
    } else {
      return null;
    }
  }

  /**
   * @see javax.swing.TransferHandler#getSourceActions(javax.swing.JComponent)
   */
  @Override
  public int getSourceActions(JComponent c) {
    return readOnly ? COPY : MOVE;
  }

}
