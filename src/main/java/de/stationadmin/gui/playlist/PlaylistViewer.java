package de.stationadmin.gui.playlist;

import java.awt.Color;
import java.awt.Component;
import java.awt.Insets;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.BitSet;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DropMode;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToggleButton;
import javax.swing.JToolBar;
import javax.swing.JViewport;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;
import javax.swing.filechooser.FileNameExtensionFilter;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.exporter.PlaylistBackupExporter;
import de.stationadmin.base.playlist.exporter.PlaylistCSVExporter;
import de.stationadmin.base.playlist.exporter.PlaylistExcelExporter;
import de.stationadmin.base.playlist.exporter.PlaylistTxtExporter;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.playlist.validation.GVLValidator;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.util.PlaylistGeneratorFactory;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.playlist.PlaylistTableModel.Column;
import de.stationadmin.gui.playlist.archive.ArchiveDialogOpenAction;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationDialog;
import de.stationadmin.gui.playlist.config.PlaylistConfigurationModel;
import de.stationadmin.gui.subscriptions.FollowArtistsAction;
import de.stationadmin.gui.subscriptions.SubscriptionResultViewer;
import de.stationadmin.gui.track.CopyTracksAction;
import de.stationadmin.gui.track.DistributeTracksAction;
import de.stationadmin.gui.track.PlaySnippetAction;
import de.stationadmin.gui.track.SearchPanel;
import de.stationadmin.gui.track.TagMenu;
import de.stationadmin.gui.track.TrackTypeRenderer;
import de.stationadmin.gui.track.TrackViewAction;
import de.stationadmin.gui.track.TrackViewer;
import de.stationadmin.gui.util.AbstractFileDialogAction;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ClipboardAction;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.IntTableCellRenderer;
import de.stationadmin.gui.util.ThreadedAction;

/**
 * Displays a playlist with
 * <ul>
 * <li>a table in the center containg the entries
 * <li>a toolbar with avaialable actions for the playlist
 * <li>a status bar
 * </ul>
 * 
 * @author Frank Korf
 */
public class PlaylistViewer extends JPanel {
  private static final long serialVersionUID = -2324586164367971173L;
  ClientContext ctx;
  TextProvider textProvider;
  ValueModel playlistHolder;
  private ValueModel entryHolder;
  PresentationModel<Playlist> presentationModel;
  private JXTable table;
  private SearchPanel searchPanel;
  private SubscriptionResultViewer subscriptionPanel;
  private ValueModel taggedTitlesHolder = new ValueHolder();
  private ValueModel hasValidationErrors = new ValueHolder(Boolean.FALSE);
  private ValueModel highlightedTrackHolder = new ValueHolder(new HashSet<Integer>(), true);

  private JLabel statisticsLabel = new JLabel("");

  boolean isSelectionUpdating = false;

  public PlaylistViewer(ClientContext ctx, ValueModel playlistHolder) {
    this(ctx, playlistHolder, new ValueHolder());
  }

