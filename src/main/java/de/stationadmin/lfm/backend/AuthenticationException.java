/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.IOException;

/**
 * @author korf
 *
 */
public class AuthenticationException extends IOException {
  private static final long serialVersionUID = -3681429179292772964L;

  /**
   * 
   */
  public AuthenticationException() {
    // TODO Auto-generated constructor stub
  }

  /**
   * @param message
   */
  public AuthenticationException(String message) {
    super(message);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param cause
   */
  public AuthenticationException(Throwable cause) {
    super(cause);
    // TODO Auto-generated constructor stub
  }

  /**
   * @param message
   * @param cause
   */
  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
    // TODO Auto-generated constructor stub
  }

}
