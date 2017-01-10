/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.SocketException;
import java.net.URLEncoder;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpHost;
import org.apache.http.client.config.RequestConfig;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPatch;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContextBuilder;
import org.apache.http.conn.ssl.TrustStrategy;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.HttpEntityWrapper;
import org.apache.http.entity.StringEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.DefaultProxyRoutePlanner;
import org.apache.http.message.AbstractHttpMessage;
import org.apache.log4j.Logger;
import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;

import de.stationadmin.base.Version;

/**
 * @author korf
 * 
 */
public class LautfmAdminService {
  private static final String BASE_URL = "https://api.radioadmin.laut.fm";
  private static final Logger log = Logger.getLogger(LautfmAdminService.class);

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
      if (System.getProperty("stationadmin.proxy", "false").equals("true")) {
        hcBuilder.setRoutePlanner(new DefaultProxyRoutePlanner(new HttpHost("localhost", 8888)));
      }
      hcBuilder.setUserAgent("Mozilla/4.0 (compatible; Station Admin " + Version.VERSION + "; " + System.getProperty("os.name") + ")");

      RequestConfig config = RequestConfig.custom().setSocketTimeout(90 * 1000).setConnectTimeout(20 * 1000).build();
      HttpClients.custom().setDefaultRequestConfig(config);

      return hcBuilder.build();

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private <T> T deserializeJson(CloseableHttpResponse response, Class<T> type) throws IOException, JsonMappingException {
    ObjectMapper mapper = new ObjectMapper();
    InputStream stream = response.getEntity().getContent();
    if (log.isInfoEnabled()) {
      String content = IOUtils.toString(stream, "UTF-8");
      log.info(response.getStatusLine().getStatusCode() + " - " + content);
      return mapper.readValue(content, type);
    } else {
      return mapper.readValue(stream, type);
    }
  }

  private void addAuthHeaders(AbstractHttpMessage request) {
    request.addHeader("Authorization", "Bearer " + this.token);
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
    String contentType = response.getEntity().getContentType().getValue();
    if (contentType != null && contentType.toLowerCase().contains("html")) {
      // HTML page - try to strip tags
      String content = IOUtils.toString(response.getEntity().getContent());
      content = content.replaceAll("\\<.*?\\>", "");
      return content;

    } else {

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
  }

  private void checkResponse(CloseableHttpResponse response) throws IOException {
    switch (response.getStatusLine().getStatusCode()) {
    case 400:
    case 500:
    case 501:
    case 502:
    case 503:
    case 504:
    case 505:
    case 506:
    case 507:
    case 508:
    case 510:
    case 511:
      log.info(response.getStatusLine().getStatusCode());
      throw new AdminServiceException(getErrorMessage(response));
    case 401:
      log.info(response.getStatusLine().getStatusCode());
      throw new AuthenticationException();
    case 404:
      log.info(response.getStatusLine().getStatusCode());
      throw new ResourceNotFoundException();
    }
  }

  private CloseableHttpResponse doGet(String path) throws IOException {
    HttpGet request = new HttpGet(BASE_URL + path);
    if (log.isInfoEnabled()) {
      log.info("GET " + BASE_URL + path);
    }
    this.addAuthHeaders(request);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);
    return response;
  }

