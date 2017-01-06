/**
 * 
 */
package de.stationadmin.base.subscription;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONException;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.Track;
import de.stationadmin.lfm.backend.TrackList;

/**
 * @author korf
 * 
 */
public class SubscriptionService extends AbstractBean implements Service {
  private static final Logger log = Logger.getLogger(SubscriptionService.class);
  private static final ExtendedTrackFormat fmt = new ExtendedTrackFormat(true);
  private static final String DIR = "/subscriptions/";
  private SessionCtx sessionCtx;
  private TrackRegistry trackRegistry;
  private int maxAgeInDays = 14;

  private List<Subscription> subscriptions = new ArrayList<Subscription>();
  private Date minSearchDate = new Date(System.currentTimeMillis() - 1000 * 60 * 60 * 24 * 2);
  private List<DetailedTrack> results = new ArrayList<DetailedTrack>();

  public SubscriptionService(SessionCtx ctx, TrackRegistry titleRegistry) {
    this.sessionCtx = ctx;
    this.trackRegistry = titleRegistry;
    new File(ctx.getStationDirectory() + DIR).mkdirs();
  }

  /**
   * @see de.stationadmin.base.Service#load()
   */
  @Override
  public void load() throws IOException {
    this.loadStatus();
    this.loadQueries();
    this.loadResults();
  }

  /**
   * @see de.stationadmin.base.Service#synchronize()
   */
  @Override
  public void synchronize() throws IOException {
    // not supported
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
    // TODO Auto-generated method stub

  }

