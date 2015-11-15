package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.gui.ClientContext;

public class TrackViewAction extends AbstractAction {
  private static final long serialVersionUID = 8839860040116586525L;
  private ClientContext ctx;
  private List<Title> titles = new ArrayList<Title>();
  
  public TrackViewAction(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.title.view"));
    this.setEnabled(false);
  }


  @Override
  public void actionPerformed(ActionEvent evt) {
    if(titles.size() > 0) {
      Title title = titles.get(0);
      
      DetailedTrack dtitle = title instanceof DetailedTrack ? (DetailedTrack)title : null;
      if (!(dtitle instanceof RegisteredTrack) || !((RegisteredTrack)title).isOwnTrack()) {
        try {
          dtitle = ctx.getAdminClient().getTrackService().getTrack(title.getId());
        } catch (Exception ex) {
        }
      }
      TrackViewer viewer = new TrackViewer(ctx, dtitle != null ? dtitle : title, null);
      viewer.setVisible(true);
    }
  }


  public List<Title> getTitles() {
    return titles;
  }


  public void setTitles(List<Title> titles) {
    this.titles = titles;
    this.setEnabled(titles.size() > 0);
  }

}
