// StationAdmin v4.0.3
// 14.03.2026

(function (tracks, opts, trackStats) {
  const SONG = "song";
  const JINGLE = "jingle";
  const MODERATION = "moderation";
  const NEWS = "news";
  var duration = "duration" in opts && opts.duration < 64800 ? opts.duration : 64800;
  var blockLength = "blockLength" in opts ? opts.blockLength : duration / 3600 + 1;
  var maxTracksPerArtist =
    "maxTracksPerArtist" in opts && opts.maxTracksPerArtist < Math.floor(duration / (60 * 60))
      ? opts.maxTracksPerArtist
      : Math.floor(duration / (60 * 60));
  var tagWeights = "tagWeights" in opts ? opts.tagWeights : null;
  var artistSeparators = "artistSeparators" in opts ? opts.artistSeparators : [" feat"];
  var artistAliases = null;
  var wordDistribution = "wordDistribution" in opts ? opts.wordDistribution : "random";
  var preserveAllJingles = "preserveAllJingles" in opts ? opts.preserveAllJingles : 0;
  var avoidRepeat = "avoidRepeat" in opts ? opts.avoidRepeat : 2;
  var excludePreviousTracks = "excludePreviousTracks" in opts ? opts.excludePreviousTracks : 0;
  var trackNameLimit = "trackNameLimit" in opts ? opts.trackNameLimit : 0;
  var adPositions = "adPositions" in opts && opts.adPositions.length > 1 ? opts.adPositions : [15, 45];
  var adJingleCollisionStrategy = "adJingleCollisionStrategy" in opts ? opts.adJingleCollisionStrategy : "keep_both";
  var trackRulesEnabled = "trackRules" in opts;
  var schedulingRulesEnabled = "scheduled" in opts;
  var newsInterval = "newsInterval" in opts ? opts.newsInterval : 60;
  var newsMin = "newsMin" in opts ? opts.newsMin : 59;
  var newsMax = "newsMax" in opts ? opts.newsMax : 15;
  var firstJingleAfterNews = "firstJingleAfterNews" in opts ? opts.firstJingleAfterNews : true;
  var tagSequenceRules = "tagSequences" in opts ? opts.tagSequences : [];
  var tagPattern = "tagPattern" in opts ? opts.tagPattern : [];
  var tagPatternPtr = 0;
  var patternTags = {};
  var random = "random" in opts ? opts.random : Math.random;
  var firstJingle;
  var adTrigger;
  var adSeparator;
  var jingles = [];
  var lastPlays = {};
  var lastStartedAt = {};
  var recentArtists = {};
  var preservedTracks = [];
  var hasPreservedTracks = false;
  var hasLinkedTracks = false;
  var tracksAfter = {};
  var tracksBefore = {};
  var boundTracks = {};
  var selectorTags = {};
  var scheduledTracks = [];
  var dateTagCache = {};
  var newsTrack;
  var preNewsJingle;
  var executionTime;
  var startTime;
  var lastJinglePlay = -1;
  var lastNewsStarted = 0;
  var startsWithNews = false;
  var debug = "debug" in opts ? opts.debug : false;
  function log(msg) {
    if (debug) console.log(msg);
  }
  function shuffle(a) {
    partialShuffle(a, a.length);
  }
  function partialShuffle(a, len) {
    var j, x, i;
    for (i = len; i; i--) {
      j = Math.floor(random() * i);
      x = a[i - 1];
      a[i - 1] = a[j];
      a[j] = x;
    }
  }
  function normalizeArtist(artistName) {
    if (artistName == null) {
      return "<no artist>";
    }
    artistName = artistName.toLowerCase();
    if (artistAliases != null && artistName in artistAliases) {
      artistName = artistAliases[artistName];
    }
    for (var i = 0; i < artistSeparators.length; i++) {
      var pos = artistName.indexOf(artistSeparators[i]);
      if (pos > 1) {
        artistName = artistName.substring(0, pos).trim();
      }
    }
    if (artistAliases != null && artistName in artistAliases) {
      artistName = artistAliases[artistName];
    }
    return artistName;
  }
  function normalizeTitle(name) {
    if (name == null) {
      return "<no title>";
    }
    name = name.toLowerCase();
    var stripped = name.replace(/\W/g, "");
    return stripped.length > 3 ? stripped : name;
  }
  function checkDateTag(tag, previousState) {
    if (previousState == 1 || !tag.startsWith("@")) return previousState;
    if (dateTagCache[tag] !== undefined) {
      return dateTagCache[tag];
    }
    let parts = /^@(\d{1,2})\.(\d{1,2})\.\s*-\s*(\d{1,2})\.(\d{1,2})\./.exec(tag);
    if (!parts) parts = /^@(\d{1,2})\.(\d{1,2})\./.exec(tag);
    if (!parts) {
      dateTagCache[tag] = 0;
      return previousState;
    }
    const fromDay = +parts[1],
      fromMonth = +parts[2];
    const toDay = parts[3] ? +parts[3] : fromDay;
    const toMonth = parts[4] ? +parts[4] : fromMonth;
    const now = new Date(startTime);
    const year = now.getFullYear();
    let fromDate = new Date(year, fromMonth - 1, fromDay, 0, 0, 0, 0);
    let toDate = new Date(year, toMonth - 1, toDay, 23, 59, 59, 999);
    if (toDate < fromDate) {
      if (now < fromDate) {
        fromDate.setFullYear(year - 1);
      } else {
        toDate.setFullYear(year + 1);
      }
    }
    const inRange = now >= fromDate && now <= toDate;
    const result = inRange ? 1 : -1;
    dateTagCache[tag] = result;
    return result;
  }
  function isExcludedByDateTag(track) {
    var dateTagState = 0;
    if (track.tags.length > 0) {
      for (var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
      }
    }
    return dateTagState == -1;
  }
  function assignTrackScore(track) {
    track.score = 100 + Math.floor(random() * 500);
    var dateTagState = 0;
    if (tagWeights != null && track.tags.length > 0) {
      var minWeight = 0;
      var maxWeight = 0;
      for (var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
        if (track.tags[i] in tagWeights) {
          var w = tagWeights[track.tags[i]];
          if (w > 0 && w > maxWeight) {
            maxWeight = w;
          } else if (w < 0 && w < minWeight) {
            minWeight = w;
          }
        }
      }
      if (minWeight < -3 || dateTagState == -1) {
        track.score = 999999;
        return;
      }
      var weight = maxWeight + minWeight;
      if (weight > 0) {
        var p = (4 - weight) / 4;
        track.score = track.score * p;
      } else if (weight < 0) {
        weight = Math.abs(weight);
        var p = 1 + weight / 4;
        track.score = track.score * p;
      }
    } else {
      for (var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
      }
      if (dateTagState == -1) {
        track.score = 999999;
        return;
      }
    }
    if (track.id in lastPlays && lastPlays[track.id] < 60 * avoidRepeat) {
      if (excludePreviousTracks) {
        track.score = 999999;
      } else {
        var penalty = 500 - (250 * lastPlays[track.id]) / (60 * avoidRepeat);
        track.score += penalty;
        track.penalty = Math.floor(penalty / 50);
      }
    } else {
      track.penalty = 0;
    }
  }
  function initTracksAndArtists(remainingDuration, iteration) {
    var artists = [];
    var artistMap = {};
    var tracksDuration = 0;
    var start = 0;
    if ((tracks.length > 2 && tracks[0].type == NEWS) || (tracks[0].type == JINGLE && tracks[1].type == NEWS)) {
      if (tracks[0].type == JINGLE) {
        preNewsJingle = tracks[0];
        start++;
      }
      newsTrack = tracks[start];
      start++;
      if (firstJingleAfterNews && tracks[start].type == JINGLE) {
        firstJingle = tracks[start];
        start++;
      }
    }
    var excludeFollowing = false;
    var songCnt = 0;
    for (var i = start; i < tracks.length; i++) {
      if (tracks[i].id == 1) {
        newsTrack = tracks[i];
        continue;
      }
      if (
        (tracks[i].title != null && tracks[i].title.indexOf("START_AD_BREAK") > -1) ||
        (tracks[i].artist != null && tracks[i].artist.indexOf("START_AD_BREAK") > -1)
      ) {
        adTrigger = tracks[i];
        continue;
      }
      if ((trackRulesEnabled || schedulingRulesEnabled) && tracks[i].id in boundTracks) {
        if (!isExcludedByDateTag(tracks[i])) {
          boundTracks[tracks[i].id] = tracks[i];
        }
        continue;
      }
      if (tracks[i].type == SONG) {
        songCnt++;
      }
      tracks[i].plays = 0;
      tracks[i].use = false;
      tracks[i].groupTags = [];
      if (schedulingRulesEnabled) {
        var skip = false;
        for (var t = 0; t < tracks[i].tags.length; t++) {
          if (tracks[i].tags[t] in selectorTags) {
            for (var r = 0; r < selectorTags[tracks[i].tags[t]].length; r++) {
              var rule = selectorTags[tracks[i].tags[t]][r];
              if (!("tracks" in rule)) {
                rule.tracks = [];
                rule.trackIdxs = [];
              }
              if ("exclude" in rule) skip = true;
              if (!isExcludedByDateTag(tracks[i])) {
                if (tracks[i].type === SONG) {
                  tracks[i].artistNormalized = normalizeArtist(tracks[i].artist);
                }
                rule.tracks.push(tracks[i]);
                rule.trackIdxs.push(i);
              }
            }
          }
        }
        if (skip) continue;
      }
      if (tracks[i].type == JINGLE) {
        if (tracks[i].id == 8664493) {
          excludeFollowing = true;
          continue;
        }
        if (iteration == 0 && !isExcludedByDateTag(tracks[i])) {
          if (tracks[i].id == 0 || tracks[i].id == opts.adTrigger) {
            adTrigger = tracks[i];
          } else if (tracks[i].id == opts.adSeparator) {
            adSeparator = tracks[i];
          } else if (preserveAllJingles) {
            tracks[i].position = songCnt;
            preservedTracks.push(tracks[i]);
            hasPreservedTracks = true;
          } else if (
            (i == 0 || (i == 1 && tracks[0].id == 1)) &&
            "protectFirstJingle" in opts &&
            opts.protectFirstJingle
          ) {
            firstJingle = tracks[i];
          } else {
            jingles.push(tracks[i]);
          }
        }
        continue;
      } else if (tracks[i].type == MODERATION) {
        if (wordDistribution == "preserve" && iteration == 0) {
          tracks[i].position = songCnt;
          preservedTracks.push(tracks[i]);
          hasPreservedTracks = true;
          continue;
        } else if (wordDistribution == "link_next" && i < tracks.length - 1 && iteration == 0) {
          hasLinkedTracks = true;
          tracksBefore[tracks[i + 1].id] = tracks[i];
          continue;
        } else if (wordDistribution == "link_previous" && i > 0 && iteration == 0) {
          hasLinkedTracks = true;
          tracksAfter[tracks[i - 1].id] = tracks[i];
          continue;
        }
      }
      if (excludeFollowing) continue;
      tracksDuration += tracks[i].duration;
      if (trackNameLimit > 0) {
        tracks[i].normTitle = normalizeTitle(tracks[i].title);
        tracks[i].groupTags = tracks[i].tags.filter((tag) => tag.startsWith("="));
        tracks[i].groupTags.push(tracks[i].normTitle);
      }
      assignTrackScore(tracks[i]);
      if (tracks[i].score > 10000) {
        continue;
      }
      var artistName = normalizeArtist(tracks[i].artist);
      tracks[i].artistNormalized = artistName;
      var artist;
      if (artistName in artistMap) {
        artist = artistMap[artistName];
        if (tracks[i].score < artist.score) {
          artist.score = tracks[i].score;
        }
      } else {
        artist = {};
        artist.name = artistName;
        artist.tracks = [];
        artist.score = tracks[i].score;
        artistMap[artistName] = artist;
        artists.push(artist);
      }
      artist.tracks.push(tracks[i]);
    }
    for (var i = 0; i < artists.length; i++) {
      artists[i].tracks.sort(function (a, b) {
        return a.score - b.score;
      });
    }
    artists.sort(function (a, b) {
      return a.score - b.score;
    });
    if (remainingDuration / (60 * 60) < maxTracksPerArtist) {
      maxTracksPerArtist = Math.max(1, Math.floor(remainingDuration / (60 * 60)));
    }
    var tracksDurationHours = Math.floor(tracksDuration / (60 * 60));
    if (tracksDurationHours < maxTracksPerArtist && tracksDurationHours > 0) {
      maxTracksPerArtist = tracksDurationHours < 3 ? tracksDurationHours : tracksDurationHours - 1;
    }
    var candidates = [];
    for (var i = 0; i < artists.length; i++) {
      for (
        var j = 0;
        j < artists[i].tracks.length &&
        (j < maxTracksPerArtist || (tagPattern.length > 0 && artists[i].tracks[j].type == MODERATION));
        j++
      ) {
        candidates.push(artists[i].tracks[j]);
      }
    }
    candidates.sort(function (a, b) {
      return a.score - b.score;
    });
    var cDuration = 0;
    var cIdx = 0;
    while ((tagPattern.length > 0 || cDuration < remainingDuration) && cIdx < candidates.length) {
      cDuration += candidates[cIdx].duration;
      candidates[cIdx].use = true;
      candidates[cIdx].plays = 0;
      cIdx++;
    }
    return artists;
  }
  function indexTracks(pool, patternIndex, start) {
    var i;
    for (i = start; i < pool.length; i++) {
      var track = pool[i];
      if (track == null) continue;
      for (var t = 0; t < track.tags.length; t++) {
        if (track.tags[t] in patternTags) {
          patternIndex[track.tags[t]].push(i);
        }
      }
      patternIndex[track.type].push(i);
    }
    return i;
  }
  function checkTagSequenceRules(track) {
    var matchingRules = [];
    for (var r = 0; r < tagSequenceRules.length; r++) {
      var rule = tagSequenceRules[r];
      if (rule.pattern != null && track.tags.includes(rule.pattern[rule.index])) {
        rule.index++;
        if (rule.index == rule.pattern.length) {
          rule.index = 0;
          matchingRules.push(rule);
        }
      } else {
        rule.index = 0;
      }
    }
    return matchingRules;
  }
  function prepareSongPool(artists, dur) {
    var numSegments = maxTracksPerArtist * 2;
    var segments = [];
    for (var i = 0; i < numSegments; i++) {
      segments.push({ tracks: [], duration: 0 });
    }
    for (var i = 0; i < artists.length; i++) {
      var artist = artists[i];
      var artistTracks = [];
      for (var j = 0; j < artist.tracks.length; j++) {
        if (artist.tracks[j].use) {
          artistTracks.push(artist.tracks[j]);
        }
      }
      if (artistTracks.length == 0) {
        continue;
      }
      var artistSegments = Math.max(1, Math.floor(numSegments / artistTracks.length));
      var minSegment = artist.name in recentArtists ? 1 : 0;
      var currentSegment = minSegment;
      var minDuration = segments[0].duration;
      for (var s = minSegment + 1; s < artistSegments; s++) {
        if (segments[s].duration < minDuration) {
          currentSegment = s;
          minDuration = segments[s].duration;
        }
      }
      for (var t = 0; t < artistTracks.length; t++) {
        segments[currentSegment].tracks.push(artistTracks[t]);
        segments[currentSegment].duration += artistTracks[t].duration;
        currentSegment = (currentSegment + artistSegments) % segments.length;
      }
    }
    var playlistTracks = [];
    var segmentTargetDuration = Math.floor(dur / segments.length);
    for (var s = 0; s < segments.length; s++) {
      var segmentTracks = segments[s].tracks;
      if (tagPattern.length == 0) {
        shuffle(segmentTracks);
      } else {
        var avgTrackLenth = Math.floor(segments[s].duration / segmentTracks.length);
        var numTracks = Math.floor(segmentTargetDuration / avgTrackLenth);
        partialShuffle(segmentTracks, Math.min(segmentTracks.length, numTracks));
      }
      segmentTracks.sort(function (a, b) {
        var _a, _b;
        return (
          ((_a = a.penalty) !== null && _a !== void 0 ? _a : 0) - ((_b = b.penalty) !== null && _b !== void 0 ? _b : 0)
        );
      });
      playlistTracks.push.apply(playlistTracks, segmentTracks);
    }
    return playlistTracks;
  }
  function pushScheduledJingle(track, minTime) {
    scheduledTracks.push({
      tracks: [track],
      minTime: minTime,
      maxTime: minTime + 1000 * 60 * 5,
      jingleCollision: "skip_scheduled",
      type: JINGLE
    });
  }
  function scheduleJingles() {
    var addFirstJingle = firstJingle != null && !startsWithNews;
    if (!addFirstJingle && jingles.length == 0) return;
    if (addFirstJingle && jingles.length == 0) {
      pushScheduledJingle(firstJingle, startTime);
      return;
    }
    var jingleOrder = "shuffle";
    if ("jingleOrder" in opts) {
      jingleOrder = opts.jingleOrder;
    }
    if (jingleOrder != "preserve") {
      shuffle(jingles);
    }
    var jingleIntervalMin = 0;
    if ("jingleInterval" in opts) {
      jingleIntervalMin = opts.jingleInterval;
    }
    if (jingleIntervalMin == 0) {
      var numJingles = addFirstJingle ? jingles.length + 1 : jingles.length;
      jingleIntervalMin = Math.floor(duration / numJingles / 60);
    }
    var jingleIntervalMs = jingleIntervalMin * 60 * 1000;
    var newsJingleTimes = [];
    for (var n = 0; n < scheduledTracks.length; n++) {
      if (scheduledTracks[n].type == NEWS && scheduledTracks[n].tracks) {
        var trackTime = scheduledTracks[n].minTime;
        for (var nt = 0; nt < scheduledTracks[n].tracks.length; nt++) {
          if (scheduledTracks[n].tracks[nt].type == JINGLE) {
            newsJingleTimes.push(trackTime);
          }
          trackTime += scheduledTracks[n].tracks[nt].duration * 1000;
        }
      }
    }
    var jingleOffset = 0;
    var jingleIdx = 0;
    var time = startTime;
    if (addFirstJingle) {
      pushScheduledJingle(firstJingle, startTime);
      jingleOffset = jingleIntervalMs;
      time = startTime + jingleOffset;
    } else {
      if (lastJinglePlay > -1) {
        jingleOffset = Math.max(0, jingleIntervalMin - lastJinglePlay) * 60 * 1000;
      } else {
        jingleOffset = Math.floor(random() * jingleIntervalMin);
      }
      time = startTime + jingleOffset;
    }
    var endTime = startTime + duration * 1000;
    var jingleCnt = 0;
    while (time < endTime) {
      var resetBase = -1;
      for (var n = 0; n < newsJingleTimes.length; n++) {
        if (time > newsJingleTimes[n] && time < newsJingleTimes[n] + jingleIntervalMs) {
          resetBase = newsJingleTimes[n];
          break;
        }
      }
      if (resetBase > -1) {
        jingleOffset = resetBase + jingleIntervalMs - startTime;
        jingleCnt = 0;
        time = startTime + jingleOffset;
        continue;
      }
      pushScheduledJingle(jingles[jingleIdx], time);
      jingleIdx++;
      if (jingleIdx == jingles.length) {
        jingleIdx = 0;
        if (jingleOrder == "shuffle_repeat") {
          shuffle(jingles);
        }
      }
      jingleCnt++;
      time = startTime + jingleOffset + jingleCnt * jingleIntervalMs;
    }
  }
  function normalizeTerm(term) {
    if (term) {
      term = term.toLowerCase();
      return term.replace(/\W/g, "");
    } else {
      return "";
    }
  }
  function isBoundTo(track, rule) {
    if (rule.filterType == "tag") {
      return track.tags.includes(rule.filter);
    }
    if (!("term" in rule)) {
      rule.term = normalizeTerm(rule.filter);
    }
    switch (rule.filterType) {
      case "artist":
        return normalizeTerm(track.artist).includes(rule.term);
      case "title":
        return normalizeTerm(track.title).includes(rule.term);
      case "artist_title":
        return normalizeTerm(track.artist + " " + track.title).includes(rule.term);
      default:
        return false;
    }
  }
  function filterApplicableRules(rules) {
    var rulesByGroup = {};
    var groupNames = [];
    for (var i = 0; i < rules.length; i++) {
      var rule = rules[i];
      var groupName = rule.groupName != null ? rule.groupName : "-";
      if (!(groupName in rulesByGroup)) {
        rulesByGroup[groupName] = [];
        groupNames.push(groupName);
      }
      rulesByGroup[groupName].push(rule);
    }
    if (groupNames.length > 1 && opts.trackRuleGroupCollisionStrategy != "all") {
      var idx = opts.trackRuleGroupCollisionStrategy == "first" ? 0 : Math.floor(random() * groupNames.length);
      var selectedGroupName = groupNames[idx];
      groupNames = [];
      groupNames.push(selectedGroupName);
    }
    var filtered = [];
    for (var g = 0; g < groupNames.length; g++) {
      var group = opts.trackRuleGroups[groupNames[g]];
      if (group == null || group.multiMatchSelection == "all" || rulesByGroup[groupNames[g]].length == 1) {
        filtered = filtered.concat(rulesByGroup[groupNames[g]]);
      } else if (group.multiMatchSelection == "first") {
        filtered.push(rulesByGroup[groupNames[g]][0]);
      } else {
        var idx = Math.floor(random() * rulesByGroup[groupNames[g]].length);
        filtered.push(rulesByGroup[groupNames[g]][idx]);
      }
    }
    return filtered;
  }
  function markRuleApplied(rule, time) {
    rule.lastPlay = time;
    if (rule.groupName in opts.trackRuleGroups) {
      opts.trackRuleGroups[rule.groupName].lastPlay = time;
    }
    return time + boundTracks[rule.trackId].duration * 1000;
  }
  function getBoundTracksForSong(song, currentTime, lastTrack, nextTrack) {
    var result = { before: [], after: [] };
    if (!trackRulesEnabled) return result;
    if (!("boundTo" in song)) {
      song.boundTo = [];
      for (var r = 0; r < opts.trackRules.length; r++) {
        if (opts.trackRules[r].active && isBoundTo(song, opts.trackRules[r])) {
          song.boundTo.push(r);
        }
      }
    }
    if (song.boundTo.length == 0) return result;
    var applicableRules = [];
    for (var r = 0; r < song.boundTo.length; r++) {
      var rIdx = song.boundTo[r];
      var group = opts.trackRuleGroups[opts.trackRules[rIdx].groupName];
      var ruleTimeMatch = currentTime - opts.trackRules[rIdx].lastPlay > opts.trackRules[rIdx].minDistance * 60000;
      var groupTimeMatch =
        group == null || !("lastPlay" in group) || currentTime - group.lastPlay > group.minDistance * 60000;
      if (ruleTimeMatch && groupTimeMatch) {
        applicableRules.push(opts.trackRules[rIdx]);
      }
    }
    if (applicableRules.length > 1) {
      applicableRules = filterApplicableRules(applicableRules);
    }
    if (applicableRules.length > 0) {
      var lastIsJingle = lastTrack != null && lastTrack.type == JINGLE;
      var nextIsJingle = nextTrack != null && nextTrack.type == JINGLE;
      for (var r = 0; r < applicableRules.length; r++) {
        var rule = applicableRules[r];
        var isJingle = boundTracks[rule.trackId].type == JINGLE;
        if (rule.position == "before") {
          if (isJingle && lastIsJingle) {
            switch (opts.trackRuleJingleCollisionStrategy) {
              case "keep_both":
                result.before.push({ track: boundTracks[rule.trackId], rule: rule });
                break;
              case "keep_rule_jingle":
                result.before.push({ track: boundTracks[rule.trackId], rule: rule, replaceLast: true });
                break;
              case "keep_standard_jingle":
                break;
            }
          } else {
            result.before.push({ track: boundTracks[rule.trackId], rule: rule });
          }
        } else {
          if (isJingle && nextIsJingle) {
            switch (opts.trackRuleJingleCollisionStrategy) {
              case "keep_both":
                result.after.push({ track: boundTracks[rule.trackId], rule: rule });
                break;
              case "keep_rule_jingle":
                result.after.push({ track: boundTracks[rule.trackId], rule: rule, skipNext: true });
                break;
              case "keep_standard_jingle":
                break;
            }
          } else {
            result.after.push({ track: boundTracks[rule.trackId], rule: rule });
          }
        }
      }
    }
    return result;
  }
  function isInNewsTimeframe(minutes) {
    if (newsMax > newsMin) {
      return minutes >= newsMin && minutes <= newsMax;
    } else {
      var diff = 60 - newsMin;
      var m = (minutes + diff) % 60;
      return m >= 0 && m <= newsMax + diff;
    }
  }
  function scheduleNews() {
    var newsTracks = [];
    var jingleCollision = "keep_both";
    if (preNewsJingle != null) {
      newsTracks.push(preNewsJingle);
      jingleCollision = "remove_jingle";
    }
    newsTracks.push(newsTrack);
    newsTrack.duration = 165;
    if (firstJingle != null && firstJingleAfterNews) {
      newsTracks.push(firstJingle);
      jingleCollision = "remove_jingle";
    }
    var ts = new Date();
    var time = startTime;
    var endTime = startTime + duration * 1000;
    var noNewsAfter = endTime - 15 * 60 * 1000;
    while (time < noNewsAfter) {
      ts.setTime(time);
      ts.setSeconds(0);
      if (isInNewsTimeframe(ts.getMinutes()) && ts.getTime() - lastNewsStarted > 1000 * 30 * 45) {
        if (time == startTime) startsWithNews = true;
        var scheduledNews = {};
        scheduledNews.tracks = newsTracks;
        scheduledNews.minTime = ts.getTime();
        var diff = ts.getMinutes() < newsMax ? newsMax - ts.getMinutes() : newsMax + 60 - ts.getMinutes();
        scheduledNews.maxTime = ts.getTime() + 1000 * 60 * diff;
        scheduledNews.jingleCollision = jingleCollision;
        scheduledNews.type = NEWS;
        scheduledTracks.push(scheduledNews);
        log("schedule news: " + ts.toLocaleString() + ", max = " + new Date(scheduledNews.maxTime).toLocaleString());
        time += newsInterval * 1000 * 60;
        if (ts.getMinutes() != newsMin) time -= 1000 * 60 * 15;
      } else {
        time += 1000 * 60;
      }
    }
  }
  function scheduleAdTriggerAt(adTracks, time) {
    scheduledTracks.push({
      tracks: adTracks,
      minTime: time,
      maxTime: time + 1000 * 60 * 25,
      jingleCollision: adJingleCollisionStrategy == "move_adtrigger" ? "move" : adJingleCollisionStrategy,
      type: "adTrigger"
    });
  }
  function scheduleAdTriggers() {
    var adTracks = [];
    if (adSeparator != null) {
      adTracks.push(adSeparator);
    }
    adTracks.push(adTrigger);
    var position1 = adPositions[0];
    var position2 = adPositions[1];
    var diff = position2 - position1;
    if (diff < 20 || diff > 40) {
      if (position1 > 30) {
        position1 = 30;
      }
      if (diff < 20) {
        position2 = position1 + 20;
      } else if (diff > 40) {
        position2 = position1 + 40;
      }
    }
    var ts = new Date();
    ts.setTime(startTime);
    ts.setSeconds(0);
    var startHour = ts.getHours();
    var endTime = startTime + duration * 1000;
    var endHour = startHour + duration / 3600;
    ts.setSeconds(0);
    ts.setMilliseconds(0);
    for (var h = startHour; h <= endHour; h++) {
      ts.setMinutes(position1);
      if (ts.getTime() > startTime && ts.getTime() < endTime) {
        scheduleAdTriggerAt(adTracks, ts.getTime());
      }
      ts.setMinutes(position2);
      if (ts.getTime() > startTime && ts.getTime() < endTime) {
        scheduleAdTriggerAt(adTracks, ts.getTime());
      }
      ts.setMinutes(0);
      ts.setTime(ts.getTime() + 3600000);
    }
  }
  function scheduleByRule(rule) {
    log("schedule " + rule.tag);
    var ts = new Date();
    ts.setTime(startTime);
    var startHour = ts.getHours();
    var endTime = startTime + duration * 1000;
    var endHour = startHour + duration / 3600;
    var dayFilter = "day" in rule ? rule.day : -1;
    var trackIdx = 0;
    var trackIdxInc = 1;
    var boundToNews = false;
    var useLateSelection = false;
    if (rule.selection == "rotate") {
      var maxTime = 0;
      for (var t = 0; t < rule.tracks.length - 1; t++) {
        if (rule.tracks[t].id in lastStartedAt && lastStartedAt[rule.tracks[t].id] > maxTime) {
          trackIdx = (t + 1) % rule.tracks.length;
          maxTime = lastStartedAt[rule.tracks[t].id];
        }
      }
    } else if (rule.selection == "calculatedaily") {
      trackIdx = Math.floor(startTime / (1000 * 60 * 60 * 24)) % rule.tracks.length;
      trackIdxInc = 0;
    } else if (rule.selection == "date") {
      var day = ts.getDate() < 10 ? "0" + ts.getDate() : "" + ts.getDate();
      var mon = ts.getMonth() < 10 ? "0" + (ts.getMonth() + 1) : "" + (ts.getMonth() + 1);
      var dateStr = day + "." + mon + ".";
      trackIdx = -1;
      for (var t = 0; t < rule.tracks.length; t++) {
        if (rule.tracks[t].title.includes(dateStr) || rule.tracks[t].album.includes(dateStr)) {
          trackIdx = t;
          break;
        }
      }
      trackIdxInc = 0;
    } else if (rule.selection == "time") {
      rule.timeTracks = [];
      var re = /\d+/g;
      for (var t = 0; t < rule.tracks.length; t++) {
        var str = rule.tracks[t].title + " " + rule.tracks[t].album;
        var m;
        while ((m = re.exec(str)) !== null) {
          var n = parseInt(m[0]);
          if (!isNaN(n) && n >= 0 && n < 24) {
            rule.timeTracks[n] = rule.tracks[t];
          }
        }
      }
      trackIdx = -2;
      trackIdxInc = 0;
    } else if (rule.selection == "index") {
      trackIdx = rule.index - 1 < rule.tracks.length ? rule.index - 1 : -1;
      trackIdxInc = 0;
    } else {
      shuffle(rule.tracks);
      if (rule.tracks.length > 0 && rule.tracks[0].type == SONG) {
        useLateSelection = true;
      }
    }
    if (trackIdx == -1) return;
    var hours = [];
    var minutes = [];
    minutes.push(rule.minute);
    if ("hour" in rule) {
      if (rule.hour == -2) {
        rule.hour = (startHour + Math.floor(random() * (duration / 3600))) % 24;
        rule.minute = Math.floor(random() * 60);
        minutes = [];
        minutes.push(rule.minute);
      } else if (rule.hour == -3 || rule.hour == -4) {
        boundToNews = true;
      }
      if (rule.hour > -1) {
        hours.push(rule.hour);
      }
    } else if ("interval" in rule) {
      var step = rule.interval > 0 ? rule.interval : rule.interval < 0 ? 1 : 99;
      for (var h = startHour; h <= endHour; h += step) {
        hours.push(h);
      }
      if (rule.interval < -1) {
        step = -rule.interval;
        for (var mm = rule.minute + step; mm < 60; mm += step) {
          minutes.push(mm);
        }
      }
    }
    for (var i = 0; i < hours.length; i++) {
      ts.setTime(hours[i] % 24 >= startHour ? startTime : startTime + 1000 * 60 * 60 * 24);
      ts.setHours(hours[i] % 24);
      ts.setSeconds(0);
      var acceptDay =
        dayFilter == -1 ||
        dayFilter == ts.getDay() ||
        (dayFilter == -2 && ts.getDay() > 0 && ts.getDay() < 6) ||
        (dayFilter == -3 && (ts.getDay() == 0 || ts.getDay() == 6));
      if (!acceptDay) continue;
      for (var j = 0; j < minutes.length; j++) {
        ts.setMinutes(minutes[j]);
        if (ts.getTime() > executionTime && ts.getTime() < startTime + duration * 1000) {
          var scheduledElement = {};
          scheduledElement.minTime = ts.getTime();
          scheduledElement.maxTime = ts.getTime() + 1000 * 60 * 15;
          scheduledElement.jingleCollision = "keep_both";
          scheduledElement.type = "rule";
          if (useLateSelection) {
            var candidates = [];
            if (
              "introJingleId" in rule &&
              rule.introJingleId in boundTracks &&
              "type" in boundTracks[rule.introJingleId]
            ) {
              candidates.push(boundTracks[rule.introJingleId]);
            }
            scheduledElement.trackCandidates = rule.tracks.slice();
            if (candidates.length > 0) {
              scheduledElement.introTracks = candidates;
            }
            log("schedule late selection at " + ts.toLocaleString() + ": " + rule.tracks.length + " candidates");
          } else {
            var selTracks = [];
            if (
              "introJingleId" in rule &&
              rule.introJingleId in boundTracks &&
              "type" in boundTracks[rule.introJingleId]
            ) {
              selTracks.push(boundTracks[rule.introJingleId]);
            }
            var track = trackIdx > -1 ? rule.tracks[trackIdx] : trackIdx == -2 ? rule.timeTracks[hours[i % 24]] : null;
            if (track == null || track == undefined) continue;
            log(
              "schedule at " + ts.toLocaleString() + ": " + trackIdx + " of " + rule.tracks.length + " " + track.title
            );
            selTracks.push(track);
            lastStartedAt[track.id] = ts.getTime();
            trackIdx = (trackIdx + trackIdxInc) % rule.tracks.length;
            scheduledElement.tracks = selTracks;
          }
          customScheduledElementCreate(rule, trackIdx, scheduledElement);
          scheduledTracks.push(scheduledElement);
        }
      }
    }
    if (boundToNews) {
      for (var i = 0; i < scheduledTracks.length; i++) {
        if (scheduledTracks[i].type != NEWS) continue;
        var tsNews = new Date(scheduledTracks[i].minTime);
        var hour = tsNews.getMinutes() < 57 ? tsNews.getHours() : (tsNews.getHours() + 1) % 24;
        if (useLateSelection) {
          if (!("trackCandidates" in scheduledTracks[i])) {
            scheduledTracks[i].trackCandidates = rule.tracks.slice();
          }
          scheduledTracks[i].newsPosition = rule.hour == -3 ? "before" : "after";
        } else {
          var track = trackIdx > -1 ? rule.tracks[trackIdx] : trackIdx == -2 ? rule.timeTracks[hour] : null;
          if (track == null || track == undefined) continue;
          scheduledTracks[i].tracks = [...scheduledTracks[i].tracks];
          if (rule.hour == -3) {
            scheduledTracks[i].tracks.unshift(track);
          } else {
            scheduledTracks[i].tracks.push(track);
          }
        }
      }
    }
  }
  function scheduleByRules() {
    for (var i = 0; i < opts.scheduled.length; i++) {
      if ("tracks" in opts.scheduled[i]) {
        scheduleByRule(opts.scheduled[i]);
      }
    }
  }
  function selectFromSongPool() {
    var bestIdx = -1;
    var bestPenalty = 9999;
    var checkedMatches = 0;
    for (var cIdx = songPoolIdx; cIdx < songPool.length && checkedMatches < 6; cIdx++) {
      var track = songPool[cIdx];
      if (track == null) continue;
      if (track.type != SONG) {
        return cIdx;
      }
      var penalty = 0;
      checkedMatches++;
      if ("artistNormalized" in track && track.artistNormalized in artistBlocked) {
        if (time < artistBlocked[track.artistNormalized]) {
          penalty += 3;
        }
      }
      for (var rr = 0; rr < matchingRules.length; rr++) {
        var result = track.tags.includes(matchingRules[rr].next);
        if ((matchingRules[rr].not && result) || (!matchingRules[rr].not && !result)) {
          penalty++;
        }
      }
      if (
        trackNameLimit > 0 &&
        track.groupTags &&
        track.groupTags.some(function (tg) {
          return recentTrackNames.some(function (rn) {
            return rn.includes(tg);
          });
        })
      ) {
        penalty += 3;
      }
      if (penalty < bestPenalty) {
        bestPenalty = penalty;
        bestIdx = cIdx;
      }
      if (penalty == 0) break;
    }
    return bestIdx;
  }
  function selectFromPatternIndex() {
    var candidates = patternIndex[tagPattern[tagPatternPtr]];
    if (candidates.length == 0 && patternIndexPtr < songPool.length) {
      patternIndexPtr = indexTracks(songPool, patternIndex, patternIndexPtr);
      candidates = patternIndex[tagPattern[tagPatternPtr]];
    }
    var bestIdx = -1;
    var bestPenalty = 9999;
    var checkedMatches = 0;
    for (var cIdx = 0; cIdx < candidates.length; cIdx++) {
      var track = songPool[candidates[cIdx]];
      if (track != null) {
        var artistAccepted =
          track.type == SONG && "artistNormalized" in track && track.artistNormalized in artistBlocked
            ? time > artistBlocked[track.artistNormalized]
            : true;
        if (artistAccepted) {
          var penalty = 0;
          checkedMatches++;
          if (track.type == SONG) {
            for (var rr = 0; rr < matchingRules.length; rr++) {
              var result = track.tags.includes(matchingRules[rr].next);
              if ((matchingRules[rr].not && result) || (!matchingRules[rr].not && !result)) {
                penalty++;
              }
            }
            if (
              track.groupTags &&
              track.groupTags.some(function (tg) {
                return recentTrackNames.some(function (rn) {
                  return rn.includes(tg);
                });
              })
            ) {
              penalty += 3;
            }
          }
          if (penalty < bestPenalty) {
            bestPenalty = penalty;
            bestIdx = cIdx;
          }
          if (penalty == 0 || checkedMatches == 5) {
            break;
          }
        }
      }
      if (cIdx == candidates.length - 1 && patternIndexPtr < songPool.length) {
        patternIndexPtr = indexTracks(songPool, patternIndex, patternIndexPtr);
      }
    }
    return { candidates: candidates, index: bestIdx };
  }
  function selectFromScheduledCandidates(candidates) {
    var bestIdx = 0;
    var bestPenalty = 9999;
    for (var cIdx = 0; cIdx < candidates.length; cIdx++) {
      var track = candidates[cIdx];
      if (track == null) continue;
      var penalty = 0;
      if (track.type == SONG && "artistNormalized" in track && track.artistNormalized in artistBlocked) {
        if (time < artistBlocked[track.artistNormalized]) {
          penalty += 3;
        }
      }
      if (
        trackNameLimit > 0 &&
        track.groupTags &&
        track.groupTags.some(function (tg) {
          return recentTrackNames.some(function (rn) {
            return rn.includes(tg);
          });
        })
      ) {
        penalty += 3;
      }
      if (track.plays > 0) {
        penalty += track.plays * 5;
      }
      if (penalty < bestPenalty) {
        bestPenalty = penalty;
        bestIdx = cIdx;
      }
      if (penalty == 0) break;
    }
    return candidates[bestIdx];
  }
  function processScheduledElement(scheduledElement) {
    if (
      "trackCandidates" in scheduledElement &&
      (!("tracks" in scheduledElement) || scheduledElement.tracks.length == 0)
    ) {
      var selectedTrack = selectFromScheduledCandidates(scheduledElement.trackCandidates);
      scheduledElement.tracks = [];
      if ("introTracks" in scheduledElement) {
        scheduledElement.tracks.push.apply(scheduledElement.tracks, scheduledElement.introTracks);
      }
      scheduledElement.tracks.push(selectedTrack);
      log("Late selection: " + selectedTrack.title);
    }
  }
  function updateSelectionState(song) {
    if (song.type == SONG) {
      if ("plays" in song) {
        song.plays++;
      }
      if (!("artistNormalized" in song)) {
        song.artistNormalized = normalizeArtist(song.artist);
      }
      artistBlocked[song.artistNormalized] = time + artistBlockDuration;
      matchingRules = checkTagSequenceRules(song);
      if (trackNameLimit > 0) {
        recentTrackNames.push(song.groupTags);
        if (recentTrackNames.length > trackNameLimit) {
          recentTrackNames.shift();
        }
      }
    }
  }
  function insertScheduledEvents(nextIsJingle, nextIsShortTrack) {
    var skipJingle = false;
    var addScheduled = true;
    var tracksAdded = false;
    while (nextScheduled != null && time >= nextScheduled.minTime && addScheduled) {
      addScheduled = true;
      if (nextScheduled.jingleCollision != "keep_both") {
        var lastIsJingle = playlistTracks.length > 0 && playlistTracks[playlistTracks.length - 1].type == JINGLE;
        if (lastIsJingle || nextIsJingle) {
          if (nextScheduled.jingleCollision == "move") {
            if (moveCnt < 2) {
              addScheduled = false;
              moveCnt++;
            }
          } else if (nextScheduled.jingleCollision == "skip_scheduled") {
            addScheduled = false;
          } else if (nextScheduled.jingleCollision == "remove_jingle") {
            if (lastIsJingle) {
              time -= playlistTracks[playlistTracks.length - 1].duration * 1000;
              playlistTracks.splice(playlistTracks.length - 1, 1);
            }
            if (nextIsJingle) {
              skipJingle = true;
            }
          }
        }
      } else if (nextIsShortTrack && playlistTracks.length > 0 && nextScheduled.type == NEWS && moveCnt == 0) {
        addScheduled = false;
        moveCnt++;
      }
      if (addScheduled) {
        log(
          new Date(time).toLocaleString() +
            " for " +
            new Date(nextScheduled.minTime).toLocaleString() +
            " / " +
            nextScheduled.type
        );
        processScheduledElement(nextScheduled);
        for (var t = 0; t < nextScheduled.tracks.length; t++) {
          playlistTracks.push(nextScheduled.tracks[t]);
          log(nextScheduled.tracks[t].title + " " + nextScheduled.tracks[t].duration);
          updateSelectionState(nextScheduled.tracks[t]);
          time += nextScheduled.tracks[t].duration * 1000;
        }
        tracksAdded = true;
        nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
        moveCnt = 0;
      } else if (time > nextScheduled.maxTime) {
        nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
        moveCnt = 0;
      }
    }
    return { tracksAdded: tracksAdded, skipJingle: skipJingle };
  }
  function addTrackToPlaylist(playlist, song) {
    var added = 0;
    if (trackRulesEnabled && song.type == SONG) {
      var lastTrack = playlistTracks.length > 0 ? playlist[playlist.length - 1] : null;
      var boundResult = getBoundTracksForSong(song, time, lastTrack, null);
      for (var b = 0; b < boundResult.before.length; b++) {
        var entry = boundResult.before[b];
        if (entry.replaceLast && playlist.length > 1 && (!opts.protectFirstJingle || playlist.length > 1)) {
          playlist.splice(playlist.length - 1, 1);
        }
        playlist.push(entry.track);
        playlist[playlist.length - 1].linked = true;
        time = markRuleApplied(entry.rule, time);
        added += entry.track.duration * 1000;
      }
    }
    if (hasLinkedTracks && song.id in tracksBefore) {
      playlist.push(tracksBefore[song.id]);
      playlist[playlist.length - 1].linked = true;
      time += tracksBefore[song.id].duration * 1000;
      added += tracksBefore[song.id].duration * 1000;
    }
    playlist.push(song);
    time += song.duration * 1000;
    added += song.duration * 1000;
    if (song.type === SONG) {
      numberOfSongs++;
    }
    updateSelectionState(song);
    if (hasPreservedTracks) {
      while (preservedTracks.length > 0 && preservedTracks[0].position == numberOfSongs) {
        playlist.push(preservedTracks[0]);
        time += preservedTracks[0].duration;
        preservedTracks.shift();
      }
    }
    if (trackRulesEnabled && "boundTo" in song && song.boundTo.length > 0) {
      var lastTrack2 = song;
      var boundResult2 = getBoundTracksForSong(song, time, lastTrack2, null);
      for (var b2 = 0; b2 < boundResult2.after.length; b2++) {
        var entryAfter = boundResult2.after[b2];
        playlist[playlist.length - 1].linked = true;
        playlist.push(entryAfter.track);
        time += entryAfter.track.duration * 1000;
        added += entryAfter.track.duration * 1000;
        markRuleApplied(entryAfter.rule, time - entryAfter.track.duration * 1000);
      }
    }
    if (hasLinkedTracks && song.id in tracksAfter) {
      playlist[playlist.length - 1].linked = true;
      playlist.push(tracksAfter[song.id]);
      time += tracksAfter[song.id].duration * 1000;
      added += tracksAfter[song.id].duration * 1000;
    }
    return added;
  }
  function hasEnoughTaggedTracks(trackList, tags, n) {
    const tagSet = new Set(tags);
    let dur = 0;
    for (const track of trackList) {
      if (track.tags.some((tag) => tagSet.has(tag)) || tagSet.has(track.type)) {
        dur += track.duration;
        if (dur >= n) return true;
      }
    }
    return false;
  }
  function customScheduledElementCreate(_rule, _trackIdx, _scheduledElement) {}
  function customInitialize() {}
  if ("time" in opts) {
    let ts = Date.parse(opts.time);
    Date.now = () => ts;
  }
  executionTime = Date.now();
  startTime = executionTime + 1000 * 120;
  if (trackRulesEnabled) {
    for (var i = 0; i < opts.trackRules.length; i++) {
      var trackId = opts.trackRules[i].trackId;
      boundTracks[trackId] = {};
      opts.trackRules[i].lastPlay = startTime - 1000 * 60 * 60 * 24;
      if (!("rules" in boundTracks[trackId])) {
        boundTracks[trackId].rules = [];
      }
      boundTracks[trackId].rules.push(opts.trackRules[i]);
    }
  }
  if (schedulingRulesEnabled) {
    for (var i = 0; i < opts.scheduled.length; i++) {
      if ("introJingleId" in opts.scheduled[i]) {
        boundTracks[opts.scheduled[i].introJingleId] = {};
      }
      if (!(opts.scheduled[i].tag in selectorTags)) {
        selectorTags[opts.scheduled[i].tag] = [];
      }
      selectorTags[opts.scheduled[i].tag].push(opts.scheduled[i]);
    }
  }
  if ("artistAliases" in opts) {
    artistAliases = {};
    Object.keys(opts.artistAliases).forEach(function (property) {
      artistAliases[property.toLowerCase()] = opts.artistAliases[property].toLowerCase();
    });
  }
  if (trackStats != null) {
    var baseTime = executionTime;
    var lastTrackEnd = 0;
    for (var i = 0; i < trackStats.length; i++) {
      if (i > trackStats.length - 12 && trackStats[i].artist != null) {
        var artistName = normalizeArtist(trackStats[i].artist.name);
        recentArtists[artistName] = true;
      }
      var started = Date.parse(trackStats[i].started_at);
      lastStartedAt[trackStats[i].id] = started;
      var endsAt = Date.parse(trackStats[i].ends_at);
      lastTrackEnd = Math.max(lastTrackEnd, endsAt);
      var diff = Math.floor((baseTime - started) / (1000 * 60));
      if (diff < avoidRepeat * 60) {
        if (lastPlays[trackStats[i].id] == null) {
          lastPlays[trackStats[i].id] = diff;
        }
      }
      if (trackStats[i].type == JINGLE) {
        lastJinglePlay = diff;
      }
      if (trackRulesEnabled && trackStats[i].id in boundTracks && "rules" in boundTracks[trackStats[i].id]) {
        for (var r = 0; r < boundTracks[trackStats[i].id].rules.length; r++) {
          markRuleApplied(boundTracks[trackStats[i].id].rules[r], started);
        }
      }
      if (trackStats[i].id == 1) {
        lastNewsStarted = started;
      }
    }
    if (lastTrackEnd > baseTime) {
      startTime = lastTrackEnd;
    }
  }
  for (var i = 0; i < artistSeparators.length; i++) {
    artistSeparators[i] = artistSeparators[i].toLowerCase();
  }
  if (tagPattern.length > 0 && !hasEnoughTaggedTracks(tracks, tagPattern, 60 * 60)) {
    log("Discarding tag pattern - not enough tracks");
    tagPattern = [];
  }
  for (var i = 0; i < tagPattern.length; i++) {
    patternTags[tagPattern[i]] = true;
  }
  customInitialize();
  log("Execution time: " + new Date(executionTime) + ", start time: " + new Date(startTime));
  var sumTrackDuration = 0;
  var songPool = [];
  var remainingDuration = duration;
  var iteration = 0;
  while (remainingDuration > 0 && iteration < 20) {
    let artists = initTracksAndArtists(Math.min(blockLength * 60 * 60, remainingDuration), iteration);
    var selectedTracks = prepareSongPool(artists, Math.min(blockLength * 60 * 60, remainingDuration));
    selectedTracks.forEach(function (t) {
      sumTrackDuration += t.duration;
    });
    songPool = songPool.concat(selectedTracks);
    remainingDuration = duration - sumTrackDuration;
    iteration++;
    if (remainingDuration > 0) {
      var addedMinutes = Math.floor(sumTrackDuration / 60);
      recentArtists = {};
      for (var id in lastPlays) {
        lastPlays[id] += addedMinutes;
      }
      var tmpDuration = 0;
      for (var i = 0; i < selectedTracks.length; i++) {
        tmpDuration += selectedTracks[i].duration;
        lastPlays[selectedTracks[i].id] = Math.floor((sumTrackDuration - tmpDuration) / 60);
        if (i >= selectedTracks.length - 12) {
          var artistName = normalizeArtist(selectedTracks[i].artist);
          recentArtists[artistName] = true;
        }
      }
    }
  }
  var tagPatternContainsJingles = false;
  if (tagPattern.length > 0) {
    if (JINGLE in patternTags) {
      tagPatternContainsJingles = true;
    }
    for (var i = 0; i < tracks.length && !tagPatternContainsJingles; i++) {
      if (tracks[i].type == JINGLE) {
        for (var t = 0; t < tracks[i].tags.length; t++) {
          if (tracks[i].tags[t] in patternTags) {
            tagPatternContainsJingles = true;
            break;
          }
        }
      }
    }
  }
  if (newsTrack != null) {
    scheduleNews();
  }
  if (tagPattern.length == 0 || !tagPatternContainsJingles) {
    scheduleJingles();
  }
  if (adTrigger != null) {
    scheduleAdTriggers();
  }
  if (schedulingRulesEnabled) {
    scheduleByRules();
  }
  if (trackRulesEnabled) {
    for (var i = 0; i < opts.trackRules.length; i++) {
      opts.trackRules[i].active = "type" in boundTracks[opts.trackRules[i].trackId];
    }
  }
  scheduledTracks.sort(function (a, b) {
    return a.minTime - b.minTime;
  });
  for (var r = 0; r < tagSequenceRules.length; r++) {
    tagSequenceRules[r].index = 0;
  }
  var playlistTracks = [];
  var time = startTime;
  var sIdx = 0;
  var nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
  var moveCnt = 0;
  var numberOfSongs = 0;
  var artistBlocked = {};
  var recentTrackNames = [];
  var matchingRules = [];
  var artistBlockDuration = tagPattern.length > 0 ? 60 * 60 * 1000 : 30 * 60 * 1000;
  var songPoolIdx = 0;
  var usePatternIndex = tagPattern.length > 0;
  var patternIndex = null;
  var patternIndexPtr = 0;
  var patternFailed = 0;
  if (usePatternIndex) {
    shuffle(jingles);
    songPool = songPool.concat(jingles);
    patternIndex = {};
    patternIndex[SONG] = [];
    patternIndex[JINGLE] = [];
    patternIndex[MODERATION] = [];
    patternIndex["news"] = [];
    for (var i = 0; i < tagPattern.length; i++) {
      if (!(tagPattern[i] in patternIndex)) {
        patternIndex[tagPattern[i]] = [];
      }
    }
    patternIndexPtr = indexTracks(songPool, patternIndex, 0);
    var playlistLen = 0;
    while (playlistLen < duration * 1000 && patternFailed < tagPattern.length) {
      patternFailed++;
      var selection = selectFromPatternIndex();
      if (selection.index > -1) {
        var track = songPool[selection.candidates[selection.index]];
        var nextIsJingle = track.type === JINGLE;
        var nextIsShortTrack = track.type != SONG && track.duration < 60 && !("linked" in track);
        var lastIsLinked = playlistTracks.length > 0 && "linked" in playlistTracks[playlistTracks.length - 1];
        var result;
        if (!lastIsLinked) {
          result = insertScheduledEvents(nextIsJingle, nextIsShortTrack);
        } else {
          result = { tracksAdded: false, skipJingle: false };
        }
        playlistLen = time - startTime;
        if (!result.skipJingle && result.tracksAdded) {
          var selectionNew = selectFromPatternIndex();
          if (selectionNew.index > -1) {
            track = songPool[selectionNew.candidates[selectionNew.index]];
            selection = selectionNew;
          }
        }
        if (!result.skipJingle) {
          playlistLen += addTrackToPlaylist(playlistTracks, track);
        }
        songPool.push(track);
        songPool[selection.candidates[selection.index]] = null;
        selection.candidates.splice(selection.index, 1);
        patternFailed = 0;
      }
      tagPatternPtr = (tagPatternPtr + 1) % tagPattern.length;
    }
  }
  if (playlistTracks.length == 0) {
    var playlistLen = 0;
    while (playlistLen < duration * 1000 && songPoolIdx < songPool.length) {
      while (songPoolIdx < songPool.length && songPool[songPoolIdx] == null) songPoolIdx++;
      if (songPoolIdx >= songPool.length) break;
      var selectedIdx = selectFromSongPool();
      if (selectedIdx == -1) break;
      var track = songPool[selectedIdx];
      var lastIsLinked = playlistTracks.length > 0 && "linked" in playlistTracks[playlistTracks.length - 1];
      var nextIsJingle = track.type === JINGLE;
      var nextIsShortTrack = track.type != SONG && track.duration < 60 && !("linked" in track);
      var result;
      if (!lastIsLinked) {
        result = insertScheduledEvents(nextIsJingle, nextIsShortTrack);
      } else {
        result = { tracksAdded: false, skipJingle: false };
      }
      if (!result.skipJingle && result.tracksAdded) {
        selectedIdx = selectFromSongPool();
        if (selectedIdx == -1) break;
        track = songPool[selectedIdx];
      }
      if (!result.skipJingle) {
        addTrackToPlaylist(playlistTracks, track);
      }
      songPool[selectedIdx] = null;
    }
  }
  return playlistTracks;
});
