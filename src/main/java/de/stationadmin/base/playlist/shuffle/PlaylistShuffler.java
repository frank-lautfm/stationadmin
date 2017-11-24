/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.Logger;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.track.BasicTrack;

/**
 * Tool class for shuffling a playlist
 * 
 * @author Frank Korf
 */
public class PlaylistShuffler {
  private static final Logger log = Logger.getLogger(PlaylistShuffler.class);
  private Random random = new Random();
  private boolean protectFirstJingle = true;
  private boolean protectAllJingles = false;
  private WordDistributionStrategy wordDistribution = WordDistributionStrategy.RANDOM;
  private ArtistNormalizer artistNormalizer = new DefaultArtistNormalizer();

  private int jingleInterval = 0;
  
  private PlaylistEnhancer playlistEnhancer;

  /**
   * Builds a map of titles for each artist
   * 
   * @param playlist
   * @param titleMap
   * @return maximum number of titles per artist
   */
  private int buildTrackMap(Playlist playlist, ShuffleCtx ctx) {
    List<Entry> entries = playlist.getEntries();
    int max = 1;

    HashSet<Integer> jingleIds = new HashSet<Integer>();
    

    for (int pos = 0; pos < entries.size(); pos++) {
      Entry entry = entries.get(pos);
      BasicTrack title = playlist.getTrackRegistry().getTrack(entry.getTrackId());
      if (title == null) {
        throw new IllegalStateException("Title with id " + entry.getTrackId() + " not known");
      }
      if(playlistEnhancer != null && playlistEnhancer.excludeFromCorePlaylist(title)) {
        continue;
      }

      if (title.getType() != BasicTrack.TYPE_JINGLE) {
        boolean randomTrack = true;
        if (title.getType() == BasicTrack.TYPE_WORD) {
          if (this.wordDistribution == WordDistributionStrategy.PROTECT) {
            ProtectedTrack fxtitle = new ProtectedTrack(pos, title);
            ctx.addProtectedTrack(fxtitle);
            randomTrack = false;
          } else if (this.wordDistribution == WordDistributionStrategy.SUCCESSOR_COUPLING && pos < entries.size() - 1) {
            BasicTrack nextTitle = playlist.getTrackRegistry().getTrack(entries.get(pos + 1).getTrackId());
            if (nextTitle != null) {
              ctx.addCoupledTitle(title, nextTitle);
              randomTrack = false;
            }
          } else if (this.wordDistribution == WordDistributionStrategy.PREDECESSOR_COUPLING && pos > 0) {
            BasicTrack prevTitle = playlist.getTrackRegistry().getTrack(entries.get(pos - 1).getTrackId());
            if (prevTitle != null) {
              ctx.addCoupledTitle(title, prevTitle);
              randomTrack = false;
            }
          }
        }
        if (randomTrack) {
          String artist = this.artistNormalizer.normalizeArtist(title.getArtist());
          int featPos = artist.indexOf(" feat");
          if (featPos > 0) {
            artist = artist.substring(0, featPos);
          }

          List<BasicTrack> titles = ctx.getTitleMap().get(artist);
          if (titles == null) {
            titles = new ArrayList<BasicTrack>();
            ctx.getTitleMap().put(artist, titles);
          }
          titles.add(title);
          max = Math.max(max, titles.size());
        }
      } else {
        // jingle
        if (protectAllJingles) {
          ProtectedTrack fxtitle = new ProtectedTrack(pos, title);
          ctx.addProtectedTrack(fxtitle);
        } else {
          if (!jingleIds.contains(title.getId())) {
            ctx.getJingles().add(title);
            jingleIds.add(title.getId());
          }
          if (pos == 0) {
            ctx.setStartsWithJingle(true);
          }
        }
      }
    }

    return max;

  }

  /**
   * @return the jingleInterval
   */
  public int getJingleInterval() {
    return jingleInterval;
  }

  /**
   * @return the wordDistribution
   */
  public WordDistributionStrategy getWordDistribution() {
    return wordDistribution;
  }

  /**
   * @return the protectFirstJingle
   */
  public boolean isProtectFirstJingle() {
    return protectFirstJingle;
  }

  /**
   * Randomizes the given list
   * 
   * @param <T>
   * @param list
   * @return randomized list
   */
  public <T> List<T> randomize(Collection<T> list) {
    ArrayList<T> available = new ArrayList<T>(list);
    if (available.size() < 2) {
      return available;
    }

    ArrayList<T> randomizedList = new ArrayList<T>(list.size());

    while (available.size() > 1) {
      int idx = this.random.nextInt(available.size());
      randomizedList.add(available.remove(idx));
    }
    randomizedList.add(available.get(0));

    return randomizedList;
  }

