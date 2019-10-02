/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.BorderLayout;

import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSplitPane;
import javax.swing.JTabbedPane;
import javax.swing.plaf.PopupMenuUI;

import com.jgoodies.binding.value.ValueHolder;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.JumpListener;
import de.stationadmin.gui.util.TitledPanel;

/**
 * Combination of {@link PlaylistSelector} and {@link PlaylistViewer}
 * 
 * @author korf
 */
public class PlaylistContainer extends JPanel {

  private static final long serialVersionUID = -8562537387008137840L;

  private ClientContext ctx;

  public PlaylistContainer(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    final ValueHolder playlistSelectionHolder = new ValueHolder(null, true);

    final PlaylistSelector selectorOnline = new PlaylistSelector(this.ctx, PlaylistType.ONLINE, playlistSelectionHolder);
    final JPopupMenu popupOnline = new JPopupMenu();
    popupOnline.add(new PlaylistNewAction(ctx, playlistSelectionHolder));
    popupOnline.add(new PlaylistDuplicateAction(ctx, playlistSelectionHolder));
    popupOnline.add(new PlaylistDeleteAction(playlistSelectionHolder, ctx.getAdminClient().getPlaylistService(), ctx.getTextProvider(), false));
    popupOnline.add(new PlaylistToArchiveAction(ctx, playlistSelectionHolder));
    popupOnline.addSeparator();
    // popupOnline.add(new ShuffleTesterAction(ctx, playlistSelectionHolder));
    // popupOnline.addSeparator();
    popupOnline.add(new PlaylistEditPropertiesAction(ctx, playlistSelectionHolder, false));
    selectorOnline.enableContextMenu(popupOnline);
    
    
    final PlaylistSelector selectorArchive = new PlaylistSelector(this.ctx, PlaylistType.ARCHIVED, playlistSelectionHolder);
    final JPopupMenu popupArchive = new JPopupMenu();
    popupArchive.add(new PlaylistDeleteAction(playlistSelectionHolder, ctx.getAdminClient().getPlaylistService(), ctx.getTextProvider(), false));
    popupArchive.add(new PlaylistToOnlineAction(ctx, playlistSelectionHolder));
    popupArchive.addSeparator();
    popupArchive.add(new PlaylistEditPropertiesAction(ctx, playlistSelectionHolder, false));
    selectorArchive.enableContextMenu(popupArchive);
    final PlaylistViewer viewer = new PlaylistViewer(this.ctx, playlistSelectionHolder);
    selectorArchive.addHighlightedTitlesHolder(viewer.getHighlightedTrackHolder());
    selectorOnline.addHighlightedTitlesHolder(viewer.getHighlightedTrackHolder());

    final JTabbedPane selector = new JTabbedPane(JTabbedPane.BOTTOM);
    selector.addTab(ctx.getTextProvider().getString("playlisttype.online"), selectorOnline);
    selector.addTab(ctx.getTextProvider().getString("playlisttype.archived"), selectorArchive);

    ctx.getJumpHandler().addJumpListener(new JumpListener() {

      @Override
      public void jumpTo(Object target) {
        if (target instanceof Playlist) {
          // re-fetch from registry as target might be an obsolete instance from before last synchronization
          Playlist playlist = ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylist(((Playlist) target).getId());
          if (playlist != null) {
            playlistSelectionHolder.setValue(playlist);
          }
        }
        if (target instanceof PlaylistEntryJumpTarget) {
          selector.setSelectedIndex(0);
          playlistSelectionHolder.setValue(((PlaylistEntryJumpTarget) target).getPlaylist());
          viewer.getEntryHolder().setValue(((PlaylistEntryJumpTarget) target).getEntry());
        }
      }

    });

    this.setLayout(new BorderLayout());

    JSplitPane split = new JSplitPane();

    split.setLeftComponent(new TitledPanel("Playlists", selector));
    split.setRightComponent(new TitledPanel(viewer.getPresentationModel().getModel("name"), viewer));
    split.setDividerLocation(250);
    this.add(split, BorderLayout.CENTER);

  }

}
