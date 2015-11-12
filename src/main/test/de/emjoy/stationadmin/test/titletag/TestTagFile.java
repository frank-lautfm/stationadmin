package de.emjoy.stationadmin.test.titletag;

import java.io.File;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

import junit.framework.Assert;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import de.stationadmin.base.tag.TagFile;

public class TestTagFile {
  private TagFile file;

  @Before
  public void setUp() throws Exception {
    String dir = System.getProperty("java.io.tmpdir");
    String filename = UUID.randomUUID().toString() + ".tag";
    this.file = new TagFile(dir + File.separatorChar + filename, "Test");
  }

  @After
  public void cleanUp() throws Exception {
    if (this.file != null) {
      this.file.delete();
      this.file = null;
    }
  }

  @Test
  public void testAddRemove() throws Exception {

    Set<Integer> control = new HashSet<Integer>();

    // add an initial set of values
    
    int[] ids = new int[200];
    int value = 1;
    for (int i = 0; i < ids.length; i++) {
      ids[i] = value;
      control.add(value);
      value += 3;
    }
    
    this.file.tag(ids);
    
    this.assertValues(control);

    // re-open and check again
    this.file = new TagFile(this.file.getFilename());
    this.assertValues(control);
    
    // append additional values
    ids = new int[20];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = value;
      control.add(value);
      value += 3;
    }
    this.file.tag(ids);
    this.assertValues(control);
    
    // add values in-between
    value = 4;
    ids = new int[20];
    for (int i = 0; i < ids.length; i++) {
      ids[i] = value;
      control.add(value);
      value += 7;
    }
    this.file.tag(ids);
    this.assertValues(control);
    
    ids = new int[20];
    int[] all = this.file.getIds();
    int aIdx = 3;
    for(int i = 0; i < ids.length; i++) {
      ids[i] = all[aIdx];
      control.remove(all[aIdx]);
      aIdx+=3;
    }
    
    this.file.untag(ids);
    this.assertValues(control);

  }
  
  private void assertValues(Set<Integer> control) throws Exception {
    HashSet<Integer> expected = new HashSet<Integer>(control);
    int[] retrieved = this.file.getIds();
    Assert.assertEquals("number of ids", expected.size(), retrieved.length);
    for(int i = 0; i < retrieved.length; i++) {
      Assert.assertTrue("invalid id: " + retrieved[i], expected.remove(retrieved[i]));
    }
    Assert.assertTrue("some ids not found: " + expected, expected.isEmpty());
  }

}
