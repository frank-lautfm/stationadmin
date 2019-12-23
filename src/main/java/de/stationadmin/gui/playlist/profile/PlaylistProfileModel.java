package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.List;

import javax.swing.DefaultComboBoxModel;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.playlist.profile.AdTriggerCfg;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.gui.util.NonObservingPresentationModel;

public class PlaylistProfileModel extends NonObservingPresentationModel<PlaylistProfile> {
  private static final long serialVersionUID = -8260503300244134905L;
  private boolean initializing = false;

  private NonObservingPresentationModel<AdTriggerCfg> adTriggerModel;
  private TrackRuleModel trackRuleModel;
  private ArtistNormalizationModel artistNormalization;

  private List<PlaylistProfile> profiles;
  private IndirectListModel<String> profileRefListModel = new IndirectListModel<>();

  public PlaylistProfileModel(ValueModel selection) {
    super((PlaylistProfile) null);
    adTriggerModel = new NonObservingPresentationModel<AdTriggerCfg>((AdTriggerCfg) null);
    trackRuleModel = new TrackRuleModel();
    artistNormalization = new ArtistNormalizationModel();

    selection.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        initializing = true;
        try {
          PlaylistProfile profile = (PlaylistProfile) evt.getNewValue();
          setBean(profile);
          adTriggerModel.setBean(profile != null ? profile.getAdTrigger() : null);
          trackRuleModel.setBean(profile != null ? profile.getTrackRules() : null);
          artistNormalization.setBean(profile != null ? profile.getArtistNormalization() : null);
          updateProfileRefModel(profile != null ? profile.getId() : null);
        } finally {
          initializing = false;
        }
      }
    });

  }

  public boolean isInitializing() {
    return initializing;
  }

  @Override
  public void triggerCommit() {
    adTriggerModel.triggerCommit();
    trackRuleModel.triggerCommit();
    artistNormalization.triggerCommit();
    super.triggerCommit();
  }

  @Override
  public void triggerFlush() {
    adTriggerModel.triggerFlush();
    trackRuleModel.triggerFlush();
    artistNormalization.triggerFlush();
    super.triggerFlush();
  }

  public NonObservingPresentationModel<AdTriggerCfg> getAdTriggerModel() {
    return adTriggerModel;
  }

  public TrackRuleModel getTrackRuleModel() {
    return trackRuleModel;
  }

  public ArtistNormalizationModel getArtistNormalization() {
    return artistNormalization;
  }

  public IndirectListModel<String> getProfileRefListModel() {
    return profileRefListModel;
  }

  public List<PlaylistProfile> getProfiles() {
    return profiles;
  }

  public void setProfiles(List<PlaylistProfile> profiles) {
    this.profiles = profiles;
    updateProfileRefModel(getBean() != null ? getBean().getId() : null);
  }
  
  public void updateProfileRefListModel(DefaultComboBoxModel<String> listModel) {
    String currentProfileId = getBean() != null ? getBean().getId() : null;
    Object selection = listModel.getSelectedItem();
    listModel.removeAllElements();
    listModel.addElement(null);

    for (PlaylistProfile profile : this.profiles) {
      if (currentProfileId == null || !currentProfileId.equals(profile.getId())) {
        listModel.addElement(profile.getId());
      }
    }
    if(selection != null) {
      listModel.setSelectedItem(selection);
    }
  }

  private void updateProfileRefModel(String currentProfileId) {
    List<String> list = new ArrayList<>();
    list.add(null);

    for (PlaylistProfile profile : this.profiles) {
      if (currentProfileId == null || !currentProfileId.equals(profile.getId())) {
        list.add(profile.getId());
      }
    }

    // String trackRuleFromProfile =
    // getBufferedModel("trackRuleFromProfile").getString();
    // String artistNormalizationFromProfile =
    // getBufferedModel("artistNormalizationFromProfile").getString();
    // System.out.println("Before update: " + trackRuleFromProfile);
    this.profileRefListModel.setList(list);
    // getBufferedModel("trackRuleFromProfile").setValue(trackRuleFromProfile);
    // getBufferedModel("artistNormalizationFromProfile").setValue(artistNormalizationFromProfile);
  }

}
