package de.stationadmin.base.playlist.shuffle;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.profile.PlaylistProfile;
import de.stationadmin.base.playlist.profile.TrackRuleCfg;
import de.stationadmin.base.playlist.shuffle.TrackRule.TrackPosition;
import de.stationadmin.base.playlist.shuffle.TrackRuleGroup.MultiMatchSelection;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.TrackRegistry;

public class TrackRuleEngine implements PlaylistEnhancer {
  /**
   * What to do if a jingle is supposed to be inserted at a postion where already
   * a jingle is placed
   */
  public enum JingleCollisionStratagy {
    KEEP_BOTH, KEEP_STANDARD_JINGLE, KEEP_RULE_JINGLE
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

    Set<Integer> getBoundTo() {
      return boundTo;
    }

    BasicTrack getTrack() {
      return track;
    }

    void initialize(List<BasicTrack> tracks) {
      track = trackRegistry.getTrack(getTrackId());

      try {
        if (getFilterType() == FilterType.TAG) {
          int[] trackIds = tagManager.getTrackIds(getFilter());
          if (trackIds != null) {
            for (int i : trackIds) {
              boundTo.add(i);
            }
          }
        } else {
          String term = normalize(getFilter());
          // log.info("filter for " + term);
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
          // if (boundTo.size() > 0) {
          // log.debug(boundTo.size() + " tracks matching " + getFilterType() + " " +
          // getFilter());
          // }
        }
      } catch (Exception e) {
        log.error("error during track rule filtering", e);
      }
    }

    boolean isValid() {
      return this.track != null && this.boundTo.size() > 0;
    }

    boolean isApplicable(BasicTrack track, int time) {
      boolean ignoreDistance = isIgnoreDistance(track);
      if (track != null && boundTo.contains(track.getId())) {
        if (ignoreDistance || timeLast == -1 || (time - timeLast) / 60 > getMinDistance()) {
          TrackRuleGroup group = groups.get(getGroupName());
          Integer lastGroupTime = groupTimes.get(getGroupName());
          if (ignoreDistance || group == null || lastGroupTime == null || (time - lastGroupTime.intValue()) / 60 > group.getMinDistance()) {
            // log.debug(getFilterType() + " " + getFilter() + " is applicable");
            return true;
          }
          // else {
          // log.info(getFilterType() + " " + getFilter() + " is rejected (group
          // distance)");
          // }
        }
        // else {
        // log.info(getFilterType() + " " + getFilter() + " is rejected (track
        // distance)");
        // }
      }
      return false;
    }

    void markApplied(int time) {
      this.timeLast = time;
      groupTimes.put(getGroupName(), time);
    }

    String normalize(String str) {
      str = str.toLowerCase().trim();
      String newStr = str.replaceAll("\\W", ""); // remove non-letters and non-digits
      if (newStr.length() > 3) {
        str = newStr;
      }
      return str;
    }
  }

  private static Logger log = LogManager.getLogger(TrackRuleEngine.class);

  private TrackRegistry trackRegistry;
  private TagManager tagManager;

  private TrackRuleCfg cfg;

  private Map<String, TrackRuleGroup> groups = new HashMap<String, TrackRuleGroup>();
  private List<TrackRuleInstance> rules = new ArrayList<TrackRuleInstance>();
  private HashSet<Integer> jingleTrackIds = new HashSet<Integer>();

  private JingleCollisionStratagy jingleCollisionStrategy = JingleCollisionStratagy.KEEP_BOTH;

  private MultiMatchSelection groupCollisionStrategy = MultiMatchSelection.ALL;
  private Map<String, Integer> groupTimes = new HashMap<String, Integer>();

  private int time = 0;

  private Random random = new Random();

  private boolean configureFromProfile = true;

  public TrackRuleEngine(TrackRegistry trackRegistry, TagManager tagManager) {
    super();
    this.trackRegistry = trackRegistry;
    this.tagManager = tagManager;
  }

