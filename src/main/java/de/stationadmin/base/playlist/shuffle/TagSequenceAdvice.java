package de.stationadmin.base.playlist.shuffle;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.lang.ArrayUtils;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import de.stationadmin.base.tag.TagChecker;
import de.stationadmin.base.track.Title;

/**
 * Based in title tags, a <code>TagRule</code> defines what titles must or must
 * not come next based on a previously seen pattern.
 * 
 * @author korf
 */
public class TagSequenceAdvice implements Advice {
  private TagChecker tagChecker;
  private String[] pattern;
  private boolean nextMustMatch = false;
  private String next;

  private Map<String, Set<Integer>> taggedTitles = new HashMap<String, Set<Integer>>();

  /**
   * Constructor
   * 
   * @param tagManager
   * @param pattern
   *          tag names - titles with these tags must appear in the given order
   *          to make the rule applyable
   * @param nextMustMatch
   *          <code>false</code> if the next title should not be tagged with
   *          <code>next</code>, <code>true</code> if it should be tagged with
   *          <code>next</code>
   * @param next
   *          name of the tag the next title should or should not be tagged with
   */
  public TagSequenceAdvice(TagChecker tagChecker, String[] pattern, boolean nextMustMatch,
      String next) throws IOException {
    super();
    this.tagChecker = tagChecker;
    this.pattern = pattern;
    this.nextMustMatch = nextMustMatch;
    this.next = next;
    this.init();
  }
  
  public TagSequenceAdvice(TagChecker tagChecker, String json) throws IOException, JSONException {
    this(tagChecker, new JSONObject(json));
  }
  
  public TagSequenceAdvice(TagChecker tagChecker, JSONObject obj) throws IOException, JSONException {
    this.tagChecker = tagChecker;
    int type = obj.getInt("type");
    if(type != 1) {
      throw new IllegalArgumentException("Wrong type");
    }
    
    JSONArray pArr = obj.getJSONArray("pattern");
    if(pArr != null) {
      this.pattern = new String[pArr.length()];
      for(int i = 0; i < pArr.length(); i++) {
        this.pattern[i] = pArr.getString(i);
      }
    }
    this.nextMustMatch = obj.getBoolean("match");
    this.next = obj.getString("next");
    this.init();
    
  }


  /**
   * Prepare {@link #taggedTitles}
   * 
   * @throws IOException
   */
  private void init() throws IOException {
    ArrayList<String> tags = new ArrayList<String>(Arrays.asList(pattern));
    tags.add(next);

    for (String tag : tags) {
      if (!this.taggedTitles.containsKey(tag)) {
        int[] ids = this.tagChecker.getTrackIds(tag);
        HashSet<Integer> set = new HashSet<Integer>();
        for (int id : ids) {
          set.add(id);
        }
        this.taggedTitles.put(tag, set);
      }
    }

  }

  @Override
  public boolean accept(List<Title> titles, Title candidate) {
    if (pattern.length > titles.size()) {
      // not applicable
      return true;
    }

    int t = titles.size() - 1;
    for (int p = pattern.length - 1; p >= 0; p--) {
      Set<Integer> tagTitles = this.taggedTitles.get(pattern[p]);
      Title title = titles.get(t);
      while (title.getType() != Title.TYPE_MUSIC) {
        t--;
        if (t < 0) {
          return true; // not applicable
        }
        title = titles.get(t);
      }
      if (!tagTitles.contains(title.getId())) {
        return true; // no pattern match
      }
      t--;
    }

    // pattern does match - check rule for next title
    boolean candidateMatch = this.taggedTitles.get(this.next).contains(
        candidate.getId());

    if (nextMustMatch) {
      return candidateMatch == true;
    } else {
      return candidateMatch == false;
    }

  }

  public String[] getPattern() {
    return pattern;
  }

  public boolean isNextMustMatch() {
    return nextMustMatch;
  }

  public String getNext() {
    return next;
  }

  @Override
  public String toString() {
    return ArrayUtils.toString(this.pattern) + " => " + (!this.nextMustMatch ? "!" : "")
        + " " + this.next;
  }
  
  public String toJSON() throws JSONException {
    
    JSONObject obj = new JSONObject();
    obj.put("type", 1);
    obj.put("pattern", Arrays.asList(this.pattern));
    obj.put("match", this.nextMustMatch);
    obj.put("next", this.next);
   
    return obj.toString();
  }

  @Override
  public int hashCode() {
    return this.toString().hashCode();
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TagSequenceAdvice && this.toString().equals(obj.toString());
  }

}
