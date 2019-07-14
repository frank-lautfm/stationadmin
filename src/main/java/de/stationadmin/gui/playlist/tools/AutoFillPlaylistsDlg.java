package de.stationadmin.gui.playlist.tools;

import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;

import org.jdesktop.swingx.JXErrorPane;

import com.jgoodies.binding.list.IndirectListModel;
import com.jgoodies.forms.layout.CellConstraints;
import com.jgoodies.forms.layout.FormLayout;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistNameCompator;
import de.stationadmin.base.playlist.util.PlaylistFiller;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;

public class AutoFillPlaylistsDlg extends JDialog {
  private static final long serialVersionUID = 5509519002944425083L;
  private ClientContext ctx;
  private JList<Playlist> list;
  private PlaylistFiller filler;

  public AutoFillPlaylistsDlg(ClientContext ctx) {
    this.ctx = ctx;
    this.filler = new PlaylistFiller(ctx.getAdminClient().getSettings(), ctx.getAdminClient().getPlaylistService().getPlaylistRegistry(),
        ctx.getAdminClient().getTrackService(), ctx.getAdminClient().getTagManager());
    this.init();
  }

  private ArrayList<Playlist> getPlaylists() {
    ArrayList<Playlist> playlists = new ArrayList<>();
    for (Playlist p : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
      if (p.getAutoFillRule().isEnabled()) {
        playlists.add(p);
      }
    }
    Collections.sort(playlists, new PlaylistNameCompator());
    return playlists;
  }

  @SuppressWarnings("unchecked")
  private void init() {
    this.getContentPane().setLayout(new FormLayout("5dlu,pref:grow,5dlu", "5dlu,pref,5dlu,pref:grow,5dlu,pref,5dlu"));
    CellConstraints cc = new CellConstraints();

    this.getContentPane().add(new JLabel(ctx.getString("autofill.dlg.label")), cc.xy(2, 2));

    IndirectListModel<Playlist> model = new IndirectListModel<>(getPlaylists());
    list = new JList<Playlist>(model);
    list.getSelectionModel().setSelectionMode(ListSelectionModel.MULTIPLE_INTERVAL_SELECTION);
    list.getSelectionModel().setSelectionInterval(0, model.getSize());
    this.getContentPane().add(new JScrollPane(list), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

    {
      JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
      buttonPanel.add(new JButton(new FillAction()));
      buttonPanel.add(new JButton(new DisposeAction(this, ctx.getString("cancel"))));
      this.getContentPane().add(buttonPanel, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));
    }

    this.setSize(400, 400);
    this.setTitle(ctx.getString("autofill.dlg.title"));
    AppUtils.centerWithinRoot(this);

  }

  private class FillAction extends AbstractAction {
    private static final long serialVersionUID = 4730621907525954549L;

    FillAction() {
      this.putValue(Action.NAME, ctx.getString("ok"));
    }

    @Override
    public void actionPerformed(ActionEvent evt) {

      for (Playlist playlist : list.getSelectedValuesList()) {
        try {
          filler.fillPlaylist(playlist);
          ctx.getAdminClient().getPlaylistService().savePlaylist(playlist);
          dispose();
        } catch (Exception e) {
          JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "action.playlist.fill.error"));
        }

      }


    }

  }

}
