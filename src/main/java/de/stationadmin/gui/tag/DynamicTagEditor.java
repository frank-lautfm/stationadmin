/**
 * 
 */
package de.stationadmin.gui.tag;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;

import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.list.SelectionInList;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.tag.DynamicTag;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.EventGate;
import de.stationadmin.gui.util.NonObservingPresentationModel;

/**
 * Editor panel for dynamic title tags
 * 
 * @author Frank
 */
public class DynamicTagEditor extends JPanel {
  private static final long serialVersionUID = 4513201211256622444L;
  private ClientContext ctx;
  private PresentationModel<DynamicTag> model = new NonObservingPresentationModel<DynamicTag>((DynamicTag) null);

  /**
   * @param ctx
   */
  public DynamicTagEditor(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  @SuppressWarnings({ "rawtypes", "unchecked" })
  private JPanel createMetaDataPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new FormLayout("3dlu,max(pref;50dlu),3dlu,pref:grow",
        "3dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref,3dlu,pref,1dlu,pref,3dlu,pref,3dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    // group
    {
      Vector<String> grps = new Vector<String>();
      grps.add(null);
      grps.addAll(ctx.getAdminClient().getTagManager().getGroups());
      final DefaultComboBoxModel cmbModel = new DefaultComboBoxModel(grps);
      final JComboBox groupCmb = new JComboBox(cmbModel);
      groupCmb.setEditable(true);
      panel.add(new JLabel(ctx.getString("titletagmanager.property.group")), cc.xy(2, row));
      panel.add(groupCmb, cc.xy(4, row));

      groupCmb.addItemListener(new ItemListener() {

        @Override
        public void itemStateChanged(ItemEvent e) {
          model.getBufferedModel("group").setValue(groupCmb.getSelectedItem());
          if (cmbModel.getIndexOf(groupCmb.getSelectedItem()) < 0) {
            cmbModel.addElement(groupCmb.getSelectedItem());
          }
        }
      });
      model.getBufferedModel("group").addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Object value = model.getBufferedModel("group").getValue();
          groupCmb.setSelectedItem(value);

        }
      });
      row += 2;
    }

    // name
    {
      JTextField tf = BasicComponentFactory.createTextField(model.getBufferedModel("name"));

      panel.add(new JLabel(ctx.getString("titletagmanager.property.name")), cc.xy(2, row));
      panel.add(tf, cc.xy(4, row));
      row += 2;
    }

    // artists
    {
      JTextArea tf = BasicComponentFactory.createTextArea(model.getBufferedModel("artists"));
      tf.setToolTipText(ctx.getString("titletagmanager.property.artists.tooltip"));
      tf.setRows(3);

      panel.add(new JLabel(ctx.getString("titletagmanager.property.artists")), cc.xy(2, row));
      panel.add(new JScrollPane(tf), cc.xy(4, row));
      row += 2;
    }

    // titles
    {
      JTextArea tf = BasicComponentFactory.createTextArea(model.getBufferedModel("titles"));
      tf.setToolTipText(ctx.getString("titletagmanager.property.titles.tooltip"));
      tf.setRows(3);

      panel.add(new JLabel(ctx.getString("titletagmanager.property.titles")), cc.xy(2, row));
      panel.add(new JScrollPane(tf), cc.xy(4, row));
      row += 2;
    }

    // albums
    {
      JTextArea tf = BasicComponentFactory.createTextArea(model.getBufferedModel("albums"));
      tf.setToolTipText(ctx.getString("titletagmanager.property.albums.tooltip"));
      tf.setRows(3);

      panel.add(new JLabel(ctx.getString("titletagmanager.property.albums")), cc.xy(2, row));
      panel.add(new JScrollPane(tf), cc.xy(4, row));
      row += 2;

      JLabel hint = new JLabel(ctx.getString("titletagmanager.property.albums.hint"));
      hint.setFont(new Font(hint.getFont().getFamily(), 0, hint.getFont().getSize() - 2));
      panel.add(hint, cc.xy(4, row));
      row += 2;
    }

    // length
    {
      JTextField minTf = BasicComponentFactory.createIntegerField(model.getBufferedModel("minLength"), 0);
      minTf.setColumns(4);
      JTextField maxTf = BasicComponentFactory.createIntegerField(model.getBufferedModel("maxLength"), Integer.MAX_VALUE);
      maxTf.setColumns(4);

      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      p.add(minTf);
      p.add(new JLabel(" - "));
      p.add(maxTf);
      p.add(new JLabel(" " + ctx.getString("titletagmanager.property.length.unit")));

      panel.add(new JLabel(ctx.getString("titletagmanager.property.length")), cc.xy(2, row));
      panel.add(p, cc.xy(4, row));
      row += 2;

    }

    return panel;
  }

  private void init() {

    JTabbedPane tabs = new JTabbedPane();
    tabs.add(ctx.getTextProvider().getString("titletagmanager.tab.meta"), this.createMetaDataPanel());
    tabs.add(ctx.getTextProvider().getString("titletagmanager.tab.plays"), this.createPlayedWithinPanel());
    tabs.add(ctx.getTextProvider().getString("titletagmanager.tab.playlists"), this.createPlaylistsPanel());
    tabs.add(ctx.getTextProvider().getString("titletagmanager.tab.tags"), this.createTagsPanel());

    this.setLayout(new BorderLayout());
    this.add(tabs, BorderLayout.CENTER);

  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JPanel createPlaylistsPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu,pref:grow,3dlu"));
    CellConstraints cc = new CellConstraints();

    final BufferedValueModel playlistModel = this.model.getBufferedModel("playlistIds");
    final EventGate evtGate = new EventGate();

    final IndirectListModel<Playlist> model = new IndirectListModel<Playlist>(this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry()
        .getAllPlaylists());
    final JList list = new JList(model);
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!evtGate.isUpdating()) {
          try {
            evtGate.setUpdating(true);
            int[] indices = list.getSelectedIndices();
            if (indices.length > 0) {
              int[] playlistIds = new int[indices.length];
              for (int i = 0; i < indices.length; i++) {
                playlistIds[i] = ((Playlist) model.getElementAt(indices[i])).getId();
              }
              playlistModel.setValue(playlistIds);
            } else {
              playlistModel.setValue(null);
            }
          } finally {
            evtGate.setUpdating(false);
          }
        }

      }
    });

    playlistModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (!evtGate.isUpdating()) {
          try {
            evtGate.setUpdating(true);
            int[] playlistIds = (int[]) evt.getNewValue();
            list.getSelectionModel().clearSelection();
            Set<Integer> idSet = new HashSet<Integer>();
            if (playlistIds != null) {
              for (int id : playlistIds) {
                idSet.add(id);
              }
              if (playlistIds != null) {
                for (int i = 0; i < model.getSize(); i++) {
                  Playlist playlist = model.getElementAt(i);
                  if (idSet.contains(playlist.getId())) {
                    list.getSelectionModel().addSelectionInterval(i, i);
                  }
                }
              }
            }
          } finally {
            evtGate.setUpdating(false);
          }
        }

      }
    });

    panel.add(new JLabel(ctx.getTextProvider().getString("titletagmanager.property.playlistIds")), cc.xy(2, 2));
    panel.add(new JScrollPane(list), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    return panel;
  }

  @SuppressWarnings({ "unchecked", "rawtypes" })
  private JPanel createTagsPanel() {
    JPanel panel = new JPanel(new FormLayout("3dlu,pref:grow,3dlu", "3dlu,pref,3dlu,pref:grow,3dlu"));
    CellConstraints cc = new CellConstraints();

    final BufferedValueModel tagModel = this.model.getBufferedModel("tags");
    final EventGate evtGate = new EventGate();

    List<String> tagNames = new ArrayList<String>();
    for (StaticTag tag : this.ctx.getAdminClient().getTagManager().getStaticTags()) {
      tagNames.add(tag.getName());
    }
    Collections.sort(tagNames);

    final IndirectListModel<String> model = new IndirectListModel<String>(tagNames);
    final JList list = new JList(model);
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.getSelectionModel().addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!evtGate.isUpdating()) {
          try {
            evtGate.setUpdating(true);
            int[] indices = list.getSelectedIndices();
            if (indices.length > 0) {
              String[] tags = new String[indices.length];
              for (int i = 0; i < indices.length; i++) {
                tags[i] = (String) model.getElementAt(indices[i]);
              }
              tagModel.setValue(tags);
            } else {
              tagModel.setValue(null);
            }
          } finally {
            evtGate.setUpdating(false);
          }
        }

      }
    });

    tagModel.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        if (!evtGate.isUpdating()) {
          try {
            evtGate.setUpdating(true);
            String[] tags = (String[]) evt.getNewValue();
            list.getSelectionModel().clearSelection();
            Set<String> idSet = new HashSet<String>();
            if (tags != null) {
              for (String tag : tags) {
                idSet.add(tag);
              }
              for (int i = 0; i < model.getSize(); i++) {
                String tag = model.getElementAt(i);
                if (idSet.contains(tag)) {
                  list.getSelectionModel().addSelectionInterval(i, i);
                }
              }
            }
          } finally {
            evtGate.setUpdating(false);
          }
        }

      }
    });

    panel.add(new JLabel(ctx.getTextProvider().getString("titletagmanager.property.playlistIds")), cc.xy(2, 2));
    panel.add(new JScrollPane(list), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    return panel;
  }

  private JPanel createPlayedWithinPanel() {
    JPanel panel = new JPanel();
    panel.setLayout(new FormLayout("3dlu,pref,3dlu,pref:grow", "3dlu,pref,5dlu,pref,5dlu,pref,3dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    // played
    {
      JTextField hoursTf = BasicComponentFactory.createIntegerField(model.getBufferedModel("playedWithin"), 0);
      hoursTf.setColumns(4);

      JPanel p = new JPanel(new FlowLayout(FlowLayout.LEFT));
      p.add(new JLabel(ctx.getString("titletagmanager.property.playedWithin.desc") + " "));
      p.add(hoursTf);
      p.add(new JLabel(" " + ctx.getString("titletagmanager.property.playedWithin.unit")));

      panel.add(new JLabel(ctx.getString("titletagmanager.property.playedWithin")), cc.xy(2, row));
      panel.add(p, cc.xy(4, row));
      row += 2;

    }

    {
      final ValueHolder playlistHolder = new ValueHolder(null);
      List<Playlist> playlists = new ArrayList<Playlist>();
      playlists.addAll(this.ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE));
      Collections.sort(playlists, new PlaylistNameCompator());
      playlists.add(0, null);

      this.model.getBufferedModel("playedWithinPlaylist").addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Integer v = (Integer) evt.getNewValue();
          Playlist p = null;
          if (v != null && v.intValue() > -1) {
            p = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(v.intValue());
          }
          if (playlistHolder.getValue() != p) {
            playlistHolder.setValue(p);
          }

        }
      });

      playlistHolder.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          Playlist p = (Playlist) evt.getNewValue();
          int id = -1;
          if (p != null) {
            id = p.getId();
          }
          model.getBufferedModel("playedWithinPlaylist").setValue(id);
        }
      });

      SelectionInList<Playlist> playlistSelection = new SelectionInList<Playlist>(playlists, playlistHolder);
      JComboBox cmb = BasicComponentFactory.createComboBox(playlistSelection);
      panel.add(new JLabel(ctx.getString("titletagmanager.property.playedWithinPlaylist")), cc.xy(2, row));
      panel.add(cmb, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;
    }

    {
      JTextField minHour = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("playedWithinMinHour"), -1);
      minHour.setColumns(2);
      JTextField maxHour = BasicComponentFactory.createIntegerField(this.model.getBufferedModel("playedWithinMaxHour"), -1);
      maxHour.setColumns(2);
      JPanel hourPanel = new JPanel(new FlowLayout(FlowLayout.LEADING, 0, 0));
      hourPanel.add(minHour);
      hourPanel.add(new JLabel(" - "));
      hourPanel.add(maxHour);
      hourPanel.add(new JLabel(" " + ctx.getString("titletagmanager.property.playedWithinHours.unit")));

      panel.add(new JLabel(ctx.getString("titletagmanager.property.playedWithinHours")), cc.xy(2, row));
      panel.add(hourPanel, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.CENTER));
      row += 2;

    }

    return panel;
  }

  /**
   * @return the model
   */
  public PresentationModel<DynamicTag> getModel() {
    return model;
  }

}
