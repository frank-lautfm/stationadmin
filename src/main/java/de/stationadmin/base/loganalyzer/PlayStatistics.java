/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.util.AbstractBean;

/**
 * Statistics on a number of {@link Play}s
 * 
 * @author korf
 */
public class PlayStatistics extends AbstractBean {

  private int numPlays;
  private int numTracks;
  private int numArtists;
  private int score;
  
  private List<ItemFrequency<BasicTrack>> frequentTracks;
  private List<ItemFrequency<String>> frequentArtists;

  public List<ItemFrequency<String>> getFrequentArtists() {
    return frequentArtists;
  }

  public List<ItemFrequency<BasicTrack>> getFrequentTracks() {
    return frequentTracks;
  }

  public int getNumArtists() {
    return numArtists;
  }

  public int getNumPlays() {
    return numPlays;
  }

  public int getNumTracks() {
    return numTracks;
  }

  public void setFrequentArtists(List<ItemFrequency<String>> frequentArtists) {
    List<ItemFrequency<String>> old = this.frequentArtists;
    this.frequentArtists = frequentArtists;
    this.firePropertyChange("frequentArtists", old, frequentArtists);
  }

  public void setFrequentTracks(List<ItemFrequency<BasicTrack>> frequentTitles) {
    List<ItemFrequency<BasicTrack>> old = this.frequentTracks;
    this.frequentTracks = frequentTitles;
    this.firePropertyChange("frequentTracks", old, frequentTitles);
  }

  public void setNumArtists(int numArtists) {
    int old = this.numArtists;
    this.numArtists = numArtists;
    this.firePropertyChange("numArtists", old, numArtists);
  }

  public void setNumPlays(int numPlays) {
    int old = this.numPlays;
    this.numPlays = numPlays;
    this.firePropertyChange("numPlays", old, numPlays);
  }

  public void setNumTracks(int numTitles) {
    int old = this.numTracks;
    this.numTracks = numTitles;
    this.firePropertyChange("numTitles", old, numTitles);
    this.firePropertyChange("numTracks", old, numTitles);
  }

  @Override
  public String toString() {
    return this.numPlays + " / " + this.numTracks + " / " + this.numArtists;
  }

  public void update(List<Play> plays) {
    
    HashMap<Integer,ItemFrequency<BasicTrack>> tracks = new HashMap<Integer, ItemFrequency<BasicTrack>>();
    HashMap<String,ItemFrequency<String>> artists = new HashMap<String, ItemFrequency<String>>();
    for(Play play : plays) {
      ItemFrequency<BasicTrack> tf = tracks.get(play.getTrack().getId());
      if(tf == null) {
        tf = new ItemFrequency<BasicTrack>(play.getTrack(), 1);
        tracks.put(play.getTrack().getId(), tf);
      }
      else {
        tf.inc();
      }
      String normArtist = play.getTrack().getArtist().toLowerCase();
      ItemFrequency<String> af = artists.get(normArtist);
      if(af == null) {
        af = new ItemFrequency<String>(play.getTrack().getArtist(), 1);
        artists.put(normArtist, af);
      }
      else {
        af.inc();
      }
    }
    
    this.setNumPlays(plays.size());
    this.setNumTracks(tracks.size());
    this.setNumArtists(artists.size());
    
    List<ItemFrequency<BasicTrack>> titleList = new ArrayList<ItemFrequency<BasicTrack>>(tracks.values());
    Collections.sort(titleList);
    this.setFrequentTracks(titleList);
    List<ItemFrequency<String>> artistList = new ArrayList<ItemFrequency<String>>(artists.values());
    Collections.sort(artistList);
    this.setFrequentArtists(artistList);
    
    if(numPlays > 0) {
      float scoreF = ((float)numTracks / numPlays) * 0.75f + ((float)numArtists / numPlays) * 0.25f;
      this.setScore((int)(scoreF * 100));
    }
    else {
      this.setScore(0);
    }
  }

  public int getScore() {
    return score;
  }

  public void setScore(int score) {
    int old = this.score;
    this.score = score;
    this.firePropertyChange("score", old, score);
  }
}