  /**
   * @param jingleInterval the jingleInterval to set
   */
  public void setJingleInterval(int jingleInterval) {
    this.jingleInterval = jingleInterval;
  }

  /**
   * @param protectFirstJingle the protectFirstJingle to set
   */
  public void setProtectFirstJingle(boolean protectFirstJingle) {
    this.protectFirstJingle = protectFirstJingle;
  }

  /**
   * @param wordDistribution the wordDistribution to set
   */
  public void setWordDistribution(WordDistributionStrategy wordDistribution) {
    this.wordDistribution = wordDistribution;
  }

  /**
   * Shuffles a single playlist
   * 
   * @param playlist
   */
  public void shuffle(Playlist playlist) {
    log.info("shuffle " + playlist);

    ShuffleCtx ctx = new ShuffleCtx();
    int numSegments = this.buildTrackMap(playlist, ctx) * 2;

    // initialize segments
    Segment[] segments = new Segment[numSegments];
    for (int i = 0; i < numSegments; i++) {
      segments[i] = new Segment();
    }
    log.debug(numSegments + " segments");

    // assign titles of artists to segments
    List<String> artists = this.randomize(ctx.getTitleMap().keySet());
    for (String artist : artists) {
      List<BasicTrack> titlesOfArtist = this.randomize(ctx.getTitleMap().get(artist));
      int artistSegments = numSegments / titlesOfArtist.size();

      // find least filled segment that can act as first segment for this artist
      int currentSegment = 0;
      int minLength = segments[0].getLength();
      for (int i = 1; i < artistSegments; i++) {
        if (segments[i].getLength() < minLength) {
          currentSegment = i;
          minLength = segments[i].getLength();
        }
      }

      // assign title of artist to segment
      for (BasicTrack title : titlesOfArtist) {
        // log.trace(title + " > " + currentSegment);
        segments[currentSegment].add(title);
        currentSegment += artistSegments;
      }
    }

    List<BasicTrack> newTrackList = new ArrayList<BasicTrack>();

    // build final playlist
    BitSet lockedPositions = new BitSet();
    for (Segment segment : segments) {
      log.debug("segement length: " + segment.getLength() / 60 + " minutes");
      List<BasicTrack> segmentTitles = this.randomize(segment.getTitles());
      for (BasicTrack title : segmentTitles) {
        BasicTrack coupledTitle = ctx.getCoupledTitles().get(title);
        if (coupledTitle != null && this.wordDistribution == WordDistributionStrategy.SUCCESSOR_COUPLING) {
          newTrackList.add(coupledTitle);
          lockedPositions.set(newTrackList.size());
        }
        newTrackList.add(title);
        if (coupledTitle != null && this.wordDistribution == WordDistributionStrategy.PREDECESSOR_COUPLING) {
          lockedPositions.set(newTrackList.size());
          newTrackList.add(coupledTitle);
        }
      }
    }

    if (ctx.jingles.size() > 0 && !this.protectAllJingles) {
      // insert jingles
      ArrayList<BasicTrack> titleList = new ArrayList<BasicTrack>(newTrackList);
      int timeNextJingle = 0;
      newTrackList.clear();

      Set<BasicTrack> unusedJingles = new HashSet<BasicTrack>(ctx.getJingles());

      int jingleInterval = this.jingleInterval;
      if (jingleInterval == 0) {
        jingleInterval = (playlist.getLength() / ctx.jingles.size()) / 60;
        log.debug(ctx.jingles.size() + " jingles for " + (playlist.getLength() / 60) + " minutes - place jingle every " + jingleInterval + " minutes");
      }

      int jingleOffset = 0;

      if (protectFirstJingle && ctx.isStartsWithJingle()) {
        log.debug("adding start jingle");
        newTrackList.add(ctx.getJingles().get(0));
        unusedJingles.remove(ctx.getJingles().get(0));
        if (ctx.getJingles().size() > 1) {
          // assume that first jingle was show opener and remove it from list
          // randomize remaining jingles
          ctx.getJingles().remove(0);
        }
        timeNextJingle = jingleInterval;
        jingleOffset = jingleInterval;
      } else {
        jingleOffset = random.nextInt(jingleInterval);
        log.debug("jingle offset: " + jingleOffset);
        timeNextJingle = jingleOffset;
      }

      int jingleIdx = 0;
      int jingleCnt = 0;
      int currentTimeSec = 0;

      if (ctx.getJingles().size() > 0) {
        // we have more jingles - distribute them
        ctx.setJingles(randomize(ctx.getJingles()));

        for (int i = 0; i < titleList.size(); i++) {
          if ((currentTimeSec / 60) >= timeNextJingle && !lockedPositions.get(i)) {
            log.debug("place jingle " + jingleIdx + " at " + (currentTimeSec / 60) + " / target time was " + timeNextJingle);
            newTrackList.add(ctx.getJingles().get(jingleIdx));
            unusedJingles.remove(ctx.getJingles().get(jingleIdx));
            jingleIdx = (jingleIdx + 1) % ctx.getJingles().size();
            jingleCnt++;
            timeNextJingle = jingleOffset + jingleCnt * jingleInterval;
          }
          newTrackList.add(titleList.get(i));
          currentTimeSec += titleList.get(i).getLength();
        }

        // append unused jingles
        for (BasicTrack title : unusedJingles) {
          newTrackList.add(title);
        }
      }
    }

    if (ctx.getProtectedTracks().size() > 0) {
      for (ProtectedTrack fxtitle : ctx.getProtectedTracks()) {
        newTrackList.add(fxtitle.getPosition(), fxtitle.getTrack());
      }
    }
    
    if(this.playlistEnhancer != null) {
      newTrackList = playlistEnhancer.process(newTrackList);
    }

    playlist.setTracks(newTrackList);
  }

