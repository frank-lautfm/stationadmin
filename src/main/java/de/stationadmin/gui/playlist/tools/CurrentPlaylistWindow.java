/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.HeadlessException;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.swing.JFrame;

import org.jdesktop.swingx.JXTable;
import org.jdesktop.swingx.decorator.AbstractHighlighter;
import org.jdesktop.swingx.decorator.ComponentAdapter;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.PlaylistViewer;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.SwingTools;
import de.stationadmin.lfm.backend.LogEntry;

/**
 * Window with a temporary playlist.
 * <p>
 * This playlist has no name and cannot be saved. The window can be used to tag
 * titles or distribute them to other playlists.
 *
 * @author Frank Korf
 */
public class CurrentPlaylistWindow extends JFrame {
  private static final long serialVersionUID = -8958610268442701703L;

  public CurrentPlaylistWindow(ClientContext ctx, Playlist playlist) throws HeadlessException {
    super();
    PlaylistViewer viewer = new CurrentPlaylistViewer(ctx, playlist);
    this.getContentPane().setLayout(new BorderLayout());
    this.getContentPane().add(viewer, BorderLayout.CENTER);
    this.setSize(500, 600);
    this.setTitle("Aktuelle Playlist: " + playlist.getName());
    SwingTools.centerWithin(ctx.getRootWindow(), this);
  }

  public static class CurrentPlaylistViewer extends PlaylistViewer {
    private static final long serialVersionUID = 1932629681709901328L;
    private int currentTrackRow = -1;
    private Playlist playlist;
    private StationAdminClient adminClient;
    private Date startTime;

    public CurrentPlaylistViewer(ClientContext ctx, Playlist playlist) {
      super(ctx, new ValueHolder(null, true), true);
      getPlaylistHolder().setValue(playlist);
      setValidationEnabled(false);
      this.init();
      this.playlist = playlist;
      this.adminClient = ctx.getAdminClient();
      PropertyChangeListener changeListener = new PropertyChangeListener() {

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
          refreshCurrentTrack();
        }
      };
      refreshCurrentTrack();
      adminClient.getStationStatus().addPropertyChangeListener("currentTrackId", changeListener);

    }

    private Date getStartTime() {
      if (startTime == null) {
        try {
          LogEntry mostRecentExport = null;
          for (LogEntry entry : adminClient.getLogs(2)) {
            if (entry.getMsgType().equals("playlist_exported_successfully")) {
              if (mostRecentExport == null || entry.getCreatedAt().getTime() > mostRecentExport.getCreatedAt().getTime()) {
                mostRecentExport = entry;
              }
            }
          }
          if (mostRecentExport != null) {
            startTime = mostRecentExport.getCreatedAt();
          }
        } catch (Exception e) {

        }

      }
      return startTime;
    }

    private void refreshCurrentTrack() {
      int currentTrack = -1;
      if (adminClient.getStationStatus().getCurrentTrackId() > 0) {
        List<Integer> hits = new ArrayList<>();
        for (int i = 0; i < playlist.getEntries().size(); i++) {
          if (playlist.getEntry(i).getTrackId() == adminClient.getStationStatus().getCurrentTrackId()) {
            hits.add(i);
          }
        }

        if (hits.size() > 0) {
          currentTrack = hits.get(0);

          if (hits.size() > 1) {
            Date startTime = getStartTime();
            if (startTime != null) {
              long time = (System.currentTimeMillis() - startTime.getTime()) / 1000;
              hits.sort((h1, h2) -> Math.abs(playlist.getEntry(h1).getStart() - time) < Math.abs(playlist.getEntry(h2).getStart() - time) ? -1 : 1);
              currentTrack = hits.get(0);
            }
          }

        }
      }

      if (currentTrack > 0) {
        JXTable table = getTable();
        table.scrollRowToVisible(currentTrack);
      }

      this.currentTrackRow = currentTrack;
    }

    private void init() {
      getTable().addHighlighter(new AbstractHighlighter() {

        @Override
        protected Component doHighlight(Component comp, ComponentAdapter adapter) {
          int row = getTable().convertRowIndexToModel(adapter.row);
          if (row == currentTrackRow) {
            comp.setFont(ComponentFactory.boldLabelFont);
          }
          return comp;
        }
      });

    }

  }

}
