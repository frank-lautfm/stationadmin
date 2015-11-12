/**
 * 
 */
package de.emjoy.stationadmin.test.playlist;

import java.io.IOException;
import java.util.HashSet;
import java.util.Random;
import java.util.Set;

import org.apache.log4j.BasicConfigurator;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.shuffle.PlaylistGenerator;
import de.stationadmin.base.playlist.shuffle.TagSequenceAdvice;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.Title;
import de.stationadmin.base.track.TrackRegistry;
import de.stationadmin.base.util.TimeFormat;

/**
 * @author korf
 * 
 */
public class TestPlaylistGenerator {
  private static Logger log;

  private static final long INITIAL_SEED = 1357220147216l;

  static final String TAG_SHOW_A = "Show A";
  static final String TAG_SHOW_B = "Show B";
  static final String TAG_SHOW_C = "Show C";
  static final String TAG_HOT = "Hot";
  static final String TAG_JINGLE = "Jingle";
  static final String TAG_STYLE_A = "Style A";
  static final String TAG_STYLE_B = "Style B";
  static final int NUM_JINGLES = 10;
  static final int MAX_TITLE_LENGTH = 360;

  private SomeTitleTagChecker titleTagManager;
  private TrackRegistry titleRegistry;
  private int seedIdx = 0;

  private PlaylistGenerator newGenerator() {
    PlaylistGenerator gen = new PlaylistGenerator(titleTagManager, titleRegistry);
    long seed = INITIAL_SEED + seedIdx * 113;
    log.debug("seed = " + seed);
    gen.setRandomSeed(seed);
    seedIdx++;
    return gen;
  }

  private Playlist newPlaylist(String name) {
    Playlist playlist = new Playlist(this.titleRegistry, PlaylistType.ONLINE);
    playlist.setName(name);
    playlist.setGenerateLength(2);
    playlist.setGenerateMinimizeArtistRepeats(true);
    playlist.setGenerateTitleRepeatLevel(-1);

    return playlist;
  }

  @BeforeClass
  public static void init() {
    ConsoleAppender appender = new ConsoleAppender(new PatternLayout("%r [%t] %p %c %x - %m%n"));
    BasicConfigurator.configure(appender);
    Logger.getRootLogger().setLevel(Level.INFO);
    log = Logger.getLogger(TestPlaylistGenerator.class);
  }

  @Before
  public void setUp() {
    this.titleTagManager = new SomeTitleTagChecker();
    this.titleRegistry = new TrackRegistry();

    int titleId = 1;

    Random r = new Random(8345345590l);
    for (int a = 0; a < 100; a++) {
      String artist = "A" + a;
      for (int t = 0; t < (a % 10) + 1; t++) {
        RegisteredTrack title = new RegisteredTrack();
        title.setArtist(artist);
        title.setId(titleId);
        title.setTitle("T" + title.getId());
        title.setLength(r.nextInt(240) + 120);
        this.titleRegistry.add(title);

        if (titleId % 2 == 0) {
          this.titleTagManager.register(TAG_SHOW_A, titleId);
        }
        if (titleId % 2 == 1) {
          this.titleTagManager.register(TAG_SHOW_B, titleId);
        }
        if (titleId % 3 == 1) {
          this.titleTagManager.register(TAG_SHOW_C, titleId);
        }
        if (titleId % 13 == 1) {
          this.titleTagManager.register(TAG_HOT, titleId);
        }

        if (titleId % 5 == 1) {
          this.titleTagManager.register(TAG_STYLE_A, titleId);
        }
        if (titleId % 5 == 2) {
          this.titleTagManager.register(TAG_STYLE_B, titleId);
        }

        titleId++;
      }
    }

    for (int j = 0; j < NUM_JINGLES; j++) {
      RegisteredTrack title = new RegisteredTrack();
      title.setArtist("Jingle");
      title.setId(titleId);
      title.setTitle("J" + title.getId());
      title.setType(Title.TYPE_JINGLE);
      title.setLength(15);
      this.titleRegistry.add(title);
      this.titleTagManager.register(TAG_JINGLE, titleId);
      titleId++;
    }

  }

