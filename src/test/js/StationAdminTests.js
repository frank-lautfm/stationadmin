#!/usr/bin/env node

/**
 * Unit tests for StationAdmin.js shuffle algorithm
 * 
 * Usage: node StationAdminTests.js
 * 
 * Uses Node.js built-in test framework (available in Node.js 18+)
 */

const test = require('node:test');
const assert = require('node:assert');
const fs = require('fs');
const path = require('path');

const boundJingles = [3435730, 3435732, 3484100, 3435733, 3435734]

const trackRules =  [ {
            "filter" : "Christian Hüser",
            "groupName" : "Artists",
            "minDistance" : 120,
            "trackId" : 3435730,
            "position" : "before",
            "filterType" : "artist"
            }, {
            "filter" : "Fug und Janina",
            "groupName" : "Artists",
            "minDistance" : 120,
            "trackId" : 3435732,
            "position" : "before",
            "filterType" : "artist"
            }, {
            "filter" : "Grünschnabel",
            "groupName" : "Artists",
            "minDistance" : 120,
            "trackId" : 3484100,
            "position" : "before",
            "filterType" : "artist"
            }, {
            "filter" : "Reinhard Horn",
            "groupName" : "Artists",
            "minDistance" : 120,
            "trackId" : 3435733,
            "position" : "before",
            "filterType" : "artist"
            }, {
            "filter" : "Robert Metcalf",
            "groupName" : "Artists",
            "minDistance" : 120,
            "trackId" : 3435734,
            "position" : "before",
            "filterType" : "artist"
            } ];


const trackRuleGroups = {
      "Standard" : {
        "minDistance" : 0,
        "multiMatchSelection" : "all"
      },
      "Artists" : {
        "minDistance" : 30,
        "multiMatchSelection" : "all"
      }
    };

 const scheduled = [ {
      "selection" : "random",
      "trackType" : "moderation",
      "index" : 1,
      "exclude" : true,
      "interval" : 3,
      "id" : "d0b1c3c8-5366-43af-aefa-aa2612719aef",
      "tag" : "CD-Vorstellung",
      "minute" : 20
    } ];
   

const time = '2026-02-09T20:57:11+01:00';
const time2 = '2026-02-09T20:58:11+01:00';



/**
 * Load and execute the StationAdmin.js shuffle function
 * @param {Array} tracks - Playlist tracks
 * @param {Object} opts - Shuffle options
 * @param {Array} trackStats - Track statistics from last 24 hours
 * @returns {Array} - Shuffled tracks
 */
function executeShuffleFunction(tracks, opts, trackStats) {
    // Load the StationAdmin.js file
    const shuffleFunctionPath = path.join(__dirname, '..', '..', 'main', 'javascript', 'shuffle', 'StationAdmin.js');
    const shuffleFunctionCode = fs.readFileSync(shuffleFunctionPath, 'utf8');
    
    // The StationAdmin.js file contains an IIFE (Immediately Invoked Function Expression)
    // Format: ( function( tracks, opts, trackStats ){ ... })
    // We evaluate it and call it with our parameters
    const shuffleFunction = eval(shuffleFunctionCode);
    return shuffleFunction(tracks, opts, trackStats);
}

/**
 * Load tracks from JSON file
 * @param {string} filename - Path to JSON file relative to resources directory
 * @returns {Array} - Array of track objects
 */
function loadTracksFromFile(filename) {
    const filePath = path.join(__dirname, 'resources', filename);
    const fileContent = fs.readFileSync(filePath, 'utf8');
    return JSON.parse(fileContent);
}

/**
 * Calculate total duration of tracks
 * @param {Array} tracks - Array of track objects
 * @returns {number} - Total duration in seconds
 */
function calculateTotalDuration(tracks) {
    return tracks.reduce((sum, track) => sum + (track.duration || 0), 0);
}

/**
 * Assert that the total duration meets the target duration constraints
 * @param {Array} tracks - Array of track objects
 * @param {number} duration - Target duration in seconds
 */
