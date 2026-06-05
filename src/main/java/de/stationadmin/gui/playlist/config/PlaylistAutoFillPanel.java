package de.stationadmin.gui.playlist.config;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.jdesktop.swingx.JXLabel;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.BufferedValueModel;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.AutoFillRule;
import de.stationadmin.base.playlist.NewsTrackOption;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.ShuffleScriptMeta;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.EnumListCellRenderer;

public class PlaylistAutoFillPanel extends JPanel {
  private static final long serialVersionUID = -6015471156309531424L;
  private ClientContext ctx;
  boolean updateInProgress = false;

  public PlaylistAutoFillPanel(ClientContext ctx, PlaylistConfigurationModel playlistCfgModel) {
    this.ctx = ctx;
    init(playlistCfgModel);
  }

  private void init(PlaylistConfigurationModel playlistCfgModel) {
    PresentationModel<AutoFillRule> model = playlistCfgModel.getAutoFillModel();

    final ArrayList<JComponent> dependentComponents = new ArrayList<>();

    this.setLayout(
        new FormLayout("5dlu,pref,5dlu,250dlu,5dlu", "5dlu,pref,5dlu,pref,1dlu,pref,8dlu,pref,5dlu,min(pref;50dlu),5dlu,min(pref;50dlu),5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;

    this.add(new JLabel(ctx.getString("playlistcfg.property.autofill.description")), cc.xywh(2, row, 3, 1));
    row += 2;

    {
      JRadioButton offRb = BasicComponentFactory.createRadioButton(model.getBufferedModel("enabled"), Boolean.FALSE, ctx.getString("playlistcfg.property.autofill.disabled"));
      this.add(offRb, cc.xywh(2, row, 3, 1));
      row += 2;
      JRadioButton onRb = BasicComponentFactory.createRadioButton(model.getBufferedModel("enabled"), Boolean.TRUE, ctx.getString("playlistcfg.property.autofill.enabled"));
      this.add(onRb, cc.xywh(2, row, 3, 1));

      model.getBufferedModel("enabled").addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          for (JComponent comp : dependentComponents) {
            if (evt.getNewValue() instanceof Boolean) {
              comp.setEnabled((Boolean) evt.getNewValue());
            } else {
              comp.setEnabled(false);
            }
          }
        }
      });

      row += 2;
    }

    this.add(new JLabel(ctx.getString("playlistcfg.property.autofill.source")), cc.xywh(2, row, 3, 1));
    row += 2;