  private static void assertLength(Playlist playlist) {
    int target = playlist.getGenerateLength() * 60 * 60;
    log.info(playlist.getName() + " length: " + TimeFormat.format(playlist.getLength(), true));
    Assert.assertTrue("Length of " + playlist.getName(), playlist.getLength() >= target && playlist.getLength() <= target + 60 * 10);
  }

  private static void assertNoTitleRepeat(Playlist playlist) {
    Set<Integer> titleIds = new HashSet<Integer>();
    for (Playlist.Entry entry : playlist.getEntries()) {
      Assert.assertFalse("duplicate title", titleIds.contains(entry.getTrackId()));
      titleIds.add(entry.getTrackId());
    }
    log.info(playlist.getName() + " contains " + titleIds.size() + " titles");
  }

  private static void assertNoArtistRepeat(Playlist playlist) {
    Set<String> artists = new HashSet<String>();
    for (Playlist.Entry entry : playlist.getEntries()) {
      Assert.assertFalse("duplicate artist", artists.contains(entry.getTrack().getArtist()));
      artists.add(entry.getTrack().getArtist());
    }
    log.info(playlist.getName() + " contains " + artists.size() + " artists");
  }

  private static void assertJingleDistance(Playlist playlist, int interval) {
    int lastJingle = -1;
    for (Playlist.Entry entry : playlist.getEntries()) {
      if (entry.getTrack().getType() == Title.TYPE_JINGLE) {
        if (lastJingle > -1) {
          int diff = entry.getStart() - lastJingle;
          Assert.assertTrue("jingles distance too small: " + diff, diff >= interval * 60);
          Assert.assertTrue("jingles distance too big: " + diff, diff <= interval * 60 + MAX_TITLE_LENGTH);
        }
        lastJingle = entry.getStart();
      }
    }
  }

  @Test
  public void testSimple() throws Exception {

    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A);

