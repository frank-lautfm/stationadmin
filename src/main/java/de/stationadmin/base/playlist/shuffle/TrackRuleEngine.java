package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.stationadmin.base.playlist.shuffle.TrackRule.TrackPosition;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;

public class TrackRuleEngine implements PlaylistEnhancer {

  private TrackRegistry trackRegistry;
  private TagManager tagManager;

  private Map<String, TrackRuleGroup> groups = new HashMap<String, TrackRuleGroup>();
  private List<TrackRuleInstance> rules = new ArrayList<TrackRuleInstance>();
  private HashSet<Integer> trackIds = new HashSet<Integer>();

  private Map<String, Integer> groupTimes = new HashMap<String, Integer>();

  private int time = 0;

  public TrackRuleEngine(TrackRegistry trackRegistry, TagManager tagManager) {
    super();
    this.trackRegistry = trackRegistry;
    this.tagManager = tagManager;
  }

  List<TrackRuleInstance> getRules() {
    return rules;
  }

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    return this.trackIds.contains(track.getId());
  }

  public void register(TrackRuleGroup group) {
    this.groups.put(group.getName(), group);
  }

  public void register(TrackRule rule) {
    this.rules.add(new TrackRuleInstance(rule));
    this.trackIds.add(rule.getTrackId());
  }

  @Override
  public List<BasicTrack> process(List<BasicTrack> tracks) {
    HashSet<Integer> triggers = new HashSet<Integer>();
    List<BasicTrack> newTracks = new ArrayList<BasicTrack>();
    for (TrackRuleInstance rule : this.rules) {
      rule.initialize(tracks);
      triggers.addAll(rule.getBoundTo());
    }

    List<BasicTrack> after = new ArrayList<BasicTrack>();

    for (BasicTrack track : tracks) {
      if (triggers.contains(track.getId())) {
        for (TrackRuleInstance rule : this.rules) {
          if (rule.isApplicable(track, time)) {
            if (rule.getPosition() == TrackPosition.BEFORE) {
              newTracks.add(rule.getTrack());
            } else {
              after.add(rule.getTrack());
            }
            rule.markApplied(time);
          }
        }
      }

      newTracks.add(track);
      if (after.size() > 0) {
        newTracks.addAll(after);
        after.clear();
      }

      time += track.getLength();
    }

    return newTracks;
  }

  @Override
  public void reset() {
    this.groupTimes.clear();
    for (TrackRuleInstance rule : rules) {
      rule.timeLast = -1;
    }
    this.time = 0;
  }

  class TrackRuleInstance extends TrackRule {
    private static final long serialVersionUID = -9089577319453159769L;

    private BasicTrack track;
    private Set<Integer> boundTo = new HashSet<Integer>();

    private int timeLast = -1;
    private Set<Integer> checkedTrackIds = new HashSet<Integer>();

    TrackRuleInstance(TrackRule rule) {
      super(rule);
    }

    void initialize(List<BasicTrack> tracks) {
      track = trackRegistry.getTrack(getTrackId());

      try {
        if (getFilterType() == FilterType.TAG) {
          for (int i : tagManager.getTrackIds(getFilter())) {
            boundTo.add(i);
          }
        } else {
          String term = normalize(getFilter());
          for (BasicTrack track : tracks) {
            boolean match = false;
            if (!checkedTrackIds.contains(track.getId())) {
              switch (getFilterType()) {
              case ARTIST:
                match = normalize(track.getArtist()).contains(term);
                break;
              case ALBUM:
                match = normalize(track.getArtist()).equals(term);
                break;
              case ARTIST_TITLE:
                match = normalize(track.getArtist() + " " + track.getTitle()).equals(term);
                break;
              default:
                break;
              }
            }
            checkedTrackIds.add(track.getId());
            if (match) {
              boundTo.add(track.getId());
            }
          }
        }
      } catch (Exception e) {

      }
    }

    String normalize(String str) {
      str = str.toLowerCase().trim();
      String newStr = str.replaceAll("\\W", ""); // remove non-letters and non-digits
      if (newStr.length() > 3) {
        str = newStr;
      }
      return str;
    }

    boolean isApplicable(BasicTrack track, int time) {
      if (track != null && boundTo.contains(track.getId()) && (timeLast == -1 || (time - timeLast) / 60 > getMinDistance())) {
        TrackRuleGroup group = groups.get(getGroupName());
        Integer lastGroupTime = groupTimes.get(getGroupName());
        if (group == null || lastGroupTime == null || (time - lastGroupTime.intValue()) / 60 > group.getMinDistance()) {
          return true;
        }
      }
      return false;
    }

    void markApplied(int time) {
      this.timeLast = time;
      groupTimes.put(getGroupName(), time);
    }

    BasicTrack getTrack() {
      return track;
    }

    Set<Integer> getBoundTo() {
      return boundTo;
    }
  }


}
