/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.json.JSONObject;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.tag.TagChecker;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.util.TimeFormat;

/**
 * Tool class for shuffling a playlist
 * 
 * @author Frank Korf
 */
public class PlaylistGenerator {
  private static final Logger log = Logger.getLogger(PlaylistGenerator.class);

  private TagChecker tagManager;
  private TrackRegistry trackRegistry;
  private Random random = new Random();
  private boolean protectFirstJingle = true;
  private boolean protectAllJingles = false;
  private int jingleInterval = 0;

  private long time = System.currentTimeMillis();
  private Map<String, Artist> artists = new HashMap<String, Artist>();
  private Map<Integer, TrackScorer> trackScores = new HashMap<Integer, TrackScorer>();

  private boolean artistPenaltyEnabled = true;
  private int trackPenaltyPeriod = 60 * 18;
  private int trackPenaltyMax = 300;
  private boolean trackPenaltyEnabled = true;
  private int artistPenaltyPeriod = 60 * 6;

  private ArtistNormalizer artistNormalizer = new DefaultArtistNormalizer();
  private ArtistTrackPreselector artistTrackPreselector = new DefaultTrackPreselector();

  private PlaylistEnhancer playlistEnhancer;

  private int minRandomValue = 100;

  private WordDistributionStrategy wordDistribution = WordDistributionStrategy.RANDOM;

  private List<TagWeight> globalPushTags = new ArrayList<TagWeight>();

  private int maxArtistTitles = 0;

  public PlaylistGenerator(TagChecker tagChecker, TrackRegistry titleRegistry) {
    super();
    this.tagManager = tagChecker;
    this.trackRegistry = titleRegistry;
  }

  /**
   * Assigns a new seed to the random generator. Should only be used for testing
   * purposes.
   * 
   * @param seed
   */
  public void setRandomSeed(long seed) {
    this.random = new Random(seed);
  }

  /**
   * Builds a map of titles for each artist
   * 
   * @param playlist
   * @param titleMap
   * @return maximum number of titles per artist
   */
  private int buildTrackMap(Set<BasicTrack> candidates, GeneratorCtx ctx) {
    int max = 1;

    HashSet<Integer> jingleIds = new HashSet<Integer>();

    for (BasicTrack title : candidates) {

      if (playlistEnhancer != null && playlistEnhancer.excludeFromCorePlaylist(title)) {
        continue;
      }

      if (title.getType() == BasicTrack.TYPE_MUSIC || (title.getType() == BasicTrack.TYPE_WORD && this.wordDistribution == WordDistributionStrategy.RANDOM)) {

        String artistName = this.artistNormalizer.normalizeArtist(title.getArtist());

        Artist artist = ctx.getTitleMap().get(artistName);
        if (artist == null) {
          artist = getArtist(artistName);
          ctx.getTitleMap().put(artistName, artist);
        }
        artist.add(title);
        if (this.getTrackScorer(title).isRepeatAllowed()) {
          // add two more instances
          artist.add(title);
          artist.add(title);
        }
        max = Math.max(max, artist.getTracks().size());
      } else {
        // jingle
        if (!jingleIds.contains(title.getId())) {
          ctx.getJingles().add(title);
          jingleIds.add(title.getId());
        }
      }

      max = Math.max(max, ctx.getJingles().size());
    }

    if (this.maxArtistTitles > 0) {
      // preselect tracks for artist
      for (String artistName : ctx.getTitleMap().keySet()) {
        Artist artist = ctx.getTitleMap().get(artistName);
        List<BasicTrack> tracks = new ArrayList<BasicTrack>();
        for (TrackInstance instance : artist.getTracks()) {
          tracks.add(instance.getTrack());
        }
        List<BasicTrack> preselected = this.artistTrackPreselector.preselect(tracks, this.maxArtistTitles);
        if (preselected.size() < tracks.size()) {
          artist.getTracks().clear();
          for (BasicTrack track : preselected) {
            artist.add(track);
          }
        }
      }
    }

    return max;
  }

  private Set<BasicTrack> collectTitles(Playlist playlist) throws IOException {
    Set<BasicTrack> titles = new HashSet<BasicTrack>();

    if (playlist.getGenerateTags() != null) {
      String[] tags = StringUtils.split(playlist.getGenerateTags(), ";");
      for (String tag : tags) {
        tag = tag.trim();
        int[] titleIds = this.tagManager.getTrackIds(tag);
        if (titleIds != null) {
          log.debug("load title ids for " + tag);
          Set<BasicTrack> tagTitles = new HashSet<BasicTrack>();
          for (int titleId : titleIds) {
            BasicTrack title = this.trackRegistry.getTrack(titleId);
            if (title != null) {
              tagTitles.add(title);
            }
          }
          if (playlist.isGenerateTagsAll() && titles.size() > 0) {
            titles.retainAll(tagTitles);
          } else {
            titles.addAll(tagTitles);
          }
        }
      }
    }

    return titles;
  }