function assertDuration(tracks, duration) {
    // Verify that tracks is an array
    assert.ok(Array.isArray(tracks), 'Result should be an array');
    
    // Verify that tracks is not empty
    assert.ok(tracks.length > 0, 'Result should contain tracks');
    
    // Calculate total duration of returned tracks
    const totalDuration = calculateTotalDuration(tracks);
    
    // Verify that total duration is at least the target duration
    assert.ok(
        totalDuration >= duration,
        `Total duration (${totalDuration}s) should be at least ${duration}s`
    );
    
    // Verify that total duration is not more than duration + 10%
    const maxDuration = duration * 1.1;
    assert.ok(
        totalDuration <= maxDuration,
        `Total duration (${totalDuration}s) should not exceed ${maxDuration}s (${duration}s + 10%)`
    );
}

/**
 * Assert that jingles are placed at regular intervals
 * @param {Array} tracks - Array of track objects
 * @param {number} interval - Expected interval in minutes between jingles
 */
function assertJingleInterval(tracks, interval) {
    // Find all jingle positions
    const jinglePositions = [];
    let cumulativeDuration = 0;
    
    for (let i = 0; i < tracks.length; i++) {
        if (tracks[i].type === 'jingle') {
            jinglePositions.push({
                index: i,
                timeInMinutes: cumulativeDuration / 60
            });
        }
        cumulativeDuration += tracks[i].duration;
    }
    
    // Verify that at least one jingle exists
    assert.ok(
        jinglePositions.length > 0,
        'At least one jingle should be present in the tracks'
    );
    
    // Check intervals between consecutive jingles
    const intervalSeconds = interval * 60;
    const tolerance = 240; // 4 minutes tolerance in seconds (to account for track boundaries)
    
    for (let i = 1; i < jinglePositions.length; i++) {
        const timeDiff = (jinglePositions[i].timeInMinutes - jinglePositions[i-1].timeInMinutes) * 60;
        const minInterval = intervalSeconds - tolerance;
        const maxInterval = intervalSeconds + tolerance;
        
        assert.ok(
            timeDiff >= minInterval && timeDiff <= maxInterval,
            `Jingle interval between position ${jinglePositions[i-1].index} and ${jinglePositions[i].index} ` +
            `is ${Math.round(timeDiff/60)}m (expected ${interval}m ± 4m)`
        );
    }
    
    console.log(`  - Jingles found: ${jinglePositions.length}`);
    console.log(`  - First jingle at: ${Math.round(jinglePositions[0].timeInMinutes)}m`);
    if (jinglePositions.length > 1) {
        console.log(`  - Average interval: ${Math.round((jinglePositions[jinglePositions.length-1].timeInMinutes - jinglePositions[0].timeInMinutes) / (jinglePositions.length - 1))}m`);
    }
}

/**
 * Assert that artist distribution meets the constraints
 * @param {Array} tracks - Array of track objects
 * @param {number} maxTracksPerArtist - Maximum number of tracks per artist
 */
