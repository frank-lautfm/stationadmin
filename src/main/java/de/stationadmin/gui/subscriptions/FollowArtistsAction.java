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
import de.stationadmin.base.track.Title;
import de.stationadmin.gui.ClientContext;

/**
 * @author korf
 *
 */
public class FollowArtistsAction extends AbstractAction {
  private static final long serialVersionUID = 6600669163746314642L;
  private ClientContext ctx;
  private List<Title> titles;

  
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
    for(Title title : titles) {
      artists.add(title.getArtist());
    }
    for(String artist : artists) {
      Subscription s = new Subscription(Field.ARTIST, artist, false);
      this.ctx.getAdminClient().getSubscriptionService().add(s);
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
