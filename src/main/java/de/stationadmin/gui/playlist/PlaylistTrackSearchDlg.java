/**
 * 
 */
package de.stationadmin.gui.playlist;

import java.awt.FlowLayout;
import java.awt.HeadlessException;
import java.awt.event.ActionEvent;
import java.util.List;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import com.jgoodies.binding.adapter.BasicComponentFactory;
import com.jgoodies.binding.value.ValueHolder;
import com.jgoodies.binding.value.ValueModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.util.PlaylistEntry;
import de.stationadmin.base.playlist.util.PlaylistTrackSearch;
import de.stationadmin.gui.ClientContext;

/**
 * Dialog for searching titles in all available playlists
 * 
 * @author Frank Korf
 */
public class PlaylistTrackSearchDlg extends JFrame {
  private static final long serialVersionUID = -3492478658440815088L;
  private ClientContext ctx;
  private ValueModel playlistHolder = new ValueHolder(null, true);
  private ValueModel entryHolder = new ValueHolder(null, true);
  private ValueModel query = new ValueHolder();

  private PlaylistTrackSearch searcher;
  private PlaylistEntryTableModel entryModel;

  public PlaylistTrackSearchDlg(ClientContext ctx) throws HeadlessException {
    super();
    this.ctx = ctx;
    this.entryModel = new PlaylistEntryTableModel(ctx);
    searcher = new PlaylistTrackSearch(ctx.getAdminClient().getTrackService().getTrackRegistry(), ctx.getAdminClient().getPlaylistService().getPlaylistRegistry());
    this.init();
  }

  private JPanel createSearchPanel() {
    Search search = new Search();
    JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEADING, 10, 10));
    JTextField tf = BasicComponentFactory.createTextField(this.query, false);
    tf.setColumns(20);
    tf.addActionListener(search);
    panel.add(tf);
    
    panel.add(new JButton(search));

    return panel;
  }

  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,120dlu,5dlu,150dlu:grow,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.getContentPane().add(this.createSearchPanel(), cc.xy(2, 2));
    this.getContentPane().add(new PlaylistEntryListViewer(this.entryModel, this.playlistHolder, this.entryHolder), cc.xy(2, 4));

    PlaylistViewer viewer = new PlaylistViewer(ctx, playlistHolder, this.entryHolder);
    this.getContentPane().add(viewer, cc.xy(2, 6, CellConstraints.FILL, CellConstraints.FILL));
    
    this.setTitle(ctx.getString("playlistsearch.title"));
    this.setSize(600, 600);

  }

  class Search extends AbstractAction {
    private static final long serialVersionUID = -2481383716638881154L;

    Search() {
      this.putValue(Action.NAME, "Suchen");
    }

    /**
     * @see java.awt.event.ActionListener#actionPerformed(java.awt.event.ActionEvent)
     */
    @Override
    public void actionPerformed(ActionEvent e) {
      String str = (String)query.getValue();
      if (str != null && str.length() > 0) {
        List<PlaylistEntry> result = searcher.search(str);
        entryModel.setEntries(result);
        playlistHolder.setValue(null);
        if(result.size() == 0) {
          JOptionPane.showMessageDialog(PlaylistTrackSearchDlg.this, ctx.getString("playlistsearch.msg.nomatch"), null, JOptionPane.INFORMATION_MESSAGE);
        }
      }
    }
    

  }

}
