/**
 * 
 */
package de.stationadmin.lfm.backend;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;
import org.codehaus.jackson.annotate.JsonProperty;

/**
 * @author korf
 * 
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class Paging {
  @JsonProperty("current_page")
  private int currentPage;
  @JsonProperty("next_page")
  private int nextPage;
  @JsonProperty("prev_page")
  private int prevPage;
  
  public int getCurrentPage() {
    return currentPage;
  }

  public void setCurrentPage(int currentPage) {
    this.currentPage = currentPage;
  }

  public int getNextPage() {
    return nextPage;
  }

  public void setNextPage(int nextPage) {
    this.nextPage = nextPage;
  }

  public int getPrevPage() {
    return prevPage;
  }

  public void setPrevPage(int prevPage) {
    this.prevPage = prevPage;
  }

}
