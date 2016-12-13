/**
 * 
 */
package de.stationadmin.lfm.backend;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * @author korf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "LiveAccess")
public class LiveAccessData {

  private String protocol;
  private String server;
  private int port;
  private String mountpoint;
  private String user;
  private String password;
  /**
   * @return the protocol
   */
  public String getProtocol() {
    return protocol;
  }
  /**
   * @param protocol the protocol to set
   */
  public void setProtocol(String protocol) {
    this.protocol = protocol;
  }
  /**
   * @return the server
   */
  public String getServer() {
    return server;
  }
  /**
   * @param server the server to set
   */
  public void setServer(String server) {
    this.server = server;
  }
  /**
   * @return the port
   */
  public int getPort() {
    return port;
  }
  /**
   * @param port the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }
  /**
   * @return the mountpoint
   */
  public String getMountpoint() {
    return mountpoint;
  }
  /**
   * @param mountpoint the mountpoint to set
   */
  public void setMountpoint(String mountpoint) {
    this.mountpoint = mountpoint;
  }
  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }
  /**
   * @param user the user to set
   */
  public void setUser(String user) {
    this.user = user;
  }
  /**
   * @return the password
   */
  public String getPassword() {
    return password;
  }
  /**
   * @param password the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }
  
}
