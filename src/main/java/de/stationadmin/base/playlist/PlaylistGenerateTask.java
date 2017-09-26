/**
 * 
 */
package de.stationadmin.base.playlist;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.tasks.AbstractTask;
import de.stationadmin.base.tasks.TaskExecutionResult;
import de.stationadmin.base.util.PlaylistGeneratorFactory;

/**
 * @author korf
 * 
 */
public class PlaylistGenerateTask extends AbstractTask {
  private int hours = 2;
  private String playlistName;

  private boolean artistPenaltyEnabled = true;
  private boolean titlePenaltyEnabled = true;
  private boolean titlePenaltyStrictEnabled = true;

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

    PlaylistGenerator generator = PlaylistGeneratorFactory.createGenerator(client);
    generator.setArtistPenaltyEnabled(artistPenaltyEnabled);
    generator.setTrackPenaltyEnabled(titlePenaltyEnabled);
    if (generator.isTrackPenaltyEnabled() && titlePenaltyStrictEnabled) {
      generator.setTrackPenaltyMax(600);
      generator.setTrackPenaltyPeriod(0);
    }

    ArrayList<Playlist> playlists = new ArrayList<Playlist>();

    List<Playlist> allPlaylists = this.hours > 0 ? client.getSchedule().getPlaylistsAfter(new Date(), hours) : client.getPlaylistService()
        .getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE);
    for (Playlist playlist : allPlaylists) {
      if (playlist.isGenerate()) {
        if (this.playlistName == null || playlist.getName().equalsIgnoreCase(this.playlistName)) {
          playlists.add(playlist);
        }
      }
    }

    for (Playlist playlist : playlists) {
      try {
        generator.generate(playlist);
        client.getPlaylistService().savePlaylist(playlist);
        result.addMessage(false, "playlist.generate", playlist.getName());
      } catch (Exception e) {
        result.addMessage(true, "playlist.generate.failed", playlist.getName(), e.getMessage() != null ? e.getMessage() : e.getLocalizedMessage());
      }
    }

    if (this.restartStation) {
      try {
        client.startRadio();
        result.addMessage(false, "station.restart");
      } catch (Exception e) {
        result.addMessage(true, "station.restart.failed", e.getMessage() != null ? e.getMessage() : e.getLocalizedMessage());
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

  public boolean isArtistPenaltyEnabled() {
    return artistPenaltyEnabled;
  }

  public void setArtistPenaltyEnabled(boolean artistPenaltyEnabled) {
    this.artistPenaltyEnabled = artistPenaltyEnabled;
  }

  public boolean isTitlePenaltyEnabled() {
    return titlePenaltyEnabled;
  }

  public void setTitlePenaltyEnabled(boolean titlePenaltyEnabled) {
    this.titlePenaltyEnabled = titlePenaltyEnabled;
  }

  public boolean isTitlePenaltyStrictEnabled() {
    return titlePenaltyStrictEnabled;
  }

  public void setTitlePenaltyStrictEnabled(boolean titlePenaltyStrictEnabled) {
    this.titlePenaltyStrictEnabled = titlePenaltyStrictEnabled;
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
