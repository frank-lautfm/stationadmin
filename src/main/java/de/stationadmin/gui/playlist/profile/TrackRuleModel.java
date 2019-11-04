package de.stationadmin.gui.playlist.profile;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;

import de.stationadmin.base.playlist.profile.TrackRuleCfg;
import de.stationadmin.base.playlist.shuffle.TrackRule;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup;
import de.stationadmin.gui.util.NonObservingPresentationModel;

public class TrackRuleModel extends NonObservingPresentationModel<TrackRuleCfg> {
  private static final long serialVersionUID = 664700622140588454L;
  
  private ArrayList<TrackRuleGroup> groups;
  private ArrayList<TrackRule> rules;

  public TrackRuleModel() {
    super((TrackRuleCfg) null);

    getBeanChannel().addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent evt) {
        TrackRuleCfg cfg = (TrackRuleCfg) evt.getNewValue();
        if(cfg != null) {
          groups = new ArrayList<>(cfg.getGroups() != null ? cfg.getGroups() : new ArrayList<>());
          if(groups.size() == 0) {
            groups.add(new TrackRuleGroup("Standard", 0));
          }
          getBufferedModel("groups").setValue(groups);
          rules = new ArrayList<>(cfg.getRules() != null ? cfg.getRules() : new ArrayList<>());
          getBufferedModel("rules").setValue(rules);
        }
      }
    });
  }

  public ArrayList<TrackRuleGroup> getGroups() {
    return groups;
  }

  public ArrayList<TrackRule> getRules() {
    return rules;
  }

}
