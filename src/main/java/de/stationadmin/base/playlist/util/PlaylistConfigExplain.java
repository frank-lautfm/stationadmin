package de.stationadmin.base.playlist.util;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;
import de.stationadmin.base.playlist.PlaylistService;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.TrackRegistry;

public class PlaylistConfigExplain {
  private TrackRegistry trackRegistry;
  private PlaylistService playlistService;

  public PlaylistConfigExplain(PlaylistService playlistService, TrackRegistry trackRegistry) {
    this.playlistService = playlistService;
    this.trackRegistry = trackRegistry;
  }

  public List<ConfigItem> explain(Playlist playlist) {
    ArrayList<ConfigItem> items = new ArrayList<>();

    PlaylistInfo info = analyzePlaylist(playlist);

    if (playlist.isShuffle()) {
      if (playlist.getShuffleType().equals(PlaylistService.SHUFFLE_CLASSIC)) {
        explainShuffleSimple(playlist, info, items);
      } else if (playlist.getShuffleType().equals(PlaylistService.SHUFFLE_BUCKET)) {
        explainShuffleBucket(playlist, info, items);
      } else if (playlist.getShuffleType().equals(PlaylistService.SHUFFLE_STATIONADMIN)) {
        explainShuffleStationAdmin(playlist, info, items);
      } else if (playlist.getShuffleType().equals("resume")) {
        explainSimple(playlist, info, items, true);
      } else if (playlist.getShuffleType().equals(PlaylistService.SHUFFLE_BLOCKSELECT)) {
        explainBlockSelect(playlist, info, items);
      } else {
        items.add(new ConfigItem("shuffled.server"));
        items.add(new ConfigItem("shuffled.server.unknown"));
      }
    } else if (playlist.isGenerate()) {
      explainGenerate(playlist, info, items);
    } else if (playlist.isLocalShuffleAllowed()) {
      explainShuffleLocal(playlist, info, items);
    } else {
      explainSimple(playlist, info, items, false);
    }

    return items;
  }

  private PlaylistInfo analyzePlaylist(Playlist playlist) {
    PlaylistInfo info = new PlaylistInfo();
    int numJingles = 0;
    int numWords = 0;
    int start = 0;

    if (playlist.getEntries().size() == 0) {
      return info;
    }
    if (playlist.getEntry(0).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
      info.setStartsWithJingle(true);
    }

    int size = playlist.getEntries().size();
    for (int i = start; i < size; i++) {
      Entry entry = playlist.getEntry(i);
      info.addTrack(entry.getTrackId());
      if (entry.getTrack() != null && entry.getTrack().getArtist() != null && entry.getTrack().getTitle() != null) {
        if (!info.isAdTriggerIncluded() && (entry.getTrack().getArtist().contains("START_AD_BREAK") || entry.getTrack().getTitle().contains("START_AD_BREAK"))) {
          info.setAdTriggerIncluded(true);
        } else if (entry.getTrackId() == TrackRegistry.LAUTFM_NEWS_ID) {
          info.setNewsIncluded(true);
        } else if (entry.getTrack() != null && entry.getTrack().getType() == BasicTrack.TYPE_JINGLE) {
          numJingles++;
        } else if (entry.getTrack() != null && entry.getTrack().getType() == BasicTrack.TYPE_WORD) {
          numWords++;
        }
      }
    }
    info.setNumJingles(numJingles);
    info.setNumWord(numWords);

    return info;

  }

  private void explainSimple(Playlist playlist, PlaylistInfo info, List<ConfigItem> items, boolean resume) {
    items.add(new ConfigItem("unshuffled"));
    if (resume) {
      items.add(new ConfigItem("unshuffled.resume"));
    } else {
      items.add(new ConfigItem("unshuffled.restart"));
    }
    if (info.isAdTriggerIncluded()) {
      items.add(new ConfigItem("adtrigger.present"));
    } else {
      items.add(new ConfigItem("adtrigger.auto"));
    }
  }

