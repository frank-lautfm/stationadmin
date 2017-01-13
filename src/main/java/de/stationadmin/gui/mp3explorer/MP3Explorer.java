/**
 * 
 */
package de.stationadmin.gui.mp3explorer;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Vector;
import java.util.prefs.Preferences;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.JToolBar;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.DefaultTableCellRenderer;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;
import com.l2fprod.common.swing.JDirectoryChooser;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.trackimport.MP3TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.Tag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.tag.TagNameComparator;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.TextProvider;
import de.stationadmin.gui.mp3explorer.MP3DirectoryTableModel.Column;
import de.stationadmin.gui.track.SearchPanel;
import de.stationadmin.gui.upload.UploadWindow;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.MP3Launcher;
import de.stationadmin.gui.util.SwingTools;

/**
 * Browser for mp3 files on local hard disk. Displays playlists and title tags
 * of selected file.
 * 
 * @author Frank Korf
 */
public class MP3Explorer extends JFrame {
  private static final long serialVersionUID = 9151876964742449054L;
  private ClientContext ctx;
  private TextProvider textProvider;
  private ValueHolder mp3Holder = new ValueHolder();
  private TrackImportHandler importHandler;
  private MP3DirectoryTableModel mp3TableModel;
  private MP3Launcher mp3Launcher;

  /**
   * map of files that have been tagged by this MP3 Explorer instance - tag as
   * key, set of files as value
   */
  private Map<String, Set<File>> taggedFiles = new HashMap<String, Set<File>>();

  public MP3Explorer(ClientContext ctx) {
    this.ctx = ctx;
    this.textProvider = ctx.getTextProvider();
    this.mp3Launcher = new MP3Launcher(ctx);
    this.mp3TableModel = new MP3DirectoryTableModel(ctx.getTextProvider(), ctx.getAdminClient().getTrackService(), ctx.getAdminClient()
        .getTagManager());
    this.importHandler = new TrackImportHandler(ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager(), new Playlist(ctx
        .getAdminClient().getTrackService().getTrackRegistry(), PlaylistType.TEMPORARY), 0);
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(this.createSelectorPanel(), BorderLayout.NORTH);
    this.getContentPane().add(this.createDetailsPanel(), BorderLayout.CENTER);
    this.initMenu();

    this.setSize(800, 700);
    this.setTitle("MP3 Explorer");

    SwingTools.centerOnScreen(this);
  }

  private void initMenu() {
    JMenuBar menuBar = new JMenuBar();
    JMenu menu = new JMenu(textProvider.getString("mp3explorer.menu.options"));

    // include subdirectories
    {
      JCheckBoxMenuItem recursiveOption = new JCheckBoxMenuItem(new ChangeRecursiveOptionAction());
      recursiveOption.setSelected(this.mp3TableModel.isRecursive());
      menu.add(recursiveOption);
    }

    menuBar.add(menu);
    this.setJMenuBar(menuBar);

  }

  private JPanel createSelectorPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref,2dlu,pref:grow,3dlu", "200dlu:grow"));
    CellConstraints cc = new CellConstraints();

    this.mp3TableModel = new MP3DirectoryTableModel(ctx.getTextProvider(), ctx.getAdminClient().getTrackService(), ctx.getAdminClient()
        .getTagManager());
    this.mp3TableModel.setMaxTitles(this.ctx.getAdminClient().getSettings().getMp3ExplorerMaxFiles());

    String last = ctx.getAdminClient().getSettings().getMp3Root();
    if (last == null) {
      last = Preferences.userRoot().get("mp3explorer.last", null);
    }
    File lastFile = null;
    if (last != null) {
      lastFile = new File(last);
      if (!lastFile.exists()) {
        lastFile = null;
      } else {
        try {
          mp3TableModel.setDirectories(new File[] { lastFile });
        } catch (TooManyTitlesException e) {
          // ignore here
        }
      }
    }

    JDirectoryChooser chooser = new JDirectoryChooser(lastFile);
    chooser.setControlButtonsAreShown(false);
    chooser.setMultiSelectionEnabled(true);

