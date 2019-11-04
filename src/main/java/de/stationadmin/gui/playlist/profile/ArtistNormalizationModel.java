package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.stationadmin.base.playlist.profile.ArtistNormalizationCfg;
import de.stationadmin.gui.util.NonObservingPresentationModel;

public class ArtistNormalizationModel extends NonObservingPresentationModel<ArtistNormalizationCfg> {
  private static final long serialVersionUID = -3745298037492613966L;

  private List<String> separators;
  private Map<String, String> aliases;

  public ArtistNormalizationModel() {
    super((ArtistNormalizationCfg) null);

    getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        ArtistNormalizationCfg cfg = (ArtistNormalizationCfg) evt.getNewValue();
        if (cfg != null) {
          separators = new ArrayList<>(cfg.getSeparators() != null ? cfg.getSeparators() : new ArrayList<>());
          aliases = new HashMap<String, String>(cfg.getAliases() != null ? cfg.getAliases() : new HashMap<>());
          getBufferedModel("separators").setValue(separators);
          getBufferedModel("aliases").setValue(aliases);
        }

      }
    });
  }

  public List<String> getSeparators() {
    return separators;
  }

  public Map<String, String> getAliases() {
    return aliases;
  }

}
