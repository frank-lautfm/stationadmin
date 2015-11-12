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
  private int totalEntries;
  private int currentPage;
  private int totalPages;
  private List<DetailedTrack> titles;
  
  public SearchResultSet(int totalEntries, int currentPage, int totalPages, List<DetailedTrack> titles) {
    super();
    this.totalEntries = totalEntries;
    this.totalPages = totalPages;
    this.currentPage = currentPage;
    this.titles = titles;
  }

  /**
   * Gets the titles
   * @return the titles
   */
  public List<DetailedTrack> getTitles() {
    return titles;
  }

  public int getTotalEntries() {
    return totalEntries;
  }

  public int getCurrentPage() {
    return currentPage;
  }

  public int getTotalPages() {
    return totalPages;
  }
  

}
