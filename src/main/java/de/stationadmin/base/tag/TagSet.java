package de.stationadmin.base.tag;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;

/**
 * A TitleTagSet is a collection of tags that are used to filter titles. If
 * includeTags are given only documents are accepted that are in at least one of
 * the given tags. If excludeTags are given only documents are accepted that are
 * not in any of the given tags.
 * 
 * @author korf
 */
public class TagSet {
  private String name;
  private String[] includeTags;
  private String[] excludeTags;
  private String registeredName;
  private String filename;

  protected TagSet() {
    super();
  }

  public TagSet(String name) {
    super();
    this.name = name;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String[] getIncludeTags() {
    return includeTags;
  }

  public void setIncludeTags(String[] includeTags) {
    this.includeTags = includeTags;
  }

  public String[] getExcludeTags() {
    return excludeTags;
  }

  public void setExcludeTags(String[] excludeTags) {
    this.excludeTags = excludeTags;
  }

  void save() throws IOException {
    StringBuilder buf = new StringBuilder();
    buf.append("name = " + this.name + "\n");
    buf.append("includes = ");
    if (this.includeTags != null) {
      for (int i = 0; this.includeTags != null && i < this.includeTags.length; i++) {
        if (i > 0) {
          buf.append('\t');
        }
        buf.append(this.includeTags[i]);
      }
    }
    buf.append('\n');
    buf.append("excludes = ");
    for (int i = 0; this.excludeTags != null && i < this.excludeTags.length; i++) {
      if (i > 0) {
        buf.append('\t');
      }
      buf.append(this.excludeTags[i]);
    }
    buf.append('\n');

    FileOutputStream out = new FileOutputStream(new File(filename));
    IOUtils.write(buf.toString(), out, "UTF-8");
    out.close();
  }

  void load() throws IOException {
    FileInputStream input = new FileInputStream(new File(filename));
    @SuppressWarnings("unchecked")
    List<String> lines = (List<String>) IOUtils.readLines(input);
    input.close();

    for (String line : lines) {
      int eq = line.indexOf('=');
      if (eq > -1 && eq < line.length() - 2) {
        String key = line.substring(0, eq).trim();
        String value = line.substring(eq + 1).trim();
        if (key.equals("name")) {
          this.name = value;
        } else if (key.equals("includes")) {
          this.includeTags = StringUtils.split(value, "\t");
        } else if (key.equals("excludes")) {
          this.excludeTags = StringUtils.split(value, "\t");
        }

      }
    }

  }

  protected String getRegisteredName() {
    return registeredName;
  }

  protected void setRegisteredName(String registeredName) {
    this.registeredName = registeredName;
  }

  protected String getFilename() {
    return filename;
  }

  protected void setFilename(String filename) {
    this.filename = filename;
  }

  @Override
  public String toString() {
    return this.name;
  }

  @Override
  public int hashCode() {
    return this.filename != null ? this.filename.hashCode() : 0;
  }

  @Override
  public boolean equals(Object obj) {
    return obj instanceof TagSet && StringUtils.equals(((TagSet)obj).filename, this.filename);
  }

}
