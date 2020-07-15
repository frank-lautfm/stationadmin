package de.stationadmin.base.playlist.scheduled;

public enum TrackType {
  Song(1),
  Jingle(2),
  Word(3);

  private int id;
  
  private TrackType(int id) {
    this.id = id;
  }

  public int getId() {
    return id;
  }

  public void setId(int id) {
    this.id = id;
  }
  
}
