// key: StationAdmin_v1
( function( tracks, opts, trackStats ){
	var maxTracksPerArtist = 0; 
	var tagWeights;
	var artistSeparators = [' feat'];
	var artistAliases;
	var wordDistribution = 'random';
	var preserveAllJingles = 0;
	var duration = 64800;
	var avoidRepeat = 2;
	var trackNameLimit = 0;
	
	var firstJingle;
	var artists = [];
	var jingles = [];
	var lastPlays = {};
	var recentArtists = {}; // last artists of history / previous iteration
	var preservedTracks = [];
	var hasLinkedTracks = false;
	var tracksAfter = {};
	var tracksBefore = {};
	var adTrigger;
	var adSeparator;
	var adPositions = [15, 45];
	var adJingleCollisionStrategy = 'keep_both';

	
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
		for(var i = 0; i < artistSeparators.length; i++) {
			var pos = artistName.indexOf(artistSeparators[i]);
			if(pos > 1) {
				var old = artistName;
				artistName = artistName.substring(0, pos).trim();
			}
		}
		if(artistAliases != null) {
			if(artistName in artistAliases) {
				artistName = artistAliases[artistName];
			}
		}
		
		return artistName;
	}
	
	function normalizeTitle(name) {
		name = name.toLowerCase();
		var stripped = name.replace(/\W/g, "");
		return stripped.length > 3 ? stripped : name;
	}

		
	function applyTrackScore(track) {
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
			if(minWeight == -4) {
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
			
			if(tracks[i].title.indexOf('START_AD_BREAK') > -1) {
				adTrigger = tracks[i];
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
					else if(i == 0 && 'protectFirstJingle' in opts && opts.protectFirstJingle) {
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
			
			applyTrackScore(tracks[i]);
			
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
			jingleOffset = Math.floor((Math.random() * jingleInterval));
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
	
	// Main code
	
	/* Configuration */
	
	if('duration' in opts) {
		duration = opts.duration < 64800 ? opts.duration : 64800;
	}

	var blockLength = 'blockLength' in opts ? opts.blockLength : duration;
	
	maxTracksPerArtist = Math.floor(duration / (60 * 60));
	if('maxTracksPerArtist' in opts && opts.maxTracksPerArtist < maxTracksPerArtist) {
		maxTracksPerArtist = opts.maxTracksPerArtist;
	}
		
	if('tagWeights' in opts) {
		tagWeights = opts.tagWeights;
	}

	if('artistSeparators' in opts) {
		artistSeparators = opts.artistSeparators;
		for(var i = 0; i < artistSeparators.length; i++) {
			artistSeparators[i] = artistSeparators[i].toLowerCase();
		}
	}
	if('artistAliases' in opts) {
		artistAliases = opts.artistAliases;
	}
	if('wordDistribution' in opts) {
		wordDistribution = opts.wordDistribution;
	}
	if('preserveAllJingles' in opts) {
		preserveAllJingles = opts.preserveAllJingles;
	}
	if('adPositions' in opts && opts.adPositions.length > 1) {
		adPositions = opts.adPositions;
	}
	if('adJingleCollisionStrategy' in opts) {
		adJingleCollisionStrategy = opts.adJingleCollisionStrategy;
	}
	if('avoidRepeat' in opts) {
		avoidRepeat = opts.avoidRepeat;
	}
	if('trackNameLimit' in opts) {
		trackNameLimit = opts.trackNameLimit;
	}
	
	if(trackStats != null) {
		var baseTime = Date.now();
		for(var i = 0; i < trackStats.length; i++) {
			if(i < 12 && trackStats[i].artist != null) {
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
		}
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
	if(adTrigger != null) {
		playlistTracks = insertAdTriggers(playlistTracks);
	}
	if(hasLinkedTracks) {
		playlistTracks = insertLinkedTracks(playlistTracks);
	}

	
	return playlistTracks;
})