function assertArtistDistribution(tracks, maxTracksPerArtist) {
    // Count tracks per artist
    const artistCounts = {};
    const artistPositions = {};
    
    for (let i = 0; i < tracks.length; i++) {
        const track = tracks[i];
        
        // Only check songs (not jingles, news, etc.)
        if (track.type !== 'song') {
            continue;
        }
        
        const artist = track.artist || 'Unknown Artist';
        
        // Count occurrences
        if (!artistCounts[artist]) {
            artistCounts[artist] = 0;
            artistPositions[artist] = [];
        }
        artistCounts[artist]++;
        artistPositions[artist].push(i);
    }
    
    // Check 1: No artist appears more than maxTracksPerArtist times
    for (const artist in artistCounts) {
        assert.ok(
            artistCounts[artist] <= maxTracksPerArtist,
            `Artist "${artist}" appears ${artistCounts[artist]} times, exceeding limit of ${maxTracksPerArtist}`
        );
    }
    
    // Check 2: At least 30 minutes between tracks of the same artist
    const minDistanceSeconds = 30 * 60; // 30 minutes in seconds
    
    for (const artist in artistPositions) {
        const positions = artistPositions[artist];
        
        if (positions.length > 1) {
            for (let i = 1; i < positions.length; i++) {
                // Calculate time between this track and previous track by same artist
                let timeBetween = 0;
                for (let j = positions[i-1]; j < positions[i]; j++) {
                    timeBetween += tracks[j].duration;
                }
                
                assert.ok(
                    timeBetween >= minDistanceSeconds,
                    `Artist "${artist}" has tracks at positions ${positions[i-1]} and ${positions[i]} ` +
                    `with only ${Math.round(timeBetween/60)}m between them (minimum 30m required)`
                );
            }
        }
    }
    
    // Log statistics
    const artistsWithMultipleTracks = Object.keys(artistCounts).filter(a => artistCounts[a] > 1).length;
    const maxCount = Math.max(...Object.values(artistCounts));
    
    console.log(`  - Unique artists: ${Object.keys(artistCounts).length}`);
    console.log(`  - Artists with multiple tracks: ${artistsWithMultipleTracks}`);
    console.log(`  - Max tracks per artist: ${maxCount} (limit: ${maxTracksPerArtist})`);
}

/**
 * Count how many tracks are tagged with the given tag name
 * @param {Array} tracks - Array of track objects
 * @param {string} tagname - Tag name to count
 * @returns {number} - Number of tracks with the given tag
 */
function countByTag(tracks, tagname) {
    return tracks.filter(track => {
        return track.tags && Array.isArray(track.tags) && track.tags.includes(tagname);
    }).length;
}

/**
 * Assert that news tracks are placed correctly
 * - First track must be a news track
 * - News tracks repeat every 60 minutes based on cumulative duration
 * @param {Array} tracks - Array of track objects
 */
function assertNews(tracks) {
    // Verify that tracks is an array and not empty
    assert.ok(Array.isArray(tracks), 'Result should be an array');
    assert.ok(tracks.length > 0, 'Result should contain tracks');
    
    // Check 1: First track must be a news track
    assert.strictEqual(
        tracks[0].type,
        'news',
        'First track must be a news track'
    );
    
    // Find all news track positions
    const newsPositions = [];
    let cumulativeDuration = 0;
    
    for (let i = 0; i < tracks.length; i++) {
        if (tracks[i].type === 'news') {
            newsPositions.push({
                index: i,
                time: cumulativeDuration
            });        
            cumulativeDuration += 165;
        }
        else {
            cumulativeDuration += tracks[i].duration;
        }
    }
    
    // Verify that at least one news track exists
    assert.ok(
        newsPositions.length > 0,
        'At least one news track should be present'
    );

    let hours = Math.floor(cumulativeDuration / (60 * 60));
    console.log(hours + " hours");
    for(let i = 0; i < hours; i++) {
        let min = i * 60 * 60 - 120;
        let max = i * 60 * 60 + (16 * 60);
        console.log(i + ": " + min + " < " + newsPositions[i].time + " < " + max);
        assert.ok(newsPositions[i].time >= min && newsPositions[i].time <= max, "News position for hour " + i + ": " + min + " < " + newsPositions[i].time + " < " + max);
    }
    

}

/**
 * Assert that tracks follow the expected tag pattern sequence
 * Pattern "Jingle" means the track must be of type jingle
 * Excludes news and ad triggers from the check
 * @param {Array} tracks - Array of track objects
 * @param {Array} pattern - Array of tag names or "Jingle" to match against
 */
