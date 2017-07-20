/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.gui.ClientContext;

/**
 * Opens a dialog to copy titles to a playlist
 * 
 * @author Frank Korf
 */
public class TrackMultiEditAction extends AbstractAction {
  private static final long serialVersionUID = 8980357896169561556L;
  private ClientContext ctx;
  private List<BasicTrack> tracks;

  public TrackMultiEditAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.multitrackedit"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent e) {
    if (tracks != null && this.tracks.size() > 0) {
      List<DetailedTrack> dtracks = new ArrayList<DetailedTrack>();
      for(BasicTrack track : tracks) {
        if(track instanceof DetailedTrack) {
          dtracks.add((DetailedTrack)track);
        }
      }
      if(dtracks.size() > 0) {
        
      }
      MultiTrackEditDlg dlg = new MultiTrackEditDlg(ctx, dtracks);
      dlg.setVisible(true);
    }
  }

  /**
   * @return the titles
   */
  public List<BasicTrack> getTracks() {
    return tracks;
  }

  /**
   * @param titles
   *          the titles to set
   */
  public void setTracks(List<BasicTrack> titles) {
    this.tracks = titles;
    this.setEnabled(this.tracks != null && this.tracks.size() > 0);
  }

}
