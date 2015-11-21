/**
 * 
 */
package de.stationadmin.base.track;

import java.util.List;


/**
 * Result object for searches on the track database
 *
 * @author Frank Korf
 */
public class SearchResultSet {
  private List<DetailedTrack> tracks;
  private int currentPage;
  private boolean hasMoreResults;
  
  public SearchResultSet(List<DetailedTrack> tracks, int currentPage, boolean hasMore) {
    super();
    this.tracks = tracks;
    this.currentPage = currentPage;
    this.hasMoreResults = hasMore;
  }

  /**
   * Gets the titles
   * @return the titles
   */
  public List<DetailedTrack> getTracks() {
    return tracks;
  }

  /**
   * @return the hasMoreResults
   */
  public boolean getHasMoreResults() {
    return hasMoreResults;
  }

  /**
   * @return the currentPage
   */
  public int getCurrentPage() {
    return currentPage;
  }
  

}
