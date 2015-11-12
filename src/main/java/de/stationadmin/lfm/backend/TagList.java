/**
 * 
 */
package de.stationadmin.lfm.backend;

/**
 * @author korf
 *
 */
public class TagList {
  private String[] tags;
  
  public TagList() {
    
  }
  
  public TagList(String... tags) {
    this.tags = tags;
  }

  public String[] getTags() {
    return tags;
  }

  public void setTags(String[] tags) {
    this.tags = tags;
  }

}
