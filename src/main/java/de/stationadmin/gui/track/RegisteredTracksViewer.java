/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.Timer;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.table.DefaultTableCellRenderer;
import javax.swing.table.TableCellRenderer;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXStatusBar;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.table.TableColumnExt;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagSet;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.RegisteredTrack.PlaylistStatistics;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.track.exporter.TrackListCSVExporter;
import de.stationadmin.base.track.exporter.TrackListExcelExporter;
import de.stationadmin.base.track.exporter.TrackListExporter;
import de.stationadmin.base.track.exporter.TrackListTxtExporter;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.track.format.ExtendedTrackFormat.TrackDetailLevel;
import de.stationadmin.base.util.TimeFormat;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.subscriptions.FollowArtistsAction;
import de.stationadmin.gui.tag.TagSetManagerDisplayAction;
import de.stationadmin.gui.track.RegisteredTracksTableModel.Column;
import de.stationadmin.gui.track.RegisteredTracksTableModel.UploadFilter;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.ClipboardAction;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.DateTableCellRenderer;
import de.stationadmin.gui.util.IntTableCellRenderer;
import de.stationadmin.gui.util.SwingTools;

/**
 * @author korf
 * 
 */
public class RegisteredTracksViewer extends JPanel {
  private static final long serialVersionUID = -7189974909473148787L;

  private static final String REGKEY_COLUMNS = "stationadmin.tracks.columns";

  private static final Color OWN_PUBLIC = new Color(240, 240, 240);
  private static final Color OWN_PRIVATE = new Color(200, 200, 200);

  private ClientContext ctx;
  private TextProvider textProvider;
  private TrackService titleService;
  private TagManager titleTagService;
  private ValueModel entryHolder = new ValueHolder();
  private ValueModel tagSetHolder = new ValueHolder();
  private ValueModel tagHolder = new ValueHolder();
  private ValueModel invertTagHolder = new ValueHolder(Boolean.FALSE);
  private ValueModel uploadFilterHolder = new ValueHolder(UploadFilter.ANYBODY);
  private ValueModel numTitles = new ValueHolder(0);
  private ValueModel length = new ValueHolder(0);
  private JXTable table;

  private int visibleColums = 1 << Column.TYPE.ordinal() | 1 << Column.ARTIST.ordinal() | 1 << Column.TITLE.ordinal() | 1 << Column.ALBUM.ordinal() | 1 << Column.LENGTH.ordinal()
      | 1 << Column.UPLOAD.ordinal() | 1 << Column.NUM_PLAYLISTS.ordinal() | 1 << Column.YEAR.ordinal();

  private RegisteredTracksTableModel tableModel;
  // private boolean allColumnsDisplayed = false;

  public RegisteredTracksViewer(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.titleService = ctx.getAdminClient().getTrackService();
    this.titleTagService = ctx.getAdminClient().getTagManager();
    this.init();
  }

