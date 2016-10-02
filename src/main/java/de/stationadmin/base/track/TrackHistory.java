/**
 * 
 */
package de.stationadmin.base.track;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import de.stationadmin.base.History;

/**
 * History of played titles
 * 
 * @author Frank Korf
 */
public class TrackHistory extends History implements TrackCollector {
	private static final DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm");

	private LinkedList<Entry> entries = new LinkedList<Entry>();
	private int maxSize = 500;
	private boolean logCurrentListeners = false;
	private int currentListeners = 0;

	public void add(Date date, BasicTrack title) {
		this.entries.add(new Entry(date, title));
	}

	public void add(BasicTrack title) {
		this.entries.add(new Entry(new Date(), title));
		while (this.entries.size() >= maxSize) {
			this.entries.removeFirst();
		}
		if(this.logCurrentListeners) {
      this.logToFile(dateFormat.format(new Date()) + "\t" + title.getArtist() + " - " + title.getTitle() + "\t" + this.currentListeners + System.getProperty("line.separator"));
		}
		else {
      this.logToFile(dateFormat.format(new Date()) + "\t" + title.getArtist() + " - " + title.getTitle() + System.getProperty("line.separator"));
		  
		}
	}

	public List<Entry> getEntries() {
		List<Entry> entries = new ArrayList<Entry>(this.entries);
		Collections.sort(entries);
		return entries;
	}

	public static class Entry implements Comparable<Entry> {
		private Date date;
		private BasicTrack title;

		public Entry(Date date, BasicTrack title) {
			super();
			this.date = date;
			this.title = title;
		}

		public Date getDate() {
			return date;
		}

		public BasicTrack getTitle() {
			return title;
		}

    @Override
    public int compareTo(Entry o) {
      return date.compareTo(o.getDate());
    }
		
		

	}

  /**
   * @return the logCurrentListeners
   */
	public boolean isLogCurrentListeners() {
    return logCurrentListeners;
  }

  /**
   * @param logCurrentListeners the logCurrentListeners to set
   */
  public void setLogCurrentListeners(boolean logCurrentListeners) {
    this.logCurrentListeners = logCurrentListeners;
  }

  /**
   * @return the currentListeners
   */
  public int getCurrentListeners() {
    return currentListeners;
  }

  /**
   * @param currentListeners the currentListeners to set
   */
  public void setCurrentListeners(int currentListeners) {
    this.currentListeners = currentListeners;
  }

}
