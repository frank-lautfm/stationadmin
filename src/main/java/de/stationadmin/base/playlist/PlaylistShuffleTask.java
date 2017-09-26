/**
 * 
 */
package de.stationadmin.base.playlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.shuffle.PlaylistShuffler;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.TaskExecutionResult;
import de.stationadmin.base.util.PlaylistGeneratorFactory;

/**
 * @author korf
 * 
 */
public class PlaylistShuffleTask extends AbstractTask {
	private int hours = 2;
	private String playlistName;
	private boolean restartStation = false;
	private boolean synchronize = false;

	/**
	 * 
	 */
	@Override
	public TaskExecutionResult execute(StationAdminClient client) {
		
		TaskExecutionResult result = new TaskExecutionResult();
		if(this.synchronize) {
			try {
				client.synchronize();
			} catch (Exception e) {
				result.addMessage(false, "playlist.shuffle.synchronizefailed");
			}
		}
		
		PlaylistShuffler shuffler = PlaylistGeneratorFactory.createShuffler(client);
		shuffler.setProtectFirstJingle(client.getSettings().isShuffleProtectFirstJingle());
		shuffler.setJingleInterval(client.getSettings().getShuffleJingleInterval());
		shuffler.setWordDistribution(client.getSettings().getShuffleWordDistributionStrategy());

		ArrayList<Playlist> playlists = new ArrayList<Playlist>();

		List<Playlist> allPlaylists = this.hours > 0 ? client.getSchedule().getPlaylistsAfter(new Date(), hours)
				: client.getPlaylistService().getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
		for (Playlist playlist : allPlaylists) {
			if (playlist.isLocalShuffleAllowed()) {
				if (this.playlistName == null || playlist.getName().equalsIgnoreCase(this.playlistName)) {
					playlists.add(playlist);
				}
			}
		}

		for (Playlist playlist : playlists) {
			try {
				shuffler.shuffle(playlist);
				client.getPlaylistService().savePlaylist(playlist);
				result.addMessage(false, "playlist.shuffle", playlist.getName());
			} catch (Exception e) {
				result.addMessage(true, "playlist.shuffle.failed", playlist.getName(),
						e.getMessage() != null ? e.getMessage() : e.getLocalizedMessage());
			}
		}

		if (this.restartStation) {
			try {
				client.startRadio();
				result.addMessage(false, "station.restart");
			} catch (Exception e) {
				result.addMessage(true, "station.restart.failed",
						e.getMessage() != null ? e.getMessage() : e.getLocalizedMessage());
			}
		}

		return result;
	}

	public int getHours() {
		return hours;
	}

	public void setHours(int hours) {
		this.hours = hours;
	}

	public String getPlaylistName() {
		return playlistName;
	}

	public void setPlaylistName(String playlistName) {
		this.playlistName = playlistName;
	}

	public boolean isRestartStation() {
		return restartStation;
	}

	public void setRestartStation(boolean restartStation) {
		this.restartStation = restartStation;
	}

	public boolean isSynchronize() {
		return synchronize;
	}

	public void setSynchronize(boolean synchronize) {
		this.synchronize = synchronize;
	}

}
