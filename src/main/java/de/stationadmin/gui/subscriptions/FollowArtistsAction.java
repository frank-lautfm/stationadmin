/**
 * 
 */
package de.stationadmin.gui.subscriptions;

import java.awt.event.ActionEvent;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.subscription.Subscription;
import de.stationadmin.base.subscription.Subscription.Field;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class FollowArtistsAction extends AbstractAction {
  private static final long serialVersionUID = 6600669163746314642L;
  private ClientContext ctx;
  private List<BasicTrack> tracks;

  
  public FollowArtistsAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.followArtists"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    Set<String> artists = new HashSet<String>();
    for(BasicTrack title : tracks) {
      artists.add(title.getArtist());
    }
    for(String artist : artists) {
      Subscription s = new Subscription(Field.ARTIST, artist, false);
      this.ctx.getAdminClient().getSubscriptionService().add(s);
    }
  }

  public List<BasicTrack> getTracks() {
    return tracks;
  }

  public void setTracks(List<BasicTrack> titles) {
    this.tracks = titles;
    this.setEnabled(titles.size() > 0);
  }

}
