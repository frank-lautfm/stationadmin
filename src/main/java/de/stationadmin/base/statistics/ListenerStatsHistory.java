/**
 * 
 */
package de.stationadmin.base.statistics;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

/**
 * History for the number of listeners
 * 
 * @author Frank Korf
 */
public class ListenerStatsHistory {
  private List<Entry> entries = new LinkedList<Entry>();

  public void addFromHistory(long time, int listeners) {
    this.entries.add(new Entry(time, listeners, 0));
  }
  
  void sortEntries() {
    this.entries.sort(new Comparator<Entry>() {

      @Override
      public int compare(Entry o1, Entry o2) {
        return o1.compareTo(o2);
      }
    });
  }

  public void add(int listeners, int rank) {
    this.entries.add(new Entry(listeners, rank));
  }

  /**
   * Gets all entries for the time after the given minimum time
   * 
   * @param minTime
   * @return
   */
  public List<Entry> getEntries(long minTime) {
    ArrayList<Entry> matchingEntries = new ArrayList<Entry>();
    for (Entry entry : this.entries) {
      if (entry.getTime() > minTime) {
        matchingEntries.add(entry);
      }
    }
    return matchingEntries;
  }

  /**
   * Gets the number of listeners at a specified time
   * 
   * @param time
   * @return listener or -1 if no matching record was found
   */
  public int getListenersAt(long time) {
    Entry best = null;
    for (Entry entry : new ArrayList<Entry>(this.entries)) {
      if (entry.getTime() < time) {
        if (best == null || best.getTime() < entry.getTime()) {
          best = entry;
        }
      }
    }
    return best != null ? best.getNumberOfListeners() : -1;
  }

  public static class Entry implements Comparable<Entry>{
    long time = System.currentTimeMillis();
    int numberOfListeners;
    int rank;

    public Entry(long time, int numberOfListeners, int rank) { 
      this(numberOfListeners, rank);
      this.time = time;
    }

    public Entry(int numberOfListeners, int rank) {
      super();
      this.numberOfListeners = numberOfListeners;
      this.rank = rank;
    }

    public int getNumberOfListeners() {
      return numberOfListeners;
    }

    public long getTime() {
      return time;
    }

    /**
     * @return the rank
     */
    public int getRank() {
      return rank;
    }

    @Override
    public int compareTo(Entry o) {
      return Long.compare(this.time, o.time);
    }

  }
}
