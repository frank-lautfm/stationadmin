package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.DefaultListCellRenderer;
import javax.swing.JButton;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JToolBar;
import javax.swing.ListModel;
import javax.swing.ListSelectionModel;
import javax.swing.SortOrder;
import javax.swing.TransferHandler;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXList;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;
import org.jdesktop.swingx.search.ListSearchable;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.schedule.Schedule.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.gui.ClientContext;

/**
 * Panel that allows to select a playlist
 * 
 * @author Frank Korf
 */
public class PlaylistSelector extends JPanel {
  private static final long serialVersionUID = -2440928931577319044L;
  private static final String PREFKEY_RENDER_COLORS = "playlistselector.rendercolors";
  private static final String TAG_USED = "#used";
  private static final String TAG_UNUSED = "#unused";

  private ClientContext ctx;
  private PlaylistRegistry playlistRegistry;
  private PlaylistType playlistType;
  private ValueModel playlistSelectionHolder;
  private JXList list;
  private IndirectListModel<Playlist> listModel;
  private IndirectListModel<String> tagModel;
  private ValueModel tagSelection = new ValueHolder(null);
  private boolean updateInProgress = false;
  private PlaylistListCellRenderer renderer;
  private HashSet<Integer> highlightedPlaylists = new HashSet<Integer>();

  /**
   * registered as property change listener for the "modified" flag of playlists - need to repaint the list if this flag changes for a playlist
   */
  private PropertyChangeListener playlistModificationListener = new PropertyChangeListener() {

    @Override
    public void propertyChange(PropertyChangeEvent evt) {
      list.repaint();
    }

  };

  private PropertyChangeListener playlistTagListener = new PropertyChangeListener() {
    public void propertyChange(PropertyChangeEvent evt) {
      refreshTags();
    }
  };

  public PlaylistSelector(ClientContext ctx, PlaylistType playlistType, ValueModel playlistSelectionHolder) {
    this.ctx = ctx;
    this.playlistRegistry = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry();
    this.playlistType = playlistType;
    this.playlistSelectionHolder = playlistSelectionHolder;
    this.init();
  }

  protected PlaylistSelector(PlaylistRegistry playlistRegistry, ValueModel playlistSelectionHolder) {
    super();
  }

