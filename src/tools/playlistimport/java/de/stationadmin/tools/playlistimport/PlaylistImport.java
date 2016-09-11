/**
 * 
 */
package de.stationadmin.tools.playlistimport;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import org.apache.commons.io.FileUtils;
import org.json.JSONException;

import de.stationadmin.base.StationAdminClient;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.playlist.validation.PlaylistValidationException;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.lfm.backend.LautfmAdminService;
import de.stationadmin.lfm.backend.Station;

/**
 * @author korf
 *
 */
public class PlaylistImport {
  
  private static String getFilename(Playlist playlist) {
    StringBuilder buf = new StringBuilder();
    for (char c : playlist.getName().toCharArray()) {
      if (Character.isLetterOrDigit(c) || c == ' ' || c == '-' || c == '_') {
        buf.append(c);
      } else {
        buf.append('_');
      }
    }
    return buf.toString() + ".lfm";
  }

  public static void importPlaylists(String token, String stationName, String dir) throws IOException, JSONException, PlaylistValidationException {
    
    LautfmAdminService adminService = new LautfmAdminService(token, "StationAdmin");
    Station station = null;
    for(Station availableStation :  adminService.getStations()) {
      if(availableStation.getName().equalsIgnoreCase(stationName)) {
        station = availableStation;
        break;
      }
    }
    
    if(station == null) {
      System.err.println("Die Station " + stationName + " ist mit dem angegeenen Token nicht zugreifbar");
      return;
    }
    
    StationAdminClient client = new StationAdminClient(adminService, station);
    client.load();
 
    
    PlaylistService playlistService = client.getPlaylistService();
    TrackService trackService = client.getTrackService();

    File df = new File(dir);
    File[] files = df.listFiles();
    if (files != null) {

      for (File file : files) {
        if (file.getName().endsWith(".lfm")) {
          System.out.println("lese " + file.getName());

          String name = file.getName().substring(0, file.getName().length() - 4);
          Playlist playlist = null;
          try {
            int id = Integer.parseInt(name);
            playlist = playlistService.getPlaylistRegistry().getPlaylist(id);
          } catch (Exception e) {
          }
          if (playlist == null) {
            // try to find by name
            for (Playlist p : playlistService.getPlaylistRegistry().getPlaylists(PlaylistType.ONLINE)) {
              // System.out.println(getFilename(playlist) + " <=> " + file.getName());
              if (getFilename(p).equalsIgnoreCase(file.getName())) {
                playlist = p;
              }
            }
          }

          if (playlist != null) {
            System.out.println("importiere " + playlist.getName());

            String str = FileUtils.readFileToString(file, "UTF-8");

            playlist.removeEntries(new ArrayList<Entry>(playlist.getEntries()));
            
            TrackImportHandler importHandler = new TrackImportHandler(trackService, client.getTagManager(), playlist, 0);
            importHandler.add(str);
            importHandler.resolveTags();
            importHandler.addTracksToPlaylist();
            playlistService.savePlaylist(playlist);
          } else {
            System.out.println("keine passende Playlist gefunden");
          }
        }
      }
    }

  }

  /**
   * @param args
   */
  public static void main(String[] args) throws Exception {
    
    if(args.length < 3) {
      System.err.println("PlaylistImport <token> <station> <verzeichnis>");
      System.exit(1);
    }
    
    String token = args[0];
    String station = args[1];
    String dir = args[2];
    importPlaylists(token, station, dir);
  }

}
