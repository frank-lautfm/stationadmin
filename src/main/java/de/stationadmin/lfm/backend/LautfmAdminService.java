/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.X509TrustManager;

import org.apache.http.HttpHost;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.FileEntity;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.AbstractHttpMessage;
import org.codehaus.jackson.map.ObjectMapper;

import de.stationadmin.base.Version;

/**
 * @author korf
 * 
 */
public class LautfmAdminService {
  private static final String BASE_URL = "https://api.radioadmin.laut.fm";

  private String token;
  private String origin;

  private CloseableHttpClient client;

  public LautfmAdminService(String token, String origin) {
    super();
    this.token = token;
    this.origin = origin;
    this.client = createClient();
  }

  private CloseableHttpClient createClient() {
    try {
      SSLContextBuilder builder = new SSLContextBuilder();
      builder.loadTrustMaterial(null, new TrustStrategy() {
        @Override
        public boolean isTrusted(X509Certificate[] chain, String authType) throws CertificateException {
          return true;
        }
      });
      SSLConnectionSocketFactory sslsf = new SSLConnectionSocketFactory(builder.build());

      HttpClientBuilder hcBuilder = HttpClients.custom();
      hcBuilder.setSSLSocketFactory(sslsf).build();
      hcBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("localhost", 8888)));
      hcBuilder.setUserAgent("Mozilla/4.0 (compatible; Station Admin " + Version.VERSION + "; " + System.getProperty("os.name") + ")");

      return hcBuilder.build();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  void disableCertificates() {
    TrustManager[] trustAllCerts = new TrustManager[] { new X509TrustManager() {

      @Override
      public java.security.cert.X509Certificate[] getAcceptedIssuers() {
        return null;
      }

      @Override
      public void checkClientTrusted(java.security.cert.X509Certificate[] certs, String authType) {
      }

      @Override
      public void checkServerTrusted(java.security.cert.X509Certificate[] certs, String authType) {
      }
    } };

    // Install the all-trusting trust manager
    try {
      SSLContext sc = SSLContext.getInstance("SSL");
      sc.init(null, trustAllCerts, new java.security.SecureRandom());
      HttpsURLConnection.setDefaultSSLSocketFactory(sc.getSocketFactory());
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  private void addAuthHeaders(AbstractHttpMessage request) {
    request.addHeader("Authorization", "Bearer " + this.token);
    // request.addHeader("TOKEN", this.token);
    request.addHeader("ORIGIN", this.origin);
  }

  @SuppressWarnings("unchecked")
  private Map<String, Object> deserializeAsMap(CloseableHttpResponse response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    Map<String, Object> map = mapper.readValue(response.getEntity().getContent(), Map.class);
    return map;
  }

  @SuppressWarnings("unchecked")
  private String getErrorMessage(CloseableHttpResponse response) throws IOException {

    Map<String, Object> map = this.deserializeAsMap(response);
    Map<String, Object> errors = (Map<String, Object>) map.get("_errors");

    StringBuffer buf = new StringBuffer();
    if (errors != null) {
      for (Entry<String, Object> entry : errors.entrySet()) {
        if (buf.length() > 0) {
          buf.append("; ");
        }
        buf.append(entry.getKey() + ": " + entry.getValue());
      }
    }

    return buf.toString();
  }

  private void checkResponse(CloseableHttpResponse response) throws IOException {
    switch (response.getStatusLine().getStatusCode()) {
    case 400:
      throw new AdminServiceException(getErrorMessage(response));
    case 401:
      throw new AuthenticationException();
    case 404:
      throw new ResourceNotFoundException();
    }
  }

  private CloseableHttpResponse doGet(String path) throws IOException {
    HttpGet request = new HttpGet(BASE_URL + path);
    this.addAuthHeaders(request);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);
    return response;
  }

  private CloseableHttpResponse doDelete(String path) throws IOException {
    HttpDelete request = new HttpDelete(BASE_URL + path);
    this.addAuthHeaders(request);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);
    return response;
  }

  private CloseableHttpResponse doPatch(String path, Object content) throws IOException {
    HttpPatch request = new HttpPatch(BASE_URL + path);
    this.addAuthHeaders(request);

    ObjectMapper mapper = new ObjectMapper();
    StringEntity entity = new StringEntity(mapper.writeValueAsString(content), ContentType.APPLICATION_JSON);
    request.setEntity(entity);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);

