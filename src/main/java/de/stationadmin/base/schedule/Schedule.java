/**
 * 
 */
package de.stationadmin.base.schedule;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TimerTask;

import org.apache.log4j.Logger;
import org.json.JSONException;

import com.thoughtworks.xstream.XStream;

import de.stationadmin.base.AccessDeniedException;
import de.stationadmin.base.Role;
import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.PlaylistRegistry;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.base.util.XStreamFactory;
import de.stationadmin.lfm.backend.ScheduleEntry;
import de.stationadmin.lfm.backend.ScheduledEvent;

/**
 * Playlist schedule
 * 
 * @author Frank Korf
 */
public class Schedule extends AbstractBean implements Service {
  private static final Logger log = Logger.getLogger(Schedule.class);
  private SessionCtx ctx;
  private PlaylistRegistry playlistRegistry;
  private int basePlaylistId;

  private List<Entry> entries = Collections.synchronizedList(new ArrayList<Entry>());
  private List<Event> events = Collections.synchronizedList(new ArrayList<Event>());
  private Entry current;

  private boolean dirty = true;

  private int numPlaylists;
  private int numTracks;
  private SchedulerRefresher refresherTask;

  public Schedule(SessionCtx ctx, PlaylistRegistry playlistRegistry) {
    super();
    this.ctx = ctx;
    this.playlistRegistry = playlistRegistry;
    this.refresherTask = new SchedulerRefresher();
    this.ctx.getTimer().schedule(this.refresherTask, 1000 * 60, 1000 * 60);
  }

  public static Weekday getTodaysWeekday() {
    return Weekday.getWeekday(new Date(System.currentTimeMillis()));
  }

  /**
   * @param entries
   *          the entries to set
   */
  public void addEntry(Entry entry) {
    this.entries.add(entry);
    this.dirty = true;
  }

  public void cleanUp() {
    // try to clean up multiple entries for the same time
    Collections.sort(this.entries);
    List<Entry> entries = new ArrayList<Entry>(this.entries);
    for (int i = 1; i < entries.size(); i++) {
      if (entries.get(i - 1).getWeekday() == entries.get(i).getWeekday() && entries.get(i - 1).getHour() == entries.get(i).getHour()) {
        if (entries.get(i - 1).getPlaylistId() == 0) {
          this.entries.remove(entries.get(i - 1));
        } else if (entries.get(i).getPlaylistId() == 0) {
          this.entries.remove(entries.get(i));
        }
      }

    }

  }

  public void clear() {
    this.dirty = true;
    this.entries.clear();
  }

  /**
   * @see de.stationadmin.base.Service#close()
   */
  @Override
  public void close() {
    this.refresherTask.cancel();
  }

  public void fillUpWithBasicPlaylist() {
    for (Weekday weekday : Weekday.values()) {
      List<Entry> entries = this.getEntriesOf(weekday);
      if (entries.size() == 0 || entries.get(0).getHour() > 0) {
        this.entries.add(new Entry(0, weekday, 0));
      }
    }
  }

  /**
   * Gets the entry that is currently active
   * 
   * @return
   */
  public Entry getCurrent() {
    return current;
  }

  /**
   * Gets the entries
   * 
   * @return the entries
   */
  public List<Entry> getEntries() {
    return entries;
  }

  /**
   * Gets the entries of the next hours, starting with the entry after the one referred by the start date
   * 
   * @param startDate
   * @param hours
   * @return
   */
  public List<Entry> getEntriesAfter(Date startDate, int hours) {
    ArrayList<Entry> filtered = new ArrayList<Entry>();

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(startDate.getTime());
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    int day = Weekday.getWeekday(cal.getTime()).ordinal();

    Entry[][] entryTable = new Entry[7][];
    for (int i = 0; i < 7; i++) {
      entryTable[i] = new Entry[24];
    }
    Entry last = null;
    for (Entry entry : this.entries) {
      entryTable[entry.getWeekday().ordinal()][entry.getHour()] = entry;
      last = entry;
    }
    for (int w = 0; w < 7; w++) {
      for (int h = 0; h < 24; h++) {
        if (entryTable[w][h] == null) {
          entryTable[w][h] = last;
        } else {
          last = entryTable[w][h];
        }
      }
    }

    Entry startEntry = entryTable[day][hour];

    for (int i = 0; i <= hours; i++) {
      if (!filtered.contains(entryTable[day][hour])) {
        filtered.add(entryTable[day][hour]);
      }
      hour++;
      if (hour == 24) {
        hour = 0;
        day++;
        if (day == 7) {
          day = 0;
        }
      }
    }

    filtered.remove(startEntry);

    return filtered;
  }