  private void configure(TrackRuleCfg cfg) {
    if (cfg == null || cfg.getGroups() == null || cfg.getRules() == null) {
      this.reset();
      this.cfg = null;
      this.rules.clear();
      this.groups.clear();
    } else if (this.cfg == null || !this.cfg.equals(cfg)) {
      this.cfg = cfg;
      this.reset();
      this.rules.clear();
      this.groups.clear();
      for (TrackRuleGroup group : cfg.getGroups()) {
        register(group);
      }
      for (TrackRule rule : cfg.getRules()) {
        register(rule);
      }
      this.jingleCollisionStrategy = cfg.getJingleCollisionStrategy();
      this.groupCollisionStrategy = cfg.getGroupCollisionStrategy();
    }
  }

  @Override
  public boolean excludeFromCorePlaylist(BasicTrack track) {
    return this.jingleTrackIds.contains(track.getId());
  }

  /**
   * Filter rules of more than one rule is applicable for a track.
   * <p>
   * If more than one rule of the same group is applicable, use
   * MultiMatchSelection strategy of group to select rules that are really
   * applied.
   * 
   * @param rules
   * @return
   */
  private List<TrackRuleInstance> filterApplicableRules(List<TrackRuleInstance> rules) {
    Map<String, List<TrackRuleInstance>> rulesByGroup = new HashMap<String, List<TrackRuleInstance>>();
    List<String> groupNames = new ArrayList<String>();
    for (TrackRuleInstance rule : rules) {
      String groupName = rule.getGroupName() != null ? rule.getGroupName() : "-";
      List<TrackRuleInstance> rulesOfGroup = rulesByGroup.get(groupName);
      if (rulesOfGroup == null) {
        rulesOfGroup = new ArrayList<TrackRuleEngine.TrackRuleInstance>();
        rulesByGroup.put(groupName, rulesOfGroup);
        groupNames.add(groupName);
      }
      rulesOfGroup.add(rule);
    }

    if (groupNames.size() > 1 && groupCollisionStrategy != MultiMatchSelection.ALL) {
      String groupName = groupCollisionStrategy == MultiMatchSelection.FIRST ? groupNames.get(0) : groupNames.get(random.nextInt(groupNames.size()));
      groupNames.clear();
      groupNames.add(groupName);
    }

    List<TrackRuleInstance> filtered = new ArrayList<TrackRuleEngine.TrackRuleInstance>();
    for (String groupName : groupNames) {
      List<TrackRuleInstance> rulesOfGroup = rulesByGroup.get(groupName);
      TrackRuleGroup group = groups.get(groupName);
      if (group == null || group.getMultiMatchSelection() == MultiMatchSelection.ALL || rulesOfGroup.size() == 1) {
        filtered.addAll(rulesOfGroup);
      } else if (group.getMultiMatchSelection() == MultiMatchSelection.FIRST) {
        // select first
        filtered.add(rulesOfGroup.get(0));
      } else {
        // select any
        int idx = random.nextInt(rulesOfGroup.size());
        filtered.add(rulesOfGroup.get(idx));
      }
    }

    return filtered;
  }

  public MultiMatchSelection getGroupCollisionStrategy() {
    return groupCollisionStrategy;
  }

  public JingleCollisionStratagy getJingleCollisionStrategy() {
    return jingleCollisionStrategy;
  }

  @Override
  public void initialize(PlaylistProfile profile) {
    if (configureFromProfile) {
      configure(profile != null ? profile.getTrackRules() : null);
    }
  }

  public boolean isConfigureFromProfile() {
    return configureFromProfile;
  }

  protected boolean isIgnoreDistance(BasicTrack track) {
    // base implementation does always return false - might be overridden in
    // subclass
    return false;
  }