function assertTagPattern(tracks, pattern) {
    // Verify that tracks is an array and not empty
    assert.ok(Array.isArray(tracks), 'Result should be an array');
    assert.ok(tracks.length > 0, 'Result should contain tracks');
    
    // Filter out news and ad triggers (id === 0), bound jingles and scheduled items
    const filteredTracks = tracks.filter(track => {
        return track.type !== 'news' && track.id !== 0 && !boundJingles.includes(track.id) && !track.tags.includes("CD-Vorstellung");
    });
    
    // Track pattern matching
    let patternIndex = 0;
    const matches = [];
    
    for (let i = 0; i < filteredTracks.length; i++) {
        const track = filteredTracks[i];
        const expectedPattern = pattern[patternIndex % pattern.length];
        
        let isMatch = false;
        
        // Check if pattern is "Jingle" - match against track type
        if (expectedPattern === 'Jingle') {
            isMatch = track.type === 'jingle';
        } else {
            // Match against track tags
            isMatch = track.tags && Array.isArray(track.tags) && track.tags.includes(expectedPattern);
        }
        
        matches.push({
            index: i,
            track: track,
            expectedPattern: expectedPattern,
            matched: isMatch
        });
        
        // Assert that the track matches the expected pattern
        assert.ok(
            isMatch,
            `Track at position ${i} (original index: ${tracks.indexOf(track)}) does not match pattern "${expectedPattern}". ` +
            `Track type: ${track.type}, tags: ${track.tags ? track.tags.join(', ') : 'none'}`
        );
        
        patternIndex++;
    }
    
    console.log(`  - Pattern verified: ${pattern.join(' -> ')}`);
    console.log(`  - Tracks checked: ${filteredTracks.length} (excluded ${tracks.length - filteredTracks.length} news/ad triggers)`);
    console.log(`  - Pattern cycles: ${Math.floor(filteredTracks.length / pattern.length)}`);
}

/**
 * Assert that ad triggers are placed correctly
 * @param {Array} tracks - Array of track objects
 */
function assertAdTriggers(tracks, p1, p2) {
    // Verify that tracks is an array and not empty
    assert.ok(Array.isArray(tracks), 'Result should be an array');
    assert.ok(tracks.length > 0, 'Result should contain tracks');
        
    // Find all news track positions
    const positions = [];
    let cumulativeDuration = 0;
    
    for (let i = 0; i < tracks.length; i++) {
        if (tracks[i].id === 0) {
            positions.push({
                index: i,
                time: cumulativeDuration
            });        
        }
        if (tracks[i].type === 'news') {
            cumulativeDuration += 165;
        }
        else {
            cumulativeDuration += tracks[i].duration;
        }
    }
    
    let hours = Math.floor(cumulativeDuration / (60 * 60));
    console.log(hours + " hours");
    for(let i = 0; i < hours; i++) {
        let t1 = i * 60 * 60 + p1 * 60;
        let t2 = i * 60 * 60 + p2 * 60;

        console.log(i + ": " + t1 + " <> " + positions[i * 2].time);
        console.log(i + ": " + t2 + " <> " + positions[i * 2 + 1].time);

        assert.ok(positions[i * 2].time >= t1 - 360 && positions[i * 2].time <= t1 + 360, "Ad position 1 for hour " + i);
        assert.ok(positions[i * 2 + 1].time >= t2 - 360 && positions[i * 2 + 1].time <= t2 + 360, "Ad position 2 for hour " + i);
    }
}

/**
 * Assert that ad bound jingles are placed
 * @param {Array} tracks - Array of track objects
 */
function assertBoundJingles(tracks) {
    assert.ok(Array.isArray(tracks), 'Result should be an array');

    let artists = 0;
    let jingles = 0;
    let cumulativeDuration = 0;
    let last = -1;
    for(let i = 1; i < tracks.length; i++) {
        let rule = trackRules.find((r) => r.filter == tracks[i].artist);
        if(rule) {
            artists++;
            if(tracks[i - 1].id === rule.trackId) {
                jingles++;
                if(last > -1) {
                    let dist = (cumulativeDuration - last) / 60;
                    assert.ok(dist >= 30, "Bound jingle distance violation" );
                }
                last = cumulativeDuration;
            }
        }
        if (tracks[i].type === 'news') {
            cumulativeDuration += 165;
        }
        else {
            cumulativeDuration += tracks[i].duration;
        }

    }

    if(artists > 0) {
        assert.ok(jingles > 0, "Expected bound jingles: " + artists + " artists");
    }
        
}

