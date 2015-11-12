/**
 * 
 */
package de.stationadmin.lfmapi;

import java.io.Serializable;

import org.json.JSONObject;

/**
 * @author Frank
 *
 */
public class Artist implements Serializable {
  private static final long serialVersionUID = -6156976101597475556L;
  private String name;
  private String imageURL;
  private String url;
  private String lautURL;
  private String lautTeaser;

  public Artist(JSONObject json) {
    this.name = JSONUtil.getString(json, "name");
    this.imageURL = JSONUtil.getString(json, "image");
    this.url = JSONUtil.getString(json, "url");
    this.lautURL = JSONUtil.getString(json, "laut_url");
    this.lautTeaser = JSONUtil.getString(json, "laut_teaser");
  }

  /**
   * @return the name
   */
  public String getName() {
    return name;
  }

  /**
   * @return the imageURL
   */
  public String getImageURL() {
    return imageURL;
  }

  /**
   * @return the url
   */
  public String getUrl() {
    return url;
  }

  /**
   * @return the lautURL
   */
  public String getLautURL() {
    return lautURL;
  }

  /**
   * @return the lautTeaser
   */
  public String getLautTeaser() {
    return lautTeaser;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.name;
  }

}