  /**
   * Generates a single playlist
   * 
   * @param playlist
   */
  public void generate(Playlist playlist) throws IOException {
    this.generate(playlist, false);
  }

  private void extractProtectedTracks(Playlist playlist, GeneratorCtx ctx) {
    int position = 0;
    if (this.wordDistribution == WordDistributionStrategy.PROTECT || this.protectAllJingles) {
      for (int i = 0; i < playlist.getEntries().size(); i++) {
        if (playlist.getEntry(i).getTrack().getType() == BasicTrack.TYPE_MUSIC) {
          position++;
        } else {
          if ((this.wordDistribution == WordDistributionStrategy.PROTECT && playlist.getEntries().get(i).getTrack().getType() == BasicTrack.TYPE_WORD)
              || (this.protectAllJingles && playlist.getEntries().get(i).getTrack().getType() == BasicTrack.TYPE_JINGLE)) {
            ctx.addProtectedTrack(position, playlist.getEntries().get(i).getTrack());
            position++;
          }
        }
      }
    }
  }

  /**
   * Generates a single playlist
   * 
   * @param playlist
   * @param append <code>true</code> to append titles to playlist,
   *        <code>false</code> to replace titles
   */
  public void generate(Playlist playlist, boolean append) throws IOException {
    log.info("generate " + playlist);
    this.prepareNextPlaylist();

    GeneratorCtx ctx = new GeneratorCtx();

    if (this.protectFirstJingle && playlist.getEntries().size() > 0 && !this.protectAllJingles) {
      // check if first title is jingle and protect it if so
      Entry entry = playlist.getEntries().get(0);
      BasicTrack firstTrack = this.trackRegistry.getTrack(entry.getTrackId());
      if (firstTrack != null && firstTrack.getType() >= 2 && (playlistEnhancer == null || !playlistEnhancer.excludeFromCorePlaylist(firstTrack))) {
        ctx.setFirstJingle(firstTrack);
      }
    }

    this.extractProtectedTracks(playlist, ctx);
    Set<BasicTrack> titles = this.collectTitles(playlist);
    HashSet<String> localWeightTags = new HashSet<String>();
    if (playlist.getGeneratePushTag() != null) {
      String[] tags = StringUtils.split(playlist.getGeneratePushTag(), ";");
      for (String tag : tags) {
        localWeightTags.add(tag);
        int pushFactor = 2;
        float maxFraction = 1f;
        String[] fields = StringUtils.split(tag, ":");
        if (fields.length > 1) {
          tag = fields[0];
          try {
            pushFactor = Integer.parseInt(fields[1]);
          } catch (NumberFormatException e) {
            log.warn("parsing problems: " + tag + " - using default push factor");
          }
          if (fields.length > 2) {
            try {
              maxFraction = Float.parseFloat(fields[2]);
            } catch (NumberFormatException e) {
              log.warn("parsing problems: " + tag + " - using default limit");
            }

          }
        }
        this.applyPushTag(ctx, playlist, titles, tag, pushFactor, maxFraction);
      }
    }

    // apply global weights as far as not overwritten by local tags
    if (this.globalPushTags != null) {
      for (TagWeight global : this.globalPushTags) {
        if (!localWeightTags.contains(global.getTag())) {
          applyPushTag(ctx, playlist, titles, global.getTag(), global.getWeight(), global.getMaxFraction());
        }
      }
    }

    if (playlist.getGenerateTitleRepeatLevel() > -1) {
      for (BasicTrack title : titles) {
        TrackScorer sc = this.getTrackScorer(title);
        if (playlist.getGenerateTitleRepeatLevel() == 0 || playlist.getGenerateTitleRepeatLevel() <= sc.getPushFactor()) {
          sc.setRepeatAllowed(true);
        }
      }
    }

    // prepare advices
    if (playlist.getGenerateAdvices() != null) {
      for (String adv : playlist.getGenerateAdvices()) {
        try {
          JSONObject json = new JSONObject(adv);
          int type = json.getInt("type");
          switch (type) {
          case 1:
            ctx.getAdvices().add(new TagSequenceAdvice(this.tagManager, json));
            break;
          case 2:
            ctx.getAdvices().add(new TitleNameLimitAdvice(json));
            break;
          }
        } catch (Exception e) {
          log.error("Advice instantiation error for " + adv, e);
        }
      }
    }

    int maxArtistTitles = playlist.getGenerateMaxArtistTitles();
    if (maxArtistTitles > 3 && maxArtistTitles > playlist.getGenerateLength()) {
      maxArtistTitles = playlist.getGenerateLength();
    }
    int numSegments = Math.min(maxArtistTitles, this.buildTrackMap(titles, ctx)) * 2;

    // initialize segments
    Segment[] segments = new Segment[numSegments];
    for (int i = 0; i < numSegments; i++) {
      segments[i] = new Segment();
    }
    log.debug(numSegments + " segments");

    // sort artists
    // - artists that have been played recently should have lower chances
    // - artists with playable pushed titles should have higher chances
    List<Artist> artists = new ArrayList<Artist>(ctx.getTitleMap().values());
    for (Artist artist : artists) {
      artist.prepareTitleList(maxArtistTitles);
    }

    // select titles
    log.info("minimize artist repeats = " + playlist.isGenerateMinimizeArtistRepeats());
    int totalSegmentsLength = 0;
    int maxLength = playlist.getGenerateLength() * 60 * 60;
    boolean hasMoreTitles = true;
    int iterationLength;
    int iterationMaxLength = playlist.isGenerateMinimizeArtistRepeats() ? Integer.MAX_VALUE : 60 * 60;
    int maxTotalSegmentsLength = maxLength;
    if (ctx.getAdvices().size() > 0) {
      maxTotalSegmentsLength += 60 * 30 * numSegments;
    } else {
      maxTotalSegmentsLength += 60 * 5 * numSegments;
    }
    while (hasMoreTitles && totalSegmentsLength < maxTotalSegmentsLength) {
      hasMoreTitles = false;
      Collections.sort(artists);

      // System.out.println("--- iteration ----");
      // for(int i = 0; i < artists.size() && i < 10; i++) {
      // System.out.println(artists.get(i).getName() + " " +
      // artists.get(i).getEffectiveScore());
      // for(int j = 0; j < artists.get(i).getTitles().size() && j < 5; j++) {
      // System.out.println(" - "
      // +artists.get(i).getTitles().get(j).getTitle().getTitle() + " " +
      // artists.get(i).getTitles().get(j).getEffectiveScore());
      // }
      // }

      iterationLength = 0;
      for (int i = 0; i < artists.size() && iterationLength < iterationMaxLength; i++) {
        Artist artist = artists.get(i);
        if (artist.hasMoreTitles()) {
          hasMoreTitles = true;
          BasicTrack title = artist.getNextTitle();
          if (totalSegmentsLength < maxTotalSegmentsLength) {
            artist.acceptCurrentTitle();
          }
          totalSegmentsLength += title.getLength();
          iterationLength += title.getLength();
        }
      }
      log.debug(totalSegmentsLength + " of " + maxTotalSegmentsLength + " / " + hasMoreTitles);
    }

    // reduce artists to selected titles
    for (Artist artist : artists) {
      artist.shrink();
    }

    // assign titles of artists to segments
    totalSegmentsLength = 0;
    int artistsWithTitles = artists.size();
    final Map<Integer, Integer> titleScores = new HashMap<Integer, Integer>();
    for (int t = 0; t < maxArtistTitles && totalSegmentsLength < maxTotalSegmentsLength && artistsWithTitles > 0; t++) {
      // System.out.println("-- iteration " + t + " / " +
      // TimeFormat.format(totalSegmentsLength, true));
      log.debug("-- iteration " + t);
      int usedArtists = 0;
      iterationLength = 0;
      Collections.sort(artists);
      for (Artist artist : artists) {
        if (artist.hasMoreTitles()) {
          usedArtists++;
          if (log.isTraceEnabled()) {
            log.trace(artist);
          }

          int currentSegment;
          if (artist.segments == null) {
            int numArtistTracks = Math.min(maxArtistTitles, artist.getTracks().size());
            int artistSegments = numSegments / numArtistTracks;

            // find least filled segment that can act as first segment for this
            // artist
            int minLength = segments[0].getLength();
            currentSegment = 0;
            for (int i = 1; i < numSegments; i++) {
              if (segments[i].getLength() < minLength) {
                currentSegment = i;
                minLength = segments[i].getLength();
              } else if (segments[i].getLength() == minLength && random.nextBoolean()) {
                currentSegment = i;
              }
            }
            artist.segments = new ArrayList<Integer>();
            int next = (currentSegment + artistSegments) % numSegments;
            for (int i = 1; i < numArtistTracks; i++) {
              artist.segments.add(next);
              next = (next + artistSegments) % numSegments;
            }
          } else {
            currentSegment = artist.segments.remove(artist.segments.size() / 2);
          }
          artist.currentSegment = currentSegment;

          // int artistScore = artist.getEffectiveScore();
          TrackInstance instance = artist.getTracks().get(artist.trackIdx);
          titleScores.put(instance.getTrack().getId(), instance.getEffectiveScore());
          BasicTrack title = artist.getNextTitle();
          segments[currentSegment].add(title);
          log.debug((totalSegmentsLength / 60) + ": add " + title + " / " + getTrackScorer(title) + " to segment " + currentSegment);
          artist.setCurrentSegment(currentSegment);
          totalSegmentsLength += title.getLength();
          iterationLength += title.getLength();

          if (totalSegmentsLength > maxTotalSegmentsLength) {
            break;
          }
        }
        artistsWithTitles = usedArtists;
      }
    }

    // randomize best tracks in segments
    int segmentTargetLength = maxLength / segments.length;
    for (Segment seg : segments) {
      Collections.sort(seg.getTracks(), new Comparator<BasicTrack>() {
        @Override
        public int compare(BasicTrack o1, BasicTrack o2) {
          return titleScores.get(o1.getId()).compareTo(titleScores.get(o2.getId()));
        }
      });
      seg.randomizeFirst(ctx, segmentTargetLength);
    }

    // jingles
    boolean distributeJingles = ctx.getJingles().size() > 0 && this.jingleInterval > 0;
    LinkedList<Integer> jinglePositions = new LinkedList<Integer>();
    int jingleInterval = this.jingleInterval * 60;
    if (ctx.getJingles().size() > 0) {
      if (this.jingleInterval == 0) {
        jingleInterval = maxLength / ctx.getJingles().size();
        log.info("jingle interval: " + TimeFormat.format(jingleInterval, false) + ", " + ctx.getJingles().size() + " jingles");
        distributeJingles = true;

        // use pre-computed positions
        int pos = ctx.getFirstJingle() == null ? random.nextInt(jingleInterval * 2 / 3) : jingleInterval;
        while (pos < maxLength) {
          jinglePositions.add(pos);
          pos += jingleInterval;
        }

      }

      if (ctx.getFirstJingle() != null) {
        ctx.getJingles().remove(ctx.getFirstJingle());
      }

      ctx.setJingles(randomize(ctx.getJingles()));

    }

    // build new list
    List<BasicTrack> newTrackList = new ArrayList<BasicTrack>();
    {
      int targetLength = maxLength;
      int length = 0;

      int jingleIdx = 0;
      int numJingles = 0;

      int nextJingleAfter = this.jingleInterval > 0 ? random.nextInt(jingleInterval) : maxLength;

      if (ctx.getFirstJingle() != null) {
        newTrackList.add(ctx.getFirstJingle());
        length += ctx.getFirstJingle().getLength();
      }

      int sIdx = 0, segCnt = 1;
      HashSet<Segment> emptySegments = new HashSet<Segment>();

      int posCnt = 0;
      while (length < targetLength && emptySegments.size() < segments.length) {
        Segment seg = segments[sIdx];
        
        boolean incPosCnt = false;

        if (ctx.getProtectedTrackAt(posCnt) != null) {
          newTrackList.add(ctx.getProtectedTrackAt(posCnt));
          length += ctx.getProtectedTrackAt(posCnt).getLength();
          incPosCnt = true;
        } else if (seg.getTracks().size() > 0) {

          // select a title from the current segment
          BasicTrack title = seg.getTracks().get(0);

          // check advices
          if (!ctx.checkAdvices(newTrackList, title)) {
            log.info("advice violoation - searching replacement for " + title);
            boolean accepted = false;
            for (int t = 1; t < seg.getTracks().size() && t < 20 && !accepted; t++) {
              BasicTrack candidate = seg.getTracks().get(t);
              if (ctx.checkAdvices(newTrackList, candidate)) {
                log.info("accepted " + candidate);
                accepted = true;
                title = candidate;
              }
            }
            if (!accepted) {
              log.warn("advice violation - no replacement found for " + title);
            }
          }

          seg.remove(title);

          // add selected title to playlist
          newTrackList.add(title);
          length += title.getLength();
          this.register(title);
          if(title.getType() == BasicTrack.TYPE_MUSIC) {
            incPosCnt = true;
          }

          boolean addJingle = false;
          if (!this.protectAllJingles && distributeJingles && length < targetLength) {
            if (this.jingleInterval == 0) {
              if (!jinglePositions.isEmpty() && jinglePositions.getFirst().intValue() <= length) {
                addJingle = true;
                while (!jinglePositions.isEmpty() && jinglePositions.getFirst().intValue() <= length) {
                  jinglePositions.removeFirst();
                }
              }
            } else {
              if (length >= nextJingleAfter) {
                addJingle = true;
                nextJingleAfter = length + jingleInterval;
              }
            }
          }

          if (addJingle) {
            // assign next jingle
            BasicTrack jingle = ctx.getJingles().get(jingleIdx);
            newTrackList.add(jingle);
            numJingles++;
            log.info("jingle " + numJingles + " (" + jingle.getTitle() + ") at " + TimeFormat.format(length, true));
            length += jingle.getLength();

            // update jingle index
            jingleIdx = (jingleIdx + 1) % ctx.getJingles().size();

          }
        } else {
          emptySegments.add(seg);
        }

        if (seg.getTracks().size() == 0 || length >= segmentTargetLength * segCnt) {
          // move on to next segment
          sIdx = (sIdx + 1) % segments.length;
          // System.out.println("Segement " + sIdx);
          segCnt++;
        }
        
        if(incPosCnt) {
          posCnt++;
        }

      }
    }

    if (playlistEnhancer != null && !this.protectAllJingles) {
      log.info("apply track rules");
      newTrackList = playlistEnhancer.process(playlist, newTrackList, this.protectFirstJingle, null);
    }

    if (append) {
      for (BasicTrack t : newTrackList) {
        playlist.addTrack(t);
      }
    } else {
      playlist.setTracks(newTrackList);
    }
  }