  private JPanel createHeadPanel() {
    JPanel panel = new JPanel(new FormLayout("pref,2dlu:grow,pref,2dlu", "2dlu,pref,2dlu"));
    CellConstraints cc = new CellConstraints();

    final JCheckBoxMenuItem colorItem = new JCheckBoxMenuItem(ctx.getString("playlistselector.menuitem.rendercolors"));
    colorItem.setSelected(Preferences.userRoot().getBoolean(PREFKEY_RENDER_COLORS, false));
    colorItem.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        if (renderer instanceof AdvancedPlaylistListCellRender) {
          renderer.setRenderColors(colorItem.isSelected());
          invalidate();
          repaint();
        }
        Preferences.userRoot().putBoolean(PREFKEY_RENDER_COLORS, colorItem.isSelected());
      }
    });

    final JPopupMenu menu = new JPopupMenu();
    menu.add(colorItem);

    JToolBar toolbar = new JToolBar();
    toolbar.setFloatable(false);
    final JButton settingsBtn = new JButton(ctx.getIcon("arrowdown.png"));
    settingsBtn.addActionListener(new ActionListener() {

      @Override
      public void actionPerformed(ActionEvent e) {
        menu.show(settingsBtn, settingsBtn.getWidth() - (int) menu.getPreferredSize().getWidth(), settingsBtn.getHeight());

      }
    });
    toolbar.add(settingsBtn);
    panel.add(toolbar, cc.xy(3, 2));

    ArrayList<String> entries = new ArrayList<String>();
    entries.add(null);
    this.tagModel = new IndirectListModel<String>(entries);
    SelectionInList<String> model = new SelectionInList<String>(this.tagModel, tagSelection);
    JComboBox combo = BasicComponentFactory.createComboBox(model, new DefaultListCellRenderer() {
      private static final long serialVersionUID = -7901905125762119676L;

      /**
       * @see javax.swing.DefaultListCellRenderer#getListCellRendererComponent(javax.swing.JList, java.lang.Object, int, boolean, boolean)
       */
      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected, boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value == null) {
          setText(ctx.getTextProvider().getString("playlistselector.all"));
        } else if (value.equals(TAG_USED)) {
          setText(ctx.getTextProvider().getString("playlistselector.used"));

        } else if (value.equals(TAG_UNUSED)) {
          setText(ctx.getTextProvider().getString("playlistselector.unused"));

        }
        return comp;
      }

    });
    panel.add(combo, cc.xy(1, 2));

    this.tagSelection.addValueChangeListener(new PropertyChangeListener() {
      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshPlaylists();
      }

    });

    this.refreshTags();

    return panel;
  }

  private JComponent createList() {
    List<Playlist> playlists = playlistRegistry.getPlaylists(this.playlistType);

    final IndirectListModel<Playlist> playlistModel = new IndirectListModel<Playlist>(this.filterPlaylists(playlists));
    this.listModel = playlistModel;
    this.registerModificationListener(playlistModel);
    playlistRegistry.addPropertyChangeListener("numPlaylists", new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        refreshPlaylists();
      }

    });

    list = new JXList(playlistModel, true);
    list.setTransferHandler(new PlaylistSelectorTransferHandler(ctx, list));
    this.setListCellRenderer(new AdvancedPlaylistListCellRender());
    list.setComparator(new PlaylistNameCompator());
    list.setSortOrder(SortOrder.ASCENDING);
    list.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    list.setDragEnabled(true);
    list.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!updateInProgress) {
          updateInProgress = true;
          try {
            if (list.getSelectionModel().getSelectionMode() == ListSelectionModel.SINGLE_SELECTION) {
              Playlist playlist = (Playlist) list.getSelectedValue();
              playlistSelectionHolder.setValue(playlist);
            } else {
              Object[] playlists = list.getSelectedValues();
              ArrayList<Playlist> selection = new ArrayList<Playlist>();
              for (Object p : playlists) {
                selection.add((Playlist) p);
              }
              playlistSelectionHolder.setValue(selection);

            }
          } finally {
            updateInProgress = false;
          }
        }
      }

    });

    list.setSearchable(new ListSearchable(list) {

      @Override
      protected SearchResult findMatchAt(Pattern pattern, int row) {
        int modelRow = list.convertIndexToModel(row);
        Matcher matcher = pattern.matcher(((Playlist) playlistModel.getElementAt(modelRow)).getName());
        if (matcher.find()) {
          return new SearchResult(pattern, matcher.toMatchResult(), row, 0);
        } else {
          return null;
        }
      }

    });

    list.addHighlighter(new AbstractHighlighter() {

      @Override
      protected Component doHighlight(Component comp, ComponentAdapter adapter) {
        if (highlightedPlaylists.size() > 0) {
          int row = list.convertIndexToModel(adapter.row);
          Playlist playlist = listModel.getElementAt(row);
          if (highlightedPlaylists.contains(playlist.getId())) {
            comp.setBackground(new Color(240, 240, 0));
          }
        }
        return comp;
      }
    });

    return list;
  }

  private List<Playlist> filterPlaylists(List<Playlist> playlists) {
    if (this.tagSelection.getValue() != null) {
      String tag = (String) this.tagSelection.getValue();

      HashSet<Integer> used = null;
      Boolean filterUsed = null;
      if(tag.equals(TAG_UNUSED) || tag.equals(TAG_USED)) {
        used = new HashSet<Integer>();
        filterUsed = tag.equals(TAG_USED);
        
        for(Entry entry : ctx.getAdminClient().getSchedule().getEntries()) {
          used.add(entry.getPlaylistId());
        }
      }

      ArrayList<Playlist> filtered = new ArrayList<Playlist>(playlists.size());
      for (Playlist playlist : playlists) {
        if(filterUsed != null) {
          if(used.contains(playlist.getId()) == filterUsed.booleanValue()) {
            filtered.add(playlist);
          }
        }
        else if (playlist.isTaggedWith(tag)) {
          filtered.add(playlist);
        }
      }
      return filtered;
    } else {
      return playlists;
    }
  }

  /**
   * @return the listModel
   */
  public ListModel getListModel() {
    return listModel;
  }

  private void init() {
    this.playlistRegistry.addPropertyChangeListener("numPlaylists", this.playlistTagListener);
    this.setLayout(new BorderLayout());
    this.add(this.createHeadPanel(), BorderLayout.NORTH);
    this.add(new JScrollPane(this.createList()), BorderLayout.CENTER);

    this.playlistSelectionHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        if (evt.getNewValue() instanceof Playlist) {
          List<Playlist> playlists = Arrays.asList((Playlist) evt.getNewValue());
          setSelected(playlists);
        } else if (evt.getNewValue() instanceof List) {
          List<Playlist> playlists = (List<Playlist>) evt.getNewValue();
          setSelected(playlists);
        } else {
          setSelected(new ArrayList<Playlist>());
        }
      }

    });
  }

  @SuppressWarnings("unchecked")
  private void refreshPlaylists() {
    try {

      Object oldSelection = this.playlistSelectionHolder.getValue();

      // unregister property change listener for modified flag
      for (Playlist playlist : listModel.getList()) {
        playlist.removePropertyChangeListener("name", playlistModificationListener);
        playlist.removePropertyChangeListener("modified", playlistModificationListener);
        playlist.removePropertyChangeListener("tags", playlistTagListener);
      }
      // set new playlists to list model
      List<Playlist> playlists = playlistRegistry.getPlaylists(this.playlistType);
      listModel.setList(this.filterPlaylists(playlists));
      // register property change listener for modified flag
      registerModificationListener(listModel);

      if (oldSelection instanceof Playlist) {
        if (!updateInProgress) {
          try {
            updateInProgress = true;
            Playlist selection = (Playlist) oldSelection;
            this.list.setSelectedValue(selection, true);
          } finally {
            updateInProgress = false;
          }
        }
      } else if (oldSelection instanceof List) {
        this.setSelected((List<Playlist>) oldSelection);
      }

      list.invalidate();
      list.repaint();
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  void refreshTags() {
    ArrayList<String> entries = new ArrayList<String>(playlistRegistry.getUsedTags());
    Collections.sort(entries);
    entries.add(0, null);
    entries.add(TAG_USED);
    entries.add(TAG_UNUSED);
    tagModel.setList(entries);

  }

  private void registerModificationListener(IndirectListModel<Playlist> playlistModel) {
    for (Playlist playlist : playlistModel.getList()) {
      playlist.addPropertyChangeListener("name", this.playlistModificationListener);
      playlist.addPropertyChangeListener("modified", playlistModificationListener);
      playlist.addPropertyChangeListener("tags", playlistTagListener);
    }
  }

  /**
   * Sets the list cell renderer
   * 
   * @param renderer
   */
  public void setListCellRenderer(PlaylistListCellRenderer renderer) {
    this.renderer = renderer;
    this.renderer.setRenderColors(Preferences.userRoot().getBoolean(PREFKEY_RENDER_COLORS, false));
    this.list.setCellRenderer(renderer);
  }

  protected void setSelected(List<Playlist> playlists) {
    if (!this.updateInProgress) {
      try {
        this.updateInProgress = true;
        int[] selected = new int[playlists.size()];
        int idx = 0;
        for (Playlist playlist : playlists) {
          int plIdx = this.listModel.getList().indexOf(playlist);
          if (plIdx > -1) {
            selected[idx++] = this.list.convertIndexToView(plIdx);
          }
        }
        if (idx < selected.length) {
          int[] tmp = new int[idx];
          System.arraycopy(selected, 0, tmp, 0, idx);
          selected = tmp;
        }
        this.list.setSelectedIndices(selected);
      } finally {
        this.updateInProgress = false;
      }
    }
  }

  /**
   * Sets the selection mode for the unerlying list - default is {@link ListSelectionModel#SINGLE_SELECTION}
   * 
   * @param selectionMode
   */
  public void setSelectionModel(int selectionMode) {
    this.list.getSelectionModel().setSelectionMode(selectionMode);
  }

  /**
   * @see javax.swing.JComponent#setTransferHandler(javax.swing.TransferHandler)
   */
  public void setTransferHandler(TransferHandler handler) {
    this.list.setTransferHandler(handler);
  }

  public void addHighlightedTitlesHolder(ValueModel titleHolder) {
    titleHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        highlightedPlaylists.clear();
        if (evt.getNewValue() != null && evt.getNewValue() instanceof Set<?>) {
          for (Object item : (Set<?>) evt.getNewValue()) {
            int titleId = (Integer) item;
            RegisteredTrack title = ctx.getAdminClient().getTrackService().getTrackRegistry().getTrack(titleId);
            if (title != null) {
              highlightedPlaylists.addAll(title.getPlaylistIds());
            }
          }
        }
        list.invalidate();
        list.repaint();
      }
    });
  }

}
