// key: StationAdmin_v1_1_2
( function( tracks, opts, trackStats ){
	
	var duration = 'duration' in opts && opts.duration < 64800 ? opts.duration : 64800;
	var blockLength = 'blockLength' in opts ? opts.blockLength : duration;
	var maxTracksPerArtist = 'maxTracksPerArtist' in opts && opts.maxTracksPerArtist < Math.floor(duration / (60 * 60)) ? opts.maxTracksPerArtist : Math.floor(duration / (60 * 60)); 
	var tagWeights = 'tagWeights' in opts ? opts.tagWeights : null;
	var artistSeparators = 'artistSeparators' in opts ? opts.artistSeparators : [' feat'];
	var artistAliases = null;
	var wordDistribution = 'wordDistribution' in opts ? opts.wordDistribution : 'random';
	var preserveAllJingles = 'preserveAllJingles' in opts ? opts.preserveAllJingles : 0;
	var avoidRepeat = 'avoidRepeat' in opts ? opts.avoidRepeat : 2;
	var trackNameLimit = 'trackNameLimit' in opts ? opts.trackNameLimit : 0;
	var adPositions = 'adPositions' in opts && opts.adPositions.length > 1 ? opts.adPositions : [15, 45];
	var adJingleCollisionStrategy = 'adJingleCollisionStrategy' in opts ? opts.adJingleCollisionStrategy : 'keep_both';
	var trackRulesEnabled = 'trackRules' in opts;
	var newsInterval= 'newsInterval' in opts ? opts.newsInterval : 60;
	var firstJingleAfterNews = 'firstJingleAfterNews' in opts ? opts.firstJingleAfterNews : true;

	var firstJingle;
	var adTrigger;
	var adSeparator;
	var artists = [];
	var jingles = [];
	var lastPlays = {};
	var recentArtists = {}; // last artists of history / previous iteration
	var preservedTracks = [];
	var hasLinkedTracks = false;
	var tracksAfter = {};
	var tracksBefore = {};

	var boundTracks = {};
	
	var newsTrack;

	var startTime;
	var lastJinglePlay = -1;
	var lastNewsStarted = 0;

	
	// basic array shuffle function
	// source: http://stackoverflow.com/questions/6274339/how-can-i-shuffle-an-array-in-javascript
	function shuffle( a ) {
		var j, x, i;
		for ( i = a.length; i; i-- ) {
			j        = Math.floor( Math.random() * i );
			x        = a[i - 1];
			a[i - 1] = a[j];
			a[j]     = x;
		}
	}
	
	function normalizeArtist(artistName) {
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
		name = name.toLowerCase();
		var stripped = name.replace(/\W/g, "");
		return stripped.length > 3 ? stripped : name;
	}

		
	function assignTrackScore(track) {
		// assign random score
		track.score = 100 + Math.floor((Math.random() * 500));
		if(tagWeights != null && track.tags.length > 0) {
			// increase / decrease score based on tag weights
			var minWeight = 0;
			var maxWeight = 0;
			for(var i = 0; i < track.tags.length; i++) {
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
			if(minWeight < -3) {
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
		if (track.id in lastPlays && lastPlays[track.id] < 60 * avoidRepeat) {
			var penalty = 500 - 250 * lastPlays[track.id] / (60 * avoidRepeat);
			track.score += penalty;
			track.penalty = Math.floor(penalty / 50);
		}
		else {
			track.penalty = 0;
		}
	}
	
	function initTracksAndArtists(remainingDuration, iteration) {
		
		var artistMap = {};
		var tracksDuration = 0;
		
		for(var i = 0; i < tracks.length; i++) {


			if(tracks[i].id == 1) {
				newsTrack = tracks[i];
				continue;
			}
			if(tracks[i].title.indexOf('START_AD_BREAK') > -1) {
				adTrigger = tracks[i];
				continue;
			}
			if(trackRulesEnabled && tracks[i].id in boundTracks) {
				// only inserted by track rule
				boundTracks[tracks[i].id] = tracks[i];
				continue;
			}
			if(tracks[i].type == 'jingle') {
				if(iteration == 0) {
					if(tracks[i].id == 0 || tracks[i].id == opts.adTrigger) {
						adTrigger = tracks[i];
					}
					else if(tracks[i].id == opts.adSeparator) {
						adSeparator = tracks[i];
					}
					else if(preserveAllJingles) {
						preservedTracks[i] = tracks[i];
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
					preservedTracks[i] = tracks[i];
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
			
			tracksDuration += tracks[i].duration;
			tracks[i].use = false;
			
			if(trackNameLimit > 0) {
				tracks[i].normTitle = normalizeTitle(tracks[i].title);
			}
			
			assignTrackScore(tracks[i]);
			if(tracks[i].score > 10000) {
				// excluded
				continue;
			}
			
			var artistName = normalizeArtist(tracks[i].artist);
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
			maxTracksPerArtist = Math.floor(remainingDuration / (60 * 60));
		}
		
		var tracksDurationHours = Math.floor(tracksDuration / (60 * 60));
		if(tracksDurationHours < maxTracksPerArtist && tracksDurationHours > 0) {
			// iteration will produce shorter list that required - need to set a stricter limit
			maxTracksPerArtist = tracksDurationHours < 3 ? tracksDurationHours : tracksDurationHours - 1;
		}
		
		var candidates = [];
		for(var i = 0; i < artists.length; i++) {
			for(var j = 0; j < artists[i].tracks.length && j < maxTracksPerArtist; j++) {
				candidates.push(artists[i].tracks[j]);
			}
		}
		candidates.sort(function(a, b) { return a.score - b.score });
		var cDuration = 0;
		var cIdx = 0;
		while(cDuration < remainingDuration && cIdx < candidates.length) {
			cDuration += candidates[cIdx].duration;
			candidates[cIdx].use = true;
			cIdx++;
		}
	}
	
	function buildPlaylist() {
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
			
			var artistSegments = Math.floor(numSegments / artistTracks.length);

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
				currentSegment += artistSegments;
			}
		}
		
		var playlistTracks = [];
		var recentTrackNames = [];
		var buffer = [];
		for(var s = 0; s < segments.length; s++) {
			var segmentTracks = segments[s].tracks;
			shuffle(segmentTracks);
			segmentTracks.sort(function(a, b) { return a.penalty - b.penalty });
			
			for(var t = 0; t < segmentTracks.length; t++) {
				var next = segmentTracks[t];
				if(buffer.length > 0 && !recentTrackNames.includes(buffer[0].normTitle)) {
					// use next track from buffer
					next = buffer.shift();
					t--;
				}
				// check if preserved track needs to be inserted
				if(typeof preservedTracks[playlistTracks.length] != 'undefined') {
					playlistTracks.push(preservedTracks[playlistTracks.length]);
				}
				if(trackNameLimit > 0 && recentTrackNames.includes(next.normTitle)) {
					buffer.push(next);
					continue;
				}
				playlistTracks.push(next);
				if(trackNameLimit > 0) {
					recentTrackNames.push(next.normTitle);
					if(recentTrackNames.length > trackNameLimit) {
						recentTrackNames.shift();
					} 
				}
			}
		}
		
		return playlistTracks;
	}
	
	function insertJingles(playlistTracks) {
		if(firstJingle == null && jingles.length == 0) {
			// nothing to do
			return playlistTracks;
		}
		else if(firstJingle != null && jingles.length == 0) {
			// just insert first jingle 
			playlistTracks.splice(0, 0, firstJingle);
			return playlistTracks;
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
		
		if(firstJingle != null) {
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
		}
		
		
		for(var i = 0; i < playlistTracks.length; i++) {
			if(!('boundTo' in playlistTracks[i])) {
				 playlistTracks[i].boundTo = [];
				 for(var r = 0; r < opts.trackRules.length; r++) {
					 if(opts.trackRules[r].active && isBoundTo(playlistTracks[i], opts.trackRules[r])) {
						 playlistTracks[i].boundTo.push(r);
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
										currentTime = markRuleApplied(rule, currentTime);
										break;
									case 'keep_rule_jingle':
										if(!opts.protectFirstJingle || newTracks.length > 1) {
											newTracks.splice(newTracks.length -1, 1);
										}
										newTracks.push(boundTracks[rule.trackId]);
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
			}
			newTracks.push(playlistTracks[i]);
			if(id in tracksAfter) {
				newTracks.push(tracksAfter[id]);
			}
		}
		return newTracks;
	}

	function insertNews(playlistTracks) {
		var newTracks = [];

		var ts = new Date();
		ts.setTime(startTime);
		// console.log("start: " + ts.toUTCString());

		var minDistance = (newsInterval - 30) * 1000 * 60;
		var newsDuration = 165 * 1000;
 
		if((ts.getMinutes() >= 58 || ts.getMinutes() < 5) && ts.getTime() - lastNewsStarted > 1000 * 30) {
			// start of playlist at full hour - add news
			newTracks.push(newsTrack);
			lastNewsStarted = ts.getTime();
		    ts.setTime(ts.getTime() + newsDuration);
		}

		var newsDelayed = false;
		for(var i = 0; i < playlistTracks.length; i++) {
			var minutesBefore = ts.getMinutes();
			newTracks.push(playlistTracks[i]);
		    ts.setTime(ts.getTime() + playlistTracks[i].duration * 1000);
			var insertNews = newsDelayed || ((ts.getMinutes() >= 59 || ts.getMinutes() < minutesBefore) && ts.getTime() - lastNewsStarted > minDistance);
			if(insertNews && !newsDelayed && playlistTracks[i].type != 'song' && playlistTracks[i].duration < 60) {
				// play jingle or word first, then news
				newsDelayed = true;
				insertNews = false;
			}
			if(insertNews && i < playlistTracks.length - 1)  {
				newTracks.push(newsTrack);
				// console.log("news at " + ts.toUTCString() + " / " + (ts.getTime() - lastNewsStarted));
				lastNewsStarted = ts.getTime();
				ts.setTime(ts.getTime() + newsDuration);
				if(firstJingle != null && firstJingleAfterNews) {
					newTracks.push(firstJingle);
					ts.setTime(ts.getTime() + firstJingle.duration * 1000);
				}
			}
		}


		return newTracks;
	}
	
	// Main code

	/* Initialization */

	startTime = new Date().getTime() + 1000 * 120; // buest guess - will try to refine with track stats
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
		var baseTime = Date.now();
		for(var i = 0; i < trackStats.length; i++) {
			if(i > trackStats.length - 12 && trackStats[i].artist != null) {
				var artistName = normalizeArtist(trackStats[i].artist.name);
				recentArtists[artistName] = true;
			}
			var started = Date.parse(trackStats[i].started_at);
			var diff = Math.floor((baseTime - started) / (1000 * 60));
			if(diff < avoidRepeat * 60) {
				if(lastPlays[trackStats[i].id] == null) {
					lastPlays[trackStats[i].id] = diff;
				}
			}
			if(trackStats[i].type == 'jingle') {
				lastJinglePlay = diff;
			}
			if(trackRulesEnabled && trackStats[i].id in boundTracks) {
				for(var r = 0; r < boundTracks[trackStats[i].id].rules.length; r++) {
					markRuleApplied(boundTracks[trackStats[i].id].rules[r], started);
				}
			}
			if(trackStats[i].id == 1) {
				lastNewsStarted = started;
			}
		}
	}

	for(var i = 0; i < artistSeparators.length; i++) {
		artistSeparators[i] = artistSeparators[i].toLowerCase();
	}

	/* Execution */
	var sumTrackDuration = 0;
	var playlistTracks = [];
	var remainingDuration = duration;
	
	var iteration = 0;
	while(remainingDuration > 0 && iteration < 20) {
		artists = [];
		initTracksAndArtists(Math.min(blockLength * 60 * 60, remainingDuration), iteration);
		var selectedTracks = buildPlaylist();
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
	
	playlistTracks = insertJingles(playlistTracks);
	if(trackRulesEnabled) {
		playlistTracks = applyTrackRules(playlistTracks);
	}
	if(adTrigger != null) {
		playlistTracks = insertAdTriggers(playlistTracks);
	}
	if(hasLinkedTracks) {
		playlistTracks = insertLinkedTracks(playlistTracks);
	}
	if(newsTrack != null) {
		playlistTracks = insertNews(playlistTracks);
	}

	
	return playlistTracks;
})
