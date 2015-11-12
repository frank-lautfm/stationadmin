/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.IOException;

/**
 * @author korf
 *
 */
public class AdminServiceException extends IOException {
  private static final long serialVersionUID = -410790512112920368L;

  public AdminServiceException() {
    super();
  }

  public AdminServiceException(String message, Throwable cause) {
    super(message, cause);
  }

  public AdminServiceException(String message) {
    super(message);
  }

  public AdminServiceException(Throwable cause) {
    super(cause);
  }

}