    chooser.addPropertyChangeListener(new PropertyChangeListener() {
      public void propertyChange(PropertyChangeEvent evt) {
        String prop = evt.getPropertyName();
        if (JFileChooser.SELECTED_FILES_CHANGED_PROPERTY.equals(prop)) {
          File[] files = (File[]) evt.getNewValue();
          MP3Explorer.this.getGlassPane().setCursor(new Cursor(Cursor.WAIT_CURSOR));
          try {
            mp3TableModel.setDirectories(files);
          } catch (TooManyTitlesException e) {
            displayTooManyTitles(e);
          } finally {
            MP3Explorer.this.getGlassPane().setCursor(new Cursor(Cursor.DEFAULT_CURSOR));
          }
          try {
            if (files.length > 0) {
              Preferences.userRoot().put("mp3explorer.last", files[0].getAbsolutePath());
            }
          } catch (Exception e) {
          }
        }

      };
    });
    panel.add(chooser, cc.xy(2, 1));

    final JXTable table = new JXTable(mp3TableModel);
    table.getColumn(Column.TRACKNO.ordinal()).setPreferredWidth(40);
    table.getColumn(Column.TRACKNO.ordinal()).setMaxWidth(40);
    table.getColumn(Column.SIZE.ordinal()).setPreferredWidth(60);
    table.getColumn(Column.SIZE.ordinal()).setMaxWidth(60);
    table.getColumn(Column.SIZE.ordinal()).setCellRenderer(new SizeRenderer());
    table.getColumnExt(Column.SIZE.ordinal()).setVisible(false);
    table.setSortable(true);
    table.setColumnControlVisible(true);