  @Override
  public List<BasicTrack> process(Playlist playlist, List<BasicTrack> tracks, boolean protectFirstJingle) {
    if (rules.size() == 0) {
      return tracks;
    }

    HashSet<Integer> triggers = new HashSet<Integer>();
    List<BasicTrack> newTracks = new ArrayList<BasicTrack>();
    List<TrackRuleInstance> validRules = new ArrayList<>();
    for (TrackRuleInstance rule : this.rules) {
      rule.initialize(tracks);
      if (rule.isValid()) {
        validRules.add(rule);
        triggers.addAll(rule.getBoundTo());
      }
    }

    List<BasicTrack> after = new ArrayList<BasicTrack>();

    for (int i = 0; i < tracks.size(); i++) {
      BasicTrack track = tracks.get(i);
      boolean skipNext = false;
      List<TrackRuleInstance> applicableRules = new ArrayList<TrackRuleInstance>();
      if (triggers.contains(track.getId())) {
        applicableRules.clear();
        for (TrackRuleInstance rule : validRules) {
          if (rule.isApplicable(track, time)) {
            applicableRules.add(rule);
          }
        }

        if (applicableRules.size() > 1) {
          // multiple rules for this track - let's check if we have to kick out some
          applicableRules = filterApplicableRules(applicableRules);
        }

        if (applicableRules.size() > 0) {
          log.info("applicable rules for " + track);
          BasicTrack previousTrack = newTracks.size() > 0 ? newTracks.get(newTracks.size() - 1) : null;
          boolean lastIsJingle = previousTrack != null && previousTrack.getType() == BasicTrack.TYPE_JINGLE && !jingleTrackIds.contains(previousTrack.getId());
          boolean nextIsJingle = i < tracks.size() - 1 && tracks.get(i + 1).getType() == BasicTrack.TYPE_JINGLE && !jingleTrackIds.contains(tracks.get(i + 1).getId());
          for (TrackRuleInstance rule : applicableRules) {
            boolean isJingle = rule.getTrack().getType() == BasicTrack.TYPE_JINGLE;
            if (rule.getPosition() == TrackPosition.BEFORE) {
              if (isJingle && lastIsJingle) {
                log.info("handle jingle collision");
                switch (jingleCollisionStrategy) {
                case KEEP_BOTH:
                  newTracks.add(rule.getTrack());
                  rule.markApplied(time);
                  break;
                case KEEP_RULE_JINGLE:
                  if (!protectFirstJingle || newTracks.size() > 1) {
                    newTracks.remove(newTracks.size() - 1);
                  }
                  // else: this is the first jingle and is supposed to be protected - preserve it
                  newTracks.add(rule.getTrack());
                  rule.markApplied(time);
                  break;
                case KEEP_STANDARD_JINGLE:
                  // preserve added jingle, don't add
                  break;
                }

              } else {
                // no jingle collision - just add
                log.info("add " + rule.getTrack() + " before " + track);
                newTracks.add(rule.getTrack());
                rule.markApplied(time);
              }
            } else {
              if (isJingle && nextIsJingle) {
                switch (jingleCollisionStrategy) {
                case KEEP_BOTH:
                  after.add(rule.getTrack());
                  rule.markApplied(time);
                  break;
                case KEEP_RULE_JINGLE:
                  skipNext = true; // next is jingle - avoid that is will be added
                  after.add(rule.getTrack());
                  rule.markApplied(time);
                  break;
                case KEEP_STANDARD_JINGLE:
                  // preserve added jingle, don't add
                  break;
                }

              } else {
                // no jingle collision - just add
                after.add(rule.getTrack());
                rule.markApplied(time);
              }
            }
          }
        }
      }

      newTracks.add(track);
      if (after.size() > 0) {
        newTracks.addAll(after);
        after.clear();
      }

      time += track.getLength();

      if (skipNext) {
        i++;
      }
    }

    return newTracks;
  }

  public void register(TrackRule rule) {
    this.rules.add(new TrackRuleInstance(rule));
    this.jingleTrackIds.add(rule.getTrackId());
  }

  public void register(TrackRuleGroup group) {
    this.groups.put(group.getName(), group);
  }

  @Override
  public void reset() {
    this.groupTimes.clear();
    for (TrackRuleInstance rule : rules) {
      rule.timeLast = -1;
    }
    this.time = 0;
  }

  public void setConfigureFromProfile(boolean configureFromProfile) {
    this.configureFromProfile = configureFromProfile;
  }

  public void setGroupCollisionStrategy(MultiMatchSelection groupCollisionStrategy) {
    this.groupCollisionStrategy = groupCollisionStrategy;
  }

  public void setJingleCollisionStrategy(JingleCollisionStratagy jingleCollisionStrategy) {
    this.jingleCollisionStrategy = jingleCollisionStrategy;
  }

}