/**
 * Assert that ad bound jingles are placed
 * @param {Array} tracks - Array of track objects
 */
function assertScheduledItem(tracks) {
    assert.ok(Array.isArray(tracks), 'Result should be an array');

    let items = 0;
    let cumulativeDuration = 0;
    let last = -1;
    var t = Date.parse(time2);
    for(let i = 0; i < tracks.length; i++) {
        if(tracks[i].tags.includes(scheduled[0].tag)) {
            items++;

            let ts = new Date();
            ts.setTime(t + cumulativeDuration * 1000);
            let minute = ts.getMinutes()
            console.log(minute);
            assert.ok(minute >= scheduled[0].minute - 5 && minute <= scheduled[0].minute + 5, "Minute: " + minute);

            if(last > -1) {
                let dist = (cumulativeDuration - last) / 60;
                let expected = 60 * scheduled[0].interval;
                assert.ok(dist < (60 * scheduled[0].interval) + 300, "Scheduled item distance violation: " + dist + " < " + expected);
            }
            last = cumulativeDuration;
        }
        if (tracks[i].type === 'news') {
            cumulativeDuration += 165;
        }
        else {
            cumulativeDuration += tracks[i].duration;
        }

    }

    assert.equal(items, 2, "Number of expected items");
}



// Test: noPatternSimple
test('noPatternSimple - basic shuffle', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_plain.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);

    // Assert duration constraints
    assertDuration(result, duration);
    assertJingleInterval(result, jingleInterval);
    assertArtistDistribution(result, 2);    
});

// Test: patternSimple
test('patternSimple - basic shuffle', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_plain.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ]
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);

    // Assert duration constraints
    assertDuration(result, duration);
    assertTagPattern(result, opts.tagPattern);
    assertArtistDistribution(result, 2);    
});


// Test: noPatternTagWeights
test('noPatternTagWeights - basic shuffle with tag weights', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_plain.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        tagWeights : {
            'A' : 3,
            'B' : 1,
            'C' : -1
        }
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);

    var aTracks = countByTag(result, 'A');
    var bTracks = countByTag(result, 'B');
    var cTracks = countByTag(result, 'C');

    console.log(aTracks + " > " + bTracks + " > " + cTracks);

    assert.ok(aTracks > bTracks, "There should be more A tracks than B tracks");
    assert.ok(bTracks > cTracks, "There should be more B tracks than C tracks");

});

// Test: dateFilter
test('dateFilter - basic shuffle with date filter tags', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_plain.json');

    const tags = ["@01.02.-28.02. Februar", "@01.03.-31.03. Maerz", "@01.12.-28.02. Winter", "@01.02.-05.01."];
    var t = 0;
    for(var i = 0; i < tracks.length; i++) {
        tracks[i].tags.push(tags[t]);
        t = (t + 1) % tags.length;
    }
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);

    var t1 = countByTag(result, tags[0]);
    var t2 = countByTag(result, tags[1]);
    var t3 = countByTag(result, tags[2]);
    var t4 = countByTag(result, tags[3]);


    assert.ok(t1 > 0, tags[0] + " should pass");
    assert.ok(t2 == 0, tags[1] + " should not pass");
    assert.ok(t3 > 0, tags[2] + " should pass");
    assert.ok(t4 > 0, tags[3] + " should pass");

});


// Test: noPatternNews
test('noPatternNews - basic shuffle with news', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_news.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        newsInterval : 60,
        newsMin : 59,
        newsMax : 15,
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertNews(result);
});

// Test: patternNews
test('patternNews - basic shuffle with news', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_news.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        newsInterval : 60,
        newsMin : 59,
        newsMax : 15,
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ]
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertNews(result);
    assertTagPattern(result, opts.tagPattern);
});