  private CloseableHttpResponse doDelete(String path) throws IOException {
    HttpDelete request = new HttpDelete(BASE_URL + path);
    if (log.isInfoEnabled()) {
      log.info("DELETE " + BASE_URL + path);
    }

    this.addAuthHeaders(request);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);
    return response;
  }

  private CloseableHttpResponse doPatch(String path, Object content) throws IOException {
    HttpPatch request = new HttpPatch(BASE_URL + path);
    this.addAuthHeaders(request);

    ObjectMapper mapper = new ObjectMapper();
    String contentStr = mapper.writeValueAsString(content);

    if (log.isInfoEnabled()) {
      log.info("PATCH " + BASE_URL + path);
      log.info(contentStr);
    }

    StringEntity entity = new StringEntity(contentStr, ContentType.APPLICATION_JSON);
    request.setEntity(entity);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);

    return response;

  }

  private CloseableHttpResponse doPost(String path, Object content) throws IOException {
    HttpPost request = new HttpPost(BASE_URL + path);

    this.addAuthHeaders(request);

    ObjectMapper mapper = new ObjectMapper();
    String contentStr = mapper.writeValueAsString(content);

    if (log.isInfoEnabled()) {
      log.info("POST " + BASE_URL + path);
      log.info(contentStr);
    }

    StringEntity entity = new StringEntity(contentStr, ContentType.APPLICATION_JSON);
    request.setEntity(entity);
    CloseableHttpResponse response = this.client.execute(request);
    this.checkResponse(response);

    return response;

  }

  public List<Station> getStations() throws IOException {
    CloseableHttpResponse response = this.doGet("/stations");
    StationList list = deserializeJson(response, StationList.class);
    response.close();
    return Arrays.asList(list.getStations());
  }

  public boolean isRunning(int stationId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/state");
    try {
      return IOUtils.toString(response.getEntity().getContent()).toLowerCase().contains("true");
    } finally {
      response.close();
    }
  }

  public TrackStatsEntry[] getTrackStatistics(int stationId, int days) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/stats?days=" + days);
    try {
      return deserializeJson(response, TrackStatsEntry[].class);
    } finally {
      response.close();
    }
  }

  public void start(int stationId) throws IOException {
    HashMap<String, Object> args = new HashMap<String, Object>();
    args.put("station_id", stationId);
    CloseableHttpResponse response = this.doPost("/stations/" + stationId + "/state", args);
    response.close();
  }

  public List<PlaylistHead> getPlaylists(int stationId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/playlists");
    PlaylistHeadList list = deserializeJson(response, PlaylistHeadList.class);
    response.close();
    return Arrays.asList(list.getPlaylists());
  }

  public Playlist getPlaylist(int stationId, int playlistId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/playlists/" + playlistId);
    Playlist playlist = deserializeJson(response, Playlist.class);
    response.close();
    return playlist;
  }

  private TrackList getTracks(CloseableHttpResponse response) throws IOException {
    TrackList list = deserializeJson(response, TrackList.class);
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
      try {
        TrackList trackList = this.getTracks(this.doGet(basePath + idLIst.toString()));
        list.addAll(Arrays.asList(trackList.getTracks()));
      } catch (Exception e) {
        // try one-by-one
        list = new ArrayList<Track>();
        for (int trackId : trackIds) {
          if (trackId > 0) {
            try {
              Track t = this.getTrack(stationId, trackId);
              if (t != null) {
                list.add(t);
              }
            } catch (ResourceNotFoundException ex) {
              // track not longer available
            } catch (Exception ex) {
              // TODO temporary solution until 500 is fixed
            }
          }
        }

      }
    }

    return list;
  }

  public Track getTrack(int stationId, int trackId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/" + trackId);
    TrackList list = this.getTracks(response);
    return list != null && list.getTracks() != null && list.getTracks().length > 0 ? list.getTracks()[0] : null;
  }

  public String getTrackPrelistenUrl(int stationId, int trackId) throws IOException {
    List<Track> tracks = this.getTracks(stationId, trackId);
    if (tracks.size() > 0) {
      return BASE_URL + tracks.get(0).getLinks().getPrelistenAuthless().getHref();
    } else {
      return null;
    }
  }

  public Playlist setPlaylistTracks(int stationId, int playlistId, int[] trackIds) throws IOException {
    Map<String, Object> map = new HashMap<String, Object>();
    map.put("entries", trackIds);
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/playlists/" + playlistId, map);
    Playlist playlist = deserializeJson(response, Playlist.class);
    response.close();
    return playlist;
  }

  public PlaylistHead createPlaylist(int stationId, PlaylistHead playlist) throws IOException {
    CloseableHttpResponse response = this.doPost("/stations/" + stationId + "/playlists", playlist);
    try {
      if (response.getStatusLine().getStatusCode() == 201) {
        return deserializeJson(response, PlaylistHead.class);
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

  public void deleteTrack(int stationId, int trackId) throws IOException {
    CloseableHttpResponse response = this.doDelete("/stations/" + stationId + "/tracks/" + trackId);
    try {
      if (response.getStatusLine().getStatusCode() != 204) {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } finally {
      response.close();
    }
  }

  public void deleteTag(int stationId, String tag) throws IOException {
    // /stations/:station_id/tracks/tags/:tagname
    CloseableHttpResponse response = this.doDelete("/stations/" + stationId + "/tracks/tags/" + URLEncoder.encode(tag, "UTF-8"));
    try {
      if (response.getStatusLine().getStatusCode() != 204 && response.getStatusLine().getStatusCode() != 200) {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } finally {
      response.close();
    }
  }

  public Playlist updatePlaylist(int stationId, PlaylistHead playlist) throws IOException {
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/playlists/" + playlist.getId(), playlist);
    try {
      return deserializeJson(response, Playlist.class);

    } finally {
      response.close();
    }
  }

  public Track updateTrack(int stationId, Track track) throws IOException {
    if (track.getArtist() == null || track.getTitle() == null) {
      // throw new NullPointerException(); // FIXME remove if reason for occasional
      // null values found
    }
    CloseableHttpResponse response = this.doPatch("/stations/" + stationId + "/tracks/" + track.getId(), track);
    try {
      return deserializeJson(response, Track.class);

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

    String suffix = "/tags/" + StringUtils.replace(URLEncoder.encode(tag, "UTF-8"), "+", "%20");
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
      return Arrays.asList(deserializeJson(response, String[].class));
    } finally {
      response.close();
    }
  }

  public int[] getTaggedTracks(int stationId, String tag) throws IOException {
    tag = URLEncoder.encode(tag, "UTF-8");
    tag = StringUtils.replace(tag, "+", "%20");
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/tracks/tags/" + tag);
    try {
      return deserializeJson(response, int[].class);
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
        result = deserializeJson(response, HashMap.class);
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

  public UploadResponse uploadTrack(int stationId, TrackUpload track, final ProgressListener progressListener) throws IOException {
    final HttpPost filePost = new HttpPost(BASE_URL + "/stations/" + stationId + "/tracks");
    if (progressListener != null) {
      progressListener.setMaxValue((int) track.getFile().length());
    }
    this.addAuthHeaders(filePost);

    HttpEntity entity = MultipartEntityBuilder.create().addBinaryBody("track", track.getFile(), ContentType.create("audio/mp3"), track.getFile().getName()).build();

    HttpEntityWrapper entityWrapper = new HttpEntityWrapper(entity) {
      @Override
      public void writeTo(OutputStream out) throws IOException {
        if (progressListener != null) {
          super.writeTo(new CountingOutputStream(out, progressListener));
        } else {
          super.writeTo(out);
        }
      }

    };

    filePost.setEntity(entityWrapper);

    CloseableHttpClient uploadClient = createClient();

    UploadAbortMonitor monitor = new UploadAbortMonitor(filePost, progressListener);
    monitor.start();
    try {
      System.out.println("start upload of " + track.getFile().getName());
      CloseableHttpResponse response = uploadClient.execute(filePost);
      System.out.println("upload completed");
      this.checkResponse(response);
      if (response.getStatusLine().getStatusCode() == 201) {
        UploadResponse uploadResponse = deserializeJson(response, UploadResponse.class);
        if (track.isPrivateTrack()) {
          MarkTrackPrivateRequest privateRequest = new MarkTrackPrivateRequest();
          privateRequest.setId(uploadResponse.getId());
          privateRequest.setPrivateTrack(true);
          CloseableHttpResponse markPrivateResponse = this.doPatch("/stations/" + stationId + "/tracks/" + privateRequest.getId(), privateRequest);
          markPrivateResponse.close();
        }

        return uploadResponse;
      } else {
        throw new AdminServiceException(this.getErrorMessage(response));
      }
    } catch (SocketException e) {
      if (!progressListener.isAbortCurrent()) {
        throw e;
      } else {
        return null;
      }
    } finally {
      monitor.requestStop();
    }

  }

  public Schedule getSchedule(int stationId) throws IOException {
    CloseableHttpResponse response = doGet("/stations/" + stationId + "/schedule");
    try {
      return deserializeJson(response, Schedule.class);
    } finally {
      response.close();
    }
  }

  public String getLivePassword(int stationId) throws IOException {
    CloseableHttpResponse response = doGet("/stations/" + stationId + "/live/password");
    try {
      return deserializeJson(response, String.class);
    } finally {
      response.close();
    }
  }

  public LiveAccessData getLiveAccessData(int stationId) throws IOException {
    CloseableHttpResponse response = doGet("/stations/" + stationId + "/live");
    try {
      return deserializeJson(response, LiveAccessData.class);
    } finally {
      response.close();
    }
  }

  public Schedule updateSchedule(int stationId, Schedule schedule) throws IOException {
    CloseableHttpResponse response = doPatch("/stations/" + stationId + "/schedule", schedule);
    try {
      return deserializeJson(response, Schedule.class);
    } finally {
      response.close();
    }
  }

  public Statistics getStatistics(int stationId) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/stats");
    try {
      return deserializeJson(response, Statistics.class);
    } finally {
      response.close();
    }
  }
  
  public LogEntry[] getLogs(int stationId, int days) throws IOException {
    CloseableHttpResponse response = this.doGet("/stations/" + stationId + "/logs?days=" + days);
    try {
      return deserializeJson(response, LogEntry[].class);
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