  public List<Entry> getEntriesOf(Weekday weekday) {
    ArrayList<Entry> filtered = new ArrayList<Entry>();

    for (Entry entry : this.entries) {
      if (entry.getWeekday() == weekday) {
        filtered.add(entry);
      }
    }
    Collections.sort(filtered);
    return filtered;
  }

  public List<Entry> getEffectiveEntriesOfToday() {
    List<Entry> scheduledEntries = getEntriesOf(Schedule.getTodaysWeekday());
    if (this.events.size() == 0) {
      return scheduledEntries;
    }
    List<Event> eventsOfToday = new ArrayList<Event>();
    Calendar calToday = Calendar.getInstance();
    calToday.setTimeInMillis(System.currentTimeMillis());
    Calendar calEvent = Calendar.getInstance();

    for (Event evt : this.events) {
      calEvent.setTime(evt.getStartTime());
      if (calToday.get(Calendar.DAY_OF_YEAR) == calEvent.get(Calendar.DAY_OF_YEAR) && calToday.get(Calendar.YEAR) == calEvent.get(Calendar.YEAR)) {
        eventsOfToday.add(evt);
      } else {
        calEvent.setTime(evt.getEndTime());
        if (calToday.get(Calendar.DAY_OF_YEAR) == calEvent.get(Calendar.DAY_OF_YEAR) && calToday.get(Calendar.YEAR) == calEvent.get(Calendar.YEAR)) {
          // create an artificial entry which just covers todays part of the event
          int duration = calEvent.get(Calendar.HOUR_OF_DAY);
          calEvent.set(Calendar.HOUR_OF_DAY, 0);
          Date startTime = calEvent.getTime();
          Event evtToday = new Event(evt.getPlaylistId(), startTime, duration);
          eventsOfToday.add(evtToday);
        }
      }
      // note: we won't catch entries spanning over the entire day
    }

    if (eventsOfToday.size() == 0) {
      return scheduledEntries;
    }

    // build hour plan for the day
    Entry[] hours = new Entry[24];
    for (Entry entry : scheduledEntries) {
      for (int i = entry.getHour(); i < hours.length; i++) {
        hours[i] = new Entry(entry.getPlaylistId(), entry.getWeekday(), i);
      }
    }
    for (Event evt : eventsOfToday) {
      Entry entry = evt.getEntry();
      for (int i = entry.getHour(); i < entry.getHour() + evt.getDuration() && i < hours.length; i++) {
        hours[i] = entry;
      }
    }

    Entry last = null;
    List<Entry> entries = new ArrayList<Schedule.Entry>();
    for (int i = 0; i < hours.length; i++) {
      if (last == null || last.getPlaylistId() != hours[i].getPlaylistId()) {
        entries.add(hours[i]);
        last = hours[i];
      }
    }

    return entries;
  }

  public List<Entry> getEntriesOfNext24h() {
    ArrayList<Entry> filtered = new ArrayList<Entry>();

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    int calDay = cal.get(Calendar.DAY_OF_WEEK);
    int hour = cal.get(Calendar.HOUR_OF_DAY);
    Weekday today = null;
    Weekday tomorrow = null;
    for (Weekday day : Weekday.values()) {
      if (calDay == day.getCalDay()) {
        today = day;
        tomorrow = Weekday.values()[(today.ordinal() + 1) % Weekday.values().length];
      }
    }

    for (Entry entry : this.entries) {
      if ((entry.getWeekday() == today && entry.getHour() > hour) || (entry.getWeekday() == tomorrow && entry.getHour() <= hour)) {
        filtered.add(entry);
      }
    }
    Collections.sort(filtered);

    return filtered;
  }

