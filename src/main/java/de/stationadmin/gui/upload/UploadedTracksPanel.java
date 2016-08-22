/**
 * 
 */
package de.stationadmin.gui.upload;

import java.awt.Component;
import java.awt.Dialog.ModalExclusionType;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultCellEditor;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableColumn;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.upload.QueuedTrack;
import de.stationadmin.base.track.upload.UploadManager;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.track.DistributeTracksDlg;
import de.stationadmin.gui.upload.UploadedTrackTableModel.Column;
import de.stationadmin.gui.util.Option;

/**
 * 
 * @author Frank Korf
 * 
 */
@SuppressWarnings("rawtypes")
public class UploadedTracksPanel extends JPanel {
  private static final long serialVersionUID = 7104176809260469070L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private UploadManager uploadManager;
  private UploadedTrackTableModel model;
  private ValueModel selection = new ValueHolder(new ArrayList<DetailedTrack>(0), true);
  private ValueModel targetPlaylist = new ValueHolder();
  private ValueModel targetTag = new ValueHolder();
  private ValueModel playlistAppend = new ValueHolder(Boolean.TRUE);
  private JPopupMenu popup;

  public UploadedTracksPanel(ClientContext ctx, UploadManager uploadManager) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.model = new UploadedTrackTableModel(this.ctx);
    this.uploadManager = uploadManager;
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("pref,5dlu,100dlu:grow,5dlu,pref", "50dlu:grow,5dlu,pref,2dlu,pref"));
    CellConstraints cc = new CellConstraints();
    final JXTable table = new JXTable(this.model);
    table.getColumn(Column.PRIVATE.ordinal()).setPreferredWidth(50);
    table.getColumn(Column.PRIVATE.ordinal()).setMaxWidth(50);
    table.getColumn(Column.GENRE.ordinal()).setPreferredWidth(80);
    table.getColumn(Column.GENRE.ordinal()).setMaxWidth(80);
    table.getColumn(Column.YEAR.ordinal()).setPreferredWidth(50);
    table.getColumn(Column.YEAR.ordinal()).setMaxWidth(50);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          List<QueuedTrack> tracks = model.getTracks();
          ArrayList<QueuedTrack> selected = new ArrayList<QueuedTrack>();
          for (int row : table.getSelectedRows()) {
            selected.add(tracks.get(row));
          }
          selection.setValue(selected);
        }
      }

    });

    TableColumn typeCol = table.getColumnModel().getColumn(Column.TYPE.ordinal());
    typeCol.setCellRenderer(new TypeRenderer());
    JComboBox<Integer> typeCmb = new JComboBox<Integer>(new Integer[] { 1, 2, 3 });
    typeCmb.setRenderer(new TrackTypeListCellRenderer(ctx));
    typeCol.setCellEditor(new DefaultCellEditor(typeCmb));

    table.setSortable(false);
    
    uploadManager.addPropertyChangeListener("numProcessedTracks", new PropertyChangeListener() {
      
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        SwingUtilities.invokeLater(new Runnable() {
          
          @Override
          public void run() {
            model.setTracks(uploadManager.getProcessedTracks());
          }
        });
      }
    });

    this.popup = new JPopupMenu();
    this.popup.add(new OpenMultiTitleEditor());

    table.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popup.show(table, e.getX(), e.getY());
        }

      }

      /**
       * @see java.awt.event.MouseAdapter#mouseClicked(java.awt.event.MouseEvent)
       */
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

    this.add(new JScrollPane(table), cc.xywh(1, 1, 5, 1));

    {
      List<Playlist> playlists = this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
      Collections.sort(playlists, new PlaylistNameCompator());
      List<Object> items = new ArrayList<Object>();
      items.add(null);
      items.addAll(playlists);
      items.add(new Option("multiplaylist", ctx.getTextProvider().getString("action.distributeTitles")));
      SelectionInList<Object> playlistSelection = new SelectionInList<Object>(items, targetPlaylist);
      JComboBox cmb = BasicComponentFactory.createComboBox(playlistSelection);

      SelectionInList<Boolean> insertModeSelection = new SelectionInList<Boolean>(new Boolean[] { Boolean.FALSE, Boolean.TRUE }, this.playlistAppend);
      final JComboBox cmbAppend = BasicComponentFactory.createComboBox(insertModeSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 6168519270635870257L;

        @Override
        public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
          Component cmp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
          if (value.equals(Boolean.FALSE)) {
            this.setText(ctx.getTextProvider().getString("upload.title.targetPlaylist.append.beginning"));
          }
          if (value.equals(Boolean.TRUE)) {
            this.setText(ctx.getTextProvider().getString("upload.title.targetPlaylist.append.end"));
          }
          return cmp;
        }

      });
      cmbAppend.setEnabled(false);
      this.targetPlaylist.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          cmbAppend.setEnabled(evt.getNewValue() instanceof Playlist);
        }
      });

      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
      p.add(cmb);
      p.add(cmbAppend);

      this.add(new JLabel(ctx.getTextProvider().getString("upload.title.targetPlaylist")), cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));
      this.add(p, cc.xy(3, 3, CellConstraints.LEFT, CellConstraints.CENTER));
    }
    
    {
      List<StaticTag> tags = this.ctx.getAdminClient().getTagManager().getStaticTags();
      List<String> names = new ArrayList<String>();
      for (StaticTag tag : tags) {
        names.add(tag.getName());
      }
      Collections.sort(names);
      names.add(0, null);
      SelectionInList<String> tagSelection = new SelectionInList<String>(names, this.targetTag);
      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEADING));
      JComboBox cmb = BasicComponentFactory.createComboBox(tagSelection);
      p.add(cmb);

      this.add(new JLabel(ctx.getTextProvider().getString("upload.title.targetTag")), cc.xy(1, 5, CellConstraints.LEFT, CellConstraints.CENTER));
      this.add(p, cc.xy(3, 5, CellConstraints.LEFT, CellConstraints.CENTER));

    }


    {
      JButton saveBtn = new JButton(new SaveAction());
      this.add(saveBtn, cc.xy(5, 5, CellConstraints.RIGHT, CellConstraints.CENTER));
    }

  }

  public class TypeRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -7860118177226476360L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      this.setText(textProvider.getString("title.type." + value));

      return this;
    }
  }

  public class SaveAction extends AbstractAction {
    private static final long serialVersionUID = -8876069765348992883L;

    SaveAction() {
      this.putValue(Action.NAME, textProvider.getString("upload.confirm"));
      this.setEnabled(model.getTracks().size() > 0);
      model.addTableModelListener(new TableModelListener() {

        @Override
        public void tableChanged(TableModelEvent e) {
          setEnabled(model.getTracks().size() > 0);
        }
      });
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        for (DetailedTrack track : model.getModifiedTracks()) {
          ctx.getAdminClient().getTrackService().updateTrack(track);
          ctx.getAdminClient().getTrackService().getTrackRegistry().registerOwnTrack(track);
        }

        List<Title> addedTracks = new ArrayList<Title>();
        for (QueuedTrack track : model.getTracks()) {
          Title t = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(track.getTrack().getId());
          if (t != null) {
            addedTracks.add(t);
          }
        }

        if (targetPlaylist.getValue() != null) {
          if (targetPlaylist.getValue() instanceof Playlist) {
            Playlist playlist = (Playlist) targetPlaylist.getValue();
            if (playlistAppend.getValue().equals(Boolean.TRUE)) {
              for (Title t : addedTracks) {
                playlist.addTrack(t);
              }
            } else {
              playlist.insertTracks(0, addedTracks);
            }
          } else if (targetPlaylist.getValue() instanceof Option) {
            Option option = (Option) targetPlaylist.getValue();
            if (option.getKey().equals("multiplaylist")) {
              DistributeTracksDlg dlg = new DistributeTracksDlg(ctx, addedTracks);
              dlg.setVisible(true);
            }
          }
        }
        
        if (targetTag.getValue() != null) {
          String tag = (String) targetTag.getValue();
          int[] ids = new int[addedTracks.size()];
          for (int i = 0; i < addedTracks.size(); i++) {
            ids[i] = addedTracks.get(i).getId();
          }
          ctx.getAdminClient().getTagManager().tagTracks(tag, ids);
        }
        
        uploadManager.clearProcessedTracks();

      } catch (Exception e) {
        JXErrorPane.showDialog(ctx.getRootWindow(), textProvider.createErrorInfo(e, "upload.error.confirm"));
      }

    }

  }

  private class OpenMultiTitleEditor extends AbstractAction {
    private static final long serialVersionUID = -3048118417240804873L;

    OpenMultiTitleEditor() {
      this.putValue(Action.NAME, textProvider.getString("upload.action.multiedit.name"));
      this.setEnabled(false);
      selection.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          checkEnabled();
        }

      });
    }

    @SuppressWarnings("unchecked")
    private void checkEnabled() {
      List<DetailedTrack> titles = (List<DetailedTrack>) selection.getValue();
      this.setEnabled(titles != null && titles.size() > 0);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
      List<QueuedTrack> tracks = (List<QueuedTrack>) selection.getValue();
      if (tracks.size() > 0) {
        MultiTrackEditDlg dlg = new MultiTrackEditDlg(ctx, tracks, model);
        dlg.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        dlg.setVisible(true);
      }
    }
  }

}
