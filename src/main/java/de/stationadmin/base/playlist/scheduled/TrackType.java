package de.stationadmin.base.playlist.scheduled;

public enum TrackType {
  Song(1),
  Jingle(2),
  Moderation(3),
  News(4);

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