  public int getNumEntries() {
    return this.entries.size();
  }

  /**
   * Gets the number of different playlists used in the schedule
   * 
   * @return
   */
  public int getNumPlaylists() {
    return numPlaylists;
  }

  /**
   * Gets the number of different titles used in the playlists of the schedule
   * 
   * @return
   */
  public int getNumTracks() {
    return numTracks;
  }

  /**
   * @return the playlistRegistry
   */
  public PlaylistRegistry getPlaylistRegistry() {
    return playlistRegistry;
  }

  public List<Playlist> getPlaylistsAfter(Date startDate, int hours) {
    List<Entry> entries = this.getEntriesAfter(startDate, hours);
    Set<Integer> known = new HashSet<Integer>();
    List<Playlist> playlists = new ArrayList<Playlist>();
    for (Entry entry : entries) {
      if (!known.contains(entry.getPlaylistId())) {
        known.add(entry.getPlaylistId());
        playlists.add(this.playlistRegistry.getPlaylist(entry.getPlaylistId()));
      }
    }

    return playlists;

  }

  private XStream getXStream() {
    XStream xstream = XStreamFactory.newXStream();
    xstream.alias("scheduledShow", Schedule.Entry.class);
    return xstream;
  }

  /**
   * Loads the schedule from a local file
   * 
   * @throws IOException
   */
  public void load() throws IOException {
    if (this.ctx.getRole() == Role.DJ) {
      return;
    }
    if (this.playlistRegistry.getAllPlaylists().size() > 1) {
      this.synchronize();
    }
    // otherwise: Not enough playlists for a schedule
  }

  private void loadEvents() throws IOException {
    this.events.clear();
    ScheduledEvent[] events = this.ctx.getServer().getScheduledEvents(ctx.getStationId());
    for (ScheduledEvent event : events) {
      this.events.add(new Event(event));
    }
    Collections.sort(this.events);
  }

  @SuppressWarnings("unchecked")
  private List<Schedule.Entry> load(File file) throws IOException {
    XStream xstream = this.getXStream();
    FileInputStream schedStream = new FileInputStream(file);
    List<Schedule.Entry> entries = (List<Schedule.Entry>) xstream.fromXML(schedStream);
    schedStream.close();
    return entries;

  }

  @SuppressWarnings("unchecked")
  public void load(InputStream stream) throws IOException {
    XStream xstream = this.getXStream();
    List<Schedule.Entry> entries = (List<Schedule.Entry>) xstream.fromXML(stream);
    this.clear();
    for (Schedule.Entry entry : entries) {
      if (entry.getHour() > -1) {
        this.addEntry(entry);
      } else {
        this.basePlaylistId = entry.getPlaylistId();
      }
    }
    this.updateCurrentEntry();
    this.updateStatistics(playlistRegistry);

  }

  public List<Schedule.Entry> loadEntries(String filename) throws IOException {
    File file = new File(filename);
    if (file.exists()) {
      return this.load(file);
    } else {
      return new ArrayList<Entry>(0);
    }
  }

  /**
   * Removes an entry from the schedule
   * 
   * @param entry
   */
  public void removeEntry(Entry entry) {
    this.entries.remove(entry);
  }

  /**
   * Saves the schedule to a local file in the data directory
   * 
   * @throws IOException
   */
  public void save() throws IOException {
    checkAccess();
    log.info("save schedule");
    this.save(new File(this.ctx.getStationDirectory() + "schedule.xml"));
  }

  private void save(File file) throws IOException {
    try {
      FileOutputStream schedStream = new FileOutputStream(file);
      this.save(schedStream);
      schedStream.flush();
      schedStream.close();
    } catch (IOException e) {
      log.error("error while saving schedule", e);
      throw e;
    }

  }

