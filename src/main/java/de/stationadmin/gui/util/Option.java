/**
 * 
 */
package de.stationadmin.gui.util;

import java.io.Serializable;

/**
 * @author Frank
 *
 */
public class Option implements Serializable {
  private static final long serialVersionUID = -7625438658417600795L;
  private Object key;
  private String label;

  /**
   * @param key
   * @param label
   */
  public Option(Object key, String label) {
    super();
    this.key = key;
    this.label = label;
  }

  /**
   * @return the key
   */
  public Object getKey() {
    return key;
  }

  /**
   * @return the label
   */
  public String getLabel() {
    return label;
  }

  /**
   * @see java.lang.Object#hashCode()
   */
  @Override
  public int hashCode() {
    return this.key.hashCode();
  }

  /**
   * @see java.lang.Object#equals(java.lang.Object)
   */
  @Override
  public boolean equals(Object obj) {
    return obj instanceof Option && ((Option) obj).key.equals(this.key);
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.label;
  }

}
