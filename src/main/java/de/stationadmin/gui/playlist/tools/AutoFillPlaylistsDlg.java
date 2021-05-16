package de.stationadmin.gui.playlist.tools;

import java.awt.Dimension;
import java.awt.GridLayout;
import java.util.ArrayList;
import java.util.Collections;

import javax.swing.Action;
import javax.swing.JButton;
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
import de.stationadmin.base.playlist.util.MissingSourceTracksException;
import de.stationadmin.base.playlist.util.PlaylistFiller;
import de.stationadmin.gui.ClientContext;
import de.stationadmin.gui.StationAdminDialog;
import de.stationadmin.gui.util.AppUtils;
import de.stationadmin.gui.util.DisposeAction;
import de.stationadmin.gui.util.ThreadedAction;

public class AutoFillPlaylistsDlg extends StationAdminDialog {
	private static final long serialVersionUID = 5509519002944425083L;
	private JList<Playlist> list;
	private PlaylistFiller filler;

	public AutoFillPlaylistsDlg(ClientContext ctx) {
		super(ctx, "AutoFill");
		this.filler = new PlaylistFiller(ctx.getAdminClient().getPlaylistService(), ctx.getAdminClient().getTrackService(),
				ctx.getAdminClient().getTagManager());
		this.init();
	}

	private ArrayList<Playlist> getPlaylists() {
		ArrayList<Playlist> playlists = new ArrayList<>();
		for (Playlist p : ctx.getAdminClient().getPlaylistService().getPlaylistRegistry()
				.getPlaylists(PlaylistType.ONLINE)) {
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
		list.getSelectionModel().setSelectionInterval(0, model.getSize() - 1);
		this.getContentPane().add(new JScrollPane(list), cc.xy(2, 4, CellConstraints.FILL, CellConstraints.FILL));

		{
			JPanel buttonPanel = new JPanel(new GridLayout(1, 2, 5, 5));
			buttonPanel.add(new JButton(new FillAction(this.ctx)));
			buttonPanel.add(new JButton(new DisposeAction(this, ctx.getString("cancel"))));
			this.getContentPane().add(buttonPanel, cc.xy(2, 6, CellConstraints.CENTER, CellConstraints.CENTER));
		}

		this.setTitle(ctx.getString("autofill.dlg.title"));
	}

	@Override
	protected Dimension getDefaultSize() {
		return new Dimension(400, 400);
	}

	private class FillAction extends ThreadedAction {

		private static final long serialVersionUID = 4730621907525954549L;


		private Playlist currentPlaylist;

		
		public FillAction(ClientContext ctx) {
			super(ctx);
			this.putValue(Action.NAME, ctx.getString("ok"));
		}


		@Override
		protected String getStatus() {
			return ctx.getString("action.playlist.fill.status", currentPlaylist != null ? currentPlaylist.getName() : "");
		}

		@Override
		protected void performAction() throws Exception {
			for (Playlist playlist : list.getSelectedValuesList()) {
				currentPlaylist = playlist;
				filler.fillPlaylist(playlist);
				ctx.getAdminClient().getPlaylistService().savePlaylist(playlist);
			}
		}

		@Override
		protected void showError(Exception e) {
			if (e instanceof MissingSourceTracksException) {
				JXErrorPane.showDialog(AppUtils.getRootFrame(),
						ctx.createErrorInfo(e, "action.playlist.fill.error.missingsource", e.getMessage()));
			} else {
				JXErrorPane.showDialog(AppUtils.getRootFrame(), ctx.createErrorInfo(e, "action.playlist.fill.error"));
			}


		}

		@Override
		protected void onSuccess() {
			dispose();
		}

	}

}