  public void save(OutputStream stream) throws IOException {
    XStream xstream = this.getXStream();
    ArrayList<Entry> entries = new ArrayList<Schedule.Entry>();
    entries.add(new Entry(basePlaylistId, Weekday.MONDAY, -1));
    entries.addAll(this.getEntries());
    BufferedOutputStream out = new BufferedOutputStream(stream, 2048);
    xstream.toXML(entries, out);
    out.flush();
  }

  public void save(String file) throws IOException {
    checkAccess();
    this.save(new File(file));
  }

  /**
   * @param entries
   *          the entries to set
   */
  public void setEntries(List<Entry> entries) {
    this.entries = Collections.synchronizedList(new ArrayList<Entry>(entries));
    this.dirty = true;
  }

  /*
   * (non-Javadoc)
   * 
   * @see de.emjoy.stationadmin.base.Service#startBackgrounTasks()
   */
  @Override
  public void initBackgroundTasks() {
  }

  public boolean isScheduled(Playlist playlist) {
    for (Entry entry : this.entries) {
      if (entry.getPlaylistId() == playlist.getId()) {
        return true;
      }
    }
    return false;
  }

  /**
   * Saves this schedule to the server
   * 
   * @throws IOException
   * @throws JSONException
   */
  public void submitToServer() throws IOException, JSONException {
    checkAccess();
    int[][] table = new int[7][24];
    for (Schedule.Entry entry : this.entries) {
      int d = entry.getWeekday().ordinal();
      Arrays.fill(table[d], entry.getHour(), 24, entry.getPlaylistId());
    }

    int slot = 0;
    ArrayList<ScheduleEntry> entries = new ArrayList<ScheduleEntry>();
    while (slot < 7 * 24) {
      int plId = table[slot / 24][slot % 24];
      if (plId != this.basePlaylistId) {
        int start = slot;
        int duration = 1;
        slot++;
        while (slot < 7 * 24 && table[slot / 24][slot % 24] == plId) {
          duration++;
          slot++;
        }
        entries.add(new ScheduleEntry(plId, start, duration));
      } else {
        slot++;
      }
    }

    de.stationadmin.lfm.backend.Schedule schedule = this.ctx.getServer().getSchedule(ctx.getStationId());
    schedule.setEntries(entries.toArray(new ScheduleEntry[entries.size()]));
    this.ctx.getServer().updateSchedule(ctx.getStationId(), schedule);
  }

  private void checkAccess() {
    if (this.ctx.isDJOnly()) {
      throw new AccessDeniedException();
    }

  }

  public void synchronize() throws IOException {
    if (this.ctx.getRole() == Role.DJ) {
      return;
    }

    this.ctx.updateStatus("getSchedule");
    de.stationadmin.lfm.backend.Schedule schedule = this.ctx.getServer().getSchedule(ctx.getStationId());
    this.basePlaylistId = schedule.getBasePlaylistId();

    // construct a day/hour matrix
    int[][] table = new int[7][24];
    for (ScheduleEntry entry : schedule.getEntries()) {
      for (int i = entry.getSlot(); i < entry.getSlot() + entry.getDuration(); i++) {
        int day = i / 24;
        int hour = i % 24;
        table[day][hour] = entry.getPlaylistId();
      }
    }
    // fill empty slots with base playlist
    for (int d = 0; d < table.length; d++) {
      for (int h = 0; h < table[d].length; h++) {
        if (table[d][h] == 0) {
          table[d][h] = schedule.getBasePlaylistId();
        }
      }
    }

    this.clear();
    for (int d = 0; d < table.length; d++) {
      int lastPlaylistId = -1;
      for (int h = 0; h < table[d].length; h++) {
        if (table[d][h] != lastPlaylistId) {
          lastPlaylistId = table[d][h];
          this.addEntry(new Schedule.Entry(table[d][h], Weekday.values()[d], h));
        }
      }
    }

    try {
      this.loadEvents();
    } catch (Exception e) {
      log.error("Unable to load events", e);
    }

    this.updateCurrentEntry();
    this.updateStatistics(playlistRegistry);
    this.save();
  }

