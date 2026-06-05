package de.stationadmin.base.playlist;

/**
 * Specifies which news/weather track(s) should be added when auto-filling a playlist.
 */
public enum NewsTrackOption {
  /** laut.fm Nachrichten &amp; Wetter combined (track ID 1) - default */
  NEWS_WITH_WEATHER,
  /** laut.fm Nachrichten only (track ID 2) */
  NEWS,
  /** laut.fm Wetter only (track ID 3) */
  WEATHER,
  /** laut.fm Nachrichten (track ID 2) at beginning + Wetter (track ID 3) separately */
  NEWS_AND_WEATHER
}