  private static class ProtectedTrack {
    private int position;
    private BasicTrack track;

    /**
     * @param position
     * @param title
     */
    ProtectedTrack(int position, BasicTrack title) {
      super();
      this.position = position;
      this.track = title;
    }

    /**
     * @return the position
     */
    int getPosition() {
      return position;
    }

    /**
     * @return the title
     */
    BasicTrack getTrack() {
      return track;
    }

  }

  /**
   * A Segment is a part of the playlist as it is generated.
   */
  private static class Segment {
    private List<BasicTrack> titles = new ArrayList<BasicTrack>();
    private int length;

    void add(BasicTrack title) {
      this.titles.add(title);
      this.length += title.getLength();
    }

    public int getLength() {
      return length;
    }

    public List<BasicTrack> getTitles() {
      return titles;
    }

  }

  private static class ShuffleCtx {
    private Map<String, List<BasicTrack>> titleMap = new HashMap<String, List<BasicTrack>>();
    private List<BasicTrack> jingles = new ArrayList<BasicTrack>();
    private List<ProtectedTrack> protectedTracks = new ArrayList<ProtectedTrack>();
    private Map<BasicTrack, BasicTrack> coupledTitles = new HashMap<BasicTrack, BasicTrack>();
    private boolean startsWithJingle = false;

    public void addCoupledTitle(BasicTrack coupledTitle, BasicTrack coupledTo) {
      this.coupledTitles.put(coupledTo, coupledTitle);
    }

    public void addProtectedTrack(ProtectedTrack title) {
      this.protectedTracks.add(title);
    }

    /**
     * @return the coupledTitles
     */
    public Map<BasicTrack, BasicTrack> getCoupledTitles() {
      return coupledTitles;
    }

    /**
     * @return the fixedTitles
     */
    public List<ProtectedTrack> getProtectedTracks() {
      return protectedTracks;
    }

    /**
     * @return the jingles
     */
    public List<BasicTrack> getJingles() {
      return jingles;
    }

    /**
     * @return the titleMap
     */
    public Map<String, List<BasicTrack>> getTitleMap() {
      return titleMap;
    }

    /**
     * @return the startsWithJingle
     */
    public boolean isStartsWithJingle() {
      return startsWithJingle;
    }

    /**
     * @param jingles the jingles to set
     */
    public void setJingles(List<BasicTrack> jingles) {
      this.jingles = jingles;
    }

    /**
     * @param startsWithJingle the startsWithJingle to set
     */
    public void setStartsWithJingle(boolean startsWithJingle) {
      this.startsWithJingle = startsWithJingle;
    }

  }

  /**
   * @return the artistNormalizer
   */
  public ArtistNormalizer getArtistNormalizer() {
    return artistNormalizer;
  }

  /**
   * @param artistNormalizer the artistNormalizer to set
   */
  public void setArtistNormalizer(ArtistNormalizer artistNormalizer) {
    this.artistNormalizer = artistNormalizer;
  }

  public boolean isProtectAllJingles() {
    return protectAllJingles;
  }

  public void setProtectAllJingles(boolean protectAllJingles) {
    this.protectAllJingles = protectAllJingles;
  }

  public PlaylistEnhancer getPlaylistEnhancer() {
    return playlistEnhancer;
  }

  public void setPlaylistEnhancer(PlaylistEnhancer trackRuleEngine) {
    this.playlistEnhancer = trackRuleEngine;
  }

}
