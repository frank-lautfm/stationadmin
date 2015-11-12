/**
 * 
 */
package de.stationadmin.base.loganalyzer;

import java.util.List;

import de.stationadmin.base.util.AbstractBean;

/**
 * @author korf
 * 
 */
public class ListenersStatistics extends AbstractBean {
  private int min;
  private int max;
  private float avg;
  private int tlh;

  public float getAvg() {
    return avg;
  }

  public int getMax() {
    return max;
  }

  public int getMin() {
    return min;
  }

  public int getTlh() {
    return tlh;
  }

  public void setAvg(float avg) {
    float old = this.avg;
    this.avg = avg;
    this.firePropertyChange("avg", old, avg);
  }

  public void setMax(int max) {
    int old = this.max;
    this.max = max;
    this.firePropertyChange("max", old, max);
  }

  public void setMin(int min) {
    int old = this.min;
    this.min = min;
    this.firePropertyChange("min", old, min);
  }

  public void setTlh(int tlh) {
    int old = this.tlh;
    this.tlh = tlh;
    this.firePropertyChange("tlh", old, tlh);
  }

  public void update(List<ListenersEntry> entries) {
    if (entries != null && entries.size() > 0) {
      int min = Integer.MAX_VALUE;
      int max = 0;

      
      int tlm = 0;
      int totalMinutes = 0;

      for (int i = 0; i < entries.size(); i++) {
        ListenersEntry entry = entries.get(i);
        min = Math.min(min, entry.getListeners());
        max = Math.max(max, entry.getListeners());
        
        int intervalLength = 10;
        if(i +1  < entries.size()) {
          long startNext = entries.get(i+1).getTime().getTime();
          int minutes = (int)((startNext - entry.getTime().getTime()) / 60000);
          if(minutes < 10) {
            intervalLength = minutes;
          }
        }
        tlm += intervalLength * entry.getListeners();
        totalMinutes += intervalLength;
      }
      
      setMax(max);
      setMin(min);
      setAvg((float) tlm / totalMinutes);
      setTlh(tlm / 60);

    } else {
      setMax(0);
      setMin(0);
      setAvg(0);
      setTlh(0);

    }

  }
}
