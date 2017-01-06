/**
 * 
 */
package de.stationadmin.base.subscription;

import org.apache.commons.lang.StringUtils;

/**
 * Single subscription query - a string that must match a field (artist, title, genre...) exactly or partially
 * 
 * @author korf
 */
public class Subscription {
  private boolean isNew = true;
  private Field field = Field.ARTIST;
  private String query;
  private boolean equals = false;

  public Subscription() {
    
  }

  /**
   * Constructor
   * 
   * @param field
   * @param query
   * @param equals
   */
  public Subscription(Field field, String query, boolean equals) {
    super();
    this.field = field;
    this.query = query;
    this.equals = equals;
  }
  
  public Subscription(String string) {
    String[] parts = StringUtils.split(string, " ", 3);
    this.field = Field.valueOf(parts[0]);
    this.equals = parts[1].equals("=");
    this.query = parts[2];
  }

  
  public String asString() {
    return field.name() + " " + (this.equals ? "=" : "~") + " " + this.query;
  }

  public enum Field {
    ARTIST("artist"), TITLE("title"), ALBUM("album"), GENRE("genre");
    
    private String rawName;
    
    private Field(String rawName) {
      this.rawName = rawName;
    }
    
    public String getRawName() {
      return this.rawName;
    }
    
    
    
  }

  public Field getField() {
    return field;
  }

  public void setField(Field field) {
    this.field = field;
  }

  public String getQuery() {
    return query;
  }

  public void setQuery(String query) {
    this.query = query;
  }

  public boolean isEquals() {
    return equals;
  }

  public void setEquals(boolean equals) {
    this.equals = equals;
  }

  @Override
  public boolean equals(Object obj) {
    if(obj instanceof Subscription) {
      Subscription other = (Subscription)obj;
      return this.field == other.field && this.equals == other.equals && StringUtils.equals(this.query, other.query);
    }
    return false;
  }

  @Override
  public int hashCode() {
    return this.query.hashCode();
  }

  public boolean isNew() {
    return isNew;
  }

  void setNew(boolean isNew) {
    this.isNew = isNew;
  }

  @Override
  public String toString() {
    return this.query;
  }
}
