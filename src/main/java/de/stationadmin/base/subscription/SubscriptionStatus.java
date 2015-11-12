/**
 * 
 */
package de.stationadmin.base.subscription;

/**
 * @author korf
 * 
 */
public class SubscriptionStatus {
  private int fetchedTill;
  private int seenTill;

  /**
   * Gets the title id of the newest title we have fetched with the last query
   * @return
   */
  public int getFetchedTill() {
    return fetchedTill;
  }

  public void setFetchedTill(int fetchedTill) {
    this.fetchedTill = fetchedTill;
  }

  /**
   * Gets the title id of the newest title the user has seen
   * @return
   */
  public int getSeenTill() {
    return seenTill;
  }

  public void setSeenTill(int seenTill) {
    this.seenTill = seenTill;
  }

}
