/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.Toolkit;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.TransferHandler;

import de.stationadmin.base.track.upload.UploadManager;
import de.stationadmin.gui.TextProvider;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadTransferHandler extends TransferHandler {
  private static final long serialVersionUID = -1892692578264823336L;
  private UploadManager uploadManager;
  private TextProvider textProvider;

  public UploadTransferHandler(TextProvider textProvider, UploadManager uploadManager) {
    super();
    this.textProvider = textProvider;
    this.uploadManager = uploadManager;
  }

  @Override
  public boolean canImport(TransferSupport support) {
    if (support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) {
      return true;
    }
    return false;
  }

  @Override
  @SuppressWarnings("unchecked")
  public boolean importData(TransferSupport support) {
    // if we can't handle the import, say so
    if (!canImport(support)) {
      return false;
    }

    boolean ok = false;
    try {
      for (DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.equals(DataFlavor.javaFileListFlavor)) {
          ok = true;
          List<File> fileList = (List<File>) support.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
          fileList = DupeTitleDlg.removeDupes(textProvider, uploadManager.getTrackService().getTrackRegistry(), fileList);
          for (File file : fileList) {
            if(!this.uploadManager.add(file)) {
              Toolkit.getDefaultToolkit().beep();
            }
          }
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

}