  public static Entry toEntry(ScheduledEvent evt) {
    Calendar cal = Calendar.getInstance();
    cal.setTime(evt.getStartTime());
    return new Entry(evt.getPlaylistId(), Weekday.getWeekday(evt.getStartTime()), cal.get(Calendar.HOUR_OF_DAY));
  }

  public void updateCurrentEntry() {
    Entry old = this.current;

    Calendar cal = Calendar.getInstance();
    cal.setTimeInMillis(System.currentTimeMillis());
    int hourOfDay = cal.get(Calendar.HOUR_OF_DAY);

    Entry current = null;

    List<Entry> entries = this.getEntriesOf(getTodaysWeekday());
    for (int i = 0; i < entries.size(); i++) {
      if (hourOfDay >= entries.get(i).getHour()) {
        current = entries.get(i);
      }
    }

    for (Event evt : this.events) {
      if (System.currentTimeMillis() > evt.getStartTime().getTime() && System.currentTimeMillis() < evt.getEndTime().getTime()) {
        current = evt.getEntry();
      }
    }

    this.current = current;
    this.firePropertyChange("current", old, current);

  }

  public void updateStatistics(PlaylistRegistry playlistRegistry) {
    if (this.dirty) {
      this.dirty = false;

      int oldNumPlaylists = this.numPlaylists;
      int oldNumTitles = this.numTracks;

      Set<Playlist> playlists = new HashSet<Playlist>();
      for (Entry entry : this.entries) {
        Playlist playlist = playlistRegistry.getPlaylist(entry.getPlaylistId());
        if (playlist != null) {
          playlists.add(playlist);
        }
      }

      this.numPlaylists = playlists.size();
      this.firePropertyChange("numPlaylists", oldNumPlaylists, this.numPlaylists);

      HashSet<Integer> titleIds = new HashSet<Integer>();
      for (Playlist playlist : playlists) {
        for (Playlist.Entry entry : playlist.getEntries()) {
          titleIds.add(entry.getTrackId());
        }
      }

      this.numTracks = titleIds.size();
      this.firePropertyChange("numTracks", oldNumTitles, this.numTracks);

    }
  }

  /**
   * Entry of playlist schedule - contains of a playlist and a time at which it is played
   */
  public static class Entry implements Comparable<Entry> {
    private int playlistId;
    private Weekday weekday;
    private int hour;
    private boolean event;

    public Entry(int playlistId, Weekday weekday, int hour) {
      this(playlistId, weekday, hour, false);
    }

    public Entry(int playlistId, Weekday weekday, int hour, boolean event) {
      super();
      this.playlistId = playlistId;
      this.weekday = weekday;
      this.hour = hour;
      this.event = event;
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Entry o) {
      int result = this.weekday.compareTo(o.weekday);
      if (result == 0) {
        result = Integer.valueOf(this.hour).compareTo(o.hour);
      }
      return result;
    }