  private void init() {

    int colsPref = Preferences.userRoot().getInt(REGKEY_COLUMNS, -1);
    if (colsPref > 0) {
      this.visibleColums = colsPref;
    }

    this.setLayout(new BorderLayout());
    this.add(this.createTablePanel(), BorderLayout.CENTER);
    this.add(this.createStatusBar(), BorderLayout.SOUTH);

    // store current selection in user preferences
    this.tagSetHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        TagSet set = (TagSet) evt.getNewValue();
        if (set != null) {
          Preferences.userRoot().put("titletagset.default." + ctx.getAdminClient().getStation(), set.getName());
          titleTagService.setCurrentTagSetName(set.getName());
        } else {
          Preferences.userRoot().remove("titletagset.default." + ctx.getAdminClient().getStation());
          titleTagService.setCurrentTagSetName(null);
        }
      }
    });

    Timer timer = new Timer(1000 * 5, new ColumnMonitor());
    timer.setInitialDelay(1000 * 10);
    timer.start();

  }

  @SuppressWarnings(value = { "rawtypes", "unchecked" })
  private JXStatusBar createStatusBar() {
    JXStatusBar statusBar = new JXStatusBar();
    statusBar.setOpaque(false);

    final DefaultComboBoxModel tagSetModel = new DefaultComboBoxModel();
    this.updateTagSetModel(tagSetModel);
    final JComboBox tagSetCmb = new JComboBox(tagSetModel);
    tagSetCmb.setRenderer(new TagSetListCellRenderer(textProvider));
    titleTagService.addPropertyChangeListener("tagSets", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateTagSetModel(tagSetModel);
        if (tagSetHolder.getValue() != tagSetCmb.getSelectedItem()) {
          tagSetCmb.setSelectedItem(tagSetHolder.getValue());
        }
      }
    });

    tagSetCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        tagSetHolder.setValue(tagSetCmb.getSelectedItem());
      }
    });
    JPanel tspanel = new JPanel(new FormLayout("pref", "pref"));
    tspanel.add(tagSetCmb, new CellConstraints(1, 1));
    statusBar.add(tspanel, new JXStatusBar.Constraint());

    final DefaultComboBoxModel tagListModel = new DefaultComboBoxModel();
    this.updateTagModel(tagListModel);
    titleTagService.addPropertyChangeListener("tags", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        updateTagModel(tagListModel);
      }
    });

    final JComboBox tagCmb = new JComboBox(tagListModel);
    tagCmb.setRenderer(new TagSelectionListCellRenderer(textProvider));

    tagCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        tagHolder.setValue(tagCmb.getSelectedItem());
      }
    });

    this.tagHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof List && ((List<?>) evt.getNewValue()).size() == 0) {
          MultiTagSelector selector = new MultiTagSelector(textProvider, titleTagService, tagHolder);
          Point frameLocation = AppUtils.getRootFrame().getLocation();
          Dimension frameDim = AppUtils.getRootFrame().getSize();
          selector.setLocation(frameLocation.x + 10, frameLocation.y + frameDim.height - selector.getHeight() - 10);
          selector.setVisible(true);
        }
      }
    });

    SelectionInList<Boolean> invertSelection = new SelectionInList<Boolean>(new Boolean[] { Boolean.FALSE, Boolean.TRUE }, this.invertTagHolder);
    JComboBox invertCmb = BasicComponentFactory.createComboBox(invertSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -8240631239175188202L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
       *      java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value.equals(Boolean.FALSE)) {
          setText(textProvider.getString("titlelist.show"));
        } else {
          setText(textProvider.getString("titlelist.hide"));
        }
        return comp;
      }

    });

    JPanel panel = new JPanel(new FormLayout("pref,2dlu,pref", "pref"));
    panel.add(tagCmb, new CellConstraints(1, 1));
    panel.add(invertCmb, new CellConstraints(3, 1));

    statusBar.add(panel, new JXStatusBar.Constraint());

    SelectionInList<UploadFilter> uploadedSelection = new SelectionInList<UploadFilter>(UploadFilter.values(), this.uploadFilterHolder);
    JComboBox uploadedCmb = BasicComponentFactory.createComboBox(uploadedSelection, new DefaultListCellRenderer() {
      private static final long serialVersionUID = 9073187826215399831L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList,
       *      java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null || value == UploadFilter.ANYBODY) {
          setText(" ");
        } else if (value.equals(UploadFilter.USER_ALL)) {
          setText(textProvider.getString("titlelist.ownUploads"));
        } else if (value.equals(UploadFilter.USER_PRIVATE)) {
          setText(textProvider.getString("titlelist.ownUploads.private"));
        } else if (value.equals(UploadFilter.USER_PUBLIC)) {
          setText(textProvider.getString("titlelist.ownUploads.public"));
        } else if (value.equals(UploadFilter.FOREIGN)) {
          setText(textProvider.getString("titlelist.foreignUploads"));
        }
        return comp;
      }

    });
    panel = new JPanel(new FormLayout("pref", "pref"));
    panel.add(uploadedCmb, new CellConstraints(1, 1));
    statusBar.add(panel, new JXStatusBar.Constraint());

    JPanel numTitlePanel = new JPanel(new FlowLayout());
    numTitlePanel.add(BasicComponentFactory.createLabel(this.numTitles, NumberFormat.getIntegerInstance()));
    numTitlePanel.add(new JLabel(textProvider.getString("titlelist.numTitles")));
    statusBar.add(numTitlePanel, new JXStatusBar.Constraint());

    JPanel lengthPanel = new JPanel(new FlowLayout());
    final JLabel lengthLabel = new JLabel("0:00");
    lengthPanel.add(lengthLabel);
    statusBar.add(lengthPanel, new JXStatusBar.Constraint());
    this.length.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        lengthLabel.setText(TimeFormat.format((Integer) length.getValue(), true));
      }

    });

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);

    toolbar.add(new TagSetManagerDisplayAction(ctx));
    toolbar.addSeparator();

    final JPopupMenu exportPopup = new JPopupMenu();
    exportPopup.add(new ExportTracksAction(new FileNameExtensionFilter("CSV", "csv"), "csv", new TrackListCSVExporter()));
    exportPopup
        .add(new ExportTracksAction(new FileNameExtensionFilter("Excel", "xls"), "xls", new TrackListExcelExporter(ctx.getAdminClient().getTrackService().getTrackRegistry())));
    exportPopup.add(new ExportTracksAction(new FileNameExtensionFilter("Text", "txt"), "txt", new TrackListTxtExporter()));

    final JButton exportBtn = new JButton(ctx.getIcon("playlist_export.png"));
    exportBtn.setToolTipText(textProvider.getString("titlelist.action.export.tooltip"));
    toolbar.add(exportBtn);
    exportBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        exportPopup.show(exportBtn, 0, -exportPopup.getPreferredSize().height);
      }
    });

    statusBar.add(toolbar);

    return statusBar;
  }

  @SuppressWarnings(value = { "unchecked", "rawtypes" })
  private void updateTagModel(DefaultComboBoxModel model) {
    model.removeAllElements();
    model.addElement(null);
    model.addElement(TagManager.USED_TITLES);
    model.addElement(TagManager.UNUSED_TITLES);
    for (String tag : this.titleTagService.getTags()) {
      model.addElement(tag);
    }
    model.addElement(new ArrayList<String>());
    model.addElement(RegisteredTracksTableModel.TAGGED_TITLES);

  }

  @SuppressWarnings(value = { "unchecked", "rawtypes" })
  private void updateTagSetModel(DefaultComboBoxModel model) {
    boolean firstInit = model.getSize() <= 1;
    model.removeAllElements();
    model.addElement(null);
    Object previous = this.tagSetHolder.getValue();
    String defaultTagSetName = Preferences.userRoot().get("titletagset.default." + ctx.getAdminClient().getStation(), null);
    this.titleTagService.setCurrentTagSetName(defaultTagSetName);
    TagSet defaultTagSet = null;
    for (TagSet set : this.titleTagService.getTagSets()) {
      model.addElement(set);
      if (defaultTagSetName != null && set.getName().equals(defaultTagSetName)) {
        defaultTagSet = set;
      }
    }
    if (firstInit) {
      previous = defaultTagSet;
    }
    if (previous != null && model.getIndexOf(previous) > -1) {
      this.tagSetHolder.setValue(previous);
    }
  }

  private JComponent createTablePanel() {
    this.tableModel = new RegisteredTracksTableModel(this.textProvider, this.titleService.getTrackRegistry(), this.titleTagService, this.tagSetHolder, this.tagHolder,
        this.invertTagHolder, this.uploadFilterHolder);
    tableModel.setNumTracks(numTitles);
    tableModel.setLength(this.length);
    table = new JXTable(tableModel) {
      private static final long serialVersionUID = -4365217830331156493L;
      private TrackTypeRenderer typeRenderer = new TrackTypeRenderer();
      private IntTableCellRenderer yearRenderer = new IntTableCellRenderer(0);
      private IntTableCellRenderer idRenderer = new IntTableCellRenderer(null);
      private PlaylistStatisticsCellRenderer playlistsRenderer = new PlaylistStatisticsCellRenderer();
      private DateTableCellRenderer uploadDateRenderer = new DateTableCellRenderer(new SimpleDateFormat(textProvider.getString("timeFormat")));

      /**
       * @see org.jdesktop.swingx.JXTable#getCellRenderer(int, int)
       */
      @Override
      public TableCellRenderer getCellRenderer(int row, int column) {
        int columnModel = this.convertColumnIndexToModel(column);
        if (columnModel == Column.TYPE.ordinal()) {
          return typeRenderer;
        }
        if (columnModel == Column.YEAR.ordinal()) {
          return yearRenderer;
        }
        if (columnModel == Column.UPLOAD.ordinal()) {
          return uploadDateRenderer;
        }
        if (columnModel == Column.NUM_PLAYLISTS.ordinal()) {
          return playlistsRenderer;
        }
        return super.getCellRenderer(row, column);
      }

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
    int timeWidth = ComponentFactory.getTableColumnWidthTime();
    int dateWidth = ComponentFactory.getTableFontWidth(17);
    int yearWidth = ComponentFactory.getTableFontWidth(6);
    int numPlaylistsWidth = ComponentFactory.getTableFontWidth(8);
    
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.LENGTH.ordinal())).setPreferredWidth(timeWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.LENGTH.ordinal())).setMaxWidth(timeWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.NUM_PLAYLISTS.ordinal())).setPreferredWidth(numPlaylistsWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.NUM_PLAYLISTS.ordinal())).setMaxWidth(numPlaylistsWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.TYPE.ordinal())).setPreferredWidth(30);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.TYPE.ordinal())).setMaxWidth(30);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.UPLOAD.ordinal())).setPreferredWidth(dateWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.UPLOAD.ordinal())).setMaxWidth(dateWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.YEAR.ordinal())).setPreferredWidth(yearWidth);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.YEAR.ordinal())).setMaxWidth(yearWidth);

    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.ID.ordinal())).setPreferredWidth(80);
    table.getColumnModel().getColumn(table.convertColumnIndexToView(Column.ID.ordinal())).setMaxWidth(80);

    for (int i = 0; i <= Column.TAGS.ordinal(); i++) {
      boolean visible = (this.visibleColums & (1 << i)) > 0;
      ((TableColumnExt) table.getColumnModel().getColumn(table.convertColumnIndexToView(i))).setVisible(visible);
    }
    table.setColumnControlVisible(true);

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component component, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        int col = table.convertColumnIndexToModel(adapter.column);
        if (col == 0) {
          RegisteredTrack title = ((RegisteredTracksTableModel) table.getModel()).getTrackAt(row);
          if (title.isPrivateTrack()) {
            component.setBackground(OWN_PRIVATE);
          } else if (title.isOwnTrack()) {
            component.setBackground(OWN_PUBLIC);
          }
        }
        return component;
      }

    });

    final JPopupMenu popup = new JPopupMenu();
    final TagMenu tagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), true);
    final TagMenu untagMenu = new TagMenu(this.ctx.getTextProvider(), ctx.getAdminClient().getTagManager(), false);
    final CopyTracksAction copyAction = new CopyTracksAction(this.ctx);
    final DistributeTracksAction distributeAction = new DistributeTracksAction(this.ctx);
    final FollowArtistsAction followAction = new FollowArtistsAction(this.ctx);
    final RemoveTracksFromPlaylistsAction removeTracksAction = new RemoveTracksFromPlaylistsAction(this.ctx);
    final ClearTagsAction clearTagsAction = new ClearTagsAction(ctx);
    final TracksDeleteAction deleteAction = new TracksDeleteAction(ctx);
    final TracksReloadAction reloadAction = new TracksReloadAction(ctx);
    final TrackViewAction viewAction = new TrackViewAction(ctx);
    final TrackMultiEditAction multiEditAction = new TrackMultiEditAction(ctx);
    popup.add(new ClipboardAction(ctx, table, this.entryHolder, TransferHandler.getCopyAction()));
    popup.addSeparator();
    popup.add(tagMenu);
    popup.add(untagMenu);
    popup.addSeparator();
    popup.add(copyAction);
    popup.add(distributeAction);
    popup.add(followAction);
    popup.addSeparator();
    popup.add(removeTracksAction);
    popup.add(clearTagsAction);
    popup.add(deleteAction);
    popup.add(reloadAction);
    popup.addSeparator();
    popup.add(multiEditAction);
    popup.add(viewAction);
    popup.add(new PlaySnippetAction(this.ctx, this.entryHolder));

    table.setTransferHandler(new TransferHandler() {
      private static final long serialVersionUID = 8486946874778173755L;
      private ExtendedTrackFormat exportFormat = new ExtendedTrackFormat(TrackDetailLevel.ENHANCED);

      /**
       * @see javax.swing.TransferHandler#exportToClipboard(javax.swing.JComponent,
       *      java.awt.datatransfer.Clipboard, int)
       */
      @Override
      public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
        String str = this.getSelectionAsString();
        if (str != null) {
          StringSelection string = new StringSelection(str);
          clip.setContents(string, string);
        }
      }

      private String getSelectionAsString() {
        int[] rows = table.getSelectedRows();
        if (rows.length > 0) {
          StringBuffer buf = new StringBuffer();
          for (int i = 0; i < rows.length; i++) {
            int row = table.convertRowIndexToModel(rows[i]);
            BasicTrack track = tableModel.getTrackAt(row);
            if (track != null) {
              buf.append(exportFormat.toString(track));
              buf.append('\n');
            }
          }
          return buf.toString();
        }
        return null;

      }

      /*
       * (non-Javadoc)
       * 
       * @see javax.swing.TransferHandler#createTransferable(javax.swing.JComponent)
       */
      @Override
      protected Transferable createTransferable(JComponent c) {
        String string = this.getSelectionAsString();
        if (string != null) {
          return new StringSelection(string);
        } else {
          return null;
        }
      }

      public boolean canImport(TransferSupport support) {
        return false;
      }

      @Override
      public int getSourceActions(JComponent c) {
        return COPY;
      }

    });
    table.setDragEnabled(true);

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
            RegisteredTrack title = tableModel.getTrackAt(row);
            if (title != null) {
              DetailedTrack dtitle = title;
              if (!title.isOwnTrack()) {
                try {
                  dtitle = ctx.getAdminClient().getTrackService().getTrack(title.getId());
                } catch (Exception ex) {
                }
              }
              TrackViewer viewer = new TrackViewer(ctx, dtitle != null ? dtitle : title, title.getPlaylistIds());
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
		SwingTools.bindPopup(table, popup);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int[] rows = table.getSelectedRows();
          int[] trackIds = new int[rows.length];
          List<BasicTrack> entries = new ArrayList<BasicTrack>();
          for (int i = 0; i < rows.length; i++) {
            int row = table.convertRowIndexToModel(rows[i]);
            entries.add(tableModel.getTrackAt(row));
            trackIds[i] = tableModel.getTrackAt(row).getId();
          }
          entryHolder.setValue(entries);
          tagMenu.setTitleIds(trackIds);
          untagMenu.setTitleIds(trackIds);
          copyAction.setTitles(entries);
          distributeAction.setTitles(entries);
          removeTracksAction.setTracks(entries);
          clearTagsAction.setTracks(entries);
          deleteAction.setTracks(entries);
          reloadAction.setTracks(entries);
          viewAction.setTitles(entries);
          followAction.setTracks(entries);
          multiEditAction.setTracks(entries);
        }
      }

    });

    JPanel container = new JPanel(new BorderLayout());
    container.add(new JScrollPane(table), BorderLayout.CENTER);
    container.add(this.createLegend(), BorderLayout.SOUTH);

    return container;

  }

  private JPanel createLegend() {
    StringBuilder colSpec = new StringBuilder();
    colSpec.append("5dlu,");
    colSpec.append("pref,3dlu,pref,5dlu,");
    colSpec.append("pref,3dlu,pref,5dlu,");
    colSpec.append("pref,3dlu,pref,10dlu,");
    colSpec.append("pref,3dlu,pref,5dlu,");
    colSpec.append("pref,3dlu,pref,5dlu,");
    colSpec.append("pref,3dlu,pref");

    FormLayout layout = new FormLayout(colSpec.toString(), "3dlu,pref,3dlu");
    // layout.setColumnGroups(new int[][] { { 4, 8, 12 }, { 16, 20, 24 } });

    JPanel panel = new JPanel(layout);
    CellConstraints cc = new CellConstraints();
    int col = 2;

    TrackTypeRenderer iconRenderer = new TrackTypeRenderer();

    for (int i = BasicTrack.TYPE_MUSIC; i <= BasicTrack.TYPE_WORD; i++) {
      panel.add(new JLabel(iconRenderer.getIcons()[i]), cc.xy(col, 2));
      panel.add(new JLabel(textProvider.getString("title.type." + i)), cc.xy(col + 2, 2));
      col += 4;
    }

    {
      JPanel color = new JPanel();
      color.setOpaque(true);
      color.setBackground(Color.WHITE);
      color.setMinimumSize(new Dimension(20, 20));
      color.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

      panel.add(color, cc.xy(col, 2));
      panel.add(new JLabel(textProvider.getString("titlelist.legend.foreignUploads")), cc.xy(col + 2, 2));
      col += 4;
    }

    {
      JPanel color = new JPanel();
      color.setOpaque(true);
      color.setBackground(OWN_PUBLIC);
      color.setMinimumSize(new Dimension(20, 20));
      color.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

      panel.add(color, cc.xy(col, 2));
      panel.add(new JLabel(textProvider.getString("titlelist.legend.ownUploads.public")), cc.xy(col + 2, 2));
      col += 4;
    }

    {
      JPanel color = new JPanel();
      color.setOpaque(true);
      color.setBackground(OWN_PRIVATE);
      color.setMinimumSize(new Dimension(20, 20));
      color.setBorder(BorderFactory.createLineBorder(Color.DARK_GRAY, 1));

      panel.add(color, cc.xy(col, 2));
      panel.add(new JLabel(textProvider.getString("titlelist.legend.ownUploads.private")), cc.xy(col + 2, 2));
      col += 4;
    }

    panel.setBorder(BorderFactory.createEtchedBorder());
    return panel;
  }

  private static class PlaylistStatisticsCellRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = -8156594878657506794L;

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      String text = null;
      if (value instanceof PlaylistStatistics) {
        PlaylistStatistics stats = (PlaylistStatistics) value;
        text = stats.toString();
        if (stats.getNumberOfPlaylistsOnline() == 0 && stats.getNumberOfPlaylistsTotal() > 0) {
          this.setIcon(AppUtils.getIcon("warning.png"));
        } else {
          this.setIcon(null);
        }
      }

      setHorizontalAlignment(JLabel.RIGHT);
      setText(text);
      return this;
    }

  }

  private class ExportTracksAction extends AbstractAction {
    private static final long serialVersionUID = 5960764787462220006L;
    private String format;
    private FileFilter filter;
    private TrackListExporter exporter;

    ExportTracksAction(FileFilter filter, String format, TrackListExporter exporter) {
      super(textProvider.getString("titlelist.action.export." + format + ".name"));
      this.filter = filter;
      this.format = format;
      this.exporter = exporter;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      JFileChooser fileChooser = new JFileChooser();
      fileChooser.setFileFilter(this.filter);

      String name;
      if (tagHolder.getValue() instanceof String) {
        name = (String) tagHolder.getValue();
      } else {
        name = "Titel";
      }

      fileChooser.setSelectedFile(new File(fileChooser.getCurrentDirectory().getAbsolutePath() + File.separatorChar + name + "." + format));

      if (fileChooser.showSaveDialog(null) == JFileChooser.APPROVE_OPTION) {
        try {
          exporter.toFile(tableModel.getTracks(), fileChooser.getSelectedFile());
        } catch (Exception ex) {
          JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(ex, "titles.action.export.msg.failed"));
        }

      }

    }

  }

  private class ColumnMonitor implements ActionListener {

    @Override
    public void actionPerformed(ActionEvent e) {

      int visibleCols = 0;
      for (int i = 0; i <= Column.TAGS.ordinal(); i++) {
        int index = table.convertColumnIndexToView(i);
        if (index > -1) {
          boolean visible = ((TableColumnExt) table.getColumnModel().getColumn(index)).isVisible();
          if (visible) {
            visibleCols |= (1 << i);
          }
        }
      }
      if (visibleColums != visibleCols) {
        Preferences.userRoot().putInt(REGKEY_COLUMNS, visibleCols);
        visibleColums = visibleCols;
      }
    }

  }

}