  private void applyPushTag(GeneratorCtx ctx, Playlist playlist, Set<BasicTrack> titles, String tag, int pushFactor, float maxFraction) {
    try {
      log.info("pushing " + tag + ": " + pushFactor);
      int[] titleIds = this.tagManager.getTrackIds(tag);
      if (titleIds != null) {
        for (int titleId : titleIds) {
          BasicTrack title = this.trackRegistry.getTrack(titleId);
          if (title != null && titles.contains(title)) {
            if (pushFactor < -3 && title.getType() != BasicTrack.TYPE_JINGLE) {
              titles.remove(title);
            } else {
              log.trace("push " + title + " " + pushFactor);
              this.getTrackScorer(title).setPushFactor(pushFactor);
            }
          }
        }
        if (maxFraction < 1) {
          ctx.getAdvices().add(new TagLimitAdvice(titleIds, (int) (playlist.getGenerateLength() * 60 * 60 * maxFraction), maxFraction));
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }

  }

  protected Artist getArtist(String normalizedName) {
    Artist artist = this.artists.get(normalizedName);
    if (artist == null) {
      artist = new Artist(normalizedName);
      this.artists.put(normalizedName, artist);
    }
    return artist;
  }

  /**
   * @return the artistNormalizer
   */
  public ArtistNormalizer getArtistNormalizer() {
    return artistNormalizer;
  }

  /**
   * @return the jingleInterval
   */
  public int getJingleInterval() {
    return jingleInterval;
  }

  /**
   * Gets the minimum value for random values that are used for title selection.
   * 
   * @return the minRandomValue
   */
  public int getMinRandomValue() {
    return minRandomValue;
  }

  /**
   * @return the time
   */
  public long getTime() {
    return time;
  }

  protected TrackScorer getTrackScorer(BasicTrack title) {
    TrackScorer occ = this.trackScores.get(title.getId());
    if (occ == null) {
      occ = new TrackScorer();
      this.trackScores.put(title.getId(), occ);
    }
    return occ;
  }

  /**
   * @return the artistPenaltyEnabled
   */
  public boolean isArtistPenaltyEnabled() {
    return artistPenaltyEnabled;
  }

  /**
   * @return the protectFirstJingle
   */
  public boolean isProtectFirstJingle() {
    return protectFirstJingle;
  }

  /**
   * @return the titlePenaltyEnabled
   */
  public boolean isTrackPenaltyEnabled() {
    return trackPenaltyEnabled;
  }

  /**
   * Assigns new random value to the existing scores. This needs to be called
   * prior to a generation of a new playlist to avoid that the artist / title
   * ordering is the same as in a previous playlist
   */
  protected void prepareNextPlaylist() {
    for (Artist artist : this.artists.values()) {
      artist.reset();
    }
    for (TrackScorer score : trackScores.values()) {
      score.reset();
    }
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
    if (available.size() <= 1) {
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

  protected int randomValue() {
    return this.minRandomValue + this.random.nextInt(500);
  }

  public void register(BasicTrack title) {
    String artistName = this.artistNormalizer.normalizeArtist(title.getArtist());
    Artist artist = this.getArtist(artistName);
    artist.incPlays();
    artist.setLastPlay(this.time);

    TrackScorer titleOcc = this.trackScores.get(title.getId());
    if (titleOcc == null) {
      titleOcc = new TrackScorer(this.time);
      this.trackScores.put(title.getId(), titleOcc);
    } else {
      titleOcc.incPlays();
      titleOcc.setLastPlay(this.time);
    }

    this.time += (title.getLength() * 1000);
  }

  /**
   * @param artistNormalizer the artistNormalizer to set
   */
  public void setArtistNormalizer(ArtistNormalizer artistNormalizer) {
    this.artistNormalizer = artistNormalizer;
  }

  /**
   * Configures whether or not to put a penalty on artists that have been used in
   * previous playlists that were generated with this instance of
   * PlaylistGenerator.
   * 
   * @param artistPenaltyEnabled the artistPenaltyEnabled to set
   */
  public void setArtistPenaltyEnabled(boolean artistPenaltyEnabled) {
    this.artistPenaltyEnabled = artistPenaltyEnabled;
  }

  /**
   * @param jingleInterval the jingleInterval to set
   */
  public void setJingleInterval(int jingleInterval) {
    this.jingleInterval = jingleInterval;
  }

  /**
   * Sets the minimum value for random values that are used for title selection.
   * <p>
   * Values between 0 and the minimum value can only be reached after applying
   * push tags. This means the higher the minimum value is set the more values are
   * only available for pushed titles. Or in other words: The higher the value is
   * the more likely it is that pushed titles are choosen.
   * <p>
   * The default value is 100.
   * 
   * @param minRandomValue minimum value
   */
  public void setMinRandomValue(int minRandomValue) {
    this.minRandomValue = minRandomValue;
  }

  /**
   * @param protectFirstJingle the protectFirstJingle to set
   */
  public void setProtectFirstJingle(boolean protectFirstJingle) {
    this.protectFirstJingle = protectFirstJingle;
  }

  /**
   * @param time the time to set
   */
  public void setTime(long time) {
    this.time = time;
  }

  /**
   * Gets the time period for which titles get a penalty once they have been used
   * 
   * @return period in minutes
   */
  public int getTrackPenaltyPeriod() {
    return trackPenaltyPeriod;
  }

  /**
   * Sets the time period for which titles get a penalty once they have been used.
   * The penalty is getting lower the longer the last play is in the past.
   * <p>
   * If set to 0 the full {@link #getTrackPenaltyMax()} value is always added.
   * 
   * @param titlePenaltyPeriod period in minutes
   */
  public void setTrackPenaltyPeriod(int titlePenaltyPeriod) {
    this.trackPenaltyPeriod = titlePenaltyPeriod;
  }

  /**
   * Gets the maximum penalty value a title gets after it has been played
   * 
   * @return
   */
  public int getTrackPenaltyMax() {
    return trackPenaltyMax;
  }

  /**
   * Sets the maximum penalty value a title can get after it has been played
   * 
   * @param titlePenaltyMax
   */
  public void setTrackPenaltyMax(int titlePenaltyMax) {
    this.trackPenaltyMax = titlePenaltyMax;
  }

  /**
   * Configures whether or not to put a penalty on titles that have been used in
   * previous playlists that were generated with this instance of
   * PlaylistGenerator.
   * 
   * @param titlePenaltyEnabled the titlePenaltyEnabled to set
   */
  public void setTrackPenaltyEnabled(boolean titlePenaltyEnabled) {
    this.trackPenaltyEnabled = titlePenaltyEnabled;
  }

  protected class Artist implements Comparable<Artist> {
    private String name;
    private int effectiveScore = -1;
    private List<TrackInstance> tracks = new ArrayList<TrackInstance>();
    private int trackIdx = 0;
    private int numAccepted = 0;
    private int currentSegment = -1;
    private int plays = 0;
    private long lastPlay = 0;

    List<Integer> segments;

    Artist(String name) {
      super();
      this.name = name;
    }

    void acceptCurrentTitle() {
      this.numAccepted++;
    }

    void add(BasicTrack title) {
      tracks.add(new TrackInstance(title));
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Artist o) {
      return Integer.valueOf(this.getEffectiveScore()).compareTo(o.getEffectiveScore());
    }

    /**
     * @return the startSegment
     */
    protected int getCurrentSegment() {
      return currentSegment;
    }

    int getEffectiveScore() {
      if (this.effectiveScore == -1) {
        if (this.trackIdx < this.tracks.size()) {
          TrackInstance instance = this.tracks.get(this.trackIdx);

          // compute an artist score based on the score of the best title,
          // but only use it if higher than the effective title score
          // => has only an effect if artist penalty is higher than title
          // penalty
          int score = instance.getWeightedScore();
          if (this.plays > 0 && artistPenaltyEnabled) {
            long diff = (time - lastPlay) / 60000;
            if (diff < artistPenaltyPeriod) {
              float penalty = 1 - ((float) diff / artistPenaltyPeriod);
              score += (int) (300 * penalty);
            }
          }
          this.effectiveScore = Math.max(score, instance.getEffectiveScore());
        } else {
          this.effectiveScore = Integer.MAX_VALUE;
        }
      }
      return this.effectiveScore;
    }

    public BasicTrack getNextTitle() {
      TrackInstance instance = this.moveToNextTitle();
      return instance != null ? instance.getTrack() : null;
    }

    /**
     * @return the lastPlay
     */
    long getLastPlay() {
      return lastPlay;
    }

    /**
     * @return the name
     */
    String getName() {
      return name;
    }

    private TrackInstance moveToNextTitle() {
      TrackInstance title = this.tracks.get(this.trackIdx);
      this.trackIdx++;
      this.effectiveScore = -1;
      return title;
    }

    // TitleScorer getNextTitleScore() {
    // int idx = this.titleIdx < titles.size() ? this.titleIdx :
    // this.titles.size() - 1;
    // return getTitleScorer(this.titles.get(idx));
    // }

    /**
     * @return the plays
     */
    int getPlays() {
      return plays;
    }

    /**
     * @return the titles
     */
    public List<TrackInstance> getTracks() {
      return tracks;
    }

    boolean hasMoreTitles() {
      return this.trackIdx < this.tracks.size();
    }

    /**
     * @param plays the plays to set
     */
    void incPlays() {
      this.plays++;
    }

    void prepareTitleList(int maxArtistTitles) {
      this.sortTitles();
      if (this.tracks.size() > maxArtistTitles) {
        this.tracks = this.tracks.subList(0, maxArtistTitles);
      }
    }

    void reset() {
      this.tracks.clear();
      this.effectiveScore = -1;
      this.currentSegment = -1;
      this.segments = null;
      this.trackIdx = 0;
      this.numAccepted = 0;
    }

    /**
     * @param startSegment the startSegment to set
     */
    protected void setCurrentSegment(int startSegment) {
      this.currentSegment = startSegment;
    }

    /**
     * @param lastPlay the lastPlay to set
     */
    void setLastPlay(long lastPlay) {
      this.lastPlay = lastPlay;
    }

    void shrink() {
      this.trackIdx = 0;
      if (this.tracks.size() > numAccepted) {
        if (this.numAccepted == 0) {
          this.tracks.clear();
        } else {
          this.tracks = this.tracks.subList(0, numAccepted);
        }
      }
      this.effectiveScore = -1;
    }

    void sortTitles() {
      if (this.tracks.size() > 1) {
        Collections.sort(this.tracks);
        if (log.isTraceEnabled()) {
          log.info(this.name + ": ");
          for (TrackInstance inst : tracks) {
            log.info("- " + inst.getTrack().getTitle() + " - " + inst.getEffectiveScore());
          }
        }
      }
    }

    public String toString() {
      return this.name + " - " + this.getEffectiveScore() + " / " + this.plays + " / " + this.tracks.get(0).getTrack();
    }

    /**
     * @param titles the titles to set
     */
    public void setTracks(List<TrackInstance> titles) {
      this.tracks = titles;
    }

  }

  private static class GeneratorCtx {
    private Map<String, Artist> titleMap = new HashMap<String, Artist>();
    private List<BasicTrack> jingles = new ArrayList<BasicTrack>();
    private Map<Integer, BasicTrack> protectedTracks = new HashMap<Integer, BasicTrack>();
    private BasicTrack firstJingle;

    private List<Advice> advices = new ArrayList<Advice>();

    public void addProtectedTrack(int pos, BasicTrack track) {
        this.protectedTracks.put(pos, track);
    }

    public BasicTrack getProtectedTrackAt(int pos) {
      return this.protectedTracks.get(pos);
    }

    /**
     * @return the firstJingle
     */
    protected BasicTrack getFirstJingle() {
      return firstJingle;
    }

    /**
     * @return the jingles
     */
    public List<BasicTrack> getJingles() {
      return jingles;
    }
    
    void setJingles(List<BasicTrack> jingles) {
      this.jingles = jingles;
    }


    /**
     * @return the titleMap
     */
    public Map<String, Artist> getTitleMap() {
      return titleMap;
    }

    /**
     * @param firstJingle the firstJingle to set
     */
    protected void setFirstJingle(BasicTrack firstJingle) {
      this.firstJingle = firstJingle;
    }

    public List<Advice> getAdvices() {
      return advices;
    }

    public boolean checkAdvices(List<BasicTrack> titles, BasicTrack candidate) {
      for (Advice advice : this.advices) {
        if (!advice.accept(titles, candidate)) {
          return false;
        }
      }
      return true;
    }
  }

  /**
   * A Segment is a part of the playlist as it is generated.
   */
  private class Segment {
    private List<BasicTrack> tracks = new ArrayList<BasicTrack>();
    private int length;

    public Segment() {
      super();
    }

    void add(BasicTrack title) {
      this.tracks.add(title);
      this.length += title.getLength();
    }

    public int getLength() {
      return length;
    }

    public List<BasicTrack> getTracks() {
      return tracks;
    }

    public void remove(BasicTrack title) {
      if (this.tracks.remove(title)) {
        this.length -= title.getLength();
      }
    }

    private boolean checkSegementAdvices(List<Advice> advices, List<BasicTrack> tracks, BasicTrack candidate) {
      for (Advice advice : advices) {
        if (!advice.accept(tracks, candidate)) {
          return false;
        }
      }

      return true;
    }

    public void randomizeFirst(GeneratorCtx ctx, int seconds) {
      int len = 0;
      int idx = 0;
      List<BasicTrack> first = new ArrayList<BasicTrack>();
      List<BasicTrack> last = new ArrayList<BasicTrack>();
      List<BasicTrack> adviceViolations = new ArrayList<BasicTrack>();

      List<Advice> segmentAdvices = new ArrayList<Advice>();
      for (Advice advice : ctx.getAdvices()) {
        if (advice instanceof TagLimitAdvice) {
          segmentAdvices.add(new TagLimitAdvice((TagLimitAdvice) advice, seconds + (int) (seconds * 0.1)));
        }
      }

      while (idx < this.tracks.size()) {
        if (len < seconds) {
          if (checkSegementAdvices(segmentAdvices, first, tracks.get(idx))) {
            len += this.tracks.get(idx).getLength();
            first.add(tracks.get(idx));
          } else {
            adviceViolations.add(tracks.get(idx));
          }
        } else {
          last.add(tracks.get(idx));
        }
        idx++;
      }

      this.tracks.clear();
      first = randomize(first);
      this.tracks.addAll(first);
      this.tracks.addAll(last);
      this.tracks.addAll(adviceViolations);
    }

  }

  protected class TrackInstance implements Comparable<TrackInstance> {
    private BasicTrack track;
    private int randomValue;
    private int effectiveScore = -1;

    TrackInstance(BasicTrack title) {
      this.track = title;
      this.randomValue = randomValue();
    }

    public int compareTo(TrackInstance o) {
      return Integer.valueOf(this.getEffectiveScore()).compareTo(o.getEffectiveScore());
    }

    int getEffectiveScore() {
      if (this.effectiveScore == -1) {
        this.effectiveScore = getTrackScorer(this.track).getEffectiveScore(this.randomValue);
      }
      return this.effectiveScore;
    }

    int getWeightedScore() {
      return getTrackScorer(track).getEffectiveScore(this.randomValue);
    }

    /**
     * @return the title
     */
    public BasicTrack getTrack() {
      return track;
    }
  }

  private class TrackScorer {
    private int plays;
    private long lastPlay;
    private int minPushFactor = 0;
    private int maxPushFactor = 0;
    private int pushFactor = 0;
    private boolean repeatAllowed = false;

    TrackScorer() {
    }

    TrackScorer(long time) {
      this.plays = 1;
      this.lastPlay = time;
    }

    int getEffectiveScore(int randomValue) {
      int score = this.getWeightedScore(randomValue);
      if (this.plays > 0 && trackPenaltyEnabled) {
        int diff = (int) ((time - lastPlay) / 60000);
        if (trackPenaltyPeriod == 0 || diff < trackPenaltyPeriod) {
          float penalty = trackPenaltyPeriod > 0 ? 1 - ((float) diff / trackPenaltyPeriod) : plays;
          score += (int) (trackPenaltyMax * penalty);
        }
      }
      return score;
    }

    /**
     * @return the pushFactor
     */
    public int getPushFactor() {
      return pushFactor;
    }

    int getWeightedScore(int randomValue) {
      if (this.pushFactor == 0) {
        return randomValue;
      } else if (this.pushFactor > 0) {
        float p = ((float) 4 - this.pushFactor) / 4;
        int wScore = (int) (randomValue * p);
        return wScore;
      } else {
        float p = 1 + (float) (-this.pushFactor) / 4;
        return (int) (randomValue * p);
      }
    }

    void incPlays() {
      this.plays++;
    }

    boolean isPushed() {
      return this.pushFactor != 0;
    }

    /**
     * @param last the last to set
     */
    void setLastPlay(long last) {
      this.lastPlay = last;
    }

    /**
     * @param pushFactor the pushFactor to set
     */
    public void setPushFactor(int pushFactor) {
      this.minPushFactor = Math.min(this.minPushFactor, pushFactor);
      this.maxPushFactor = Math.max(this.maxPushFactor, pushFactor);
      this.pushFactor = minPushFactor + maxPushFactor;
    }

    public void reset() {
      this.minPushFactor = 0;
      this.maxPushFactor = 0;
      this.pushFactor = 0;
      this.repeatAllowed = false;
    }

    /**
     * @return the repeatAllowed
     */
    public boolean isRepeatAllowed() {
      return repeatAllowed;
    }

    /**
     * @param repeatAllowed the repeatAllowed to set
     */
    public void setRepeatAllowed(boolean repeatAllowed) {
      this.repeatAllowed = repeatAllowed;
    }

  }

  public WordDistributionStrategy getWordDistribution() {
    return wordDistribution;
  }

  public void setWordDistribution(WordDistributionStrategy wordDistribution) {
    this.wordDistribution = wordDistribution;
  }

  /**
   * @return the artistPenaltyPeriod
   */
  public int getArtistPenaltyPeriod() {
    return artistPenaltyPeriod;
  }

  /**
   * @param artistPenaltyPeriod the artistPenaltyPeriod to set
   */
  public void setArtistPenaltyPeriod(int artistPenaltyPeriod) {
    this.artistPenaltyPeriod = artistPenaltyPeriod;
  }

  /**
   * @return the maxArtistTitles
   */
  public int getMaxArtistTitles() {
    return maxArtistTitles;
  }

  /**
   * @param maxArtistTitles the maxArtistTitles to set
   */
  public void setMaxArtistTitles(int maxArtistTitles) {
    this.maxArtistTitles = maxArtistTitles;
  }

  /**
   * @return the globalWeightTags
   */
  public List<TagWeight> getGlobalWeightTags() {
    return globalPushTags;
  }

  /**
   * @param globalWeightTags the globalWeightTags to set
   */
  public void setGlobalWeightTags(List<TagWeight> globalWeightTags) {
    this.globalPushTags = globalWeightTags;
  }

  public void addGlobalWeightTag(TagWeight weight) {
    this.globalPushTags.add(weight);
  }

  /**
   * @return the artistTrackPreselector
   */
  public ArtistTrackPreselector getArtistTrackPreselector() {
    return artistTrackPreselector;
  }

  /**
   * @param artistTrackPreselector the artistTrackPreselector to set
   */
  public void setArtistTrackPreselector(ArtistTrackPreselector artistTrackPreselector) {
    this.artistTrackPreselector = artistTrackPreselector;
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

  public void setPlaylistEnhancer(PlaylistEnhancer playlistEnhancer) {
    this.playlistEnhancer = playlistEnhancer;
  }

}
