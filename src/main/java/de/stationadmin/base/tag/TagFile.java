/**
 * 
 */
package de.stationadmin.base.tag;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

/**
 * 
 * @author Frank Korf
 * 
 */
public class TagFile {
  private static final Logger log = LogManager.getLogger(TagFile.class);
  private static byte[] FILE_ID = { 0, 0, 3, 5 };
  private static final int DATA_OFFSET = 1024;
  private String filename;
  private String tagname;
  private String group;
  private int[] ids = new int[0];

  public TagFile(String filename) throws IOException {
    this.filename = filename;
    this.open();
  }

  private void open() throws IOException {
    RandomAccessFile file = new RandomAccessFile(filename, "rw");
    try {
      if (startsWithFileId(file)) {
        file.seek(4);
        this.tagname = file.readUTF();
        try {
          this.group = StringUtils.trimToNull(file.readUTF());
        } catch (Exception e) {

        }
      } else {
        this.convertLegacy(file);
      }
      this.ids = this.readIds(file);
      Arrays.sort(this.ids);
    } finally {
      file.close();
    }

  }

  private void convertLegacy(RandomAccessFile file) throws IOException {
    log.info("convert legacy file: " + this.filename);
    file.seek(0);
    this.tagname = file.readUTF();

    ArrayList<Integer> ids = new ArrayList<Integer>();
    for (int offset = DATA_OFFSET; offset < file.length(); offset++) {
      file.seek(offset);
      int value = file.read();
      for (int b = 0; b < 8; b++) {
        if ((value & (1 << b)) > 0) {
          int id = (offset - DATA_OFFSET) * 8 + b;
          ids.add(id);
        }
      }
    }

    int[] idArray = new int[ids.size()];
    for (int i = 0; i < ids.size(); i++) {
      idArray[i] = ids.get(i);
    }
    Arrays.sort(idArray);

    file.seek(0);
    file.write(FILE_ID);
    file.writeUTF(tagname);
    this.writeIds(file, idArray);

  }

  private int[] readIds(RandomAccessFile file) throws IOException {
    int bufSize = (int) file.length() - DATA_OFFSET;
    if (bufSize > 0) {
      byte[] buf = new byte[bufSize];
      file.seek(DATA_OFFSET);
      file.read(buf);

      int[] ids = new int[buf.length / 4];
      ByteArrayInputStream in = new ByteArrayInputStream(buf);
      DataInputStream dIn = new DataInputStream(in);
      for (int i = 0; i < ids.length; i++) {
        ids[i] = dIn.readInt();
      }

      return ids;
    } else {
      return new int[0];
    }
  }

  private void writeIds(RandomAccessFile file, int[] ids) throws IOException {
    ByteArrayOutputStream out = new ByteArrayOutputStream(ids.length * 4);
    DataOutputStream dOut = new DataOutputStream(out);
    for (int id : ids) {
      dOut.writeInt(id);
    }
    dOut.flush();
    out.flush();
    byte[] data = out.toByteArray();

    file.setLength(DATA_OFFSET + data.length);
    file.seek(DATA_OFFSET);
    file.write(data);

  }

  public TagFile(String filename, String tagname) throws IOException {
    this.filename = filename;
    this.tagname = tagname;
    RandomAccessFile file = new RandomAccessFile(filename, "rw");
    if (file.length() == 0) {
      file.setLength(DATA_OFFSET);
    }
    file.seek(0);
    file.write(FILE_ID);
    file.writeUTF(tagname);
    file.writeUTF("");
    file.close();
  }

  public static boolean startsWithFileId(RandomAccessFile file) throws IOException {
    file.seek(0);
    if (file.length() > 4) {
      byte[] head = new byte[4];
      if (file.read(head) == 4) {
        for (int i = 0; i < 4; i++) {
          if (FILE_ID[i] != head[i]) {
            return false;
          }
        }
        return true;
      }
    }
    return false;
  }

  /**
   * @return the tagname
   */
  protected String getTagname() {
    return tagname;
  }

  private RandomAccessFile getFile() throws IOException {
    return new RandomAccessFile(filename, "rw");
  }

  public void setTagname(String tagname) throws IOException {
    this.tagname = tagname;
    this.writeHeader();
  }

