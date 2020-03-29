package de.stationadmin.gui.playlist.config.shuffle;

import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultListModel;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.TransferHandler;

import org.apache.commons.lang.StringUtils;
import org.codehaus.jackson.map.ObjectMapper;
import org.jdesktop.swingx.JXList;

public class TagPatternTransferHandler extends TransferHandler {
  private static final long serialVersionUID = 821151148548057552L;
  private ObjectMapper mapper = new ObjectMapper();
  private DefaultListModel<String> model;
  private JXList list;
  private boolean readOnly;

  @SuppressWarnings("unchecked")
  public TagPatternTransferHandler(JXList list, boolean readOnly) {
    this.list = list;
    this.model = (DefaultListModel<String>) list.getModel();
    this.readOnly = readOnly;
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

    int index = -1;
    if (support.isDrop()) {
      // fetch the drop location
      JList.DropLocation dl = (JList.DropLocation) support.getDropLocation();
      index = dl.getIndex();
    }
    else if(list.getSelectedIndices().length > 0) {
      int[] selected = list.getSelectedIndices();
      index = selected[selected.length - 1] < list.getModel().getSize() - 1 ? selected[selected.length - 1] + 1 : -1; 
    }

    boolean ok = false;
    try {
      for (DataFlavor flavor : support.getDataFlavors()) {
        if (flavor.equals(DataFlavor.stringFlavor)) {
          list.clearSelection();
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
    if (row < 0) {
      String[] tags = getTags(string);
      for (int i = 0; i < tags.length; i++) {
        model.addElement(tags[i]);
      }
      return true;
    } else if (row >= 0 && row < this.model.getSize()) {
      row = list.convertIndexToModel(row);
      String[] tags = getTags(string);
      for (int i = 0; i < tags.length; i++) {
        model.insertElementAt(tags[i], row + i);
      }
      return true;
    } else {
      return false;
    }

  }

  private String[] getTags(String string) {
    try {
      String[] tags;
      if (string.startsWith("[")) {
        tags = mapper.readValue(string, String[].class);

      } else {
        tags = StringUtils.split(string, ',');
      }
      return tags;
    } catch (Exception e) {
      return new String[0];
    }

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
    int[] rows = list.getSelectedIndices();
    if (rows.length > 0) {
      List<String> tags = new ArrayList<>();
      List<Integer> indicesToRemove = new ArrayList<>();
      for (int i = 0; i < rows.length; i++) {
        int row = list.convertIndexToModel(rows[i]);
        indicesToRemove.add(row);
        String tag = model.get(row);
        tags.add(tag);
      }

      if (!readOnly && action != COPY) {
        indicesToRemove.sort((i1, i2) -> -Integer.compare(i1, i2));
        for (int i = 0; i < indicesToRemove.size(); i++) {
          model.remove(indicesToRemove.get(i));
        }
      }
      try {
        return mapper.writeValueAsString(tags);
      } catch (Exception e) {
        e.printStackTrace();
        return null;
      }
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
