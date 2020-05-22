package de.stationadmin.base.playlist.util;

public class MissingSourceTracksException extends Exception {
  private static final long serialVersionUID = 5681126939297872987L;

  public MissingSourceTracksException() {
  }

  public MissingSourceTracksException(String message) {
    super(message);
  }


}
