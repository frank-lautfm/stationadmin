package de.stationadmin.base.mp3splitter;

/**
 * @author fkorf
 */
public class SplitPoint implements Comparable<SplitPoint> {

  long position;

  private String artist;

  private String title;
  private String album;
  private int offset;
  public SplitPoint() {

  }

  /**
   * @param artist
   * @param title
   * @param album
   * @param offset
   */
  public SplitPoint(long position, String artist, String title, String album) {
    super();
    this.artist = artist;
    this.title = title;
    this.album = album;
    this.position = position;
  }

  @Override
  public int compareTo(SplitPoint o) {
    return Long.valueOf(this.position).compareTo(o.position);
  }

  /**
   * Gets the album of the title starting at the split point
   * 
   * @return
   */
  public String getAlbum() {
    return album;
  }

  /**
   * Gets the artist of the title starting at the split point
   * 
   * @return
   */
  public String getArtist() {
    return artist;
  }

  protected int getOffset() {
    return offset;
  }

  /**
   * Gets the position of the split point in seconds
   * 
   * @return
   */
  public long getPosition() {
    return position;
  }

  /**
   * Gets the name of the title starting at the split point
   * 
   * @return
   */
  public String getTitle() {
    return title;
  }

  public void setAlbum(String album) {
    this.album = album;
  }

  public void setArtist(String artist) {
    this.artist = artist;
  }

  protected void setOffset(int offset) {
    this.offset = offset;
  }

  public void setPosition(long position) {
    this.position = position;
  }

  public void setTitle(String title) {
    this.title = title;
  }

}