    table.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        int row = table.convertRowIndexToModel(adapter.row);
        MP3File file = mp3TableModel.getFileAt(row);
        if (file != null && file.getStatus() == TrackStatus.IN_LOCAL_POOL) {
          comp.setFont(ComponentFactory.boldLabelFont);
        }
        return comp;
      }
    });

    final JPopupMenu popup = new JPopupMenu();
    popup.add(new PlayTitlesAction(table));
    popup.add(new UploadTitlesAction(table));
    popup.addSeparator();
    popup.add(new SelectAllTitlesAction(table));
    popup.add(new SelectUnusedTitlesAction(table));

    table.addMouseListener(new MouseAdapter() {

      private void showPopup(MouseEvent evt) {
        if (evt.isPopupTrigger()) {
          popup.show(table, evt.getX(), evt.getY());
        }
      }

      @Override
      public void mouseClicked(MouseEvent e) {
        this.showPopup(e);
      }

      @Override
      public void mousePressed(MouseEvent e) {
        this.showPopup(e);
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        this.showPopup(e);
      }
    });

    JPanel outer = new JPanel(new BorderLayout());
    outer.add(new JScrollPane(table), BorderLayout.CENTER);

    table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          int row = table.getSelectedRow();
          if (row > -1) {
            row = table.convertRowIndexToModel(row);
            mp3Holder.setValue(mp3TableModel.getFileAt(row));
          } else {
            mp3Holder.setValue(null);
          }
        }
      }

    });

    panel.add(outer, cc.xy(4, 1));

    return panel;
  }

  private void displayTooManyTitles(TooManyTitlesException e) {
    ErrorInfo info = textProvider.createErrorInfo(e, "mp3explorer.msg.toomanytitles", Integer.toString(mp3TableModel.getMaxTitles()));
    JXErrorPane.showDialog(this, info);
  }

  private JPanel createDetailsPanel() {
    FormLayout layout = new FormLayout("3dlu,pref:grow,3dlu,pref:grow,3dlu", "5dlu,pref,5dlu,pref:grow,5dlu");
    layout.setColumnGroups(new int[][] { { 2, 4 } });
    JPanel panel = new JPanel(layout);
    CellConstraints cc = new CellConstraints();

    final PresentationModel<TrackModel> pm = new PresentationModel<TrackModel>((TrackModel) null);
    mp3Holder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() != null) {
          TrackModel tm = new TrackModel((MP3File) evt.getNewValue());
          importHandler.clear();
          if (tm.getFile().getTag() != null) {
            importHandler.add(new MP3TrackImportTask(tm.getFile().getFile(), tm.getFile().getTag()));
            importHandler.resolveTags();
            importHandler.resolveTracksLocal();
            if (importHandler.isEverythingResolved()) {
              tm.setStatus(TrackStatus.IN_LOCAL_POOL);
              tm.setTitle(importHandler.getTasks().get(0).getTrackLibraryTitle());
            } else {
              tm.setStatus(TrackStatus.UNRESOLVED);
            }
          } else {
            tm.setStatus(TrackStatus.UNRESOLVED);
          }
          pm.setBean(tm);
        } else {
          pm.setBean(null);
        }
      }

    });

    panel.add(this.createTitleInfoPanel(pm), cc.xywh(2, 2, 3, 1, CellConstraints.FILL, CellConstraints.FILL));
    panel.add(this.createPlaylistPanel(pm), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));
    panel.add(this.createTagsPanel(pm), cc.xy(4, 4, CellConstraints.FILL, CellConstraints.FILL));

    return panel;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JPanel createPlaylistPanel(final PresentationModel<TrackModel> model) {
    List<Playlist> playlists = this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
    Collections.sort(playlists, new PlaylistNameCompator());
    final JPanel outerPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu,", "3dlu,20dlu:grow,3dlu,pref,3dlu,"));
    CellConstraints cc = new CellConstraints();
    // outerPanel.setBorder(BorderFactory.createTitledBorder("Playlists"));
    final JPanel panel = new JPanel();
    panel.setBackground(Color.WHITE);
    panel.setOpaque(true);

    ActionListener playlistChangeListener = new PlaylistChangeListener(model);
    final List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("pref,1dlu,");
    for (Playlist playlist : playlists) {
      JCheckBox checkBox = new JCheckBox(playlist.getName());
      checkBox.putClientProperty("playlist", playlist);
      if (playlist.getTags() != null) {
        checkBox.putClientProperty("tags", playlist.getTags());
      }
      checkBox.addActionListener(playlistChangeListener);
      checkBox.setEnabled(false);
      checkBox.setBackground(Color.WHITE);
      checkBoxes.add(checkBox);
      if (checkBoxes.size() % 2 == 0) {
        rowSpec.append("pref,1dlu,");
      }
    }
    FormLayout layout = new FormLayout("pref:grow,2dlu,pref:grow", rowSpec.toString());
    layout.setColumnGroups(new int[][] { { 1, 3 } });
    panel.setLayout(layout);
    for (int i = 0; i < checkBoxes.size(); i++) {
      panel.add(checkBoxes.get(i), getCC(i, 2));
    }

    JScrollPane scroll = new JScrollPane(panel);
    outerPanel.add(scroll, new CellConstraints(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    PropertyChangeListener checkBoxUpdater = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (model.getBean() != null && model.getBean().getTitle() != null) {
          for (JCheckBox checkBox : checkBoxes) {
            Playlist playlist = (Playlist) checkBox.getClientProperty("playlist");
            checkBox.setSelected(playlist.containsTitle(model.getBean().getTitle().getId()));
            checkBox.setEnabled(true);
          }
        } else {
          for (JCheckBox checkBox : checkBoxes) {
            checkBox.setSelected(false);
            checkBox.setEnabled(false);
          }
        }
      }

    };

    model.getModel("name").addValueChangeListener(checkBoxUpdater);
    model.getModel("status").addValueChangeListener(checkBoxUpdater);

    Vector<String> tagOptions = new Vector<String>();
    tagOptions.add(null);
    List<String> usedTags = new ArrayList<String>(ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getUsedTags());
    Collections.sort(usedTags);
    tagOptions.addAll(usedTags);
    final JComboBox groupCmb = new JComboBox(tagOptions);
    groupCmb.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = -4878016871369214384L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(ctx.getString("playlistselector.all"));
        }
        return c;
      }

    });

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 3));
    bottomPanel.add(groupCmb);

    outerPanel.add(bottomPanel, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    groupCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        String tag = (String) groupCmb.getSelectedItem();
        panel.removeAll();
        int idx = 0;
        for (JCheckBox cb : checkBoxes) {
          boolean accept = tag == null;
          if (!accept) {
            Set<String> tags = (Set<String>) cb.getClientProperty("tags");
            accept = (tags != null && tags.contains(tag));
          }
          if (accept) {
            panel.add(cb, getCC(idx, 2));
            idx++;
          }
        }
        MP3Explorer.this.validate();
        MP3Explorer.this.repaint();
      }
    });

    return outerPanel;
  }

  static CellConstraints getCC(int idx, int numCols) {
    int col = ((idx % numCols) + 1) * 2 - 1;
    int row = ((idx / numCols) + 1) * 2 - 1;
    return new CellConstraints(col, row);
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JPanel createTagsPanel(final PresentationModel<TrackModel> model) {
    final TagManager tagManager = this.ctx.getAdminClient().getTagManager();

    List<StaticTag> tags = tagManager.getStaticTags();
    Collections.sort(tags, new TagNameComparator());
    final JPanel outerPanel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu,", "3dlu,20dlu:grow,3dlu,pref,3dlu,"));
    CellConstraints cc = new CellConstraints();
    // outerPanel.setBorder(BorderFactory.createTitledBorder("Tags"));

    final JPanel panel = new JPanel(new GridLayout(0, 2, 5, 5));
    panel.setBackground(Color.WHITE);
    panel.setOpaque(true);

    ActionListener tagChangeListener = new TagChangeListener(model);
    final List<JCheckBox> checkBoxes = new ArrayList<JCheckBox>();

    StringBuilder rowSpec = new StringBuilder();
    rowSpec.append("pref,1dlu,");
    for (Tag tag : tags) {
      JCheckBox checkBox = new JCheckBox(tag.getName());
      checkBox.setBackground(Color.WHITE);
      checkBox.putClientProperty("tag", tag.getName());
      checkBox.putClientProperty("group", tag.getGroup());
      checkBox.addActionListener(tagChangeListener);
      checkBox.setEnabled(false);
      checkBoxes.add(checkBox);
      if (checkBoxes.size() % 2 == 0) {
        rowSpec.append("pref,1dlu,");
      }
    }
    FormLayout layout = new FormLayout("pref:grow,2dlu,pref:grow", rowSpec.toString());
    layout.setColumnGroups(new int[][] { { 1, 3 } });
    panel.setLayout(layout);
    for (int i = 0; i < checkBoxes.size(); i++) {
      panel.add(checkBoxes.get(i), getCC(i, 2));
    }

    // panel.setPreferredSize(new Dimension(100, 100));

    JScrollPane scroll = new JScrollPane(panel);
    outerPanel.add(scroll, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    PropertyChangeListener checkBoxUpdater = new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (model.getBean() != null && model.getBean().getTitle() != null) {
          for (JCheckBox checkBox : checkBoxes) {
            String tag = (String) checkBox.getClientProperty("tag");
            try {
              checkBox.setSelected(tagManager.isTagged(tag, model.getBean().getTitle().getId()));
            } catch (IOException e) {
              checkBox.setSelected(false);
            }
            checkBox.setEnabled(true);
          }
        } else {
          for (JCheckBox checkBox : checkBoxes) {
            checkBox.setSelected(false);
            checkBox.setEnabled(false);
          }
        }
      }

    };

    model.getModel("name").addValueChangeListener(checkBoxUpdater);
    model.getModel("status").addValueChangeListener(checkBoxUpdater);

    outerPanel.addMouseListener(new MouseAdapter() {

      @Override
      public void mouseClicked(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showPopup(e);
        }
      }

      @Override
      public void mousePressed(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showPopup(e);
        }
      }

      @Override
      public void mouseReleased(MouseEvent e) {
        if (e.isPopupTrigger()) {
          showPopup(e);
        }
      }

      void showPopup(MouseEvent e) {
        JPopupMenu menu = new JPopupMenu();
        menu.add(new SaveTaggedFiles());
        menu.show(outerPanel, e.getX(), e.getY());
      }
    });

    Vector<String> groupOptions = new Vector<String>();
    groupOptions.add(null);
    groupOptions.add("");
    groupOptions.addAll(tagManager.getGroups(true));
    final JComboBox groupCmb = new JComboBox(groupOptions);
    groupCmb.setRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = -4878016871369214384L;

      @Override
      public Component getListCellRendererComponent(JList<?> list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component c = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(ctx.getString("tagselector.all"));
        } else if (value.equals("")) {
          setText(ctx.getString("tagselector.main"));
        }
        return c;
      }

    });

    JPanel bottomPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 3, 3));
    bottomPanel.add(groupCmb);

    outerPanel.add(bottomPanel, cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    groupCmb.addItemListener(new ItemListener() {

      @Override
      public void itemStateChanged(ItemEvent e) {
        String group = (String) groupCmb.getSelectedItem();
        panel.removeAll();
        int idx = 0;
        for (JCheckBox cb : checkBoxes) {
          boolean accept = group == null;
          if (!accept) {
            String cbGroup = (String) cb.getClientProperty("group");
            accept = (cbGroup == null && group.equals("")) || StringUtils.equals(cbGroup, group);
          }
          if (accept) {
            panel.add(cb, getCC(idx, 2));
            idx++;
          }
        }
        MP3Explorer.this.validate();
        MP3Explorer.this.repaint();
      }
    });

    return outerPanel;
  }

  private JPanel createTitleInfoPanel(final PresentationModel<TrackModel> model) {

    JPanel panel = new JPanel(new FormLayout("3dlu,pref,3dlu,pref,5dlu:grow,max(pref;80px),3dlu", "3dlu,pref,3dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();

    panel.add(new JLabel(textProvider.getString("mp3explorer.property.status") + ":"), cc.xy(2, 4));
    final JLabel statusLabel = new JLabel("");
    model.getModel("status").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        TrackStatus status = (TrackStatus) evt.getNewValue();
        statusLabel.setText(status != null ? textProvider.getString("mp3explorer.property.status." + status.name().toLowerCase()) : "");
      }

    });
    panel.add(statusLabel, cc.xy(4, 4));

    final JLabel titleLabel = new JLabel();

    panel.add(new JLabel(textProvider.getString("trackviewer.property.title") + ":"), cc.xy(2, 2));
    panel.add(titleLabel, cc.xy(4, 2));

    model.getModel("title").addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        BasicTrack t = model.getBean() != null ? (BasicTrack) model.getBean().getTitle() : null;
        if (t != null) {
          String text = model.getBean().getArtist() + " - " + model.getBean().getName();
          if (model.getBean().getAlbum() != null) {
            text += " (" + model.getBean().getAlbum() + ")";
          }
          titleLabel.setText(text);
        } else {
          titleLabel.setText("");
        }
      }
    });

    final PlayAction playAction = new PlayAction();
    final SearchAction searchAction = new SearchAction();
    final UploadAction uploadAction = new UploadAction();

    model.getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        playAction.setTitleModel((TrackModel) evt.getNewValue());
        searchAction.setTitleModel((TrackModel) evt.getNewValue());
        uploadAction.setTrackModel((TrackModel) evt.getNewValue());
      }
    });

    JToolBar toolbar = new JToolBar();
    toolbar.add(playAction);
    toolbar.addSeparator();
    toolbar.add(searchAction);
    toolbar.add(uploadAction);
    toolbar.setFloatable(false);

    panel.add(toolbar, cc.xywh(6, 2, 1, 3, CellConstraints.CENTER, CellConstraints.CENTER));
    panel.setBorder(BorderFactory.createTitledBorder(""));

    return panel;
  }

  private class TagChangeListener implements ActionListener {
    private PresentationModel<TrackModel> presentationModel;

    public TagChangeListener(PresentationModel<TrackModel> presentationModel) {
      super();
      this.presentationModel = presentationModel;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      TrackModel model = presentationModel.getBean() != null ? presentationModel.getBean() : null;
      if (e.getSource() instanceof JCheckBox && model != null && model.getTitle() != null) {
        TagManager tagManager = ctx.getAdminClient().getTagManager();
        String tag = (String) ((JCheckBox) e.getSource()).getClientProperty("tag");
        boolean selected = ((JCheckBox) e.getSource()).isSelected();
        if (tag != null) {
          Set<File> files = taggedFiles.get(tag);
          try {
            if (selected) {
              tagManager.tagTracks(tag, model.getTitle().getId());
              if (files == null) {
                files = new HashSet<File>();
                taggedFiles.put(tag, files);
              }
              files.add(model.getFile().getFile());
            } else {
              tagManager.untagTracks(tag, model.getTitle().getId());
              if (files != null) {
                files.remove(model.getFile().getFile());
              }
            }
          } catch (IOException ex) {
            ErrorInfo errorInfo = textProvider.createErrorInfo(ex, "trackviewer.tag.error", tag);
            JXErrorPane.showDialog(ctx.getRootWindow(), errorInfo);
          }
        }
      }

    }

  }

  private class PlaylistChangeListener implements ActionListener {
    private PresentationModel<TrackModel> presentationModel;

    public PlaylistChangeListener(PresentationModel<TrackModel> presentationModel) {
      super();
      this.presentationModel = presentationModel;
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      TrackModel model = presentationModel.getBean() != null ? presentationModel.getBean() : null;
      if (e.getSource() instanceof JCheckBox && model != null && model.getTitle() != null) {
        Playlist playlist = (Playlist) ((JCheckBox) e.getSource()).getClientProperty("playlist");
        boolean selected = ((JCheckBox) e.getSource()).isSelected();
        if (playlist != null) {
          if (selected) {
            playlist.addTrack(model.getTitle());
          } else {
            playlist.removeTrack(model.getTitle().getId());
          }
        }
      }

    }

  }

  class SearchAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = -4065705669359479366L;
    private TrackModel titleModel;

    SearchAction() {
      this.setEnabled(false);
      this.putValue(Action.SMALL_ICON, ctx.getIcon("searching.png"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      final JFrame frame = new JFrame();
      frame.getContentPane().setLayout(new BorderLayout());

      final SearchPanel panel = new SearchPanel(ctx, false);
      panel.getModel().getBean().setTitle(titleModel.getID3Name());

      frame.getContentPane().add(panel, BorderLayout.CENTER);

      JPanel buttonBar = new JPanel(new GridLayout(-1, 2, 10, 10));
      JButton okBtn = new JButton(textProvider.getString("ok"));
      okBtn.addActionListener(new ActionListener() {
        @Override
        @SuppressWarnings("unchecked")
        public void actionPerformed(ActionEvent e) {
          List<BasicTrack> titles = (List<BasicTrack>) panel.getSelectionHolder().getValue();
          if (titles != null && titles.size() > 0) {
            titleModel.setTitle(titles.get(0));
            titleModel.setStatus(TrackStatus.IN_LAUTFM_POOL);
            ctx.getAdminClient().getTrackService().getTrackRegistry()
                .registerAlias(titles.get(0).getId(), titleModel.getID3Artist(), titleModel.getID3Name());
            try {
              ctx.getAdminClient().getTrackService().saveAliases();
            } catch (IOException ex) {
            }
          }
          frame.dispose();
        }

      });
      buttonBar.add(okBtn);
      JButton cancelBtn = new JButton(textProvider.getString("cancel"));
      cancelBtn.addActionListener(new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
          frame.dispose();
        }
      });
      buttonBar.add(cancelBtn);
      frame.getContentPane().add(buttonBar, BorderLayout.SOUTH);

      frame.setSize(500, 400);
      SwingTools.centerWithin(MP3Explorer.this, frame);
      frame.setVisible(true);

    }

    /**
     * @return the titleModel
     */
    public TrackModel getTitleModel() {
      return titleModel;
    }

    /**
     * @param titleModel
     *          the titleModel to set
     */
    public void setTitleModel(TrackModel titleModel) {
      if (this.titleModel != null) {
        this.titleModel.removePropertyChangeListener("status", this);
      }
      this.titleModel = titleModel;
      if (this.titleModel != null) {
        this.titleModel.addPropertyChangeListener("status", this);
      }
      this.checkEnabled();
    }

    private void checkEnabled() {
      this.setEnabled(this.titleModel != null && this.titleModel.getStatus() == TrackStatus.UNRESOLVED);
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      checkEnabled();
    }
  }

  class UploadAction extends AbstractAction implements PropertyChangeListener {
    private static final long serialVersionUID = 2851733588145314338L;
    private TrackModel trackModel;

    UploadAction() {
      this.putValue(Action.SMALL_ICON, ctx.getIcon("upload.png"));
      this.setEnabled(false);
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      UploadWindow win = ctx.getUploadWindowManager().getUploadWindow();
      if (!win.isVisible()) {
        win.setVisible(true);
      }
      win.addFiles(new File[] { trackModel.getFile().getFile() }, false);
      trackModel.setStatus(TrackStatus.UPLOAD);
    }

    /**
     * @return the titleModel
     */
    public TrackModel getTrackModel() {
      return trackModel;
    }

    /**
     * @param titleModel
     *          the titleModel to set
     */
    public void setTrackModel(TrackModel titleModel) {
      if (this.trackModel != null) {
        this.trackModel.removePropertyChangeListener("status", this);
      }
      this.trackModel = titleModel;
      if (this.trackModel != null) {
        this.trackModel.addPropertyChangeListener("status", this);
      }
      this.checkEnabled();
    }

    private void checkEnabled() {
      this.setEnabled(this.trackModel != null && this.trackModel.getStatus() == TrackStatus.UNRESOLVED);
    }

    /**
     * @see java.beans.PropertyChangeListener#propertyChange(java.beans.PropertyChangeEvent)
     */
    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      checkEnabled();
    }
  }

  class PlayAction extends AbstractAction {
    private static final long serialVersionUID = -5761869709815616309L;
    private TrackModel titleModel;

    PlayAction() {
      this.setEnabled(false);
      this.putValue(Action.SMALL_ICON, ctx.getIcon("player_play.png"));
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent evt) {
      mp3Launcher.play(this.titleModel.getFile().getFile());
    }

    /**
     * @return the titleModel
     */
    public TrackModel getTitleModel() {
      return titleModel;
    }

    /**
     * @param titleModel
     *          the titleModel to set
     */
    public void setTitleModel(TrackModel titleModel) {
      this.titleModel = titleModel;
      this.checkEnabled();
    }

    private void checkEnabled() {
      this.setEnabled(mp3Launcher.isAvailable() && this.titleModel != null);
    }
  }

  class SaveTaggedFiles extends AbstractAction {
    private static final long serialVersionUID = -5920939948220781971L;

    SaveTaggedFiles() {
      super("Neue Tags als m3u speichern");
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      if (taggedFiles.size() > 0) {
        JDirectoryChooser chooser = new JDirectoryChooser();
        chooser.setDialogTitle("Verzeichnis für m3u-Playlists");
        if (chooser.showSaveDialog(MP3Explorer.this) == JDirectoryChooser.APPROVE_OPTION) {
          File directory = chooser.getSelectedFile();

          for (String tag : taggedFiles.keySet()) {
            Set<File> files = taggedFiles.get(tag);
            if (files.size() > 0) {
              try {
                FileWriter writer = new FileWriter(directory.getAbsolutePath() + File.separatorChar + tag + ".m3u");
                for (File file : files) {
                  writer.write(file.getAbsolutePath() + "\n");
                }
                writer.close();
              } catch (IOException e) {
                e.printStackTrace();
              }
            }
          }

        }
      }
    }

  }

  class PlayTitlesAction extends AbstractAction {
    private static final long serialVersionUID = 4270861760602991573L;
    private JXTable table;

    /**
     * @param table
     */
    public PlayTitlesAction(JXTable table) {
      super(textProvider.getString("mp3explorer.action.playtitles"));
      this.table = table;
      this.setEnabled(false);
      if (mp3Launcher.isAvailable()) {
        table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

          @Override
          public void valueChanged(ListSelectionEvent evt) {
            setEnabled(PlayTitlesAction.this.table.getSelectedRowCount() > 0);
          }
        });
      }
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      if (table.getSelectedRowCount() > 0) {
        List<String> files = new ArrayList<String>();
        for (int row : table.getSelectedRows()) {
          row = table.convertRowIndexToModel(row);
          MP3File file = ((MP3DirectoryTableModel) table.getModel()).getFileAt(row);
          files.add(file.getFile().getAbsolutePath());
        }
        mp3Launcher.play(files.toArray(new String[files.size()]));
      }
    }
  }

  /**
   * Uploads all titles that are currently selected in the table
   */
  class UploadTitlesAction extends AbstractAction {
    private static final long serialVersionUID = 4270861760602991573L;
    private JXTable table;

    /**
     * @param table
     */
    public UploadTitlesAction(JXTable table) {
      super(textProvider.getString("mp3explorer.action.uploadtitles"));
      this.table = table;
      this.setEnabled(false);
      table.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent evt) {
          setEnabled(UploadTitlesAction.this.table.getSelectedRowCount() > 0);
        }
      });

    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      if (table.getSelectedRowCount() > 0) {
        List<File> files = new ArrayList<File>();
        for (int row : table.getSelectedRows()) {
          row = table.convertRowIndexToModel(row);
          MP3File file = ((MP3DirectoryTableModel) table.getModel()).getFileAt(row);
          files.add(file.getFile());
        }
        UploadWindow win = ctx.getUploadWindowManager().getUploadWindow();
        if (!win.isVisible()) {
          win.setVisible(true);
        }
        win.addFiles(files.toArray(new File[files.size()]), false);
      }
    }
  }

  private class ChangeRecursiveOptionAction extends AbstractAction {
    private static final long serialVersionUID = -8882800436338660185L;

    public ChangeRecursiveOptionAction() {
      super(textProvider.getString("mp3explorer.menu.option.recursive"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {
      JCheckBoxMenuItem item = (JCheckBoxMenuItem) evt.getSource();
      try {
        mp3TableModel.setRecursive(item.isSelected());
        Preferences.userRoot().putBoolean("mp3explorer.recursive", item.isSelected());
      } catch (TooManyTitlesException e) {
        displayTooManyTitles(e);
      }
    }

  }

  private class SelectAllTitlesAction extends AbstractAction {
    private static final long serialVersionUID = -4384045692021700708L;
    JXTable table;

    SelectAllTitlesAction(JXTable table) {
      this.table = table;
      this.putValue(Action.NAME, ctx.getTextProvider().getString("mp3explorer.action.select.all"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      table.getSelectionModel().setSelectionInterval(0, table.getModel().getRowCount() - 1);
    }

  }

  private class SelectUnusedTitlesAction extends AbstractAction {
    private static final long serialVersionUID = 8430287694376561334L;
    JXTable table;

    SelectUnusedTitlesAction(JXTable table) {
      this.table = table;
      this.putValue(Action.NAME, ctx.getTextProvider().getString("mp3explorer.action.select.unused"));
    }

    @Override
    public void actionPerformed(ActionEvent e) {
      MP3DirectoryTableModel model = (MP3DirectoryTableModel) table.getModel();
      table.getSelectionModel().clearSelection();
      for (int i = 0; i < model.getRowCount(); i++) {
        if (model.getFileAt(i).getStatus() != TrackStatus.IN_LOCAL_POOL) {
          int idx = table.convertRowIndexToView(i);
          table.getSelectionModel().addSelectionInterval(idx, idx);
        }
      }

    }

  }

  private static class SizeRenderer extends DefaultTableCellRenderer {
    private static final long serialVersionUID = 8788367178180535238L;
    private NumberFormat fmt;

    SizeRenderer() {
      fmt = NumberFormat.getInstance();
      fmt.setMaximumFractionDigits(1);
    }

    public Component getTableCellRendererComponent(JTable table, Object value, boolean isSelected, boolean hasFocus, int row, int column) {
      Component comp = super.getTableCellRendererComponent(table, value, isSelected, hasFocus, row, column);
      if (value instanceof Float) {
        this.setText(fmt.format((Float) value) + " MB");
      }
      this.setHorizontalAlignment(JLabel.RIGHT);
      return comp;
    }

  }

}
