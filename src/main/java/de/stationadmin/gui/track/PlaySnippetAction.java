/**
 * 
 */
package de.stationadmin.gui.track;

import java.awt.event.ActionEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JOptionPane;

import org.jdesktop.swingx.JXErrorPane;
import org.jdesktop.swingx.error.ErrorInfo;

import com.jgoodies.binding.value.ValueModel;

import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.player.Snippet;
import de.stationadmin.gui.player.SnippetPlayer;
import de.stationadmin.gui.util.MP3Launcher;

/**
 * @author Frank
 */
public class PlaySnippetAction extends AbstractAction {
  private static final long serialVersionUID = 2871799895796258913L;
  private ClientContext ctx;
  private TrackService titleService;
  private MP3Launcher mp3Launcher;
  private ValueModel selectionHolder;

  public PlaySnippetAction(ClientContext ctx, ValueModel selectionHolder) {
    this.putValue(Action.NAME, ctx.getTextProvider().getString("action.playsnippet"));
    this.ctx = ctx;
    this.titleService = ctx.getAdminClient().getTrackService();
    this.mp3Launcher = new MP3Launcher(ctx);
    this.selectionHolder = selectionHolder;

    checkEnabled();
    selectionHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      public void propertyChange(PropertyChangeEvent arg0) {
        checkEnabled();
      }
    });
  }

  @Override
  public void actionPerformed(ActionEvent arg0) {
    try {
      playSnippetInternal();
    } catch (Throwable e) {
      ErrorInfo info = ctx.createErrorInfo(e, "action.playsnippet.msg.error");
      JXErrorPane.showDialog(ctx.getRootWindow(), info);
    }
  }

  private void checkEnabled() {
    this.setEnabled(mp3Launcher.isAvailable() && selectionHolder.getValue() != null && ((List<?>) selectionHolder.getValue()).size() > 0);
  }

  @SuppressWarnings("unchecked")
  public void playSnippet() {
    if (selectionHolder.getValue() != null) {
      List<Title> titles = (List<Title>) selectionHolder.getValue();
      if (titles.size() > 0 && mp3Launcher.isAvailable()) {
        List<String> urls = new ArrayList<String>();

        if (titles.size() > 20) {
          JOptionPane.showMessageDialog(ctx.getRootWindow(), ctx.getTextProvider().getString("action.playsnippet.msg.toomanytitles"));
        }

        for (int i = 0; i < titles.size() && i < 20; i++) {
          try {
            String url = this.titleService.getSnippetURL(titles.get(i).getId());
            if (url != null) {
              urls.add(url);
            }
          } catch (Exception e) {

          }
        }
        if (urls.size() > 0) {
          mp3Launcher.play(urls.toArray(new String[urls.size()]));
        }
      }
    }

  }

  @SuppressWarnings("unchecked")
  public void playSnippetInternal() {
    if (selectionHolder.getValue() != null) {
      List<Title> titles = (List<Title>) selectionHolder.getValue();
      if (titles.size() > 0) {
        List<Snippet> snippets = new ArrayList<Snippet>();

        if (titles.size() > 20) {
          JOptionPane.showMessageDialog(ctx.getRootWindow(), ctx.getTextProvider().getString("action.playsnippet.msg.toomanytitles"));
        }

        for (int i = 0; i < titles.size() && i < 20; i++) {
          try {
            String url = this.titleService.getSnippetURL(titles.get(i).getId());
            if (url != null) {
              snippets.add(new Snippet(titles.get(i), new URL(url)));
            }
          } catch (Exception e) {
            e.printStackTrace();

          }
        }
        if (snippets.size() > 0) {
          SnippetPlayer player = new SnippetPlayer(ctx, snippets);
          player.setVisible(true);
        }
      }
    }

  }

}