    return response;

  }

  private CloseableHttpResponse doPost(String path, Object content) throws IOException {
    HttpPost request = new HttpPost(BASE_URL + path);
    this.addAuthHeaders(request);

    ObjectMapper mapper = new ObjectMapper();
    StringEntity entity = new StringEntity(mapper.writeValueAsString(content), ContentType.APPLICATION_JSON);
    // entity.setContentEncoding(new BasicHeader(HTTP.CONTENT_TYPE,
    // "application/json"));
    request.setEntity(entity);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);

    return response;

  }

  public List<Station> getStations() throws IOException {
    CloseableHttpResponse response = this.doGet("/stations");
    ObjectMapper mapper = new ObjectMapper();
    StationList list = mapper.readValue(response.getEntity().getContent(), StationList.class);
    response.close();
    return Arrays.asList(list.getStations());
  }

  public List<PlaylistHead> getPlaylists(int stationId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/playlists");
    ObjectMapper mapper = new ObjectMapper();
    PlaylistHeadList list = mapper.readValue(response.getEntity().getContent(), PlaylistHeadList.class);
    response.close();
    return Arrays.asList(list.getPlaylists());
  }

  public Playlist getPlaylist(int stationId, int playlistId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/playlists/" + playlistId);
    ObjectMapper mapper = new ObjectMapper();
    Playlist playlist = mapper.readValue(response.getEntity().getContent(), Playlist.class);
    response.close();
    return playlist;
  }

  private TrackList getTracks(CloseableHttpResponse response) throws IOException {
    ObjectMapper mapper = new ObjectMapper();
    TrackList list = mapper.readValue(response.getEntity().getContent(), TrackList.class);
    response.close();
    return list;
  }

  public TrackList getTracks(int stationId, int page, Map<String, String> filter, String orderBy, boolean orderAscending) throws IOException {
    StringBuilder queryString = new StringBuilder();
    if (page > 1) {
      queryString.append("page=" + page);
    }
    if (filter != null) {
      for (Entry<String, String> entry : filter.entrySet()) {
        if (queryString.length() > 0) {
          queryString.append('&');
        }
        queryString.append(URLEncoder.encode(entry.getKey(), "UTF-8"));
        queryString.append('=');
        queryString.append(URLEncoder.encode(entry.getValue(), "UTF-8"));
      }
    }
    if (orderBy != null) {
      if (queryString.length() > 0) {
        queryString.append('&');
      }
      queryString.append("order=" + URLEncoder.encode(orderBy, "UTF-8"));
      queryString.append(orderAscending ? "+ASC" : "+DESC");

    }

    return this.getTracks(this.doGet("/stations/" + stationId + "/tracks" + (queryString.length() > 0 ? "?" + queryString.toString() : "")));
  }

  public TrackList getTracksQueued(int stationId) throws IOException {
    return this.getTracks(this.doGet("/stations/" + stationId + "/tracks;queued"));
  }

  public TrackList getTracksIncomplete(int stationId) throws IOException {
    return this.getTracks(this.doGet("/stations/" + stationId + "/tracks;incomplete"));
  }

  public List<Track> getTracks(int stationId, int... trackIds) throws IOException {
    ArrayList<Track> list = new ArrayList<Track>();

    String basePath = "/stations/" + stationId + "/tracks/";
    int baseLen = BASE_URL.length() + basePath.length();
    int maxLen = 800 - baseLen - 10;

    StringBuilder idLIst = new StringBuilder();
    for (int trackId : trackIds) {
      if (trackId != 0) {
        if (idLIst.length() > 0) {
          idLIst.append(',');
        }
        idLIst.append(trackId);

        if (idLIst.length() > maxLen) {
          TrackList trackList = this.getTracks(this.doGet(basePath + idLIst.toString()));
          list.addAll(Arrays.asList(trackList.getTracks()));
          idLIst.setLength(0);
        }
      }
    }

    if (idLIst.length() > 0) {
      TrackList trackList = this.getTracks(this.doGet(basePath + idLIst.toString()));
      list.addAll(Arrays.asList(trackList.getTracks()));
    }

    return list;
  }

  public Track getTrack(int stationId, int trackId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/" + trackId);
    TrackList list = this.getTracks(response);
    return list.getTracks().length > 0 ? list.getTracks()[0] : null;
  }

  public String getTrackPrelistenUrl(int stationId, int trackId) throws IOException {
    List<Track> tracks = this.getTracks(stationId, trackId);
    if (tracks.size() > 0) {
      return BASE_URL + tracks.get(0).getLinks().getPrelistenAuthless().getHref();
    } else {
      return null;
    }
  }

  public void setPlaylistTracks(int stationId, int playlistId, int[] trackIds) throws IOException {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("entries", trackIds);
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/playlists/" + playlistId, map);
    response.close();
  }

  public PlaylistHead createPlaylist(int stationId, PlaylistHead playlist) throws IOException {
    CloseableHttpResponse response = this.doPost("/stations/" + stationId + "/playlists", playlist);
    try {
      if (response.getStatusLine().getStatusCode() == 201) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.getEntity().getContent(), PlaylistHead.class);
      } else {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } finally {
      response.close();
    }
  }

  public void deletePlaylist(int stationId, int playlistId) throws IOException {
    CloseableHttpResponse response = this.doDelete("/stations/" + stationId + "/playlists/" + playlistId);
    try {
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } finally {
      response.close();
    }
  }

  public Playlist updatePlaylist(int stationId, PlaylistHead playlist) throws IOException {
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/playlists/" + playlist.getId(), playlist);
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(response.getEntity().getContent(), Playlist.class);

    } finally {
      response.close();
    }
  }

  public Track updateTrack(int stationId, Track track) throws IOException {
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/tracks/" + track.getId(), track);
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(response.getEntity().getContent(), Track.class);

    } finally {
      response.close();
    }
  }

  public Map<Integer, Track> tagTracks(int stationId, String tag, int... trackIds) throws IOException {
    TagList tags = new TagList(tag);
    String prefix = "/stations/" + stationId + "/tracks/";
    String suffix = "/tags";
    StringBuilder trackList = new StringBuilder();
    Map<Integer, Track> tracks = new HashMap<Integer, Track>();
    for (int i = 0; i < trackIds.length; i++) {
      if (trackList.length() > 0) {
        trackList.append(',');
      }
      trackList.append(trackIds[i]);

      if (trackList.length() > 800) {
        CloseableHttpResponse response = this.doPost(prefix + trackList.toString() + suffix, tags);
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new AdminServiceException(this.getErrorMessage(response));
        }
        for (Track track : this.getTracks(response).getTracks()) {
          tracks.put(track.getId(), track);
        }
        trackList.setLength(0);
      }

    }
    if (trackList.length() > 0) {
      CloseableHttpResponse response = this.doPost(prefix + trackList.toString() + suffix, tags);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
      for (Track track : this.getTracks(response).getTracks()) {
        tracks.put(track.getId(), track);
      }
    }
    return tracks;
  }

  public void untagTracks(int stationId, String tag, int... trackIds) throws IOException {
    String prefix = "/stations/" + stationId + "/tracks/";
    String suffix = "/tags/" + URLEncoder.encode(tag, "UTF-8");
    StringBuilder trackList = new StringBuilder();
    for (int i = 0; i < trackIds.length; i++) {
      if (trackList.length() > 0) {
        trackList.append(',');
      }
      trackList.append(trackIds[i]);

      if (trackList.length() > 800) {
        CloseableHttpResponse response = this.doDelete(prefix + trackList.toString() + suffix);
        if (response.getStatusLine().getStatusCode() != 200) {
          throw new AdminServiceException(this.getErrorMessage(response));
        }
        response.close();
        trackList.setLength(0);
      }

    }
    if (trackList.length() > 0) {
      CloseableHttpResponse response = this.doDelete(prefix + trackList.toString() + suffix);
      if (response.getStatusLine().getStatusCode() != 200) {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
      response.close();
    }
  }

  public List<String> getTags(int stationId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/tags");
    try {
      ObjectMapper mapper = new ObjectMapper();
      return Arrays.asList(mapper.readValue(response.getEntity().getContent(), String[].class));
    } finally {
      response.close();
    }
  }

  public int[] getTaggedTracks(int stationId, String tag) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/tags/" + URLEncoder.encode(tag, "UTF-8"));
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(response.getEntity().getContent(), int[].class);
    } finally {
      response.close();
    }
  }

  @SuppressWarnings("unchecked")
  public HashMap<Integer, Integer> getTrackMappings() throws IOException {
    HashMap<Integer, Integer> map = new HashMap<Integer, Integer>();
    int offset = 0;
    HashMap<Object, Object> result;
    do {
      CloseableHttpResponse response = this.doGet("/track_mappings/" + offset);
      try {
        ObjectMapper mapper = new ObjectMapper();
        result = mapper.readValue(response.getEntity().getContent(), HashMap.class);
        for (Entry<Object, Object> entry : result.entrySet()) {
          Integer key, value;
          if (entry.getKey() instanceof Integer) {
            key = (Integer) entry.getKey();
          } else {
            key = Integer.parseInt(entry.getKey().toString());
          }
          if (entry.getValue() instanceof Integer) {
            value = (Integer) entry.getValue();
          } else {
            value = Integer.parseInt(entry.getValue().toString());
          }
          map.put(key, value);
        }
        offset += result.size();
        System.out.println(result.size() + "/" + map.size() + " tracks");
      } finally {
        response.close();
      }
    } while (result.size() > 0);
    return map;
  }

  public Track uploadTrack(int stationId, File file, final ProgressListener progressListener) throws IOException {
    final HttpPost filePost = new HttpPost(BASE_URL + "/stations/" + stationId + "/tracks");
    if (progressListener != null) {
      progressListener.setMaxValue((int) file.length());
    }
    this.addAuthHeaders(filePost);

    FileEntity fileEntity = new FileEntity(file, ContentType.create("audio/mp3")) {

      @Override
      public void writeTo(OutputStream out) throws IOException {
        if (progressListener != null) {
          super.writeTo(new CountingOutputStream(out, progressListener));
        } else {
          super.writeTo(out);
        }
      }
    };
    filePost.setEntity(fileEntity);

    CloseableHttpClient uploadClient = createClient();

    UploadAbortMonitor monitor = new UploadAbortMonitor(filePost, progressListener);
    monitor.start();
    try {
      CloseableHttpResponse response = uploadClient.execute(filePost);
      this.checkResponse(response);
      if (response.getStatusLine().getStatusCode() == 201) {
        ObjectMapper mapper = new ObjectMapper();
        return mapper.readValue(response.getEntity().getContent(), Track.class);
      } else {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } finally {
      monitor.requestStop();
    }

  }

  public Schedule getSchedule(int stationId) throws IOException {
    CloseableHttpResponse response = doGet("/stations/" + stationId + "/schedule");
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(response.getEntity().getContent(), Schedule.class);
    } finally {
      response.close();
    }
  }

  public Schedule updateSchedule(int stationId, Schedule schedule) throws IOException {
    CloseableHttpResponse response = doPatch("/stations/" + stationId + "/schedule", schedule);
    try {
      ObjectMapper mapper = new ObjectMapper();
      return mapper.readValue(response.getEntity().getContent(), Schedule.class);
    } finally {
      response.close();
    }
  }

  public String getToken() {
    return token;
  }

  public String getOrigin() {
    return origin;
  }

  private static class UploadAbortMonitor extends Thread {
    private HttpPost post;
    private ProgressListener progressListener;
    private volatile boolean stop = false;

    /**
     * @param post
     * @param progressListener
     */
    private UploadAbortMonitor(HttpPost post, ProgressListener progressListener) {
      super();
      this.setPriority(Thread.MIN_PRIORITY);
      this.setDaemon(true);
      this.post = post;
      this.progressListener = progressListener;
    }

    /**
     * @see java.lang.Thread#run()
     */
    @Override
    public void run() {
      while (!stop) {
        try {
          Thread.sleep(500);
        } catch (Exception e) {
        }
        if (this.progressListener != null && this.progressListener.isAbortCurrent() && !post.isAborted()) {
          post.abort();
        }
      }
    }

    public void requestStop() {
      this.stop = true;
    }

  }

}
