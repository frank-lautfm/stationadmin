/**
 * 
 */
package de.emjoy.stationadmin.test.adminservice;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.stationadmin.lfm.backend.AuthenticationException;
import de.stationadmin.lfm.backend.ExtendedPlaylistHead;
import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfm.backend.Playlist;
import de.stationadmin.lfm.backend.PlaylistHead;
import de.stationadmin.lfm.backend.ResourceNotFoundException;
import de.stationadmin.lfm.backend.Station;
import de.stationadmin.lfm.backend.TrackList;

/**
 * @author korf
 * 
 */
public class TestAdminService {
  private static final Logger log = Logger.getLogger(TestAdminService.class);
  private String token;
  private String origin = "StationAdmin";

  private LautfmAdminService service;

  @Before
  public void setUp() throws Exception {
    BasicConfigurator.configure();
    Logger.getRootLogger().setLevel(Level.INFO);
    Properties props = new Properties();
    props.load(this.getClass().getClassLoader().getResourceAsStream("account.properties"));
    this.token = props.getProperty("token");
    this.service = new LautfmAdminService(this.token, this.origin);
  }

  @Test
  public void testGetStations() throws Exception {
    log.info("- test getStations -");

    // valid access
    {
      List<Station> stations = this.service.getStations();
      Assert.assertTrue("expected stations", stations != null && stations.size() > 0);
      Station station = stations.get(0);
      Assert.assertTrue("station id", station.getId() > 0);
      Assert.assertNotNull("name", station.getName());
      Assert.assertNotNull("created", station.getCreatedAt());
      Assert.assertNotNull("updated", station.getUpdatedAt());
    }

    // incorrect authentication
    {
      try {
        this.service = new LautfmAdminService("xxx", this.origin);
        this.service.getStations();
        Assert.fail("expected authentication error");
      } catch (AuthenticationException e) {
        // expected
      }
    }
  }

  @Test
  public void testPlaylistManagement() throws Exception {
    log.info("- test playlist management -");

    List<Station> stations = this.service.getStations();
    int stationId = stations.get(0).getId();

    List<ExtendedPlaylistHead> playlists = this.service.getPlaylists(stationId);
    log.info("found " + playlists.size() + " playlists");

    log.info("create a new playlist");
    PlaylistHead newPlaylist = new PlaylistHead();
    int playlistId = 0;
    {
      newPlaylist.setTitle("Playlist " + UUID.randomUUID().toString());
      newPlaylist.setDescription("Description for " + newPlaylist.getTitle());
      newPlaylist.setColor("#123456");
      newPlaylist.setShuffled(true);
      newPlaylist = this.service.createPlaylist(stationId, newPlaylist);
      playlistId = newPlaylist.getId();
      log.info("created playlist with id " + newPlaylist.getId());

      Playlist retrieved = this.service.getPlaylist(stationId, playlistId);
      Assert.assertEquals("title", newPlaylist.getTitle(), retrieved.getTitle());
      Assert.assertEquals("description", newPlaylist.getDescription(), retrieved.getDescription());
      Assert.assertEquals("color", newPlaylist.getColor(), retrieved.getColor());
      Assert.assertEquals("shuffled", newPlaylist.isShuffled(), retrieved.isShuffled());
      Assert.assertNotNull("created", retrieved.getCreatedAt());
      Assert.assertNotNull("updated", retrieved.getUpdatedAt());
    }

    log.info("add some tracks");
    {
      TrackList tracks = this.service.getTracks(stationId, 0, new HashMap<String, String>(), null, true);
      int[] trackIds = new int[Math.min(3, tracks.getTracks().length)];
      for (int i = 0; i < tracks.getTracks().length && i < 3; i++) {
        trackIds[i] = tracks.getTracks()[i].getId();
      }
      this.service.setPlaylistTracks(stationId, playlistId, trackIds);

      Playlist retrieved = this.service.getPlaylist(stationId, playlistId);
      Assert.assertEquals("number of tracks", trackIds.length, retrieved.getEntries().length);
      for (int i = 0; i < trackIds.length; i++) {
        Assert.assertEquals("track " + i, trackIds[i], retrieved.getEntries()[i].getTrackId());
      }

    }

    log.info("update playlist");
    {
      newPlaylist.setTitle("Modified " + newPlaylist.getTitle());
      Playlist retrieved = this.service.updatePlaylist(stationId, newPlaylist);
      Assert.assertEquals("title", newPlaylist.getTitle(), retrieved.getTitle());
    }

    log.info("delete playlist");
    this.service.deletePlaylist(stationId, playlistId);
    try {
      this.service.getPlaylist(stationId, playlistId);
      Assert.fail("expected exception when trying to delete non-existen playlist");
    } catch (ResourceNotFoundException e) {
      // expected
    }

  }
  
  @Test
  public void testTrackSearch() throws Exception {
    
  }
}