    for (int run = 0; run < 3; run++) {
      log.info("run " + (run + 1));
      PlaylistGenerator gen = newGenerator();
      gen.generate(playlist);

      assertLength(playlist);
      assertNoTitleRepeat(playlist);
      assertNoArtistRepeat(playlist);
    }

  }

  @Test
  public void testPushTags() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A);

    float[] fractions = new float[4];

    for (int w = 0; w < 4; w++) {
      playlist.setGeneratePushTag(TAG_HOT + ":" + w);
      int titlesTotal = 0;
      int titlesHot = 0;
      for (int run = 0; run < 3; run++) {
        log.info("run " + (run + 1));
        PlaylistGenerator gen = newGenerator();
        gen.generate(playlist);

        titlesTotal += playlist.getEntries().size();
        titlesHot += countTaggedTitles(playlist, TAG_HOT);
      }

      float fraction = (float) titlesHot / titlesTotal;
      log.info("weight " + w + ": " + titlesHot + " of " + titlesTotal + " tagged as hot (" + fraction + ")");
      fractions[w] = fraction;
    }

    for (int i = 1; i < fractions.length; i++) {
      log.info(fractions[i - 1] + " < " + fractions[i]);
      Assert.assertTrue("expected higher fraction for weight " + i, fractions[i - 1] < fractions[i]);
    }

  }

  @Test
  public void testPushTagsWithLimit() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A);
    playlist.setGeneratePushTag(TAG_HOT + ":2:0.25");

    for (int run = 0; run < 3; run++) {
      int lenHot = 0;
      log.info("run " + (run + 1));
      PlaylistGenerator gen = newGenerator();
      gen.generate(playlist);

      for (Playlist.Entry entry : playlist.getEntries()) {
        if (this.titleTagManager.isTagged(TAG_HOT, entry.getTrackId())) {
          lenHot += entry.getTrack().getLength();
        }
      }

      log.info(TimeFormat.format(lenHot, true) + " of " + playlist.getGenerateLength() + ":00:00 tagged as hot");
      Assert.assertTrue("too many pushed titles: " + TimeFormat.format(lenHot, true) + " of " + playlist.getGenerateLength() + ":00:00",
          lenHot <= playlist.getGenerateLength() * 60 * 60 * 0.25f);

    }

  }

  private int countTaggedTitles(Playlist playlist, String tag) throws IOException {
    int cnt = 0;
    for (Playlist.Entry entry : playlist.getEntries()) {
      if (this.titleTagManager.isTagged(tag, entry.getTrackId())) {
        cnt++;
      }
    }
    return cnt;
  }

  @Test
  public void testJinglesEqualDistribution() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A + ";" + TAG_JINGLE);

    PlaylistGenerator gen = newGenerator();
    gen.setJingleInterval(0);
    gen.setProtectFirstJingle(false);

    gen.generate(playlist);

    assertLength(playlist);
    Assert.assertEquals("number of jingles", NUM_JINGLES, this.countTaggedTitles(playlist, TAG_JINGLE));

  }

  @Test
  public void testJinglesEqualDistributionProtectFirstJingle() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A + ";" + TAG_JINGLE);

    Title someJingle = this.titleRegistry.getTrack(this.titleTagManager.getTrackIds(TAG_JINGLE)[0]);
    log.info("protect " + someJingle);
    playlist.addTrack(someJingle);

    PlaylistGenerator gen = newGenerator();
    gen.setJingleInterval(0);
    gen.setProtectFirstJingle(true);

    gen.generate(playlist);

    assertLength(playlist);
    Assert.assertEquals("first jingle", playlist.getEntries().get(0).getTrackId(), someJingle.getId());
    Assert.assertEquals("number of jingles", NUM_JINGLES, this.countTaggedTitles(playlist, TAG_JINGLE));

  }

  /**
   * Tests playlist with jingles, static interval
   * 
   * @throws Exception
   */
  @Test
  public void testJinglesFixedDistribution() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A + ";" + TAG_JINGLE);

    for (int interval : new int[] { 5, 10, 15, 30 }) {
      PlaylistGenerator gen = newGenerator();
      gen.setJingleInterval(interval);
      gen.setProtectFirstJingle(false);

      gen.generate(playlist);

      assertLength(playlist);
      assertJingleDistance(playlist, gen.getJingleInterval());
    }

  }

  @Test
  public void testTagSequenceAdvice() throws Exception {
    Playlist playlist = newPlaylist("Test");
    playlist.setGenerateTags(TAG_SHOW_A);

    TagSequenceAdvice advice1 = new TagSequenceAdvice(this.titleTagManager, new String[] { TAG_STYLE_A }, false, TAG_STYLE_B);
    TagSequenceAdvice advice2 = new TagSequenceAdvice(this.titleTagManager, new String[] { TAG_STYLE_B }, false, TAG_STYLE_A);
    playlist.setGenerateAdvices(new String[] { advice1.toJSON(), advice2.toJSON() });

    PlaylistGenerator gen = newGenerator();
    gen.generate(playlist);
    assertLength(playlist);

    int lastTitleId = -1;
    for (Playlist.Entry entry : playlist.getEntries()) {
      if (lastTitleId > -1 && this.titleTagManager.isTagged(TAG_STYLE_A, entry.getTrackId())) {
        Assert.assertFalse(TimeFormat.format(entry.getStart(), true) + ": B => !A", this.titleTagManager.isTagged(TAG_STYLE_B, lastTitleId));
      }
      if (lastTitleId > -1 && this.titleTagManager.isTagged(TAG_STYLE_B, entry.getTrackId())) {
        Assert.assertFalse(TimeFormat.format(entry.getStart(), true) + ": A => !B", this.titleTagManager.isTagged(TAG_STYLE_A, lastTitleId));
      }
      lastTitleId = entry.getTrackId();
    }

  }

}