  public PlaylistViewer(ClientContext ctx, ValueModel playlistHolder, ValueModel entryHolder) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.playlistHolder = playlistHolder;
    this.presentationModel = new PresentationModel<Playlist>(playlistHolder);
    this.entryHolder = entryHolder;
    this.searchPanel = new SearchPanel(ctx);
    this.searchPanel.setVisible(false);
    this.searchPanel.getSelectionHolder().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof List<?>) {
          HashSet<Integer> ids = new HashSet<Integer>();
          for (Object item : (List<?>) evt.getNewValue()) {
            ids.add(((DetailedTrack) item).getId());
          }
          highlightedTrackHolder.setValue(ids);
          invalidate();
          repaint();
        } else {
          highlightedTrackHolder.setValue(null);
        }

      }
    });
    this.searchPanel.setSelectionEnterAction(new AbstractAction() {
      private static final long serialVersionUID = -4485102580616529596L;

      @SuppressWarnings("unchecked")
      @Override
      public void actionPerformed(ActionEvent e) {
        List<BasicTrack> list = (List<BasicTrack>) searchPanel.getSelectionHolder().getValue();
        if (list != null && list.size() > 0 && PlaylistViewer.this.playlistHolder.getValue() != null) {
          Playlist playlist = (Playlist) PlaylistViewer.this.playlistHolder.getValue();
          for (BasicTrack track : list) {
            playlist.addTrack(track);
          }
          table.scrollRowToVisible(playlist.getEntries().size() - 1);
        }
      }
    });

    this.subscriptionPanel = new SubscriptionResultViewer(ctx, new ValueHolder(), true);
    this.subscriptionPanel.setVisible(false);

    this.init();
  }

  private JComponent createStatusBar() {
    JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    final JLabel lengthLabel = new JLabel("00:00:00");
    this.presentationModel.getModel("length").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null) {
          int length = (Integer) evt.getNewValue();
          lengthLabel.setText(TimeFormat.format(length, true));
        } else {
          lengthLabel.setText("");
        }
      }
    });

    final JLabel shuffleLabel = new JLabel("");
    this.presentationModel.getModel("shuffle").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Boolean && ((Boolean) evt.getNewValue()).booleanValue()) {
          shuffleLabel.setText("shuffle");
        } else {
          shuffleLabel.setText("");
        }
      }
    });

    
    this.presentationModel.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() instanceof Playlist) {
            Playlist playlist = (Playlist) evt.getNewValue();
            updateStatsLabel(playlist);
          }
        }
      });
    this.taggedTitlesHolder.addValueChangeListener(new PropertyChangeListener() {
		
		@Override
		public void propertyChange(PropertyChangeEvent evt) {
            Playlist playlist = (Playlist) playlistHolder.getValue();
            updateStatsLabel(playlist);
			
		}
	});

    // if the playlist is modified, remove statistics text - it is not longer up
    // to date
    /*
    this.presentationModel.getModel("modified").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Boolean && evt.getNewValue().equals(Boolean.TRUE)) {
          statisticsLabel.setText("");
        }
      }

    });
    */

    final JLabel type = new JLabel("");
    this.presentationModel.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Playlist) {
          Playlist playlist = (Playlist) evt.getNewValue();
          type.setText(textProvider.getString("playlisttype." + playlist.getType().name().toLowerCase()));
        } else {
          type.setText("");
        }
      }

    });

    JXStatusBar.Constraint typeConst = new JXStatusBar.Constraint(new Insets(0, 5, 0, 5));
    statusBar.add(type, typeConst);

    JXStatusBar.Constraint lengthConst = new JXStatusBar.Constraint(new Insets(0, 5, 0, 5));
    statusBar.add(lengthLabel, lengthConst);

    JXStatusBar.Constraint statisticsConst = new JXStatusBar.Constraint();
    statusBar.add(statisticsLabel, statisticsConst);

    JXStatusBar.Constraint shuffleConst = new JXStatusBar.Constraint(new Insets(0, 5, 0, 5));
    statusBar.add(shuffleLabel, shuffleConst);

    return statusBar;
  }
  
  private void updateStatsLabel(Playlist playlist) {
	if(playlist != null) {
      int numSelectedForTag = taggedTitlesHolder.getValue() != null ?  ((BitSet) taggedTitlesHolder.getValue()).cardinality() : 0;
      int numTagged = 0;
      if(numSelectedForTag > 0) {
      	BitSet bs = (BitSet) taggedTitlesHolder.getValue();
      	for(Entry entry : playlist.getEntries()) {
      		if(bs.get(entry.getTrackId())) {
      			numTagged++;
      		}
      	}
      }
      if(numTagged > 0) {
          statisticsLabel.setText(textProvider.getString("playlistviewer.statistics.highlighted", Integer.toString(playlist.getEntries().size()), Integer.toString(playlist.getNumDifferentArtists()), Integer.toString(numTagged)));
      }
      else {
        statisticsLabel.setText(textProvider.getString("playlistviewer.statistics", Integer.toString(playlist.getEntries().size()), Integer.toString(playlist.getNumDifferentArtists())));
      }
    } else {
      statisticsLabel.setText("");
    }
	  
  }

  private JComponent createTabelPanel() {

    final PlaylistTableModel tableModel = new PlaylistTableModel(this.textProvider, this.playlistHolder, this.entryHolder, ctx.getAdminClient().getTagManager());
    tableModel.setHasValidationErrors(this.hasValidationErrors);
    tableModel.addTableModelListener(new TableModelListener() {
      
      @Override
      public void tableChanged(TableModelEvent e) {
        if(playlistHolder.getValue() instanceof Playlist) {
          updateStatsLabel((Playlist)playlistHolder.getValue());
        }
      }
    });
    
    final TrackTypeRenderer typeRenderer = new TrackTypeRenderer();
    final DateTableCellRenderer timeRenderer = new DateTableCellRenderer(new SimpleDateFormat(this.ctx.getTextProvider().getString("timeFormat")));
    this.table = new JXTable(tableModel) {
      @Override
      public String getToolTipText(MouseEvent evt) {
        int col = columnAtPoint(evt.getPoint());
        if (col > -1) {
          int row = rowAtPoint(evt.getPoint());
          col = convertColumnIndexToModel(col);
          if (row > -1 && col == Column.TAGS.ordinal()) {
            Object value = getModel().getValueAt(row, col);
            if(value instanceof String) {
              return (String)value;
            }
          }
        }

        return super.getToolTipText(evt);

      }
      
    };

    table.getColumnModel().getColumn(Column.ENTRYNO.ordinal()).setPreferredWidth(40);
    table.getColumnModel().getColumn(Column.ENTRYNO.ordinal()).setMaxWidth(40);
    table.getColumnModel().getColumn(Column.STARTTIME.ordinal()).setPreferredWidth(70);
    table.getColumnModel().getColumn(Column.STARTTIME.ordinal()).setMaxWidth(70);
    table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setPreferredWidth(70);
    table.getColumnModel().getColumn(Column.LENGTH.ordinal()).setMaxWidth(70);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setPreferredWidth(30);
    table.getColumnModel().getColumn(Column.TYPE.ordinal()).setMaxWidth(30);
    table.getColumnModel().getColumn(Column.YEAR.ordinal()).setPreferredWidth(40);
    table.getColumnModel().getColumn(Column.YEAR.ordinal()).setMaxWidth(40);
    table.getColumnModel().getColumn(Column.ADDED.ordinal()).setPreferredWidth(110);
    table.getColumnModel().getColumn(Column.ADDED.ordinal()).setMaxWidth(110);
    table.getColumnModel().getColumn(Column.NUMPLAYLISTS.ordinal()).setMaxWidth(40);
    table.getColumnModel().getColumn(Column.NUMPLAYLISTS.ordinal()).setPreferredWidth(40);
    table.getColumn(Column.TYPE.ordinal()).setCellRenderer(typeRenderer);
    table.getColumn(Column.ADDED.ordinal()).setCellRenderer(timeRenderer);
    table.getColumn(Column.YEAR.ordinal()).setCellRenderer(new IntTableCellRenderer(0));
    
    table.setDropMode(DropMode.INSERT_ROWS);
    table.setDragEnabled(true);
    table.setTransferHandler(new PlaylistTableTransferHandler(ctx, table));

    table.setColumnControlVisible(true);
    table.getColumnExt(table.convertColumnIndexToView(Column.ALBUM.ordinal())).setVisible(false);
    table.getColumnExt(table.convertColumnIndexToView(Column.YEAR.ordinal())).setVisible(false);
    table.getColumnExt(table.convertColumnIndexToView(Column.GENRE.ordinal())).setVisible(false);
    table.getColumnExt(table.convertColumnIndexToView(Column.ADDED.ordinal())).setVisible(false);
    table.getColumnExt(table.convertColumnIndexToView(Column.NUMPLAYLISTS.ordinal())).setVisible(false);
    table.getColumnExt(table.convertColumnIndexToView(Column.TAGS.ordinal())).setVisible(false);

    if (entryHolder != null) {
      entryHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (!isSelectionUpdating) {
            if (evt.getNewValue() instanceof Entry) {
              int row = tableModel.getPlaylist().getEntries().indexOf(evt.getNewValue());
              if (row > -1) {
                table.getSelectionModel().clearSelection();
                row = table.convertRowIndexToView(row);
                table.getSelectionModel().setSelectionInterval(row, row);
                table.scrollRowToVisible(row);
              }
            }
            if (evt.getNewValue() instanceof List) {
              table.getSelectionModel().clearSelection();
              List<?> list = (List<?>) evt.getNewValue();
              for (int i = 0; i < list.size(); i++) {
                Entry entry = (Entry) list.get(i);
                int row = tableModel.getPlaylist().getEntries().indexOf(entry);
                if (row > -1) {
                  row = table.convertRowIndexToView(row);
                  table.getSelectionModel().addSelectionInterval(row, row);
                }
              }

            }
          }
        }

      });
    }

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int[] rows = table.getSelectedRows();
          List<Entry> entries = new ArrayList<Entry>();
          for (int i = 0; i < rows.length; i++) {
            int row = table.convertRowIndexToModel(rows[i]);
            entries.add(tableModel.getPlaylist().getEntries().get(row));
          }
          isSelectionUpdating = true;
          try {
            entryHolder.setValue(entries);
          } finally {
            isSelectionUpdating = false;
          }
        }
      }

    });

    table.getInputMap().put(KeyStroke.getKeyStroke(KeyEvent.VK_DELETE, 0), "delete");
    table.getActionMap().put("delete", new TitleDeleteAction());

    final JPopupMenu popup = new JPopupMenu();
    popup.add(new ClipboardAction(ctx, table, this.entryHolder, TransferHandler.getCutAction()));
    popup.add(new ClipboardAction(ctx, table, this.entryHolder, TransferHandler.getCopyAction()));
    popup.add(new ClipboardAction(ctx, table, this.entryHolder, TransferHandler.getPasteAction()));
    popup.addSeparator();
    final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
    final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
    final TagHighlightMenu tagHighlightMenu = new TagHighlightMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), taggedTitlesHolder);
    final CopyTracksAction copyAction = new CopyTracksAction(this.ctx);
    final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);
    final TrackViewAction viewAction = new TrackViewAction(ctx);
    final ValueModel titleHolder = new ValueHolder(new ArrayList<BasicTrack>());
    final FollowArtistsAction followAction = new FollowArtistsAction(this.ctx);
    this.entryHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (playlistHolder.getValue() != null) {
          if (evt.getNewValue() instanceof Entry) {
            tagMenu.setTitleIds(new int[] { ((Entry) evt.getNewValue()).getTrackId() });
            BasicTrack title = ((Entry) evt.getNewValue()).getTrack();
            distributeAction.setTitles(Arrays.asList(title));
            titleHolder.setValue(Arrays.asList(title));
          } else if (evt.getNewValue() instanceof List) {
            ArrayList<BasicTrack> titles = new ArrayList<BasicTrack>();
            int[] titleIds = new int[((List<?>) evt.getNewValue()).size()];
            int i = 0;
            for (Object item : (List<?>) evt.getNewValue()) {
              titleIds[i++] = ((Entry) item).getTrackId();
              titles.add(((Entry) item).getTrack());
            }
            tagMenu.setTitleIds(titleIds);
            untagMenu.setTitleIds(titleIds);
            copyAction.setTitles(titles);
            distributeAction.setTitles(titles);
            titleHolder.setValue(titles);
            viewAction.setTitles(titles);
            followAction.setTracks(titles);
          } else {
            distributeAction.setTitles(new ArrayList<BasicTrack>());
          }
        } else {
          distributeAction.setTitles(new ArrayList<BasicTrack>());
        }
      }

    });
    popup.add(tagMenu);
    popup.add(untagMenu);
    popup.add(tagHighlightMenu);
    popup.addSeparator();
    popup.add(copyAction);
    popup.add(distributeAction);
    popup.add(followAction);
    popup.addSeparator();
    popup.add(new TitleDeleteAction());
    popup.addSeparator();
    popup.add(viewAction);
    popup.add(new PlaySnippetAction(ctx, titleHolder));

    table.addMouseListener(new MouseAdapter() {

      private void checkPopup(MouseEvent e) {
        if (e.isPopupTrigger()) {
          popup.show(table, e.getX(), e.getY());
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.getClickCount() > 1) {
          int row = table.rowAtPoint(e.getPoint());
          row = table.convertRowIndexToModel(row);
          if (row > -1) {
            BasicTrack title = tableModel.getTitleAt(row);
            if (title != null) {
              Set<Integer> playlistIds = (title instanceof RegisteredTrack) ? ((RegisteredTrack) title).getPlaylistIds() : new HashSet<Integer>();
              if (!(title instanceof RegisteredTrack) || !((RegisteredTrack) title).isOwnTrack()) {
                // load full data from server
                try {
                  title = ctx.getAdminClient().getTrackService().getTrack(title.getId());
                } catch (Exception ex) {
                }
              }
              TrackViewer viewer = new TrackViewer(ctx, title, playlistIds);
              viewer.setVisible(true);
            }
          }
        }
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

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        if (tableModel.isGVLValidationError(row)) {
          comp.setFont(ComponentFactory.boldLabelFont);
          comp.setForeground(Color.RED);
        } else {
          BasicTrack title = tableModel.getTitleAt(row);
          if (taggedTitlesHolder.getValue() != null) {
            BitSet bs = (BitSet) taggedTitlesHolder.getValue();
            if (title != null && bs.get(title.getId())) {
              if (!adapter.isSelected()) {
                comp.setBackground(new Color(240, 240, 240));
              }
            }
          }
          if (highlightedTrackHolder.getValue() != null) {
            Set<?> values = (Set<?>) highlightedTrackHolder.getValue();
            if (values.contains(title.getId())) {
              comp.setBackground(new Color(240, 240, 0));
            }
          }
        }
        return comp;
      }

    });

    this.taggedTitlesHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        table.validate();
        table.repaint();
      }
    });

    return new JScrollPane(table);
  }

  private JToolBar createToolBar() {
    JToolBar toolbar = new JToolBar();
    toolbar.add(new NewPlaylistAction());
    toolbar.addSeparator();
    toolbar.add(new SaveAction(ctx));
    toolbar.add(new ResetAction());
    toolbar.add(new PlaylistDeleteAction(this.playlistHolder, this.ctx.getAdminClient().getPlaylistService(), this.textProvider, true));
    ValidationErrorFilterAction vAction = new ValidationErrorFilterAction();
    JToggleButton validationErrorFilterBtn = new JToggleButton(vAction);
    vAction.setButton(validationErrorFilterBtn); // ouch
    toolbar.add(validationErrorFilterBtn);
    toolbar.addSeparator();
    toolbar.add(new PlaylistEditPropertiesAction(ctx, playlistHolder, true));
    toolbar.addSeparator();
    final JToggleButton searchBtn = new JToggleButton(AppUtils.getIcon("searching.png"));
    searchBtn.setToolTipText(ctx.getString("action.search.tooltip"));
    searchBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        searchPanel.setVisible(searchBtn.isSelected());
        if (!searchBtn.isSelected()) {
          highlightedTrackHolder.setValue(null);
        }
      }
    });

    toolbar.add(searchBtn);

    final JToggleButton subscriptionBtn = new JToggleButton(AppUtils.getIcon("subscriptions.png"));
    subscriptionBtn.setToolTipText(ctx.getString("action.subscription.tooltip"));
    subscriptionBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        subscriptionPanel.setVisible(subscriptionBtn.isSelected());
      }
    });
    toolbar.add(subscriptionBtn);

    toolbar.add(new ImportAction());
    toolbar.add(new SortAction());
    toolbar.add(new ShuffleAction());
    toolbar.add(new GenerateAction());
    toolbar.addSeparator();
    toolbar.add(new ArchiveDialogOpenAction(this.ctx, this.playlistHolder));

    final JPopupMenu exportMenu = new JPopupMenu();
    exportMenu.add(new PlaylistExportAction(ctx, playlistHolder, "lfm", new PlaylistBackupExporter()));
    exportMenu.add(new PlaylistExportAction(ctx, playlistHolder, "csv", new PlaylistCSVExporter()));
    exportMenu.add(new PlaylistExportAction(ctx, playlistHolder, "xls", new PlaylistExcelExporter()));
    exportMenu.add(new PlaylistExportAction(ctx, playlistHolder, "txt", new PlaylistTxtExporter()));

    final JButton exportBtn = new JButton(ctx.getIcon("playlist_export.png"));
    toolbar.add(exportBtn);
    exportBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        exportMenu.show(exportBtn, 0, exportBtn.getPreferredSize().height);
      }
    });
    exportBtn.setEnabled(false);
    this.playlistHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        exportBtn.setEnabled(playlistHolder.getValue() != null);

      }
    });

    toolbar.add(exportBtn);

    return toolbar;
  }

  /**
   * @return the entryHolder
   */
  public ValueModel getEntryHolder() {
    return entryHolder;
  }

  public PresentationModel<Playlist> getPresentationModel() {
    return presentationModel;
  }

  private void init() {
    this.setLayout(new FormLayout("pref:grow", "pref,min(pref;150dlu),min(pref;150dlu),80dlu:grow,pref"));
    CellConstraints cc = new CellConstraints();
    this.add(this.createToolBar(), cc.xy(1, 1));
    this.add(this.searchPanel, cc.xy(1, 2));
    this.add(this.subscriptionPanel, cc.xy(1, 3));
    this.add(this.createTabelPanel(), cc.xy(1, 4, CellConstraints.FILL, CellConstraints.FILL));
    this.add(this.createStatusBar(), cc.xy(1, 5));
  }

  public void setValidationEnabled(boolean validate) {
    ((PlaylistTableModel) this.table.getModel()).setValidationEnabled(validate);
  }

  public boolean isValidationEnabled() {
    return ((PlaylistTableModel) this.table.getModel()).isValidationEnabled();
  }

  void scrollToRow(JXTable table, int row) {
    // scroll
    Rectangle rect = table.getCellRect(row, -1, true);

    JViewport viewport = (JViewport) table.getParent();

    Rectangle viewRect = viewport.getViewRect();

    // Translate the cell location so that it is relative
    // to the view, assuming the northwest corner of the
    // view is (0,0).
    rect.setLocation(rect.x - viewRect.x, rect.y - viewRect.y);

    // Calculate location of rect if it were at the center of view
    int centerX = (viewRect.width - rect.width) / 2;
    int centerY = (viewRect.height - rect.height) / 2;

    // Fake the location of the cell so that scrollRectToVisible
    // will move the cell to the center
    if (rect.x < centerX) {
      centerX = -centerX;
    }
    if (rect.y < centerY) {
      centerY = -centerY;
    }
    rect.translate(centerX, centerY);

    // Scroll the area into view.
    viewport.scrollRectToVisible(rect);

  }

  protected class ImportAction extends AbstractFileDialogAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1127269220503072051L;

    public ImportAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("import.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("playlistviewer.import.tooltip"));
      this.setTitle(textProvider.getString("playlistviewer.import.title"));
      this.setFileFilter(new FileNameExtensionFilter("MP3 / Playlists", "mp3", "m3u", "m3u8", "lfm"));
      this.setUserPrefencesKey("filedir.playlistimport");
      setEnabled(false);
      presentationModel.getBeanChannel().addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void performAction(JFileChooser fileChooser, File file) {

      int row = table.getSelectedRow();
      if (row == -1) {
        row = table.getModel().getRowCount();
      }

      TrackImportHandler handler = new TrackImportHandler(ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager(), (Playlist) playlistHolder.getValue(), row);
      try {
        handler.add(file);
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
      } catch (Throwable e) {
        JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "playlist.transfer.error.import"));
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(evt.getNewValue() != null);
    }

  }

  protected class ResetAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = 4692227689345527634L;

    public ResetAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("undo.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("action.playlist.reset.tooltip"));
      setEnabled(false);
      presentationModel.getModel("modified").addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        playlist.reset();
      }

    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      if (evt.getNewValue() instanceof Boolean) {
        setEnabled(((Boolean) evt.getNewValue()).booleanValue());
      }
    }

  }

  protected class SaveAction extends ThreadedAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1127269220503072051L;

    public SaveAction(ClientContext ctx) {
      super(ctx);
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("save.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("action.playlist.save.tooltip"));
      setEnabled(false);
      presentationModel.getModel("modified").addValueChangeListener(this);
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (evt.getNewValue() instanceof Boolean) {
        setEnabled(((Boolean) evt.getNewValue()).booleanValue() && playlist != null && playlist.getType().isSaveToDiskSupported());
      }
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#getStatus()
     */
    @Override
    protected String getStatus() {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      return textProvider.getString("action.playlist.msg", playlist != null ? playlist.getDisplayName() : "Playlist");
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#performAction()
     */
    @Override
    protected void performAction() throws Exception {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        ctx.getAdminClient().getPlaylistService().savePlaylist(playlist);
      }
    }

    /**
     * @see de.stationadmin.gui.util.ThreadedAction#showError(java.lang.Exception)
     */
    @Override
    protected void showError(Exception e) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        if (e instanceof PlaylistValidationException) {
          JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "playlist.validationerror." + ((PlaylistValidationException) e).getError().name().toLowerCase(), playlist.getDisplayName()));
        } else {
          JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "action.playlist.save.error", playlist.getDisplayName()));
        }
      }
    }

    @Override
    protected boolean beforeExecution() {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        if (ctx.getAdminClient().getSchedule().isScheduled(playlist)) {
          List<Entry> violations = new ArrayList<Entry>();
          new GVLValidator().validate(playlist, violations);
          if (violations.size() > 0) {
            return JOptionPane.showConfirmDialog(ctx.getRootWindow(), textProvider.getString("action.playlist.save.msg.validationerror", playlist.getName()), null, JOptionPane.OK_CANCEL_OPTION,
                JOptionPane.WARNING_MESSAGE) == JOptionPane.OK_OPTION;
          }
        }
      }
      return true;
    }

  }

  protected class ShuffleAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1127269220503072051L;

    public ShuffleAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("shuffle.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("playlistviewer.shuffle.tooltip"));
      setEnabled(false);
      presentationModel.getBeanChannel().addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        if (playlist.isShuffle()) {
          if (JOptionPane.showOptionDialog(ctx.getRootWindow(), ctx.getTextProvider().getString("playlistviewer.shuffle.msg.confirm"), null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
              null, null, null) == JOptionPane.NO_OPTION) {
            return;
          }
        }
        PlaylistShuffler shuffler = PlaylistGeneratorFactory.createShuffler(ctx.getAdminClient());
        shuffler.shuffle(playlist);
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(evt.getNewValue() != null && ((Playlist) evt.getNewValue()).isLocalShuffleAllowed());
    }

  }

  protected class GenerateAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = 4424705922134106876L;

    public GenerateAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("generate.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("playlistviewer.generate.tooltip"));
      setEnabled(false);
      presentationModel.getBeanChannel().addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        if (playlist.isShuffle()) {
          if (JOptionPane.showOptionDialog(ctx.getRootWindow(), ctx.getTextProvider().getString("playlistviewer.generate.msg.confirm"), null, JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
              null, null, null) == JOptionPane.NO_OPTION) {
            return;
          }
        }
        PlaylistGenerator generator = PlaylistGeneratorFactory.createGenerator(ctx.getAdminClient());
        try {
          generator.generate(playlist);
        } catch (IOException e) {
          JXErrorPane.showDialog(null, textProvider.createErrorInfo(e, "action.playlist.generate.error", playlist.getDisplayName()));
        }
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(evt.getNewValue() != null && StringUtils.isNotEmpty(((Playlist) evt.getNewValue()).getGenerateTags()) && ((Playlist) evt.getNewValue()).getGenerateLength() > 0);
    }

  }

  protected class NewPlaylistAction extends PlaylistNewAction {
    private static final long serialVersionUID = -1127269220503072051L;

    public NewPlaylistAction() {
      super(ctx, playlistHolder);
      this.putValue(Action.NAME, null);
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("filenew.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("playlistviewer.new.tooltip"));
      setEnabled(true);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = new Playlist(ctx.getAdminClient().getTrackService().getTrackRegistry(), PlaylistType.ONLINE);
      playlistHolder.setValue(playlist);
      PlaylistConfigurationModel model = new PlaylistConfigurationModel(playlist, ctx.getAdminClient().getTagManager());
      PlaylistConfigurationDialog dlg = new PlaylistConfigurationDialog(ctx, model);
      dlg.setVisible(true);
    }

  }

  protected class SortAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -1127269220503072051L;

    public SortAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("sort.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("playlistviewer.sort.tooltip"));
      setEnabled(false);
      presentationModel.getBeanChannel().addValueChangeListener(this);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        playlist.sortByArtist();
      }
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      setEnabled(evt.getNewValue() != null);
    }

  }

  protected class TitleDeleteAction extends AbstractAction {
    private static final long serialVersionUID = 3279957643214213264L;

    public TitleDeleteAction() {
      this.putValue(Action.NAME, textProvider.getString("action.playlist.titledelete"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    @SuppressWarnings("unchecked")
    public void actionPerformed(ActionEvent e) {
      Playlist playlist = (Playlist) playlistHolder.getValue();
      if (playlist != null) {
        if (entryHolder.getValue() instanceof Entry) {
          playlist.removeEntry((Entry) entryHolder.getValue());

        }
        if (entryHolder.getValue() instanceof List) {
          playlist.removeEntries((List<Entry>) entryHolder.getValue());
        }
      }

    }

  }

  private class ValidationErrorFilterAction extends AbstractAction {
    private static final long serialVersionUID = 614169305315740902L;
    private JToggleButton button;

    ValidationErrorFilterAction() {
      this.putValue(Action.SMALL_ICON, AppUtils.getIcon("messagebox_warning.png"));
      this.putValue(Action.SHORT_DESCRIPTION, textProvider.getString("action.playlist.validationerrorfilter.tooltip"));
      this.setEnabled(false);
      hasValidationErrors.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue().equals(Boolean.TRUE)) {
            setEnabled(true);
          } else {
            setEnabled(false);
            button.setSelected(false);
            setFilterEnabled(false);
          }
        }
      });
    }

    private void setFilterEnabled(boolean enabled) {
      // FIXME
      // if (enabled) {
      // ValidationErrorFilter filter = new
      // ValidationErrorFilter((PlaylistTableModel) table.getModel());
      // table.setFilters(new FilterPipeline(filter));
      // } else {
      // table.setFilters(null);
      // }

    }

    @Override
    public void actionPerformed(ActionEvent e) {
      if (button != null) {
        setFilterEnabled(button.isSelected());
      }

    }

    /**
     * @param button
     *          the button to set
     */
    void setButton(JToggleButton button) {
      this.button = button;
    }

  }

  /**
   * Gets a value model that holds the ids of titles that are currently requested for highlighting because they are selected in the search panel.
   * 
   * @return the highlightedTitleHolder
   */
  public ValueModel getHighlightedTrackHolder() {
    return highlightedTrackHolder;
  }

}
