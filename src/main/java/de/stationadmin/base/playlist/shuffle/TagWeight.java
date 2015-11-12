/**
 * 
 */
package de.stationadmin.base.playlist.shuffle;

import java.io.Serializable;

/**
 * @author korf
 * 
 */
public class TagWeight implements Serializable, Comparable<TagWeight> {
  private static final long serialVersionUID = -3576031795553153260L;

  private String tag;
  private int weight;
  private float maxFraction;
  
  public TagWeight() {
    
  }
  
  /**
   * @param tag
   * @param weight
   * @param maxFraction
   */
  public TagWeight(String tag, int weight, float maxFraction) {
    super();
    this.tag = tag;
    this.weight = weight;
    this.maxFraction = maxFraction;
  }

  /**
   * @return the maxFraction
   */
  public float getMaxFraction() {
    return maxFraction;
  }

  /**
   * Gets the tag which weighting information is configured
   * 
   * @return the tag
   */
  public String getTag() {
    return tag;
  }

  /**
   * Gets the weight for the tag - a value between -4 and 3
   * 
   * @return the weight
   */
  public int getWeight() {
    return weight;
  }

  /**
   * Gets the maximum fraction for tracks tagged with the configured tag within
   * a playlist
   * 
   * @param maxFraction
   *          the maxFraction to set
   */
  public void setMaxFraction(float maxFraction) {
    this.maxFraction = maxFraction;
  }

  /**
   * @param tag
   *          the tag to set
   */
  public void setTag(String tag) {
    this.tag = tag;
  }

  /**
   * @param weight
   *          the weight to set
   */
  public void setWeight(int weight) {
    this.weight = weight;
  }

  /* (non-Javadoc)
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(TagWeight o) {
    String n1 = this.tag != null ? this.tag.toLowerCase() : "";
    String n2 = o.tag != null ? o.tag.toLowerCase() : "";
    return n1.compareTo(n2);
  }

}
