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
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.track.DistributeTracksDlg;
import de.stationadmin.gui.upload.UploadedTitleTableModel.Column;
import de.stationadmin.gui.util.Option;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadedTitlePanel extends JPanel {
  private static final long serialVersionUID = 7104176809260469070L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private UploadedTitleTableModel model;
  private ValueModel selection = new ValueHolder(new ArrayList<DetailedTrack>(0), true);
  private ValueModel targetPlaylist = new ValueHolder();
  private ValueModel playlistAppend = new ValueHolder(Boolean.TRUE);
  private JPopupMenu popup;
  private List<TitleConfirmationInterceptor> confirmationInterceptors;

  public UploadedTitlePanel(ClientContext ctx, List<TitleConfirmationInterceptor> confirmationInterceptors) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.confirmationInterceptors = confirmationInterceptors;
    this.model = new UploadedTitleTableModel(this.ctx);
    this.init();
  }

  private void init() {
    this.setLayout(new FormLayout("100dlu:grow,5dlu,pref", "50dlu:grow,5dlu,pref"));
    CellConstraints cc = new CellConstraints();
    final JXTable table = new JXTable(this.model);
    table.getColumn(Column.SAVE.ordinal()).setPreferredWidth(50);
    table.getColumn(Column.SAVE.ordinal()).setMaxWidth(50);
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
          List<DetailedTrack> titles = model.getTitles();
          ArrayList<DetailedTrack> selected = new ArrayList<DetailedTrack>();
          for (int row : table.getSelectedRows()) {
            selected.add(titles.get(row));
          }
          selection.setValue(selected);
        }
      }

    });

    TableColumn typeCol = table.getColumnModel().getColumn(Column.TYPE.ordinal());
    typeCol.setCellRenderer(new TypeRenderer());
    JComboBox typeCmb = new JComboBox(new Integer[]{1, 2, 3});
    typeCmb.setRenderer(new TitleTypeListCellRenderer(ctx));
    typeCol.setCellEditor(new DefaultCellEditor(typeCmb));

    table.setSortable(false);

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

    this.add(new JScrollPane(table), cc.xywh(1, 1, 3, 1));

    {
      List<Playlist> playlists = this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry()
          .getPlaylists(PlaylistType.ONLINE);
      Collections.sort(playlists, new PlaylistNameCompator());
      List<Object> items = new ArrayList<Object>();
      items.add(null);
      items.addAll(playlists);
      items.add(new Option("multiplaylist", ctx.getTextProvider().getString("action.distributeTitles")));
      SelectionInList<Object> playlistSelection = new SelectionInList<Object>(items, targetPlaylist);
      JComboBox cmb = BasicComponentFactory.createComboBox(playlistSelection);

      SelectionInList<Boolean> insertModeSelection = new SelectionInList<Boolean>(new Boolean[]{Boolean.FALSE,
          Boolean.TRUE}, this.playlistAppend);
      final JComboBox cmbAppend = BasicComponentFactory.createComboBox(insertModeSelection, new DefaultListCellRenderer() {
        private static final long serialVersionUID = 6168519270635870257L;

        @Override
        public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
            boolean cellHasFocus) {
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
      p.add(new JLabel(ctx.getTextProvider().getString("upload.title.targetPlaylist")));
      p.add(cmb);
      p.add(cmbAppend);

      this.add(p, cc.xy(1, 3, CellConstraints.LEFT, CellConstraints.CENTER));

    }

    {
      JButton saveBtn = new JButton(new SaveAction());
      this.add(saveBtn, cc.xy(3, 3, CellConstraints.RIGHT, CellConstraints.CENTER));
    }

  }

  public void refresh() {
    // try {
    // List<UploadedTitle> titles =
    // ctx.getAdminClient().getTitleService().getUnconfirmedUploadedTitles();
    // for (TitleConfirmationInterceptor ic : this.confirmationInterceptors) {
    // ic.beforeDisplay(titles);
    // }
    // model.setTitles(titles);
    // } catch (Exception e) {
    // JXErrorPane.showDialog(ctx.getRootWindow(),
    // textProvider.createErrorInfo(e, "upload.gettitle.error"));
    // }
  }

  public class TypeRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -7860118177226476360L;

    @Override
    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus,
        int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      this.setText(textProvider.getString("title.type." + value));

      return this;
    }
  }

  public class SaveAction extends AbstractAction {
    private static final long serialVersionUID = -8876069765348992883L;

    SaveAction() {
      this.putValue(Action.NAME, textProvider.getString("upload.confirm"));
      this.setEnabled(model.getTitles().size() > 0);
      model.addTableModelListener(new TableModelListener() {

        @Override
        public void tableChanged(TableModelEvent e) {
          setEnabled(model.getTitles().size() > 0);
        }
      });
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      try {
        List<DetailedTrack> titles = model.getTitles();
        for (TitleConfirmationInterceptor ic : confirmationInterceptors) {
          ic.beforeSave(titles);
        }
        // FIXME ctx.getAdminClient().getTitleService().confirmUploadedTitles(titles);
        for (TitleConfirmationInterceptor ic : confirmationInterceptors) {
          ic.afterSave(titles);
        }

        ctx.getAdminClient().getTrackService().updateOwnTitles();

        if (targetPlaylist.getValue() != null) {
          List<Title> addedTitles = new ArrayList<Title>();
          for (DetailedTrack utitle : titles) {
            Title t = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(utitle.getId());
            if (t != null) {
              addedTitles.add(t);
            }
          }
          if (targetPlaylist.getValue() instanceof Playlist) {
            Playlist playlist = (Playlist) targetPlaylist.getValue();
            if (playlistAppend.getValue().equals(Boolean.TRUE)) {
              for (Title t : addedTitles) {
                playlist.addTrack(t);
              }
            } else {
              playlist.insertTracks(0, addedTitles);
            }
          } else if (targetPlaylist.getValue() instanceof Option) {
            Option option = (Option) targetPlaylist.getValue();
            if (option.getKey().equals("multiplaylist")) {
              DistributeTracksDlg dlg = new DistributeTracksDlg(ctx, addedTitles);
              dlg.setVisible(true);
            }
          }
        }

        refresh();
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
      List<DetailedTrack> titles = (List<DetailedTrack>) selection.getValue();
      if (titles.size() > 0) {
        MultiTitleEditDlg dlg = new MultiTitleEditDlg(ctx, titles, model);
        dlg.setModalExclusionType(ModalExclusionType.APPLICATION_EXCLUDE);
        dlg.setVisible(true);
      }
    }
  }

}