  private void explainShuffleSimple(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("shuffled.server"));
    items.add(new ConfigItem("adtrigger.auto"));
    if (info.isNewsIncluded()) {
      items.add(new ConfigItem("news.once"));
    }
    int minJingles = 0;
    if (info.isStartsWithJingle()) {
      items.add(new ConfigItem("jingle.first"));
      minJingles++;
    }
    if (info.getNumJingles() > minJingles) {
      items.add(new ConfigItem("jingle.dist.equal"));
    }
  }

  private void explainShuffleBucket(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("shuffled.server"));
    items.add(new ConfigItem("pattern"));
    items.add(new ConfigItem("adtrigger.auto"));
    if (info.isNewsIncluded()) {
      items.add(new ConfigItem("news.once"));

      List<Entry> entries = playlist.getEntries();
      int[] firstTypes = new int[2];
      firstTypes[0] = playlist.getEntry(0).getTrack().getType();
      firstTypes[1] = entries.size() > 1 ? playlist.getEntry(1).getTrack().getType() : -1;

      if (firstTypes[0] == BasicTrack.TYPE_JINGLE && firstTypes[1] == BasicTrack.TYPE_NEWS) {
        items.add(new ConfigItem("news.before", entries.get(0).getTrack().toString()));
        info.setStartsWithJingle(false);
      } else if (firstTypes[0] == BasicTrack.TYPE_NEWS && firstTypes[1] == BasicTrack.TYPE_JINGLE) {
        items.add(new ConfigItem("news.after", entries.get(1).getTrack().toString()));
        info.setStartsWithJingle(false);
      }
    }
    int minJingles = 0;
    if (info.isStartsWithJingle()) {
      items.add(new ConfigItem("jingle.first"));
      minJingles++;
    }
    if (info.getNumJingles() > minJingles) {
      items.add(new ConfigItem("jingle.dist.pattern"));
    }
  }

  @SuppressWarnings("unchecked")
  private List<Integer> getIntList(Object value) {
    if (value instanceof List) {
      return (List<Integer>) value;
    }
    List<Integer> list = new ArrayList<>();
    if (value instanceof int[]) {
      int[] values = (int[]) value;
      for (int v : values) {
        list.add(v);
      }
    }
    return list;
  }

  private void explainShuffleStationAdmin(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("shuffled.server"));
    Map<String, Object> opts = playlist.getShuffleOpts();
    boolean tagPattern = opts.containsKey("tagPattern");
    if (tagPattern) {
      items.add(new ConfigItem("pattern"));
    }

    // --- Ad Trigger ---
    if (!info.isAdTriggerIncluded() || !opts.containsKey("adPositions")) {
      items.add(new ConfigItem("adtrigger.auto"));
    } else {
      List<Integer> pos = getIntList(opts.get("adPositions"));
      if (pos.size() == 2) {
        items.add(new ConfigItem("adtrigger.presetAt", Integer.toString(pos.get(0)), Integer.toString(pos.get(1))));
      }
      if (opts.containsKey("adSeparator")) {
        int separatorId = (int) opts.get("adSeparator");
        if (info.contains(separatorId)) {
          RegisteredTrack track = this.trackRegistry.getTrack(separatorId);
          items.add(new ConfigItem("adtrigger.separator", track.getArtist() + " - " + track.getTitle()));
        } else {
          items.add(new ConfigItem("adtrigger.separator.missing"));
        }
      }
    }

    // --- News ---
    List<Entry> entries = playlist.getEntries();
    if (info.isNewsIncluded()) {
      int[] firstTypes = new int[3];
      firstTypes[0] = playlist.getEntry(0).getTrack().getType();
      firstTypes[1] = entries.size() > 1 ? playlist.getEntry(1).getTrack().getType() : -1;
      firstTypes[2] = entries.size() > 2 ? playlist.getEntry(2).getTrack().getType() : -1;

      int newsInterval = opts.containsKey("newsInterval") ? (int) opts.get("newsInterval") : 0;
      if (newsInterval == 60) {
        items.add(new ConfigItem("news.hour1"));
      } else if (newsInterval > 60) {
        items.add(new ConfigItem("news.hourx", Integer.toString(newsInterval / 60)));
      }

      if (firstTypes[0] == BasicTrack.TYPE_JINGLE && firstTypes[1] == BasicTrack.TYPE_NEWS) {
        items.add(new ConfigItem("news.before", entries.get(0).getTrack().toString()));
        if (firstTypes[2] == BasicTrack.TYPE_JINGLE) {
          items.add(new ConfigItem("news.after", entries.get(2).getTrack().toString()));
        }
        info.setStartsWithJingle(false);
      } else if (firstTypes[0] == BasicTrack.TYPE_NEWS && firstTypes[1] == BasicTrack.TYPE_JINGLE) {
        items.add(new ConfigItem("news.after", entries.get(1).getTrack().toString()));
        info.setStartsWithJingle(false);
      }
    }

    // --- Jingles ---
    int minJingles = 0;
    if (info.isStartsWithJingle() && opts.containsKey("protectFirstJingle") && opts.get("protectFirstJingle").equals(1)) {
      items.add(new ConfigItem("jingle.first", entries.get(0).getTrack().toString()));
      minJingles++;
    }
    if (info.getNumJingles() > minJingles) {
      if (tagPattern) {
        items.add(new ConfigItem("jingle.dist.pattern.sa"));
      }
      if (opts.containsKey("preserveAllJingles") && opts.get("preserveAllJingles").equals(1)) {
        items.add(new ConfigItem("jingle.dist.preserve"));
      } else {
        int interval = opts.containsKey("jingleInterval") ? (int) opts.get("jingleInterval") : 0;
        if (interval > 0) {
          items.add(new ConfigItem("jingle.dist.interval", Integer.toString(interval)));
        } else {
          items.add(new ConfigItem("jingle.dist.equal"));
        }
      }
    }

    // --- Moderation ---
    if (info.getNumWord() > 0 && !tagPattern) {
      String wordDist = opts.containsKey("wordDistribution") ? (String) opts.get("wordDistribution") : "random";
      if (wordDist.equals("random")) {
        items.add(new ConfigItem("word.dist.random"));
      } else if (wordDist.equals("preserve")) {
        items.add(new ConfigItem("word.dist.preserve"));
      } else if (wordDist.equals("link_next")) {
        items.add(new ConfigItem("word.dist.link.next"));
      } else if (wordDist.equals("link_previous")) {
        items.add(new ConfigItem("word.dist.link.prev"));
      }
    }
  }

  private void explainBlockSelect(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("unshuffled.blockselect"));
    Map<String, Object> opts = playlist.getShuffleOpts();
    int iterationStepHours = opts.containsKey("iterationStepHours") ? (int) opts.get("iterationStepHours") : 0;
    if (iterationStepHours == 0) {
      items.add(new ConfigItem("unshuffled.blockselect.random"));
    } else {
      items.add(new ConfigItem("unshuffled.blockselect.calculated"));
    }
  }

  private void explainShuffleLocal(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("shuffled.local"));
    items.add(new ConfigItem("shuffled.local.how"));
    if (info.isNewsIncluded()) {
      items.add(new ConfigItem("news.hour1"));
      if (info.isStartsWithJingle()) {
        PlaylistProfile profile = this.playlistService.getProfile(playlist.getProfileId());
        if (profile != null && profile.isProtectFirstJingle()) {
          BasicTrack track = playlist.getEntry(0).getTrack();
          items.add(new ConfigItem("news.after", track.toString()));
          info.setStartsWithJingle(false);
        }
      }
    }

    this.explainProfile(playlist, info, items, false);
  }

  private void explainGenerate(Playlist playlist, PlaylistInfo info, List<ConfigItem> items) {
    items.add(new ConfigItem("generate"));
    items.add(new ConfigItem("generate.how"));

    if (playlist.isGenerateTagsAll()) {
      items.add(new ConfigItem("generate.details.and", Integer.toString(playlist.getGenerateLength()), playlist.getGenerateTags()));
    } else {
      items.add(new ConfigItem("generate.details.or", Integer.toString(playlist.getGenerateLength()), playlist.getGenerateTags()));
    }

    BasicTrack firstJingle = null;
    List<Entry> entries = playlist.getEntries();
    if (entries.size() > 0 && entries.get(0).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
      firstJingle = entries.get(0).getTrack();
    } else if (entries.get(0).getTrack().getType() == BasicTrack.TYPE_NEWS && entries.size() > 1 && entries.get(1).getTrack().getType() == BasicTrack.TYPE_JINGLE) {
      firstJingle = entries.get(1).getTrack();
    }

    // --- News ---
    if (playlist.getGenerateNewsInterval() > 0) {
      if (playlist.getGenerateNewsInterval() == 60) {
        items.add(new ConfigItem("news.hour1"));
      } else {
        items.add(new ConfigItem("news.hourx", Integer.toString(playlist.getGenerateNewsInterval())));
      }

      if (firstJingle != null && playlist.getGenerateFirstJingleAfterNews()) {
        items.add(new ConfigItem("news.after", firstJingle.toString()));
        info.setStartsWithJingle(false);
      }
    }

    this.explainProfile(playlist, info, items, true);
  }

  private void explainProfile(Playlist playlist, PlaylistInfo info, List<ConfigItem> items, boolean generate) {
    PlaylistProfile profile = this.playlistService.getProfile(playlist.getProfileId());
    if (profile != null) {

      // --- Ad-Trigger --
      if (profile.getAdTrigger() != null && profile.getAdTrigger().getPos1() > -1) {
        items.add(new ConfigItem("adtrigger.presetAt", Integer.toString(profile.getAdTrigger().getPos1()), Integer.toString(profile.getAdTrigger().getPos2())));
        if (profile.getAdTrigger().getSeperatorId() > 0) {
          RegisteredTrack track = this.trackRegistry.getTrack(profile.getAdTrigger().getSeperatorId());
          if (track != null) {
            items.add(new ConfigItem("adtrigger.separator", track.toString()));
          } else {
            items.add(new ConfigItem("adtrigger.separator.missing2"));
          }
        }

      } else {
        items.add(new ConfigItem("adtrigger.auto"));
      }

      // --- Jingles ---
      List<Entry> entries = playlist.getEntries();
      int minJingles = 0;
      if (info.isStartsWithJingle() && profile.isProtectFirstJingle()) {
        items.add(new ConfigItem("jingle.first", entries.get(0).getTrack().toString()));
        minJingles++;
      }
      if (generate || info.getNumJingles() > minJingles) {
        if (profile.isProtectAllJingles()) {
          items.add(new ConfigItem("jingle.dist.preserve"));
        } else {
          int interval = profile.getJingleInterval();
          if (interval > 0) {
            items.add(new ConfigItem("jingle.dist.interval", Integer.toString(interval)));
          } else {
            items.add(new ConfigItem("jingle.dist.equal"));
          }
        }
      }

      // --- Moderation ---
      if (info.getNumWord() > 0) {
        WordDistributionStrategy wordDist = profile.getWordDistributionStrategy();
        if (wordDist == WordDistributionStrategy.RANDOM) {
          items.add(new ConfigItem("word.dist.random"));
        } else if (wordDist == WordDistributionStrategy.PROTECT) {
          items.add(new ConfigItem("word.dist.preserve"));
        } else if (wordDist == WordDistributionStrategy.SUCCESSOR_COUPLING) {
          items.add(new ConfigItem("word.dist.link.next"));
        } else if (wordDist == WordDistributionStrategy.PREDECESSOR_COUPLING) {
          items.add(new ConfigItem("word.dist.link.prev"));
        }
      }

    }
  }

  public static class ConfigItem {
    private String valueKey;
    private String[] valueOpts;

    public ConfigItem(String valueKey, String... valueOpts) {
      super();
      this.valueKey = valueKey;
      this.valueOpts = valueOpts;
    }

    public String getValueKey() {
      return valueKey;
    }

    public String[] getValueOpts() {
      return valueOpts;
    }

    public String toString() {
      return valueKey;
    }
  }

  static class PlaylistInfo {
    private boolean startsWithJingle;
    private boolean adTriggerIncluded;
    private boolean newsIncluded;
    private int numJingles = 0;
    private int numWord = 0;

    private Set<Integer> trackIds = new HashSet<>();

    public boolean isStartsWithJingle() {
      return startsWithJingle;
    }

    public void setStartsWithJingle(boolean startsWithJingle) {
      this.startsWithJingle = startsWithJingle;
    }

    public boolean isAdTriggerIncluded() {
      return adTriggerIncluded;
    }

    public void setAdTriggerIncluded(boolean adTriggerIncluded) {
      this.adTriggerIncluded = adTriggerIncluded;
    }

    public boolean isNewsIncluded() {
      return newsIncluded;
    }

    public void setNewsIncluded(boolean newsIncluded) {
      this.newsIncluded = newsIncluded;
    }

    public int getNumJingles() {
      return numJingles;
    }

    public void setNumJingles(int numJingles) {
      this.numJingles = numJingles;
    }

    public int getNumWord() {
      return numWord;
    }

    public void setNumWord(int numWord) {
      this.numWord = numWord;
    }

    public void addTrack(int trackId) {
      trackIds.add(trackId);
    }

    public boolean contains(int trackId) {
      return this.trackIds.contains(trackId);
    }
  }

}
