// StationAdmin v3.0.12
// 31.01.2026
( function( tracks, opts, trackStats ){
  
  var duration = 'duration' in opts && opts.duration < 64800 ? opts.duration : 64800;
  var blockLength = 'blockLength' in opts ? opts.blockLength : (duration / 3600) + 1;
  var maxTracksPerArtist = 'maxTracksPerArtist' in opts && opts.maxTracksPerArtist < Math.floor(duration / (60 * 60)) ? opts.maxTracksPerArtist : Math.floor(duration / (60 * 60)); 
  var tagWeights = 'tagWeights' in opts ? opts.tagWeights : null;
  var artistSeparators = 'artistSeparators' in opts ? opts.artistSeparators : [' feat'];
  var artistAliases = null;
  var wordDistribution = 'wordDistribution' in opts ? opts.wordDistribution : 'random';
  var preserveAllJingles = 'preserveAllJingles' in opts ? opts.preserveAllJingles : 0;
  var avoidRepeat = 'avoidRepeat' in opts ? opts.avoidRepeat : 2;
  var excludePreviousTracks = 'excludePreviousTracks' in opts ? opts.excludePreviousTracks : 0;
  var trackNameLimit = 'trackNameLimit' in opts ? opts.trackNameLimit : 0;
  var adPositions = 'adPositions' in opts && opts.adPositions.length > 1 ? opts.adPositions : [15, 45];
  var adJingleCollisionStrategy = 'adJingleCollisionStrategy' in opts ? opts.adJingleCollisionStrategy : 'keep_both';
  var trackRulesEnabled = 'trackRules' in opts;
  var schedulingRulesEnabled = 'scheduled' in opts;
  var newsInterval= 'newsInterval' in opts ? opts.newsInterval : 60;
  var newsMin = 'newsMin' in opts ? opts.newsMin : 59;
  var newsMax = 'newsMax' in opts ? opts.newsMax : 15;
  var firstJingleAfterNews = 'firstJingleAfterNews' in opts ? opts.firstJingleAfterNews : true;
  var tagSequenceRules = 'tagSequences' in opts ? opts.tagSequences : [];
  var tagPattern = 'tagPattern' in opts ? opts.tagPattern : [];
  var tagPatternPtr = 0;
  var jinglesInsertedByPattern = false;
  var patternTags = {};
  var tagTracks = {};

  var firstJingle;
  var adTrigger;
  var adSeparator;
  var artists = [];
  var jingles = [];
  var lastPlays = {};
  var lastStartedAt = {};
  var recentArtists = {}; // last artists of history / previous iteration
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

  var debug = 'debug' in opts ? opts.debug : false;

  function log(msg) {
    if(debug) console.log(msg);
  }
  
  // basic array shuffle function
  // source: http://stackoverflow.com/questions/6274339/how-can-i-shuffle-an-array-in-javascript
  function shuffle( a ) {
    partialShuffle(a, a.length);
  }

  function partialShuffle( a, len ) {
    var j, x, i;
    for ( i = len; i; i-- ) {
      j        = Math.floor( Math.random() * i );
      x        = a[i - 1];
      a[i - 1] = a[j];
      a[j]     = x;
    }
  }
  
  
  function normalizeArtist(artistName) {
    if(artistName == null) {
      return "<no artist>";
    }
    artistName = artistName.toLowerCase();
    if(artistAliases != null && artistName in artistAliases) {
      artistName = artistAliases[artistName];
    }
    for(var i = 0; i < artistSeparators.length; i++) {
      var pos = artistName.indexOf(artistSeparators[i]);
      if(pos > 1) {
        var old = artistName;
        artistName = artistName.substring(0, pos).trim();
      }
    }
    if(artistAliases != null && artistName in artistAliases) {
      artistName = artistAliases[artistName];
    }
    return artistName;
  }
  
  function normalizeTitle(name) {
    if(name == null) {
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

    const fromDay = +parts[1], fromMonth = +parts[2];
    const toDay = parts[3] ? +parts[3] : fromDay;
    const toMonth = parts[4] ? +parts[4] : fromMonth;

    const now = new Date(startTime);
    const year = now.getFullYear();

    let fromDate = new Date(year, fromMonth - 1, fromDay, 0, 0, 0, 0);
    let toDate   = new Date(year, toMonth - 1, toDay, 23, 59, 59, 999);
    // handle wrap into next year
    if (toDate < fromDate) {
      if (now < fromDate) {
        fromDate.setFullYear(year - 1);
      }
      else {
        toDate.setFullYear(year + 1);
      }
    }

    const inRange = now >= fromDate && now <= toDate;
    const result = inRange ? 1 : -1;
    dateTagCache[tag] = result;

    // console.log("result: " + tag + " " + result);
    
    return result;
  }

  function isExcludedByDateTag(track) {
    var dateTagState = 0;
    if(track.tags.length > 0) {
      for(var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
      }
    }
    return dateTagState == -1;
  }
    
  function assignTrackScore(track) {
    // assign random score
    track.score = 100 + Math.floor((Math.random() * 500));
    var dateTagState = 0;
    if(tagWeights != null && track.tags.length > 0) {
      // increase / decrease score based on tag weights
      var minWeight = 0;
      var maxWeight = 0;
      for(var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
        if(track.tags[i] in tagWeights) {
          var w = tagWeights[track.tags[i]];
          if(w > 0 && w > maxWeight) {
            maxWeight = w;
          }
          else if(w < 0 && w < minWeight) {
            minWeight = w;
          }
        }
      }
      if(minWeight < -3 || dateTagState == -1) {
        // not at all
        track.score = 999999;
        return;
      }
      var weight = maxWeight + minWeight;
      if(weight > 0) {
        // reduce score - prefer track
        var p = (4 - weight) / 4;
        track.score = track.score * p;
      }
      else if(weight < 0) {
        // increase score
        weight = Math.abs(weight);
        var p = 1 + (weight/ 4);
        track.score = track.score * p;
      }
    }
    else {
      for(var i = 0; i < track.tags.length; i++) {
        dateTagState = checkDateTag(track.tags[i], dateTagState);
      }
      if(dateTagState == -1) {
        // not at all
        track.score = 999999;
        return;
      }
    }
    if (track.id in lastPlays && lastPlays[track.id] < 60 * avoidRepeat) {
      if(excludePreviousTracks) {
        track.score = 999999;
      }
      else {
        var penalty = 500 - 250 * lastPlays[track.id] / (60 * avoidRepeat);
        track.score += penalty;
        track.penalty = Math.floor(penalty / 50);
      }
    }
    else {
      track.penalty = 0;
    }
  }
  
  function initTracksAndArtists(remainingDuration, iteration) {
    var artistMap = {};
    var tracksDuration = 0;

    var start = 0;

    // check for jingle-news pattern at the beginning
    if(tracks.length > 2 && (tracks[0].type == 'news') || (tracks[0].type == 'jingle' && tracks[1].type == 'news')) {
      if(tracks[0].type == 'jingle') {
        preNewsJingle = tracks[0];
        start++;
      }
      newsTrack = tracks[start];
      start++;
      if(firstJingleAfterNews && tracks[start].type == 'jingle') {
        firstJingle = tracks[start];
        start++;
      }
    }

    var excludeFollowing = false;

    for(var i = start; i < tracks.length; i++) {

      if(tracks[i].id == 1) {
        newsTrack = tracks[i];
        continue;
      }
      if((tracks[i].title != null && tracks[i].title.indexOf('START_AD_BREAK') > -1) ||
        (tracks[i].artist != null && tracks[i].artist.indexOf('START_AD_BREAK') > -1)) {
        adTrigger = tracks[i];
        continue;
      }
      if((trackRulesEnabled || schedulingRulesEnabled) && tracks[i].id in boundTracks) {
        // only inserted by track rule
        if(!isExcludedByDateTag(tracks[i])) {
          boundTracks[tracks[i].id] = tracks[i];
        }
        continue;
      }
      if(schedulingRulesEnabled) {
        var skip = false;
        for(var t = 0; t < tracks[i].tags.length; t++) {
          if(tracks[i].tags[t] in selectorTags) {
            for(var r = 0; r < selectorTags[tracks[i].tags[t]].length; r++) {
              var rule = selectorTags[tracks[i].tags[t]][r];
              if(!('tracks' in rule)) {
                rule.tracks = [];
                rule.trackIdxs = [];
              }
              if('exclude' in rule) skip = true;
              if(!isExcludedByDateTag(tracks[i])) {
                rule.tracks.push(tracks[i]);
                rule.trackIdxs.push(i);
              }
            }
          }
        }
        if(skip) continue;
      }
      if(tracks[i].type == 'jingle') {
        if(tracks[i].id == 8664493) {
          excludeFollowing = true;
          continue;
        }
        if(iteration == 0 && !isExcludedByDateTag(tracks[i])) {
          if(tracks[i].id == 0 || tracks[i].id == opts.adTrigger) {
            adTrigger = tracks[i];
          }
          else if(tracks[i].id == opts.adSeparator) {
            adSeparator = tracks[i];
          }
          else if(preserveAllJingles) {
            preservedTracks[i - start] = tracks[i];
            hasPreservedTracks = true;
          }
          else if((i == 0 || (i == 1 && tracks[0].id == 1)) && 'protectFirstJingle' in opts && opts.protectFirstJingle) {
            firstJingle = tracks[i];
          }
          else  {
            jingles.push(tracks[i]);
          }
        }
        continue;
      }
      else if(tracks[i].type == 'moderation') {
        if(wordDistribution == 'preserve' && iteration == 0) {
          preservedTracks[i - start] = tracks[i];
          hasPreservedTracks = true;
          continue;
        }
        else if(wordDistribution == 'link_next' && i < tracks.length - 1 && iteration == 0) {
          hasLinkedTracks = true;
          tracksBefore[tracks[i+1].id] = tracks[i];
          continue;
        }
        else if(wordDistribution == 'link_previous' && i > 0  && iteration == 0) {
          hasLinkedTracks = true;
          tracksAfter[tracks[i-1].id] = tracks[i];
          continue;
        }
      }

      if(excludeFollowing) continue;
      
      tracksDuration += tracks[i].duration;
      tracks[i].use = false;
      tracks[i].groupTags = [];
      
      if(trackNameLimit > 0) {
        tracks[i].normTitle = normalizeTitle(tracks[i].title);
        tracks[i].groupTags = tracks[i].tags.filter(t => t.startsWith("=")); 
        tracks[i].groupTags.push(tracks[i].normTitle); 
      }
      
      assignTrackScore(tracks[i]);
      if(tracks[i].score > 10000) {
        // excluded
        continue;
      }
      
      var artistName = normalizeArtist(tracks[i].artist);
      tracks[i].artistNormalized = artistName;
      var artist;
      if(artistName in artistMap) {
        artist = artistMap[artistName];
        if(tracks[i].score < artist.score) {
          artist.score = tracks[i].score;
        }
      }
      else {
        artist = {};
        artist.name = artistName;
        artist.tracks = [];
        artist.score = tracks[i].score;
        artistMap[artistName] = artist;
        artists.push(artist);
      }
      artist.tracks.push(tracks[i]);
    }
    
    for(var i = 0; i < artists.length; i++) {
      artists[i].tracks.sort(function(a, b) { return a.score - b.score });
    }
    artists.sort(function(a, b) { return a.score - b.score });
    
    if(remainingDuration / (60 * 60) < maxTracksPerArtist) {
      maxTracksPerArtist = Math.max(1, Math.floor(remainingDuration / (60 * 60)));
    }
    
    var tracksDurationHours = Math.floor(tracksDuration / (60 * 60));
    if(tracksDurationHours < maxTracksPerArtist && tracksDurationHours > 0) {
      // iteration will produce shorter list that required - need to set a stricter limit
      maxTracksPerArtist = tracksDurationHours < 3 ? tracksDurationHours : tracksDurationHours - 1;
    }
    
    var candidates = [];
    for(var i = 0; i < artists.length; i++) {
      for(var j = 0; 
                j < artists[i].tracks.length 
                && (j < maxTracksPerArtist || (tagPattern.length > 0 && artists[i].tracks[j].type == 'moderation')); 
                j++) {
        candidates.push(artists[i].tracks[j]);
      }
    }
    candidates.sort(function(a, b) { return a.score - b.score });
    var cDuration = 0;
    var cIdx = 0;
    while((tagPattern.length > 0 || cDuration < remainingDuration) && cIdx < candidates.length) {
      cDuration += candidates[cIdx].duration;
      candidates[cIdx].use = true;
      candidates[cIdx].plays = 0;
      cIdx++;
    }

    // console.console.log(tracks.length + " tracks, " + candidates.length + " candidates, " + cIdx + " preselected, " + (cDuration / (60 * 60) + " hours"));
  }
  
  /* tag pattern - start */

  function indexTracks(tracks, patternIndex, start) {
    var i;
    for(i = start; i < tracks.length; i++) {
      var track = tracks[i];
      for(var t = 0; t < track.tags.length; t++) {
        if(track.tags[t] in patternTags) {
          patternIndex[track.tags[t]].push(i);
        }
      }
      patternIndex[track.type].push(i);
    }
    // console.log("indexed tracks " + start + " => " + i);
    return i;
  }
  
  function applyTagPattern(tracks, duration) {
    var playlist = [];
    // console.log("applyTagPattern " + tracks.length + " / " +  duration);
    shuffle(jingles);
    tracks = tracks.concat(jingles);
    
    var patternIndex = {};
    patternIndex["song"] = [];
    patternIndex["jingle"] = [];
    patternIndex["moderation"] = [];
    patternIndex["news"] = [];
    for(var i = 0; i < tagPattern.length; i++) {
      if(!(tagPattern[i] in patternIndex)) {
        patternIndex[tagPattern[i]] = [];
      }
    }
    var patternIndexPtr = indexTracks(tracks, patternIndex, 0);

    var artistBlocked = {};
    var recentTrackNames = [];

    var playlistLen = 0;
    var failed = 0;
    var matchingRules = [];
    while(playlistLen < duration && failed < tagPattern.length) {
      failed++;
      var candidates = patternIndex[tagPattern[tagPatternPtr]];
      // console.log("next: " + tagPattern[tagPatternPtr] + " - " + candidates.length + " / " + tracks.length);
      var bestIdx = -1;
      var bestPenalty = 9999;
      var penalty = 0;
      if(candidates.length == 0 && patternIndexPtr < tracks.length) {
        patternIndexPtr = indexTracks(tracks, patternIndex, patternIndexPtr);
      }
      var checkedMatches = 0;
      for(var cIdx = 0; cIdx < candidates.length; cIdx++)
      {
        var track = tracks[candidates[cIdx]];
        if(track != null) { 
          var artistCheck = track.type == 'song' && 'artistNormalized' in track && track.artistNormalized in artistBlocked ? playlistLen > artistBlocked[track.artistNormalized] : true;
          if(artistCheck) {
            penalty = 0;
            checkedMatches++;
            if(track.type == 'song') {
              for(var r = 0; r < matchingRules.length; r++) {
                var result = track.tags.includes(matchingRules[r].next);
                if((matchingRules[r].not && result) ||(!matchingRules[r].not && !result))  {
                  penalty++;
                }
              }
              if(track.groupTags.some(t => recentTrackNames.some(r => r.includes(t)))) {
                penalty += 3;
              }
            }
            if(penalty < bestPenalty) {
              bestPenalty = penalty;
              bestIdx = cIdx;
            }
            if(penalty == 0 || checkedMatches == 5) {
              break;
            }  // otherwise: check for better matches
          }
        }
        if(cIdx == candidates.length - 1 && patternIndexPtr < tracks.length) {
          patternIndexPtr = indexTracks(tracks, patternIndex, patternIndexPtr);
        }
      }
      if(bestIdx > -1) {
        var track = tracks[candidates[bestIdx]];
        playlist.push(track);
        playlistLen += track.duration;
        tracks.push(track); 
        tracks[candidates[bestIdx]] = null;
        candidates.splice(bestIdx, 1);
        failed = 0;
        if(track.type == 'song' && 'artistNormalized' in track) {
          artistBlocked[track.artistNormalized] = playlistLen + 60 * 60;
          matchingRules = checkTagSequenceRules(track);
          if(trackNameLimit > 0) {
            recentTrackNames.push(track.groupTags);
            if(recentTrackNames.length > trackNameLimit) {
              recentTrackNames.shift();
            } 
          }
        }
        jinglesInsertedByPattern = jinglesInsertedByPattern || track.type == 'jingle';
        track.plays++;
        if(track.plays > 1) {
          // console.log("repeat " + track.artist + " - " + track.title + ": " + track.plays);
        }
      }
      tagPatternPtr = (tagPatternPtr + 1) % tagPattern.length;
    }
    
    return playlist.length > 0 ? playlist : tracks;
  }

  /* tag pattern - end */

  function checkTagSequenceRules(track) {
    var matchingRules = [];
    for(var r = 0; r < tagSequenceRules.length; r++) {
      var rule = tagSequenceRules[r];
      if(rule.pattern != null && track.tags.includes(rule.pattern[rule.index])) {
        rule.index++;
        if(rule.index == rule.pattern.length) {
          rule.index = 0;
          matchingRules.push(rule);
        }
      }
      else {
        rule.index = 0;
      }
    }
    return matchingRules;
  }
  
  function buildPlaylist(duration) {
    var numSegments = maxTracksPerArtist * 2;
    
    var segments = [];
    for(var i = 0; i < numSegments; i++) {
      var segment = {};
      segment.tracks = [];
      segment.duration = 0;
      segments.push(segment);
    }
    
    for(var i = 0; i < artists.length; i++) {
      var artist = artists[i];
      var artistTracks = [];
      for(var j = 0; j < artist.tracks.length; j++) {
        if(artist.tracks[j].use) {
          artistTracks.push(artist.tracks[j]);
        }
      }
      
      if(artistTracks.length == 0) {
        continue;
      }
      
      var artistSegments = Math.max(1, Math.floor(numSegments / artistTracks.length));

      // find least filled segment that can act as first segment for this artist
      var minSegment = artist.name in recentArtists ? 1 : 0;
      var currentSegment = minSegment;
      var minDuration = segments[0].duration;
      for(var s = minSegment + 1; s < artistSegments; s++) {
        if(segments[s].duration < minDuration) {
          currentSegment = s;
          minDuration = segments[s].duration;
        }
      }
          
      // assign tracks of artist to segments
      for(var t = 0; t < artistTracks.length; t++) {
        segments[currentSegment].tracks.push(artistTracks[t]);
        segments[currentSegment].duration += artistTracks[t].duration;
        currentSegment = (currentSegment + artistSegments) % segments.length;
      }
    }
    
    var playlistTracks = [];
    var recentTrackNames = [];
    var buffer = [];
  var segmentTargetDuration = Math.floor(duration / segments.length);
    for(var s = 0; s < segments.length; s++) {
      var segmentTracks = segments[s].tracks;
    if(tagPattern.length == 0) {
      shuffle(segmentTracks);
    }
    else {
    var avgTrackLenth = Math.floor(segments[s].duration / segmentTracks.length);
    var numTracks = Math.floor(segmentTargetDuration / avgTrackLenth);
    partialShuffle(segmentTracks, Math.min(segmentTracks.length, numTracks));
    }
      segmentTracks.sort(function(a, b) { return a.penalty - b.penalty });
      
      for(var t = 0; t < segmentTracks.length; t++) {
        playlistTracks.push(segmentTracks[t]);
      }
    }

    for(var r = 0; r < tagSequenceRules.length; r++) {
      tagSequenceRules[r].index = 0;
    }
    
    if(tagPattern.length > 0) {
      playlistTracks = applyTagPattern(playlistTracks, duration);
    }
    // apply tag sequence rules
    else if(tagSequenceRules.length > 0 || trackNameLimit > 0) {
      for(var t = 1; t < playlistTracks.length ; t++) {
        var matchingRules = checkTagSequenceRules(playlistTracks[t - 1]);
        if(matchingRules.length > 0 || trackNameLimit > 0) {
          for(var t2 = t; t2 < playlistTracks.length && t2 < t + 5; t2++) {
            var check = playlistTracks[t2];
            var accept = true;
            if(trackNameLimit > 0 && check.groupTags.some(t => recentTrackNames.some(r => r.includes(t)))) {
              accept = false;
            }
            for(var r = 0; r < matchingRules.length && accept; r++) {
              var result = check.tags.includes(matchingRules[r].next);
              if((matchingRules[r].not && result) ||(!matchingRules[r].not && !result))  {
                accept = false;
              }
            }
            if(accept) {
              if(t2 > t) { 
                // swap
                playlistTracks[t2] = playlistTracks[t];
                playlistTracks[t] = check;
              }
              break;
            }
          }
          if(trackNameLimit > 0) {
            recentTrackNames.push(playlistTracks[t].groupTags);
            if(recentTrackNames.length > trackNameLimit) {
              recentTrackNames.shift();
            } 
          }
        }
      }
    }

    // insert protected tracks
    if(hasPreservedTracks) {
      var newTracks = [];
      for(var i = 0; i < playlistTracks.length; i++) {
        while(typeof preservedTracks[newTracks.length] != 'undefined') {
          newTracks.push(preservedTracks[newTracks.length]);
        }
        newTracks.push(playlistTracks[i]);
      }
      playlistTracks = newTracks;
    }
    
    return playlistTracks;
  }
  
  function insertJingles(playlistTracks) {
    var addFirstJingle = firstJingle != null && !startsWithNews;
    if(!addFirstJingle && jingles.length == 0) {
      // nothing to do
      return playlistTracks;
    }
    else if(addFirstJingle && (jingles.length == 0  || jinglesInsertedByPattern)) {
      // just insert first jingle 
      playlistTracks.splice(0, 0, firstJingle);
      return playlistTracks;
    }
        else if(jinglesInsertedByPattern) {
            return playlistTracks
        }
    
    var jingleOrder = 'shuffle';
    if('jingleOrder' in opts) {
      jingleOrder = opts.jingleOrder;
    }
    if(jingleOrder != 'preserve') {
      shuffle(jingles);
    }
    
    var newTracks = [];
    
    var timeNextJingle = 0;
    var jingleOffset = 0;
    var jingleInterval = 0;
    if('jingleInterval' in opts) {
      jingleInterval = opts.jingleInterval;
    }
    if(jingleInterval == 0) {
      var numJingles = firstJingle != null ? jingles.length + 1 : jingles.length;
      jingleInterval = Math.floor((duration / numJingles) / 60);
    }
    
    if(addFirstJingle) {
      newTracks.push(firstJingle);
      timeNextJingle = jingleInterval;
      jingleOffset = jingleInterval;
    }
    else {
      if(lastJinglePlay > -1) {
        jingleOffset = Math.max(0, jingleInterval - lastJinglePlay);
      }
      else {
        jingleOffset = Math.floor((Math.random() * jingleInterval));
      }
      timeNextJingle = jingleOffset;
    }
    
    var jingleIdx = 0;
      var jingleCnt = 0;
      var currentTimeSec = 0;
      
      for(var i = 0; i < playlistTracks.length; i++) {
        if(currentTimeSec / 60 >= timeNextJingle) {
          newTracks.push(jingles[jingleIdx]);
          currentTimeSec += jingles[jingleIdx].duration;
          jingleIdx++;
          if(jingleIdx == jingles.length) {
            jingleIdx = 0;
            if(jingleOrder == 'shuffle_repeat') {
              shuffle(jingles);
            }
          }
          jingleCnt++;
            timeNextJingle = jingleCnt * jingleInterval + jingleOffset;
        }
        newTracks.push(playlistTracks[i]);
        currentTimeSec += playlistTracks[i].duration;
      }
    return newTracks;
  }

  function normalizeTerm(term) {
    term = term.toLowerCase();
    return term.replace(/\W/g, "");
  }

  function isBoundTo(track, rule) {
    if(rule.filterType == 'tag') {
      return track.tags.includes(rule.filter);
    }
    if(!('term' in rule)) {
      rule.term = normalizeTerm(rule.filter);
    }

    switch(rule.filterType) {
      case 'artist':
        return normalizeTerm(track.artist).includes(rule.term);
      case 'title':
        return normalizeTerm(track.title).includes(rule.term);
        break;
      case 'artist_title':
        return normalizeTerm(track.artist + " " + track.title).includes(rule.term);
      default:
        break;
    }

    return false;
  }

  function filterApplicableRules(rules) {
    // console.log("filter applicable rules " + rules.length);
    var rulesByGroup =  {};
    var groupNames = [];
    for(var i = 0; i < rules.length; i++) {
      var rule = rules[i];
      var groupName = rule.groupName != null ? rule.groupName  : "-";
      if(!(groupName in rulesByGroup)) {
        rulesByGroup[groupName] = [];
        groupNames.push(groupName);
      }
      rulesByGroup[groupName].push(rule);
    }
    if(groupNames.length > 1 && opts.trackRuleGroupCollisionStrategy != 'all') {
      var idx = opts.trackRuleGroupCollisionStrategy == 'first' ? 0 : Math.floor(Math.random() * groupNames.length);
      var selectedGroupName = groupNames[idx];
      groupNames = [];
      groupNames.push(selectedGroupName);
    }

    var filtered = [];
    for(var g = 0;  g < groupNames.length; g++) {
      var group = opts.trackRuleGroups[groupNames[g]];
      if(group == null || group.multiMatchSelection == 'all' || rulesByGroup[groupNames[g]].length == 1) {
        filtered = filtered.concat(rulesByGroup[groupNames[g]]);
      } else if( group.multiMatchSelection == 'first') {
        filtered.push(rulesByGroup[groupNames[g]][0]);
      } else {
        // select any
        var idx = Math.floor(Math.random() *  rulesByGroup[groupNames[g]].length);
        filtered.push(rulesByGroup[groupNames[g]][idx]);
      }
    }
    return filtered;
  }

  function markRuleApplied(rule, time) {
    // console.log("insert " + boundTracks[rule.trackId].title + " at "  + new Date(time));
    rule.lastPlay = time;
    if(rule.groupName in opts.trackRuleGroups) {
      opts.trackRuleGroups[rule.groupName].lastPlay = time;
    }
    return time + boundTracks[rule.trackId].duration * 1000;
  }

  function applyTrackRules(playlistTracks) {
    var newTracks = [];
    var currentTime = baseTime;
    
    // check rules - do we have the bound track?
    for(var i = 0; i < opts.trackRules.length; i++) {
      opts.trackRules[i].active = 'type' in  boundTracks[opts.trackRules[i].trackId];
      // console.log(opts.trackRules[i].active + " " + opts.trackRules[i].trackId);
    }
    
    
    for(var i = 0; i < playlistTracks.length; i++) {
      if(!('boundTo' in playlistTracks[i])) {
         playlistTracks[i].boundTo = [];
         for(var r = 0; r < opts.trackRules.length; r++) {
           if(opts.trackRules[r].active && isBoundTo(playlistTracks[i], opts.trackRules[r])) {
             playlistTracks[i].boundTo.push(r);
             // console.log(playlistTracks[i].artist + " bound");
           }
         }
      }
      // else: repeated track, no need to identify rules again

      var jinglesAfter = [];
      var skipNext = false;
      if(playlistTracks[i].boundTo.length > 0) {
        var applicableRules = [];
        // filter for applicable rules
        for(var r = 0; r < playlistTracks[i].boundTo.length; r++) {
          var rIdx = playlistTracks[i].boundTo[r];
          var group = opts.trackRuleGroups[opts.trackRules[rIdx].groupName];
          var ruleTimeMatch = currentTime - opts.trackRules[rIdx].lastPlay > opts.trackRules[rIdx].minDistance * 60000;
          var groupTimeMatch = group == null || !('lastPlay' in group) || currentTime - group.lastPlay > group.minDistance * 60000;
          if(ruleTimeMatch && groupTimeMatch) {
            applicableRules.push(opts.trackRules[rIdx]);
          }
        }

        if(applicableRules.length > 1) {
          applicableRules = filterApplicableRules(applicableRules);
        }

        if(applicableRules.length > 0) {
          var previousTrack = newTracks.length > 0 ? newTracks[newTracks.length - 1] : null;
          var lastIsJingle = previousTrack != null && previousTrack.type == 'jingle';
          var nextIsJingle = i < playlistTracks.length -1 && playlistTracks[i+1].type == 'jingle';
          for(var r = 0; r < applicableRules.length; r++) {
            var rule = applicableRules[r];
            var isJingle = boundTracks[rule.trackId].type == 'jingle'; 
            if(rule.position == 'before') {
              if(isJingle && lastIsJingle) {
                switch(opts.trackRuleJingleCollisionStrategy) {
                  case 'keep_both':
                    newTracks.push(boundTracks[rule.trackId]);
                    newTracks[newTracks.length - 1].linked = true;
                    currentTime = markRuleApplied(rule, currentTime);
                    break;
                  case 'keep_rule_jingle':
                    if(!opts.protectFirstJingle || newTracks.length > 1) {
                      newTracks.splice(newTracks.length -1, 1);
                    }
                    newTracks.push(boundTracks[rule.trackId]);
                    newTracks[newTracks.length - 1].linked = true;
                    currentTime = markRuleApplied(rule, currentTime);
                    break;
                  case 'keep_standard_jingle':
                    // preserve added jingle, don't add rule jingle
                    break;
                }
              }
              else {
                // no jingle collision - just add
                newTracks.push(boundTracks[rule.trackId]);
                newTracks[newTracks.length - 1].linked = true;
                currentTime = markRuleApplied(rule, currentTime);
              }
            }
            else {
              if(isJingle && nextIsJingle) {
                switch(opts.trackRuleJingleCollisionStrategy) {
                  case 'keep_both':
                    jinglesAfter.push(boundTracks[rule.trackId]);
                    markRuleApplied(rule, currentTime);
                    break;
                  case 'keep_rule_jingle':
                    jinglesAfter.push(boundTracks[rule.trackId]);
                    markRuleApplied(rule, currentTime);
                    skipNext = true;
                    break;
                  case 'keep_standard_jingle':
                    // preserve added jingle, don't add rule jingle
                    break;
                }
              }
              else {
                        // no jingle collision - just add
                jinglesAfter.push(boundTracks[rule.trackId]);
                markRuleApplied(rule, currentTime);
              }
            }
          }
        }

      }

      newTracks.push(playlistTracks[i]);
      currentTime += playlistTracks[i].duration * 1000;
      for(var j = 0; j < jinglesAfter.length; j++) {
        newTracks[newTracks.length - 1].linked = true;
        newTracks.push(jinglesAfter[j]);
        currentTime += jinglesAfter[j].duration * 1000;
      }

      if(skipNext) {
        i++;
      }
    }

    return newTracks;
  }
  
  function insertAdTriggers(playlistTracks) {
    var newTracks = [];

    var position1 = adPositions[0];
    var position2 = adPositions[1];
    
      var diff = position2 - position1;
      if (diff < 20 && diff > 40) {
        if (position1 > 30) {
          position1 = 30;
        }
        if (diff < 20) {
          position2 = position1 + 20;
        } else if (diff > 40) {
          position2 = position1 + 40;
        }
      }

      var currentPosition = 0;
      var adCnt = 0;
      var nextAdPosition = position1 * 60;

      var allowMove = true;
      for (var i = 0; i < playlistTracks.length; i++) {
        
        var addTrigger = currentPosition > nextAdPosition;
        var addTrack = true;

        if (addTrigger && adJingleCollisionStrategy != 'keep_both') {
          var lastIsJingle = newTracks.length > 0 && newTracks[newTracks.length - 1].type == 'jingle';
          var nextIsJingle = playlistTracks[i].type == 'jingle';
          if (lastIsJingle || nextIsJingle) {
            if (adJingleCollisionStrategy == 'move_adtrigger') {
                if (allowMove) {
                  // prevent adding trigger for now and move next position 60 seconds ahead
                  addTrigger = false;
                  nextAdPosition += 60;
                    allowMove = false;
                }
              } else if (adJingleCollisionStrategy == 'remove_jingle') {
                if (lastIsJingle) {
                    newTracks.splice(newTracks.length - 1, 1);
                }
                if (nextIsJingle) {
                    addTrack = false; // block adding of next track (which is a jingle)
                }
              }
          }
        }

        if (addTrigger) {
        if (adSeparator != null) {
          currentPosition += adSeparator.duration;
            newTracks.push(adSeparator);
        }
        currentPosition +=  adTrigger.duration;
        newTracks.push(adTrigger);
          
          adCnt++;
          var nextAdBase = adCnt % 2 == 0 ? position1 : position2;
          nextAdPosition = (nextAdBase * 60) + Math.floor(adCnt / 2) * 60 * 60;
          allowMove = true;
        }
        if (addTrack) {
          newTracks.push(playlistTracks[i]);
          currentPosition += playlistTracks[i].duration;
        }
      }
    if(currentPosition > nextAdPosition) {
        if (adSeparator != null) {
            newTracks.push(adSeparator);
        }
        newTracks.push(adTrigger);
    }
      
    return newTracks;
  }


  function insertLinkedTracks(playlistTracks) {
    var newTracks = [];
    for(var i = 0; i < playlistTracks.length; i++) {
      var id = playlistTracks[i].id;
      if(id in tracksBefore) {
        newTracks.push(tracksBefore[id]);
        newTracks[newTracks.length - 1].linked = true;
      }
      newTracks.push(playlistTracks[i]);
      if(id in tracksAfter) {
        newTracks[newTracks.length - 1].linked = true;
        newTracks.push(tracksAfter[id]);
      }
    }
    return newTracks;
  }
  
  function isInNewsTimeframe(minutes) {
    if(newsMax > newsMin) { // sth like 0 - 15 or 30 - 45
      return minutes >= newsMin && minutes <= newsMax;
    }
    else { // sth like 59 - 15
      var diff = 60 - newsMin;
      var m = (minutes + diff) % 60;
      return m >= 0 && m <= newsMax + diff;
    }
    
  }

  function scheduleNews() {
    var newsTracks = [];
    var jingleCollision = 'keep_both';
    if(preNewsJingle != null) {
      newsTracks.push(preNewsJingle);
      jingleCollision = 'remove_jingle';
    }
    newsTracks.push(newsTrack);
    newsTrack.duration = 165;
    if(firstJingle != null && firstJingleAfterNews) {
      newsTracks.push(firstJingle);
      jingleCollision = 'remove_jingle';
    }

    var ts = new Date();
    var time = startTime;
    var endTime = startTime + duration * 1000;
    var noNewsAfter = endTime - 15 * 60 * 1000;

    while(time < noNewsAfter) {
      ts.setTime(time);
      ts.setSeconds(0);
      if(isInNewsTimeframe(ts.getMinutes()) && ts.getTime() - lastNewsStarted > 1000 * 30 * 45) {
        if(time == startTime) startsWithNews = true;
        var scheduledNews = {};
        scheduledNews.tracks = newsTracks;
        scheduledNews.minTime = ts.getTime();
        var diff = ts.getMinutes() < newsMax ? newsMax - ts.getMinutes() : newsMax + 60 - ts.getMinutes();
        scheduledNews.maxTime = ts.getTime() + 1000 * 60 * diff; 
        scheduledNews.jingleCollision = jingleCollision;
        scheduledNews.type = 'news';
        scheduledTracks.push(scheduledNews);
        log("schedule news: " + ts.toLocaleString() + ", max = " + new Date(scheduledNews.maxTime).toLocaleString());

        time += newsInterval * 1000 * 60;
        if(ts.getMinutes() != newsMin) time -= 1000 * 60 * 15;
      }
      else {
        time += 1000 * 60;
      }
    }
  }

  function scheduleAdTriggerAt(tracks, time) {
    // console.log("schedule ad trigger: " + new Date(time).toUTCString());
    var scheduledTrigger = {};
    scheduledTrigger.tracks = tracks;
    scheduledTrigger.minTime = time;
    scheduledTrigger.maxTime = time + 1000 * 60 * 25;
    if(adJingleCollisionStrategy == 'move_adtrigger') {
      scheduledTrigger.jingleCollision = 'move';
    }
    else {
      scheduledTrigger.jingleCollision = adJingleCollisionStrategy;
    }
    scheduledTrigger.type = 'adTrigger';
    scheduledTracks.push(scheduledTrigger);
  }

  function scheduleAdTriggers() {
    // console.log("schedule ad triggers");
    var adTracks = [];
    if(adSeparator != null) {
      adTracks.push(adSeparator);
    }
    adTracks.push(adTrigger);

    var position1 = adPositions[0];
    var position2 = adPositions[1];
    
    var diff = position2 - position1;
    if (diff < 20 && diff > 40) {
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
    var endHour = startHour + (duration / 3600);

    ts.setSeconds(0);
    ts.setMilliseconds(0);

    for(var h = startHour; h <= endHour; h++) {
      ts.setMinutes(position1);
      // console.log(h + " => " + (h % 24) + " " + ts.toUTCString());
      if(ts.getTime() > startTime && ts.getTime() < endTime) {
        scheduleAdTriggerAt(adTracks, ts.getTime());
      }
      ts.setMinutes(position2);
      if(ts.getTime() > startTime && ts.getTime() < endTime) {
        scheduleAdTriggerAt(adTracks, ts.getTime());
      }
      // move one hour
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
    var endHour = startHour + (duration / 3600);
    var dayFilter = 'day' in rule ? rule.day : -1;

    var trackIdx = 0;
    var trackIdxInc = 1;
    var boundToNews = false;

    if(rule.selection == 'rotate') {
      // find last track that was played
      var maxTime = 0;
      for(var t = 0; t < rule.tracks.length - 1; t++) {
        if(rule.tracks[t].id in lastStartedAt && lastStartedAt[rule.tracks[t].id] > maxTime) {
          trackIdx = (t + 1) % rule.tracks.length;
          maxTime = lastStartedAt[rule.tracks[t].id];
        }
      }
    }
    else if(rule.selection == 'calculatedaily') {
      trackIdx = Math.floor(startTime / (1000 * 60 * 60 * 24)) % rule.tracks.length;
      trackIdxInc = 0;
      }
    else if(rule.selection == 'date') {
      var day = ts.getDate() < 10 ? "0" + ts.getDate() : "" + ts.getDate();
      var mon = ts.getMonth() < 10 ? "0" + (ts.getMonth() + 1) : "" + (ts.getMonth() + 1);
      var dateStr = day + "." + mon + ".";
      trackIdx = -1;
      for(var t = 0; t < rule.tracks.length; t++) {
        if(rule.tracks[t].title.includes(dateStr) || rule.tracks[t].album.includes(dateStr)) {
          trackIdx = t;
          break;
        }
      }
      trackIdxInc = 0;
    }
    else if(rule.selection == 'time') {
      rule.timeTracks = [];
      var r = /\d+/g;
      for(var t = 0; t < rule.tracks.length; t++) {
        var str = rule.tracks[t].title + " " + rule.tracks[t].album;
        var m;
        while((m = r.exec(str)) !== null) {
          var n = parseInt(m);
          if(!isNaN(n) && n >=0 && n < 24) {
            rule.timeTracks[n] = rule.tracks[t];
          }
        }
      }
      trackIdx = -2;
      trackIdxInc = 0;
    }
    else if(rule.selection == 'index') {
      trackIdx = rule.index -1 < rule.tracks.length ? rule.index -1 : -1;
      trackIdxInc = 0;
    } else {
      // random
      shuffle(rule.tracks);
    }
 
    if(trackIdx == -1) return; // no track found
    
    var hours = [];
    var minutes = [];
    minutes.push(rule.minute);
    if('hour' in rule) {
      if(rule.hour == -2) {
        rule.hour = (startHour + Math.floor(Math.random() * (duration / 3600))) % 24; // random
        rule.minute = Math.floor(Math.random() * 60); 
        minutes = [];
        minutes.push(rule.minute);
      }
      else if(rule.hour == -3 || rule.hour == -4) {
        boundToNews = true;
      }
      if(rule.hour > -1) {
        hours.push(rule.hour);
      }
    }
    else if('interval' in rule) {
      var step = rule.interval > 0 ? rule.interval : (rule.interval < 0 ? 1 : 99);
      for(var h = startHour; h <= endHour; h += step) {
        hours.push(h);
      }
      if(rule.interval < -1) {
        step = -rule.interval;
        for(var m = rule.minute + step; m < 60; m += step) {
          minutes.push(m);
        }
      }
    }
    
    for(var i = 0; i < hours.length; i++) {
      ts.setTime((hours[i] % 24) >= startHour ? startTime : startTime + 1000 * 60 * 60 * 24);
      ts.setHours(hours[i] % 24);
      ts.setSeconds(0);
      var acceptDay = dayFilter == -1 || dayFilter == ts.getDay() ||
          (dayFilter == -2 && ts.getDay() > 0 && ts.getDay() < 6) ||
          (dayFilter == -3 && (ts.getDay() == 0 || ts.getDay() == 6));
      if(!acceptDay) continue;

      for(var j = 0; j < minutes.length; j++) {
        ts.setMinutes(minutes[j]);
        if(ts.getTime() > executionTime && ts.getTime() < startTime + duration * 1000) {
          var tracks = [];
          if('introJingleId' in rule && rule.introJingleId in boundTracks && 'type' in boundTracks[rule.introJingleId]) {
            tracks.push(boundTracks[rule.introJingleId]);
          }
          var track = trackIdx > -1 ? rule.tracks[trackIdx] : (trackIdx == -2 ? rule.timeTracks[hours[i % 24]] : null);
          if(track == null || track == undefined) continue;
          log("schedule at " +ts.toLocaleString() + ": " + trackIdx + " of " + rule.tracks.length + " "  + track.title);
          tracks.push(track);
          lastStartedAt[track.id] = ts.getTime();
          trackIdx = (trackIdx + trackIdxInc) % rule.tracks.length;

          var scheduledElement = {};
          scheduledElement.tracks = tracks;
          scheduledElement.minTime = ts.getTime();
          scheduledElement.maxTime = ts.getTime() + 1000 * 60 * 15;
          scheduledElement.jingleCollision = 'keep_both';
          scheduledElement.type = 'rule';
          customScheduledElementCreate(rule, trackIdx, scheduledElement);
          scheduledTracks.push(scheduledElement);
        }
      }
    }
    if(boundToNews) {
      for(var i = 0; i < scheduledTracks.length; i++) {
        if(scheduledTracks[i].type != 'news') continue;
        var ts = new Date(scheduledTracks[i].minTime);
        var hour = ts.getMinutes() < 57 ? ts.getHours() : (ts.getHours() + 1) % 24;
        var track = trackIdx > -1 ? rule.tracks[trackIdx] : (trackIdx == -2 ? rule.timeTracks[hour] : null);
        if(track == null || track == undefined) continue;
        scheduledTracks[i].tracks = [...scheduledTracks[i].tracks];
        if(rule.hour == -3) { 
          // before news
          scheduledTracks[i].tracks.unshift(track);
        }
        else {
          // after news
          scheduledTracks[i].tracks.push(track);
        }
      }
    }
  }


  function scheduleByRules() {
    for(var i = 0; i < opts.scheduled.length; i++) {
      if('tracks' in opts.scheduled[i]) {
        scheduleByRule(opts.scheduled[i]);
      }
    }
  }


  function insertScheduledTracks(playlistTracks) {
    log("insert scheduled tracks");
    scheduledTracks.sort(function(a,b) { return  a.minTime - b.minTime });

    var sIdx = 0;
    var nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
    var moveCnt = 0;

    var newTracks = [];
    var time = startTime;
    var addTrack = true;

    for(var i = 0; i < playlistTracks.length; i++) {
      addTrack = true;
      var addScheduled = true;
      while(nextScheduled != null && time >= nextScheduled.minTime && addScheduled) {
        if(nextScheduled.jingleCollision != 'keep_both') {
          var lastIsJingle = newTracks.length > 0 && newTracks[newTracks.length - 1].type == 'jingle';
          var nextIsJingle = playlistTracks[i].type == 'jingle';
          if (lastIsJingle || nextIsJingle) {
            if (nextScheduled.jingleCollision == 'move') {
              if (moveCnt < 2) {
                // prevent adding trigger for now and move next position 60 seconds ahead
                addScheduled = false;
                moveCnt++;
              }
            } else if (nextScheduled.jingleCollision == 'remove_jingle') {
              if (lastIsJingle) {
                time -= newTracks[newTracks.length - 1].duration;
                newTracks.splice(newTracks.length - 1, 1);
              }
              if (nextIsJingle) {
                addTrack = false; // block adding of next track (which is a jingle)
              }
            }
          }
        }
        else if(i > 0 && 'linked' in playlistTracks[i-1]) {
          log("push back - linked track");
          addScheduled = false;
          moveCnt++;
        }
        else if(i > 0 && nextScheduled.type == 'news' && playlistTracks[i].type != 'song' && playlistTracks[i].duration < 60 && moveCnt == 0 && !('linked' in playlistTracks[i])) {
          // play short jingle or word track first
          addScheduled = false;
          moveCnt++;
        }

        if(addScheduled) {
          log(new Date(time).toLocaleString() + " for " + new Date(nextScheduled.minTime).toLocaleString() + " / " + nextScheduled.type);
          for(var t = 0; t < nextScheduled.tracks.length; t++) {
            newTracks.push(nextScheduled.tracks[t]);
            log(nextScheduled.tracks[t].title + " " + nextScheduled.tracks[t].duration);
            time += nextScheduled.tracks[t].duration * 1000;
          }
          nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
          moveCnt = 0;
        }
        else if(time > nextScheduled.maxTime) {
          nextScheduled = sIdx < scheduledTracks.length ? scheduledTracks[sIdx++] : null;
          moveCnt = 0;
        }
      }
      if(addTrack) {
        newTracks.push(playlistTracks[i]);
        time += playlistTracks[i].duration * 1000;        
      }
    }

    return newTracks;
    
  }

  function insertNews(playlistTracks) {
    var newTracks = [];

    var ts = new Date();
    ts.setTime(startTime);
    // console.log("start: " + ts.toUTCString());
    var endTime = startTime + duration * 1000;
    var noNewsAfter = endTime - 15 * 60 * 1000;
    // var ts2 = new Date();
    // ts2.setTime(noNewsAfter);
    // console.log("no news after: " + ts2.toUTCString());

    var minDistance = (newsInterval - 30) * 1000 * 60;
    var newsDuration = 165 * 1000;
 
    if(isInNewsTimeframe(ts.getMinutes()) && ts.getTime() - lastNewsStarted > 1000 * 30 * 45) {
      // start of playlist at full hour - add news
      lastNewsStarted = ts.getTime();
      if(preNewsJingle != null) {
        newTracks.push(preNewsJingle);
        ts.setTime(ts.getTime() + preNewsJingle.duration * 1000);
      }
      newTracks.push(newsTrack);
        ts.setTime(ts.getTime() + newsDuration);
    }

    var newsDelayed = false;
    var skipNextIfJingle = false;
    for(var i = 0; i < playlistTracks.length; i++) {
      if(playlistTracks[i].type == 'jingle' && skipNextIfJingle) {
        continue;
      }
      newTracks.push(playlistTracks[i]);
        ts.setTime(ts.getTime() + playlistTracks[i].duration * 1000);
      var insertNews = newsDelayed 
        || (isInNewsTimeframe(ts.getMinutes()) && ts.getTime() - lastNewsStarted > minDistance);
      if(insertNews && !newsDelayed && playlistTracks[i].type != 'song' && playlistTracks[i].duration < 60) {
        // play jingle or word first, then news
        newsDelayed = true;
        insertNews = false;
      }
      skipNextIfJingle = false;
      if(insertNews && ts.getTime() < noNewsAfter)  {
        newsDelayed = false;
        lastNewsStarted = ts.getTime();
        if(preNewsJingle != null) {
          newTracks.push(preNewsJingle);
          ts.setTime(ts.getTime() + preNewsJingle.duration * 1000);
        }
        newTracks.push(newsTrack);
        ts.setTime(ts.getTime() + newsDuration);
        if(firstJingle != null && firstJingleAfterNews) {
          newTracks.push(firstJingle);
          ts.setTime(ts.getTime() + firstJingle.duration * 1000);
          skipNextIfJingle = true;
        }
      }
    }


    return newTracks;
  }

  // Customizable functions

  function customScheduledElementCreate(rule, trackIdx, scheduledElement) {}

  function customInitialize() {}
  
  // Main code

  /* Initialization */

  executionTime = 'time' in opts ? Date.parse(opts.time) : new Date().getTime();
  startTime = executionTime + 1000 * 120; // buest guess - will try to refine with track stats
  if(trackRulesEnabled) {
    for(var i = 0; i < opts.trackRules.length; i++) {
      var trackId = opts.trackRules[i].trackId;
      boundTracks[trackId] = {};
      opts.trackRules[i].lastPlay = startTime -  (1000 * 60 * 60 * 24);
      if(!('rules' in boundTracks[trackId])) {
        boundTracks[trackId].rules = [];
      }
      boundTracks[trackId].rules.push(opts.trackRules[i]);
    }
  }

  if(schedulingRulesEnabled) {
    for(var i = 0; i < opts.scheduled.length; i++) {
      if('introJingleId' in opts.scheduled[i]) {
        boundTracks[opts.scheduled[i].introJingleId] = {};
      }
      if(!(opts.scheduled[i].tag in selectorTags)) {
        selectorTags[opts.scheduled[i].tag] = [];
      }
      selectorTags[opts.scheduled[i].tag].push(opts.scheduled[i]);
    }
  }

  if('artistAliases' in opts) {
    artistAliases = {};
    for (var property in opts.artistAliases) {
      if (opts.artistAliases.hasOwnProperty(property)) {
        var key = property.toLowerCase();
        var value = opts.artistAliases[property].toLowerCase();
        artistAliases[key] = value;
      }
    }   
  }
  
  if(trackStats != null) {
    var baseTime = 'time' in opts ? Date.parse(opts.time) : Date.now;
    var lastTrackEnd = 0;
    for(var i = 0; i < trackStats.length; i++) {
      if(i > trackStats.length - 12 && trackStats[i].artist != null) {
        var artistName = normalizeArtist(trackStats[i].artist.name);
        recentArtists[artistName] = true;
      }
      var started = Date.parse(trackStats[i].started_at);
      lastStartedAt[trackStats[i].id] = started;
      var endsAt = Date.parse(trackStats[i].ends_at);
      lastTrackEnd = Math.max(lastTrackEnd, endsAt);
      var diff = Math.floor((baseTime - started) / (1000 * 60));
      if(diff < avoidRepeat * 60) {
        if(lastPlays[trackStats[i].id] == null) {
          lastPlays[trackStats[i].id] = diff;
        }
      }
      if(trackStats[i].type == 'jingle') {
        lastJinglePlay = diff;
      }
      if(trackRulesEnabled && trackStats[i].id in boundTracks && 'rules' in boundTracks[trackStats[i].id]) {
        for(var r = 0; r < boundTracks[trackStats[i].id].rules.length; r++) {
          markRuleApplied(boundTracks[trackStats[i].id].rules[r], started);
        }
      }
      if(trackStats[i].id == 1) {
        lastNewsStarted = started;
      }
    }
    if(lastTrackEnd > baseTime) {
      startTime = lastTrackEnd;
    }
  }

  for(var i = 0; i < artistSeparators.length; i++) {
    artistSeparators[i] = artistSeparators[i].toLowerCase();
  }
  
  for(var i = 0; i < tagPattern.length; i++) {
    patternTags[tagPattern[i]] = true;
  }

  customInitialize();

  /* Execution */
  var sumTrackDuration = 0;
  var playlistTracks = [];
  var remainingDuration = duration;
  
  var iteration = 0;
  while(remainingDuration > 0 && iteration < 20) {
    artists = [];
    initTracksAndArtists(Math.min(blockLength * 60 * 60, remainingDuration), iteration);
    var selectedTracks = buildPlaylist(Math.min(blockLength * 60 * 60, remainingDuration));
    selectedTracks.forEach(t => sumTrackDuration += t.duration);
    playlistTracks = playlistTracks.concat(selectedTracks);
    remainingDuration = duration - sumTrackDuration;
    iteration++;
    if(remainingDuration > 0) {
      var addedMinutes = (Math.floor(sumTrackDuration / 60));
      recentArtists = {};
      // increase lastPlays
      for (var id in lastPlays) {
        lastPlays[id] += addedMinutes;
      }
      // register added tracks in lastPlays
      var tmpDuration = 0;
      for(var i = 0; i < selectedTracks.length; i++) {
        tmpDuration += selectedTracks[i].duration;
        lastPlays[selectedTracks[i].id] = Math.floor((sumTrackDuration - tmpDuration) / 60);
        if(i >= selectedTracks.length - 12) {
          var artistName = normalizeArtist(selectedTracks[i].artist);
          recentArtists[artistName] = true;
        }
      }
    }
  }

  if(newsTrack != null) {
    scheduleNews();
  }
  if(adTrigger != null) {
    scheduleAdTriggers();
  }
  if(schedulingRulesEnabled) {
    scheduleByRules();
  }
  
  playlistTracks = insertJingles(playlistTracks);
  if(trackRulesEnabled) {
    playlistTracks = applyTrackRules(playlistTracks);
  }
  if(hasLinkedTracks) {
    playlistTracks = insertLinkedTracks(playlistTracks);
  }
  if(scheduledTracks.length > 0) {
    playlistTracks = insertScheduledTracks(playlistTracks);
  }

  
  return playlistTracks;
})