/**
 * 
 */
package de.stationadmin.gui.track;

import com.jgoodies.binding.beans.Model;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackAlias;

/**
 *
 * @author Frank Korf
 *
 */
public class TrackAliasModel extends Model implements Comparable<TrackAliasModel> {
  private RegisteredTrack title;
  private TrackAlias alias;
  
  public TrackAliasModel(RegisteredTrack title, TrackAlias alias) {
    super();
    this.title = title;
    this.alias = alias;
  }
  
  /**
   * @return the title
   */
  public RegisteredTrack getTitle() {
    return title;
  }
  /**
   * @return the alias
   */
  public TrackAlias getAlias() {
    return alias;
  }
  
  public String getTitleArtist() {
    return this.title.getArtist();
  }

  public String getTitleName() {
    return this.title.getTitle();
  }
  
  public String getAliasArtist() {
    return this.alias.getArtist();
  }
  
  public String getAliasName() {
    return this.alias.getTitle();
  }

  public void setAliasArtist(String artist) {
    this.alias.setArtist(artist);
  }
  
  public void setAliasName(String title) {
    this.alias.setTitle(title);
  }
  
  public String toString() {
    return this.alias.toString() + ": " + title.toString();
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(TrackAliasModel o) {
    return this.toString().compareToIgnoreCase(o.toString());
  }
}
