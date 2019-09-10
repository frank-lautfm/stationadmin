/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * Provides access to the public Laut.fm API
 * 
 * @author Frank
 */
public class LautfmService {
  public static String BASE_URL = "http://api.laut.fm/";

  private DefaultHttpClient client;

  public LautfmService() {
    this.client = this.createClient();
  }

  public LautfmService(DefaultHttpClient client) {
    this.client = client;
  }

  private DefaultHttpClient createClient() {
    DefaultHttpClient client = new DefaultHttpClient();
    client.getParams().setParameter("http.useragent",
        "Mozilla/4.0 (compatible; Laut.fm API 4 Java; " + System.getProperty("os.name") + ")");
    return client;
  }
  
  public String get(String path) throws IOException {
    String url = "https://api.laut.fm/" + path;
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      return raw;
    }
    
  }

  /**
   * Gets the basic info about a single station.
   * @param station name of the station
   * @return station info
   * @throws IOException
   * @throws JSONException
   */
  public Station getStation(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station;
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONObject obj = new JSONObject(raw);
      return new Station(obj);
    }
  }

  /**
   * Gets the currently playing song of a single station.
   * @param station name of the station
   * @return song
   * @throws IOException
   * @throws JSONException
   */
  public Song getCurrentSong(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station + "/current_song";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONObject obj = new JSONObject(raw);
      return new Song(obj);
    }
  }

  /**
   * Gets the 10 last songs of a single station.
   * @param station
   * @return last 10 songs
   * @throws IOException
   * @throws JSONException
   */
  public Song[] getLastSongs(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station + "/last_songs";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONArray array = new JSONArray(raw);
      Song[] songs = new Song[array.length()];
      for (int i = 0; i < array.length(); i++) {
        songs[i] = new Song(array.getJSONObject(i));
      }
      return songs;
    }
  }

  public SchedulerEntry[] getSchedule(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station + "/schedule";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONArray array = new JSONArray(raw);
      SchedulerEntry[] entries = new SchedulerEntry[array.length()];
      for (int i = 0; i < array.length(); i++) {
        entries[i] = new SchedulerEntry(array.getJSONObject(i));
      }
      return entries;
    }

  }

  public PlaylistSchedules[] getPlaylists(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station + "/playlists";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONArray array = new JSONArray(raw);
      PlaylistSchedules[] entries = new PlaylistSchedules[array.length()];
      for(int i = 0; i < array.length(); i++) {
        entries[i] = new PlaylistSchedules(array.getJSONObject(i));
      }
      return entries;
    }
  }

  public NetworkEntry[] getNetwork(String station) throws IOException, JSONException {
    String url = BASE_URL + "station/" + station + "/network";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      JSONArray array = new JSONArray(raw);
      NetworkEntry[] entries = new NetworkEntry[array.length()];
      for (int i = 0; i < array.length(); i++) {
        entries[i] = new NetworkEntry(array.getJSONObject(i));
      }
      return entries;

    }

  }
  
  public Date getTime() throws IOException, ParseException {
    String url = BASE_URL + "time";
    HttpGet action = new HttpGet(url);
    synchronized (this.client) {
      HttpResponse response = this.client.execute(action);
      String raw = IOUtils.toString(response.getEntity().getContent(), "UTF-8");
      if(raw.length() > 2) {
        if(raw.charAt(0) == '"') {
          raw = raw.substring(1);
        }
        if(raw.charAt(raw.length() - 1) == '"') {
          raw = raw.substring(0, raw.length() - 1);
        }
      }
      
      SimpleDateFormat df = new SimpleDateFormat(JSONUtil.DEFAULT_DATE_FORMAT);
      return df.parse(raw);
    }
    
  }

}