// Test: noPatternAdTrigger
test('noPatternAdTrigger - basic shuffle with ad triggers', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_ad_trigger.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        adTrigger : 0,
        adPositions : [ 15, 45 ]
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertAdTriggers(result, 15, 45);
});

// Test: patternAdTrigger
test('patternAdTrigger - basic shuffle with ad triggers', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_ad_trigger.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    // Options with only duration set to 7200 seconds (2 hours)
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        adTrigger : 0,
        adPositions : [ 15, 45 ],
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ]
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertAdTriggers(result, 15, 45);
    assertTagPattern(result, opts.tagPattern);
});


// Test: noPatternBoundJingles
test('noPatternBoundJingles - basic shuffle with bound jingles', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_bound_jingles.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        trackRules : JSON.parse(JSON.stringify(trackRules)),
        trackRuleGroups : JSON.parse(JSON.stringify(trackRuleGroups)),
        trackRuleJingleCollisionStrategy: 'keep_both'
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertBoundJingles(result);
});

// Test: patternBoundJingles
test('patternBoundJingles - tag pattern shuffle with bound jingles', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_bound_jingles.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time,
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ],
        trackRules : JSON.parse(JSON.stringify(trackRules)),
        trackRuleGroups : JSON.parse(JSON.stringify(trackRuleGroups)),
        trackRuleJingleCollisionStrategy: 'keep_both'
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertBoundJingles(result);
    assertTagPattern(result, opts.tagPattern);
});

// Test: noPatternScheduledItem
test('noPatternScheduledItem - basic shuffle with a scheduled item', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_scheduled_items.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time2,
        scheduled : JSON.parse(JSON.stringify(scheduled))
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertScheduledItem(result);
});

// Test: patternScheduledItem
test('patternScheduledItem - tag pattern shuffle with a scheduled item', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_scheduled_items.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time2,        
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ],
        scheduled : JSON.parse(JSON.stringify(scheduled))
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertScheduledItem(result);
});


// Test: noPatternMixed
test('noPatternMixed - basic shuffle with mixed use cases', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_full.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time2,
        newsInterval : 60,
        newsMin : 59,
        newsMax : 15,
        adTrigger : 0,
        adPositions : [ 15, 45 ],
        trackRules : JSON.parse(JSON.stringify(trackRules)),
        trackRuleGroups : JSON.parse(JSON.stringify(trackRuleGroups)),
        trackRuleJingleCollisionStrategy: 'keep_both',
        scheduled : JSON.parse(JSON.stringify(scheduled))

    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertNews(result);
    assertAdTriggers(result, 15, 45);
    assertBoundJingles(result);
    assertScheduledItem(result);
});

// Test: patternMixed
test('patternMixed - basic shuffle with mixed use cases', (t) => {
    // Load tracks from the test resource file
    const tracks = loadTracksFromFile('tracks_full.json');
    
    // Empty array for track stats (no previous plays)
    const trackStats = [];

    const duration = 14400;
    const jingleInterval = 20; // 20 minutes
    
    const opts = {
        duration: duration,
        jingleInterval : jingleInterval,
        maxTracksPerArtist : 2,
        time: time2,
        newsInterval : 60,
        newsMin : 59,
        newsMax : 15,
        adTrigger : 0,
        adPositions : [ 15, 45 ],
        trackRules : JSON.parse(JSON.stringify(trackRules)),
        trackRuleGroups : JSON.parse(JSON.stringify(trackRuleGroups)),
        trackRuleJingleCollisionStrategy: 'keep_both',
        scheduled : JSON.parse(JSON.stringify(scheduled)),
        tagPattern : [ "K1", "K2", "K1", "K3", "Jingle", "K1", "K2", "K1", "K2", "Jingle", "K1", "K2", "K3", "K1", "Jingle" ],
    };
    
    // Execute the shuffle function
    const result = executeShuffleFunction(tracks, opts, trackStats);
    assertNews(result);
    assertAdTriggers(result, 15, 45);
    assertBoundJingles(result);
    assertScheduledItem(result);
    assertTagPattern(result, opts.tagPattern);
});

