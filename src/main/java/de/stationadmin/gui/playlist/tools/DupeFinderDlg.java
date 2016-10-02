/**
 * 
 */
package de.stationadmin.gui.playlist.tools;

import java.awt.Component;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JDialog;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.util.DupeFinder;
import de.stationadmin.base.playlist.util.PlaylistEntry;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackComparator;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.playlist.PlaylistEntryJumpTarget;
import de.stationadmin.gui.playlist.PlaylistSelector;
import de.stationadmin.gui.playlist.SimplePlaylistListCellRender;
import de.stationadmin.gui.util.ComponentFactory;
import de.stationadmin.gui.util.SwingTools;

/**
 * Frontend for {@link DupeFinder}
 * 
 * @author Frank Korf
 */
public class DupeFinderDlg extends JDialog {
  private static final long serialVersionUID = 7874344180522967799L;
  private ClientContext ctx;
  private DupeFinder dupeFinder = new DupeFinder();

  public DupeFinderDlg(ClientContext ctx) {
    super();
    this.ctx = ctx;
    this.init();
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref,5dlu,pref:grow,5dlu", "5dlu,pref:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    ValueHolder selectionHolder = new ValueHolder(new ArrayList<Playlist>(), true);
    PlaylistSelector selector = new PlaylistSelector(ctx, null, selectionHolder);
    selector.setSelectionModel(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    selector.setListCellRenderer(new SimplePlaylistListCellRender());
    this.getContentPane().add(selector, cc.xy(2, 2, CellConstraints.FILL, CellConstraints.FILL));

    final DefaultListModel dupeListModel = new DefaultListModel();
    final JList list = new JList(dupeListModel);
    list.setCellRenderer(new DefaultListCellRenderer() {
      private static final long serialVersionUID = -2990430763178027052L;

      @Override
      public Component getListCellRendererComponent(JList list, Object value, int index, boolean isSelected,
          boolean cellHasFocus) {
        Component comp = super.getListCellRendererComponent(list, value, index, isSelected, cellHasFocus);
        if (value instanceof BasicTrack) {
          this.setFont(ComponentFactory.boldLabelFont);
        }
        if (value instanceof PlaylistEntry) {
          this.setFont(ComponentFactory.defaultLabelFont);
          this.setText("  " + value.toString());
        }
        return comp;
      }

    });

    list.addListSelectionListener(new ListSelectionListener() {

      @Override
      public void valueChanged(ListSelectionEvent e) {
        if (!e.getValueIsAdjusting()) {
          if (list.getSelectedValue() instanceof PlaylistEntry) {
            PlaylistEntry entry = (PlaylistEntry) list.getSelectedValue();
            BasicTrack title = ctx.getAdminClient().getTrackService().getTrackRegistry()
                .getTrack(entry.getEntry().getTrackId());
            ctx.getJumpHandler().jumpTo(
                new PlaylistEntryJumpTarget(entry.getPlaylist(), title, entry.getEntry().getStart()));
          }
        }
      }

    });

    this.getContentPane().add(new JScrollPane(list), cc.xy(4, 2, CellConstraints.FILL, CellConstraints.FILL));

    this.setSize(600, 400);
    this.setTitle(ctx.getTextProvider().getString("dupefinder.title"));
    SwingTools.centerOnScreen(this);

    selectionHolder.addValueChangeListener(new PropertyChangeListener() {

      @Override
      @SuppressWarnings("unchecked")
      public void propertyChange(PropertyChangeEvent evt) {
        List<Playlist> playlists = (List<Playlist>) evt.getNewValue();
        Map<BasicTrack, List<PlaylistEntry>> dupes = dupeFinder.findDupes(playlists);

        dupeListModel.clear();
        List<BasicTrack> titles = new ArrayList<BasicTrack>(dupes.keySet());
        Collections.sort(titles, new TrackComparator());
        for (BasicTrack title : titles) {
          dupeListModel.addElement(title);
          List<PlaylistEntry> infos = dupes.get(title);
          Collections.sort(infos, new Comparator<PlaylistEntry>() {

            @Override
            public int compare(PlaylistEntry o1, PlaylistEntry o2) {
              int result = o1.getPlaylist().getName().compareToIgnoreCase(o2.getPlaylist().getName());
              if (result == 0) {
                result = Integer.valueOf(o1.getEntry().getStart()).compareTo(o2.getEntry().getStart());
              }
              return result;
            }

          });

          for (PlaylistEntry info : infos) {
            dupeListModel.addElement(info);
          }
        }

      }

    });

    this.setDefaultCloseOperation(JDialog.DISPOSE_ON_CLOSE);

  }

}