  void writeHeader() throws IOException {
    RandomAccessFile file = this.getFile();
    try {
      file.seek(0);
      file.write(FILE_ID);
      file.writeUTF(this.tagname);
      file.writeUTF(StringUtils.trimToEmpty(this.group));
    } finally {
      file.close();
    }

  }

  public void set(int... ids) throws IOException {
    Arrays.sort(ids);
    this.ids = ids;
    RandomAccessFile file = this.getFile();
    this.writeIds(file, this.ids);
    file.close();

  }

  public int[] tag(int... addIds) throws IOException {
    if (addIds.length == 0) {
      return new int[0];
    }
    Arrays.sort(addIds);

    int[] newArray = new int[this.ids.length + addIds.length];
    int newIdx = 0;

    int addIdx = 0;

    int[] newIds = new int[addIds.length];
    int newIdsIdx = 0;

    if (this.ids.length == 0 || addIds[0] > this.ids[this.ids.length - 1]) {
      // all new ids are greater than the existing ids - simplified merging
      System.arraycopy(this.ids, 0, newArray, 0, this.ids.length);
      newIdx = this.ids.length;
      // return addIds;
    } else {
      for (int i = 0; i < this.ids.length; i++) {
        while (addIdx < addIds.length && addIds[addIdx] <= this.ids[i]) {
          if (addIds[addIdx] != this.ids[i]) {
            newArray[newIdx++] = addIds[addIdx];
            newIds[newIdsIdx++] = addIds[addIdx];
          }
          addIdx++;
        }
        newArray[newIdx++] = this.ids[i];
      }
    }
    while (addIdx < addIds.length) {
      newArray[newIdx++] = addIds[addIdx];
      newIds[newIdsIdx++] = addIds[addIdx];
      addIdx++;
    }

    if (newIdx < newArray.length) {
      int[] tmp = new int[newIdx];
      System.arraycopy(newArray, 0, tmp, 0, newIdx);
      newArray = tmp;
    }
    if (newIdsIdx < newIds.length) {
      int[] tmp = new int[newIdsIdx];
      System.arraycopy(newIds, 0, tmp, 0, newIdsIdx);
      newIds = tmp;
    }

    this.ids = newArray;

    RandomAccessFile file = this.getFile();
    this.writeIds(file, this.ids);
    file.close();

    return newIds;

  }

  public int[] untag(int... removeIds) throws IOException {
    if (removeIds.length == 0) {
      return new int[0];
    }

    int[] removedIds = new int[removeIds.length];
    int rIdx = 0;

    Set<Integer> removeSet = new HashSet<Integer>();
    for (int id : removeIds) {
      removeSet.add(id);
    }

    int[] newArray = new int[this.ids.length];
    int newIdx = 0;
    for (int i = 0; i < this.ids.length; i++) {
      if (!removeSet.contains(this.ids[i])) {
        newArray[newIdx++] = this.ids[i];
      } else {
        removedIds[rIdx++] = this.ids[i];
      }
    }

    if (newIdx < newArray.length) {
      int[] tmp = new int[newIdx];
      System.arraycopy(newArray, 0, tmp, 0, newIdx);
      newArray = tmp;
    }

    if (rIdx < removedIds.length) {
      int[] tmp = new int[rIdx];
      System.arraycopy(removedIds, 0, tmp, 0, rIdx);
      removedIds = tmp;

    }

    this.ids = newArray;

    if (removedIds.length > 0) {
      RandomAccessFile file = this.getFile();
      this.writeIds(file, this.ids);
      file.close();
    }

    return removedIds;
  }

  public boolean isTagged(int id) throws IOException {
    return Arrays.binarySearch(this.ids, id) >= 0;
  }

  public void delete() throws IOException {
    new File(this.filename).delete();
  }

  /**
   * Gets all ids that are tagged
   * 
   * @return
   * @throws IOException
   */
  public int[] getIds() throws IOException {
    return this.ids;
  }

  public void writeRaw(InputStream stream) throws IOException {
    new File(this.filename).delete();
    try ( FileOutputStream out = new FileOutputStream(this.filename)) {
      IOUtils.copyLarge(stream, out);
    }
    this.open();
  }

  /**
   * @return the filename
   */
  public String getFilename() {
    return filename;
  }

  public String getGroup() {
    return group;
  }

  public void setGroup(String group) {
    this.group = group;
  }

}
