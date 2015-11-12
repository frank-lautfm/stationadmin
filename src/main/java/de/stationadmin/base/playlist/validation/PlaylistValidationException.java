/**
 * 
 */
package de.stationadmin.base.playlist.validation;

/**
 * 
 * @author Frank Korf
 * 
 */
public class PlaylistValidationException extends Exception {
  private static final long serialVersionUID = 3983738996328329232L;
  
  public enum Reason {
    MIN_LENGTH, LAUT_RULE_VIOLATION, GVL_ARTIST_RULE_VIOLOATION, MISSING_NAME
  }

  private Reason error;

  public PlaylistValidationException(Reason error) {
    super(error.name());
    this.error = error;
  }

  /**
   * @return the error
   */
  public Reason getError() {
    return error;
  }
}
