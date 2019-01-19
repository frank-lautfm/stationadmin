package de.stationadmin.base.playlist.validation;

import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.Entry;

public class AdTriggerValidator implements PlaylistValidator {

  @Override
  public boolean validate(Playlist playlist, List<Entry> violations) {

    List<Entry> adTriggers = null;
    if (!playlist.isShuffle()) {
      for (Entry entry : playlist.getEntries()) {
        if (entry.getTrack() != null && entry.getTrack().getArtist() != null && entry.getTrack().getTitle() != null) {
          if (entry.getTrack().getArtist().contains("START_AD_BREAK") || entry.getTrack().getTitle().contains("START_AD_BREAK")) {
            if (adTriggers == null) {
              adTriggers = new ArrayList<Playlist.Entry>();
            }
            adTriggers.add(entry);
          }
        }
      }

      if (adTriggers != null) {
        boolean valid = true;

        // add first entry again to check rules when playlists starts from beginning
        Entry firstEntry = adTriggers.get(0);
        adTriggers.add(new Entry(firstEntry.getStart() + playlist.getLength(), firstEntry.getStart(), firstEntry.getTrack()));

        int len = playlist.getLength() / 60;
        BitSet semiHours = new BitSet();
        for (int i = 0; i < adTriggers.size(); i++) {
          if (i > 0) {
            int dist = adTriggers.get(i).getStart() - adTriggers.get(i - 1).getStart();
            if (dist < 15 * 60) {
              // distance violation
              valid = false;
              violations.add(adTriggers.get(i - 1));
              violations.add(adTriggers.get(i));
            }
          }

          int startMinute = adTriggers.get(i).getStart() / 60;
          int semiHour = startMinute / 30;
          semiHours.set(semiHour);
        }

        // This approach accepts a missing ad trigger in the last semi hour if that is
        // not completely filled
        // (e. g. playlist time of 2 hours and 15 minutes)
        int numSemiHours = len / 30;
        for (int semiHour = 0; semiHour < numSemiHours; semiHour++) {
          if (!semiHours.get(semiHour)) {
            valid = false;
          }
        }
        /*
         * // This approach would not accept a missing trigger in the last semi hour for
         * (int i = 0; i < len; i += 30) { int semiHour = i / 30;
         * if(!semiHours.get(semiHour)) { valid = false; } }
         */

        return valid;
      }
    }

    return true;
  }

}