  /**
   * @see de.stationadmin.base.Service#initBackgroundTasks()
   */
  @Override
  public void initBackgroundTasks() {
    this.sessionCtx.getTimer().schedule(new TimerTask() {

      @Override
      public void run() {
        try {
          executeQueries();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    }, 30 * 1000, 60 * 60 * 1000 * 6);

  }

  private boolean checkTrack(Subscription subscription, Track result) {
    boolean match = false;
    String value = null;
    switch (subscription.getField()) {
    case ARTIST:
      value = result.getArtist();
      break;
    case ALBUM:
      value = result.getAlbum();
      break;
    case GENRE:
      value = result.getGenre();
      break;
    case TITLE:
      value = result.getTitle();
      break;
    }
    if (value != null) {
      if (subscription.isEquals()) {
        match = BasicTrack.equals(value, subscription.getQuery());
      } else {
        match = value.toLowerCase().contains(subscription.getQuery().toLowerCase());
      }
    }
    return match;
  }

  public void executeQueries() throws IOException, JSONException {
    if (this.subscriptions.size() == 0) {
      return; // nothing to do
    }

    Set<Integer> known = new HashSet<Integer>();
    for (DetailedTrack t : this.results) {
      known.add(t.getId());
    }

    int matches = 0;
    TrackList list;
    for (Subscription subscription : this.subscriptions) {
      if (subscription.getQuery().length() < 2) {
        continue;
      }
      int page = 0;
      HashMap<String, String> filter = new HashMap<String, String>();

      boolean abort = false;
      do {
        filter.put(subscription.getField().getRawName(), subscription.getQuery());
        list = this.sessionCtx.getServer().getTracks(this.sessionCtx.getStationId(), page, filter, "created_at", false);

        for (Track track : list.getTracks()) {
          if(track.getUpdatedAt().getTime() < minSearchDate.getTime()) {
            abort = true;
          }
          if (!abort && !known.contains(track.getId()) && checkTrack(subscription, track) && !track.isPrivateTrack() && this.trackRegistry.getTrack(track.getId()) == null) {
            synchronized (this.results) {
              this.results.add(new DetailedTrack(track));
              matches++;
            }
          }
        }

        page++;
      } while (list.getTracks().length > 0 && !abort);
      try {
        Thread.sleep(1000);
      } catch (Exception e) {
      }
    }

    saveStatus();
    if (matches > 0) {
      this.firePropertyChange("results", new ArrayList<DetailedTrack>(), this.results);
      this.saveResults();
      this.firePropertyChange("newMatches", 0, matches);
    }
  }

  public List<DetailedTrack> getResults() {
    return Collections.unmodifiableList(results);
  }

  private synchronized void saveResults() {
    log.info("save subscription results");
    List<DetailedTrack> titles = new ArrayList<DetailedTrack>(this.results);
    Date minDate = new Date(System.currentTimeMillis() - this.maxAgeInDays * 24 * 60 * 60 * 1000);

    try {
      String fileName = this.sessionCtx.getStationDirectory() + DIR + "results.tsv";
      FileWriter writer = new FileWriter(fileName);
      for (DetailedTrack title : titles) {
        if (title.getUploadDate().getTime() > minDate.getTime()) {
          writer.write(fmt.toString(title).replaceAll("[\r|\n]", " ") + "\n");
        }
      }

      writer.close();
    } catch (IOException e) {
      log.error("failed to save subscription results", e);
    }
  }

  public void saveSubscriptions() {
    log.info("save subscription queries");
    try {
      String fileName = this.sessionCtx.getStationDirectory() + DIR + "queries";
      FileWriter writer = new FileWriter(fileName);
      for (Subscription query : this.subscriptions) {
        writer.write(query.asString() + "\n");
      }

      writer.close();
    } catch (IOException e) {
      log.error("failed to save subscription results", e);
    }
  }

  public boolean add(Subscription subscription) {
    for (Subscription s : subscriptions) {
      if (s.getField() == subscription.getField() && StringUtils.equals(s.getQuery(), subscription.getQuery())) {
        // this is a dupe
        return false;
      }
    }

    this.subscriptions.add(subscription);
    subscription.setNew(false);
    this.saveSubscriptions();
    this.firePropertyChange("subscriptions", new ArrayList<Subscription>(), this.subscriptions);
    return true;
  }

  public void remove(Subscription subscription) {
    this.subscriptions.remove(subscription);
    this.saveSubscriptions();
    this.firePropertyChange("subscriptions", new ArrayList<Subscription>(), this.subscriptions);
  }

  /**
   * Removes titles from the list of stored results
   * 
   * @param titles
   */
  public void remove(List<DetailedTrack> titles) {
    synchronized (this.results) {
      this.results.removeAll(titles);
    }
    this.saveResults();
    this.firePropertyChange("results", null, this.results);
  }

  private void loadResults() {
    String fileName = this.sessionCtx.getStationDirectory() + DIR + "results.tsv";
    if (new File(fileName).exists()) {
      try {
        this.results.clear();
        FileReader reader = new FileReader(fileName);
        BufferedReader bufReader = new BufferedReader(reader);
        String line = bufReader.readLine();
        while (line != null) {
          DetailedTrack title = (DetailedTrack) fmt.fromString(line);
          this.results.add(title);
          line = bufReader.readLine();
        }
        bufReader.close();
      } catch (IOException e) {
        log.error("failed to load subscription results", e);
      }
      this.firePropertyChange("results", new ArrayList<DetailedTrack>(), this.results);
    }

  }

  private void loadQueries() {
    String fileName = this.sessionCtx.getStationDirectory() + DIR + "queries";
    if (new File(fileName).exists()) {
      try {
        this.subscriptions.clear();
        FileReader reader = new FileReader(fileName);
        BufferedReader bufReader = new BufferedReader(reader);
        String line = bufReader.readLine();
        while (line != null) {
          try {
            Subscription sub = new Subscription(line);
            sub.setNew(false);
            subscriptions.add(sub);
          } catch (Exception e) {
            log.error("parse error for " + line, e);
          }
          line = bufReader.readLine();
        }
        bufReader.close();
      } catch (IOException e) {
        log.error("failed to load subscription queries", e);
      }
      this.firePropertyChange("subscriptions", new ArrayList<Subscription>(), this.subscriptions);
    }

  }

  private synchronized void saveStatus() {
    String fileName = this.sessionCtx.getStationDirectory() + DIR + "status";
    try {
      FileOutputStream out = new FileOutputStream(fileName);
      DataOutputStream dOut = new DataOutputStream(out);
      dOut.writeLong(this.minSearchDate.getTime());
      dOut.flush();
      out.flush();
      out.close();
    } catch (IOException e) {
      log.error("failed to save subscription status", e);
    }

  }

  private synchronized void loadStatus() {
    String fileName = this.sessionCtx.getStationDirectory() + DIR + "status";
    if (new File(fileName).exists()) {
      try {
        FileInputStream in = new FileInputStream(fileName);
        DataInputStream dIn = new DataInputStream(in);
        this.minSearchDate = new Date(dIn.readLong());
        in.close();
      } catch (IOException e) {
        log.error("failed to load subscription status", e);
      }
    }

  }

  public List<Subscription> getSubscriptions() {
    return Collections.unmodifiableList(subscriptions);
  }

}
