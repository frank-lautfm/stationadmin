package de.stationadmin.gui.playlist.config.shuffle;

import java.util.HashMap;
import java.util.Map;

public class TagSequenceRule {
  private String[] pattern;
  private boolean not = false;
  private String next;

  public TagSequenceRule() {
  }
  
  public TagSequenceRule(String[] pattern, boolean not, String next) {
    super();
    this.pattern = pattern;
    this.not = not;
    this.next = next;
  }

  public String[] getPattern() {
    return pattern;
  }

  public void setPattern(String[] pattern) {
    this.pattern = pattern;
  }

  public boolean isNot() {
    return not;
  }

  public void setNot(boolean not) {
    this.not = not;
  }

  public String getNext() {
    return next;
  }

  public void setNext(String next) {
    this.next = next;
  }
  
  public Map<String, Object> toMap() {
    HashMap<String, Object> map = new HashMap<>();
    map.put("pattern", pattern);
    map.put("not", not);
    map.put("next", next);
    return map;
  }

}
