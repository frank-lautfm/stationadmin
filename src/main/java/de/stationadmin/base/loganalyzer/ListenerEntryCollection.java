/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.ArrayList;
import java.util.List;

/**
 * @author korf
 * 
 */
class ListenerEntryCollection {

  private List<ListenersEntry> entries = new ArrayList<ListenersEntry>();

  void add(ListenersEntry entry) {
    // ensure that we are approximately in a 10 minute interval, even if we have recorded values more often
    if (entries.size() == 0 || entry.getTime().getTime() - entries.get(entries.size() - 1).getTime().getTime() >= 1000 * 60 * 9) {
      this.entries.add(entry);
    }
  }

  public List<ListenersEntry> getEntries() {
    return entries;
  }

  void add(ListenerEntryCollection collection) {
    this.entries.addAll(collection.entries);
  }

  int getTotalListeners() {
    if (this.entries.size() == 0) {
      return 0;
    }
    int total = 0;
    for (int i = 0; i < this.entries.size(); i++) {
      total += this.entries.get(i).getListeners();
    }
    return total;

  }

  int getAvgListeners() {
    if (this.entries.size() == 0) {
      return 0;
    }
    return Math.round((float) getTotalListeners() / this.entries.size());
  }

}
