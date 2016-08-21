package de.stationadmin.base;
/**
 * 
 */


/**
 * @author korf
 * 
 */
public class LiveAccount {
  private String server;
  private int port;
  private String user;
  private String password;

  /**
   * @return the server
   */
  public String getServer() {
    return server;
  }

  /**
   * @param server
   *          the server to set
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
   * @param port
   *          the port to set
   */
  public void setPort(int port) {
    this.port = port;
  }

  /**
   * @return the user
   */
  public String getUser() {
    return user;
  }

  /**
   * @param user
   *          the user to set
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
   * @param password
   *          the password to set
   */
  public void setPassword(String password) {
    this.password = password;
  }

}
