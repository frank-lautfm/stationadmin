/**
 * 
 */
package de.emjoy.stationadmin.test.playlist;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import de.stationadmin.base.tag.TagChecker;
import de.stationadmin.base.track.Title;

public class SomeTitleTagChecker implements TagChecker {
  private Map<String, Set<Integer>> taggedTitles = new HashMap<String, Set<Integer>>();

  public void register(String tag, int titleId) {
    Set<Integer> set = this.taggedTitles.get(tag);
    if (set == null) {
      set = new HashSet<Integer>();
      this.taggedTitles.put(tag, set);
    }
    set.add(titleId);
  }

  public void register(String tag, List<Title> titles) {
    Set<Integer> set = this.taggedTitles.get(tag);
    if (set == null) {
      set = new HashSet<Integer>();
      this.taggedTitles.put(tag, set);
    }
    for (Title title : titles) {
      set.add(title.getId());
    }
  }

  /**
   * @see de.stationadmin.base.tag.TagChecker#getTrackIds(java.lang.String)
   */
  @Override
  public int[] getTrackIds(String tagname) throws IOException {
    Set<Integer> set = this.taggedTitles.get(tagname);
    if (set != null) {
      int[] ids = new int[set.size()];
      int idx = 0;
      for (Integer id : set) {
        ids[idx++] = id;
      }
      return ids;
    }
    return new int[0];
  }

  /**
   * @see de.stationadmin.base.tag.TagChecker#isTagged(java.lang.String,
   *      int)
   */
  @Override
  public boolean isTagged(String tag, int titleId) throws IOException {
    return this.taggedTitles.containsKey(tag)
        && this.taggedTitles.get(tag).contains(titleId);
  }

}