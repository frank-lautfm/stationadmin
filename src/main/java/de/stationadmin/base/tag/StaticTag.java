/**
 * 
 */
package de.stationadmin.base.tag;

import java.io.IOException;
import java.io.InputStream;

/**
 *
 * @author Frank Korf
 *
 */
public class StaticTag implements Tag {
  private TagFile tagFile;
  private String name;
  private String group;
  
  public StaticTag() {
  }
  
  protected StaticTag(TagFile file) {
    this.tagFile = file;
    this.name = this.tagFile.getTagname();
    this.group = this.tagFile.getGroup();
  }

  /**
   * @see de.stationadmin.base.tag.Tag#getName()
   */
  @Override
  public String getName() {
    return name;
  }

  /**
   * Replaces the content of the underlying tag file with the data from
   * the given input stream. Used for backup purposes.
   * @param stream
   * @throws IOException
   */
  public void writeRaw(InputStream stream) throws IOException {
    this.tagFile.writeRaw(stream);
  }

  /**
   * @return the tagFile
   */
  protected TagFile getTagFile() {
    return tagFile;
  }

  /**
   * @see java.lang.Object#toString()
   */
  @Override
  public String toString() {
    return this.tagFile.getTagname();
  }

  /**
   * @see java.lang.Comparable#compareTo(java.lang.Object)
   */
  @Override
  public int compareTo(Tag o) {
    return this.getName().compareToIgnoreCase(o.getName());
  }

  /**
   * @param name the name to set
   */
  public void setName(String name) {
    this.name = name;
  }

  /**
   * @param tagFile the tagFile to set
   */
  protected void setTagFile(TagFile tagFile) {
    this.tagFile = tagFile;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }
  
  public boolean isDateFilterTag() {
  	return this.name != null && this.name.startsWith("@") && this.name.matches("\\@\\d{1,2}\\.\\d{1,2}\\.\\s*-\\s*\\d{1,2}\\.\\d{1,2}\\..*");
  }
  
  public boolean isGroupingTag() {
  	return this.name != null && this.name.startsWith("=");
  }
}
