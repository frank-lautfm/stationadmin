package de.stationadmin.streamlive;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URL;
import java.net.URLEncoder;

import org.apache.commons.codec.binary.Base64;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * Establishes a connection to an Icecast server.
 * <p>
 * This class is based on code from Icecast Brewer
 * (https://sourceforge.net/projects/icebrew/)
 * 
 * @author btoddb, Frank Korf
 */
public class IcecastServerConnector {
  private Logger logger = LogManager.getLogger(IcecastServerConnector.class);

  private Socket socket;
  private String hostName = "live.laut.fm";
  private int port = 8080;
  private OutputStream outStream;

  private String mountPoint;
  private String userName = "source";
  private String password;
  private String userAgent = "Station Admin Streamer; " + System.getProperty("os.name");
  private String sourceName;
  private String sourceDescription;
  private boolean publicSource;

  public IcecastServerConnector() {
  }

  /**
   * Connect to the IceCast server, sending initial server/stream meta data.
   * 
   * @throws IOException
   * @throws IOException
   * 
   */
  public int connect() throws IOException {
    InetAddress addr;

    //
    // this is a nice way to get an internet address (host + port)
    // worked out before opening the socket
    //
    addr = InetAddress.getByName(hostName);
    logger.debug("addr = " + addr);

    //
    // setup packet
    //
    String httpHeader = createHttpHeader();

    //
    // connect to icecast server
    //
    socket = new Socket(addr, port);

    //
    // send init packet to icecast server
    //
    outStream = socket.getOutputStream();
    OutputStreamWriter writer = new OutputStreamWriter(outStream);
    writer.write(httpHeader);
    writer.flush();

    //
    // get response from initialize call to server
    //
    return parseHttpResponse(getServerResponse());
  }

  /**
   * Create the HTTP headers to send to icecast server.
   * 
   * @return HTTP header
   */
  private String createHttpHeader() {
    String encPass = new String(Base64.encodeBase64(new String(userName + ":" + password).getBytes()));

    StringBuffer outBuffer = new StringBuffer();
    outBuffer.append("SOURCE /" + mountPoint + " HTTP/1.0\r\n");
    outBuffer.append("Authorization: Basic " + encPass + "\r\n");
    outBuffer.append("User-Agent: " + userAgent + "\r\n");
    outBuffer.append("Content-Type: audio/mpeg\r\n");
    outBuffer.append("ice-name: " + sourceName + "\r\n");
    outBuffer.append("ice-public: " + (publicSource ? "1" : "0") + "\r\n");
    outBuffer.append("ice-description: " + sourceDescription + "\r\n");
    outBuffer.append("\r\n");

    logger.debug(outBuffer);

    return outBuffer.toString();

  }

  public int updateSong(String song) throws IOException {
    String encPass = new String(Base64.encodeBase64(new String(userName + ":" + password).getBytes()));

    String str = "http://" + this.hostName + ":" + this.port + "/admin/metadata?mount=/" + this.mountPoint + "&mode=updinfo&song="
        + URLEncoder.encode(song, "UTF-8");
    URL url = new URL(str);
    HttpURLConnection con = (HttpURLConnection) url.openConnection();
    con.setRequestProperty("User-Agent", this.userAgent);
    con.setRequestProperty("Authorization", "Basic " + encPass);
    con.connect();
    int rc = con.getResponseCode();
    con.disconnect();
    return rc;
  }

  /**
   * Disconnect from IceCast server.
   * 
   */
  public void disconnect() {
    if (isConnected()) {
      try {
        socket.close();
      } catch (IOException e) {
        logger.error(e);
      }
    }
  }

  /**
   * Get response from IceCast server.
   * 
   * @return icecast server response
   * @throws IOException
   */
  public String getServerResponse() throws IOException {
    InputStreamReader reader = new InputStreamReader(socket.getInputStream());
    char[] inCharBuffer = new char[4096];
    int length = reader.read(inCharBuffer);
    return String.valueOf(inCharBuffer, 0, length);
  }

  /**
   * Are we connected to the IceCast server?
   * 
   * @return true if server is connected, false otherwise
   */
  public boolean isConnected() {
    return null != socket && socket.isConnected() && !socket.isClosed();
  }

  /**
   * Returns true if response data is available from IceCast server.
   * 
   * @return true if server response is ready, false otherwise
   * @throws IOException
   */
  public boolean isServerResponseReady() throws IOException {
    return 0 < socket.getInputStream().available();
  }

  /**
   * Parse the HTTP response to get the response code, that's it.
   * 
   * <p>
   * Example: HTTP/1.0 200 OK
   * 
   * @param httpResponse
   * @return HTTP Response code
   */
  private int parseHttpResponse(String httpResponse) {
    logger.debug("RAW HTTP RESPONSE : " + httpResponse);

    String[] respParts = httpResponse.split(" ");
    if (3 > respParts.length) {
      logger.error("HTTP response is weird, doesn't have enough info : " + httpResponse);
      return -1;
    }

    // we only care about the 2nd part
    String retCode = respParts[1];
    logger.debug("HTTP response code : " + retCode);

    // convert to integer
    try {
      return Integer.valueOf(retCode).intValue();
    } catch (NumberFormatException e) {
      logger.error(e);
      logger.error("HTTP response is weird, the return code isn't a valid integer : " + retCode);
      return -1;
    }
  }

  /**
   * @param hostName
   *          The hostName to set.
   */
  public void setHostName(String hostName) {
    this.hostName = hostName;
  }

  /**
   * @param mountPoint
   *          The mountPoint to set.
   */
  public void setMountPoint(String mountPoint) {
    this.mountPoint = mountPoint;
  }

  /**
   * @param password
   *          The password to set.
   */
  public void setPassword(String password) {
    this.password = password;
  }

  /**
   * @param port
   *          The port to set.
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * @param sourceDescription
   *          The sourceDescription to set.
   */
  public void setSourceDescription(String sourceDescription) {
    this.sourceDescription = sourceDescription;
  }

  /**
   * @param sourceName
   *          The sourceName to set.
   */
  public void setSourceName(String sourceName) {
    this.sourceName = sourceName;
  }

  /**
   * @param userAgent
   *          The userAgent to set.
   */
  public void setUserAgent(String userAgent) {
    this.userAgent = userAgent;
  }

  /**
   * @param userName
   *          The userName to set.
   */
  public void setUserName(String userName) {
    this.userName = userName;
  }

  /**
   * @param publicSource
   *          The publicSource to set.
   */
  public void setPublicSource(boolean publicSource) {
    this.publicSource = publicSource;
  }

  /**
   * Gets the stream to write data to (available after a successful connect)
   * @return the outStream
   */
  public OutputStream getOutStream() {
    return outStream;
  }

}
