/**
 * 
 */
package de.stationadmin.base.loganalyzer;

/**
 * Holds the frequency of an item (artist or title)
 * 
 * @author korf
 */
public class ItemFrequency<T> implements Comparable<ItemFrequency<?>>{
  private T item;
  private int frequency;

  public ItemFrequency(T item, int frequency) {
    super();
    this.item = item;
    this.frequency = frequency;
  }
  
  public void inc() {
    this.frequency++;
  }

  public T getItem() {
    return item;
  }

  public int getFrequency() {
    return frequency;
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(ItemFrequency<?> o) {
    return -Integer.valueOf(this.frequency).compareTo(o.frequency);
  }

  @Override
  public String toString() {
    return this.frequency + "x " + this.item;
  }
}
