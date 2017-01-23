/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;

import org.jdesktop.swingx.JXErrorPane;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.tag.StaticTag;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.gui.ClientContext;

/**
 * 
 * @author Frank Korf
 * 
 */
public class ClearTagsAction extends AbstractAction {
  private static final long serialVersionUID = 4072864315683614234L;
  private ClientContext ctx;
  private List<BasicTrack> tracks;

  public ClearTagsAction(ClientContext ctx) {
    this.ctx = ctx;
    this.putValue(Action.NAME, ctx.getString("action.clearTags"));
    this.setEnabled(false);
  }

  /**
   * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
   */
  @Override
  public void actionPerformed(ActionEvent evt) {
    if (tracks != null && this.tracks.size() > 0) {
      try {
        for (BasicTrack track : tracks) {
          this.untag(track);
        }
      } catch (Exception e) {
        JXErrorPane.showDialog(ctx.getRootWindow(), ctx.createErrorInfo(e, "action.clearTags.error"));
      }
    }
  }

  private void untag(BasicTrack track) throws IOException {
    if (track != null) {
      TagManager tagManager = ctx.getAdminClient().getTagManager();
      for (StaticTag tag : tagManager.getStaticTags()) {
        if (tagManager.isTagged(tag.getName(), track.getId())) {
          tagManager.untagTracks(tag.getName(), track.getId());
        }
      }
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
  public void setTracks(List<BasicTrack> tracks) {
    this.tracks = tracks;
    this.setEnabled(this.tracks != null && this.tracks.size() > 0);
  }

}
