package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;

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

}