    // source tags
    {
      final ValueModel tagsModel = model.getBufferedModel("sourceTags");

      this.add(new JLabel(ctx.getString("playlistcfg.property.autofill.tags")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      final List<String> tags = ctx.getAdminClient().getTagManager().getTags();
      Collections.sort(tags);
      final JList<String> list = new JList<String>(tags.toArray(new String[tags.size()]));
      list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      dependentComponents.add(list);

      String[] selection = (String[]) tagsModel.getValue();
      updateSelection(list, tags, selection);
      tagsModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() instanceof String[]) {
            updateSelection(list, tags, (String[]) evt.getNewValue());

          } else {
            String[] selectedTags = (String[]) evt.getNewValue();
            updateSelection(list, tags, selectedTags);
          }
        }
      });

      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            updateInProgress = true; // block further UI updates as we are already reactiving to an UI update
            try {
              List<String> values = list.getSelectedValuesList();
              tagsModel.setValue(values.toArray(new String[values.size()]));
            } finally {
              updateInProgress = false;
            }
          }
        }

      });

      this.add(new JScrollPane(list), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
    }

    {
      final ValueModel playlistModel = model.getBufferedModel("sourcePlaylists");

      this.add(new JLabel(ctx.getString("playlistcfg.property.autofill.playlists")), cc.xy(2, row, CellConstraints.LEFT, CellConstraints.TOP));
      final List<Playlist> playlists = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getAllPlaylists();
      playlists.sort((p1, p2) -> p1.getName().compareToIgnoreCase(p2.getName()));
      final JList<Playlist> list = new JList<Playlist>(playlists.toArray(new Playlist[playlists.size()]));
      list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
      dependentComponents.add(list);

      int[] selection = (int[]) playlistModel.getValue();
      updateSelection(list, playlists, selection);
      playlistModel.addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() instanceof int[]) {
            updateSelection(list, playlists, (int[]) evt.getNewValue());

          }
        }
      });

      list.addListSelectionListener(new ListSelectionListener() {

        @Override
        public void valueChanged(ListSelectionEvent e) {
          if (!e.getValueIsAdjusting()) {
            updateInProgress = true; // block further UI updates as we are already reactiving to an UI update
            try {
              List<Playlist> values = list.getSelectedValuesList();
              int[] ids = new int[values.size()];
              for (int i = 0; i < values.size(); i++) {
                ids[i] = values.get(i).getId();
              }
              playlistModel.setValue(ids);
            } finally {
              updateInProgress = false;
            }
          }
        }

      });

      this.add(new JScrollPane(list), cc.xy(4, row, CellConstraints.FILL, CellConstraints.FILL));
      row += 2;
      
      JCheckBox duplicateCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("duplicatesFromPlaylists"), ctx.getString("playlistcfg.property.autofill.duplicatesFromPlaylists"));
      this.add(duplicateCb, cc.xy(4, row));
      row += 2;
    }

    // News checkbox + news type combobox on the same row
    {
      final ValueModel newsTypeModel = model.getBufferedModel("newsTrack");

      JCheckBox newsCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeNews"), ctx.getString("playlistcfg.property.autofill.includeNews"));
      this.add(newsCb, cc.xy(2, row));
      dependentComponents.add(newsCb);

      final JComboBox<NewsTrackOption> newsTypeCombo = new JComboBox<>(NewsTrackOption.values());
      newsTypeCombo.setRenderer(new EnumListCellRenderer(ctx.getTextProvider(), "autofill.newstrack"));
      newsTypeCombo.setPreferredSize(new Dimension(180, newsTypeCombo.getPreferredSize().height));

      // Sync model -> combobox
      Object currentNewsType = newsTypeModel.getValue();
      if (currentNewsType instanceof NewsTrackOption) {
        newsTypeCombo.setSelectedItem(currentNewsType);
      } else {
        newsTypeCombo.setSelectedItem(NewsTrackOption.NEWS_WITH_WEATHER);
      }
      newsTypeModel.addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          if (evt.getNewValue() instanceof NewsTrackOption) {
            newsTypeCombo.setSelectedItem(evt.getNewValue());
          }
        }
      });

      // Sync combobox -> model
      newsTypeCombo.addActionListener(new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
          newsTypeModel.setValue(newsTypeCombo.getSelectedItem());
        }
      });

      // Enable/disable combobox based on news checkbox state
      boolean newsChecked = Boolean.TRUE.equals(model.getBufferedModel("includeNews").getValue());
      newsTypeCombo.setEnabled(newsChecked);
      model.getBufferedModel("includeNews").addValueChangeListener(new PropertyChangeListener() {
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          boolean enabled = Boolean.TRUE.equals(evt.getNewValue());
          newsTypeCombo.setEnabled(enabled);
        }
      });

      this.add(newsTypeCombo, cc.xy(4, row, CellConstraints.LEFT, CellConstraints.DEFAULT));
      // newsTypeCombo is NOT added to dependentComponents directly for the outer
      // enabled/disabled logic - its enabled state is controlled by the news checkbox above.
      // However we still need it disabled when autofill itself is disabled:
      dependentComponents.add(newsTypeCombo);

      row += 2;
    }

    {
      JCheckBox adTriggerCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeAdTrigger"), ctx.getString("playlistcfg.property.autofill.includeAdTrigger"));
      this.add(adTriggerCb, cc.xywh(2, row, 3, 1));
      dependentComponents.add(adTriggerCb);

      row += 2;
    }

    {
      final JCheckBox jinglesCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeTrackRules"),
          ctx.getString("playlistcfg.property.autofill.includeTrackRules"));
      ShuffleScriptMeta script = playlistCfgModel.getShuffleScript() != null ? (ShuffleScriptMeta) playlistCfgModel.getShuffleScript().getValue() : null;
      jinglesCb.setEnabled(script != null && script.isSupportsGlobalOpts());
      playlistCfgModel.getBufferedModel("shuffleType").addValueChangeListener(new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          ShuffleScriptMeta script = playlistCfgModel.getShuffleScript() != null ? (ShuffleScriptMeta) playlistCfgModel.getShuffleScript().getValue() : null;
          jinglesCb.setEnabled(script != null && script.isSupportsGlobalOpts());

        }
      });
      this.add(jinglesCb, cc.xywh(2, row, 3, 1));
      dependentComponents.add(jinglesCb);

      row += 2;
    }

    BufferedValueModel enabled = model.getBufferedModel("enabled");
    for (JComponent comp : dependentComponents) {
      if (enabled != null && enabled.getValue() instanceof Boolean) {
        comp.setEnabled(enabled.booleanValue());
      } else {
        comp.setEnabled(false);
      }
    }

    // After applying the global enabled state, re-apply the news-type combobox
    // enabled state based on the news checkbox (it may have been overridden above)
    if (enabled != null && Boolean.TRUE.equals(enabled.getValue())) {
      boolean newsChecked = Boolean.TRUE.equals(model.getBufferedModel("includeNews").getValue());
      // find the newsTypeCombo - it was the last item added to dependentComponents before adTrigger
      // Re-apply: iterate dependentComponents and find JComboBox instances
      for (JComponent comp : dependentComponents) {
        if (comp instanceof JComboBox) {
          comp.setEnabled(newsChecked);
        }
      }
    }

  }

  private void updateSelection(JList<String> list, List<String> tags, String[] selection) {
    if (!updateInProgress) {
      if (selection == null) {
        list.setSelectedIndices(new int[0]);
        return;
      }
      int[] idxs = new int[selection.length];
      for (int i = 0; i < selection.length; i++) {
        idxs[i] = tags.indexOf(selection[i]);
      }
      list.setSelectedIndices(idxs);
    }
  }

  private void updateSelection(JList<Playlist> list, List<Playlist> playlists, int[] selection) {
    if (!updateInProgress) {
      if (selection == null) {
        list.setSelectedIndices(new int[0]);
        return;
      }
      int[] idxs = new int[selection.length];
      for (int i = 0; i < selection.length; i++) {
        Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(selection[i]);
        idxs[i] = playlists.indexOf(playlist);
      }
      list.setSelectedIndices(idxs);
    }
  }

}
