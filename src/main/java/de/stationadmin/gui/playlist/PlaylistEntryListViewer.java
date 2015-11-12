/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.util.PlaylistEntry;

/**
 * Viewer for Playlist.Entry objects (from different playlists)
 * 
 * @author Frank Korf
 */
public class PlaylistEntryListViewer extends JPanel {
  private static final long serialVersionUID = 8884928437846129993L;
  private PlaylistEntryTableModel model;
  private ValueModel playlistHolder;
  private ValueModel entryHolder;
  private ValueModel selectionHolder = new ValueHolder(new ArrayList<PlaylistEntry>(), true);

  public PlaylistEntryListViewer(PlaylistEntryTableModel model, ValueModel playlistHolder, ValueModel entryHolder) {
    super();
    this.model = model;
    this.playlistHolder = playlistHolder;
    this.entryHolder = entryHolder;
    this.init();
  }

  private void init() {
    this.setLayout(new BorderLayout());
    final JXTable table = new JXTable(this.model);
    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          boolean first = true;
          ArrayList<PlaylistEntry> newSelection = new ArrayList<PlaylistEntry>();
          for (int row : table.getSelectedRows()) {
            if (row > -1) {
              row = table.convertRowIndexToModel(row);
              PlaylistEntry entry = model.getEntries().get(row);
              if (first) {
                playlistHolder.setValue(entry.getPlaylist());
                entryHolder.setValue(entry.getEntries());
                first = false;
              }
              newSelection.add(entry);
            }
          }
          selectionHolder.setValue(newSelection);
        }

      }

    });

    final JPopupMenu popup = new JPopupMenu();
    popup.add(new EntryDeleteAction());

    table.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popup.show(table, e.getX(), e.getY());
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        this.checkPopup(e);
      }

      /**
       * @see java.awt.event.MouseAdapter#mousePressed(java.awt.event.MouseEvent)
       */
      @Override
      public void mousePressed(MouseEvent e) {
        this.checkPopup(e);
      }

      /**
       * @see java.awt.event.MouseAdapter#mouseReleased(java.awt.event.MouseEvent)
       */
      @Override
      public void mouseReleased(MouseEvent e) {
        this.checkPopup(e);
      }

    });

    this.add(new JScrollPane(table), BorderLayout.CENTER);

  }

  private class EntryDeleteAction extends AbstractAction {
    private static final long serialVersionUID = -1043833572023003464L;

    EntryDeleteAction() {
      this.putValue(Action.NAME, model.getCtx().getTextProvider().getString("playlistentryviewer.action.delete.name"));
      this.setEnabled(false);
      selectionHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        @SuppressWarnings("unchecked")
        public void propertyChange(PropertyChangeEvent evt) {
          ArrayList<PlaylistEntry> list = (ArrayList<PlaylistEntry>) selectionHolder.getValue();
          setEnabled(list.size() > 0);

        }
      });
    }

    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent evt) {
      ArrayList<PlaylistEntry> list = (ArrayList<PlaylistEntry>) selectionHolder.getValue();
      for (PlaylistEntry entry : list) {
        for (Playlist.Entry e : entry.getEntries()) {
          entry.getPlaylist().removeEntry(e);
        }
      }
      model.removeEntries(list);
    }

  }

}