    /**
     * @see java.lang.Object#equals(java.lang.Object)
     */
    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Entry) {
        Entry other = (Entry) obj;
        return this.weekday == other.weekday && this.hour == other.hour && this.playlistId == other.playlistId;
      }
      return false;
    }

    /**
     * @return the hour
     */
    public int getHour() {
      return hour;
    }

    /**
     * @return the playlistId
     */
    public int getPlaylistId() {
      return playlistId;
    }

    /**
     * @return the weekday
     */
    public Weekday getWeekday() {
      return weekday;
    }

    /**
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode() {
      return this.weekday.ordinal() * 10 + this.hour;
    }

    public boolean isToday() {
      return this.weekday == getTodaysWeekday();
    }

    @Override
    public String toString() {
      return this.weekday + " - " + this.hour + ": " + this.playlistId;
    }

    /**
     * @return the event
     */
    boolean isEvent() {
      return event;
    }

    /**
     * @param playlistId the playlistId to set
     */
    public void setPlaylistId(int playlistId) {
      this.playlistId = playlistId;
    }

  }

  public static class Event implements Comparable<Event> {
    private int id;
    private int playlistId;
    private Date startTime;
    private Date endTime;
    private int duration;
    private Entry entry;

    public Event(ScheduledEvent evt) {
      this(evt.getPlaylistId(), evt.getStartTime(), evt.getDuration());
      this.id = evt.getId();
    }

    /**
     * @param playlistId
     * @param startTime
     * @param duration
     */
    public Event(int playlistId, Date startTime, int duration) {
      super();
      this.playlistId = playlistId;
      this.startTime = startTime;
      this.endTime = new Date(this.startTime.getTime() + duration * 1000 * 60 * 60);
      this.duration = duration;

      Calendar cal = Calendar.getInstance();
      cal.setTime(startTime);
      entry = new Entry(playlistId, Weekday.getWeekday(startTime), cal.get(Calendar.HOUR_OF_DAY));
    }

    /**
     * @return the playlistId
     */
    public int getPlaylistId() {
      return playlistId;
    }

    /**
     * @return the startTime
     */
    public Date getStartTime() {
      return startTime;
    }

    /**
     * @return the duration
     */
    public int getDuration() {
      return duration;
    }

    /**
     * @return the endTime
     */
    public Date getEndTime() {
      return endTime;
    }

    /**
     * @return the entry
     */
    public Entry getEntry() {
      return entry;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Event o) {
      return Long.compare(this.startTime.getTime(), o.getStartTime().getTime());
    }

    /**
     * @return the id
     */
    public int getId() {
      return id;
    }

    /**
     * @param id
     *          the id to set
     */
    public void setId(int id) {
      this.id = id;
    }

    @Override
    public boolean equals(Object obj) {
      if (obj instanceof Event) {
        return id == ((Event) obj).id || (playlistId == ((Event) obj).playlistId && startTime.equals(((Event) obj).startTime));
      }
      return false;
    }

  }

  private class SchedulerRefresher extends TimerTask {

    /**
     * @see java.util.TimerTask#run()
     */
    @Override
    public void run() {
      updateCurrentEntry();
      updateStatistics(playlistRegistry);
    }

  }

  public enum Weekday {
    MONDAY(1, 2), TUESDAY(2, 3), WEDNESDAY(3, 4), THURSDAY(4, 5), FRIDAY(5, 6), SATURDAY(6, 7), SUNDAY(7, 1);

    int rawDay;
    int calDay;

    private Weekday(int rawDay, int calDay) {
      this.rawDay = rawDay;
      this.calDay = calDay;
    }

    static Weekday fromRaw(int day) {
      for (Weekday wday : Weekday.values()) {
        if (wday.rawDay == day) {
          return wday;
        }
      }
      return null;
    }

    public static Weekday getWeekday(Date date) {
      Calendar cal = Calendar.getInstance();
      cal.setTime(date);
      int calDay = cal.get(Calendar.DAY_OF_WEEK);
      for (Weekday day : Weekday.values()) {
        if (calDay == day.getCalDay()) {
          return day;
        }
      }
      return null;
    }

    /**
     * @return the calDay
     */
    public int getCalDay() {
      return calDay;
    }

    /**
     * @return the rawDay
     */
    public int getRawDay() {
      return rawDay;
    }

  }

  public Playlist getBasePlaylist() {
    return this.playlistRegistry.getPlaylist(basePlaylistId);
  }

  /**
   * @return the events
   */
  public List<Event> getEvents() {
    return Collections.unmodifiableList(events);
  }

  public void scheduleEvent(Event event) throws IOException {
    ScheduledEvent evt = new ScheduledEvent();
    evt.setPlaylistId(event.getPlaylistId());
    evt.setStartTime(event.getStartTime());
    evt.setDuration(event.getDuration());
    this.events.add(new Event(ctx.getServer().scheduleEvent(ctx.getStationId(), evt)));
    Collections.sort(this.events);
  }

  public void deleteEvent(Event event) throws IOException {
    this.ctx.getServer().deleteScheduledEvent(ctx.getStationId(), event.getId());
    this.events.remove(event);
  }

}
