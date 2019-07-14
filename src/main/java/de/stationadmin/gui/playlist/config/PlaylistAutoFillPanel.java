package de.stationadmin.gui.playlist.config;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.PresentationModel;
import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.AutoFillRule;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.ShuffleScriptMeta;
import de.stationadmin.gui.ClientContext;

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
    
    this.setLayout(new FormLayout("5dlu,pref,5dlu,170dlu,5dlu", "5dlu,pref,8dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();
    int row = 2;
    
    
    {
      JCheckBox enabledCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("enabled"), ctx.getString("playlistcfg.property.autofill.enabled"));
      this.add(enabledCb, cc.xywh(2, row, 3, 1));
      
      model.getBufferedModel("enabled").addValueChangeListener(new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          for(JComponent comp : dependentComponents) {
            comp.setEnabled((Boolean)evt.getNewValue());
          }
        }
      });

      row += 2;
    }

    
    this.add(new JLabel(ctx.getString("playlistcfg.property.autofill.source")), cc.xywh(2, row, 3, 1));
    row+=2;

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
      // Collections.sort(playlists);
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
              for(int i = 0; i < values.size(); i++) {
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
    }

    {
      JCheckBox newsCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeNews"), ctx.getString("playlistcfg.property.autofill.includeNews"));
      this.add(newsCb, cc.xywh(2, row, 3, 1));
      dependentComponents.add(newsCb);

      row += 2;
    }

    {
      JCheckBox adTriggerCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeAdTrigger"), ctx.getString("playlistcfg.property.autofill.includeAdTrigger"));
      this.add(adTriggerCb, cc.xywh(2, row, 3, 1));
      dependentComponents.add(adTriggerCb);

      row += 2;
    }

    {
      final JCheckBox jinglesCb = BasicComponentFactory.createCheckBox(model.getBufferedModel("includeTrackRules"), ctx.getString("playlistcfg.property.autofill.includeTrackRules"));
      ShuffleScriptMeta script = playlistCfgModel.getShuffleScript() != null ? (ShuffleScriptMeta)playlistCfgModel.getShuffleScript().getValue() : null;
      jinglesCb.setEnabled(script != null && script.isSupportsGlobalOpts());
      playlistCfgModel.getBufferedModel("shuffleType").addValueChangeListener(new PropertyChangeListener() {
        
        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          ShuffleScriptMeta script = playlistCfgModel.getShuffleScript() != null ? (ShuffleScriptMeta)playlistCfgModel.getShuffleScript().getValue() : null;
          jinglesCb.setEnabled(script != null && script.isSupportsGlobalOpts());
          
        }
      });
      this.add(jinglesCb, cc.xywh(2, row, 3, 1));
      dependentComponents.add(jinglesCb);

      row += 2;
    }

    for(JComponent comp : dependentComponents) {
      comp.setEnabled(model.getBufferedModel("enabled").booleanValue());
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
