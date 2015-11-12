/**
 * 
 */
package de.emjoy.stationadmin.test.playlist;


import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

import de.stationadmin.base.playlist.shuffle.TagSequenceAdvice;
import de.stationadmin.base.track.Title;

/**
 * @author korf
 *
 */
public class TagTagSequenceAdvice {
  private SomeTitleTagChecker tagChecker;
  
  private List<Title> titlesA = new ArrayList<Title>();
  private List<Title> titlesB = new ArrayList<Title>();
  private List<Title> titlesC = new ArrayList<Title>();
  private List<Title> jingles = new ArrayList<Title>();
  
  @Before
  public void setUp() {
    this.tagChecker = new SomeTitleTagChecker();
    
    int id = 1;
    for(int i = 0; i < 10; i++) {
      this.titlesA.add(createTitle(id++, "A" + i, false));
      this.titlesB.add(createTitle(id++, "B" + i, false));
      this.titlesC.add(createTitle(id++, "C" + i, false));
      this.jingles.add(createTitle(id++, "J" + i, true));
    }
    
    this.tagChecker.register("A", this.titlesA);
    this.tagChecker.register("B", this.titlesB);
    this.tagChecker.register("C", this.titlesC);
  }
  
  private Title createTitle(int id, String name, boolean jingle) {
    Title title = new Title();
    title.setId(id);
    title.setArtist("Artist of " + name);
    title.setTitle(name);
    title.setLength(300);
    title.setType(jingle ? Title.TYPE_JINGLE : Title.TYPE_MUSIC);
    return title;
  }

  @Test
  public void testSimplePositive() throws Exception {
    // A => B
    TagSequenceAdvice rule = new TagSequenceAdvice(this.tagChecker, new String[] { "A" }, true, "B");

    // applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3));
      Assert.assertTrue("A => B: A => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertFalse("A => B: A => C", rule.accept(titles, this.titlesC.get(0)));
    }
    
    // applicable title list with jingle
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3), this.jingles.get(0));
      Assert.assertTrue("A => B: A,J => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertFalse("A => B: A,J => C", rule.accept(titles, this.titlesC.get(0)));
    }
    
    // non-applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesB.get(3));
      Assert.assertTrue("A => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertTrue("A => B", rule.accept(titles, this.titlesC.get(0)));
    }
    
  }
  
  @Test
  public void testSimpleNegative() throws Exception {
    // A =>! B
    TagSequenceAdvice rule = new TagSequenceAdvice(this.tagChecker, new String[] { "A" }, false, "B");

    // applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3));
      Assert.assertFalse("A => B: A => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertTrue("A => B: A => C", rule.accept(titles, this.titlesC.get(0)));
    }
    
    // applicable title list with jingle
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3), this.jingles.get(0));
      Assert.assertFalse("A => B: A,J => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertTrue("A => B: A,J => C", rule.accept(titles, this.titlesC.get(0)));
    }
    
    // non-applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesB.get(3));
      Assert.assertTrue("A => B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertTrue("A => B", rule.accept(titles, this.titlesC.get(0)));
    }
    
  }
  
  
  @Test
  public void testComplexPositive() throws Exception {
    // A, B => C
    TagSequenceAdvice rule = new TagSequenceAdvice(this.tagChecker, new String[] { "A", "B" }, true, "C");

    // applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3), this.titlesB.get(1));
      Assert.assertTrue("A,B => C: A,B => C", rule.accept(titles, this.titlesC.get(0)));
      Assert.assertFalse("A,B => C: A,B => C", rule.accept(titles, this.titlesA.get(0)));
    }
    
    // applicable title list with jingle
    {
      List<Title> titles = Arrays.asList(this.titlesA.get(3), this.jingles.get(0), this.titlesB.get(1));
      Assert.assertTrue("A,B => C: A,J,B => C", rule.accept(titles, this.titlesC.get(0)));
      Assert.assertFalse("A,B => C: A,J,B => C", rule.accept(titles, this.titlesA.get(0)));
    }
    
    // non-applicable title list
    {
      List<Title> titles = Arrays.asList(this.titlesB.get(2), this.titlesB.get(3));
      Assert.assertTrue("A,B => C: B,B", rule.accept(titles, this.titlesB.get(0)));
      Assert.assertTrue("A,B => C: B,B", rule.accept(titles, this.titlesC.get(0)));
    }
    
  }
 
  @Test
  public void testJSONSerialization() throws Exception {
    TagSequenceAdvice rule = new TagSequenceAdvice(this.tagChecker, new String[] { "A", "B" }, true, "C");
    TagSequenceAdvice copy = new TagSequenceAdvice(this.tagChecker, rule.toJSON());
    
    Assert.assertArrayEquals("pattern", rule.getPattern(), copy.getPattern());
    Assert.assertEquals("next", rule.getNext(), copy.getNext());
    Assert.assertEquals("match", rule.isNextMustMatch(), copy.isNextMustMatch());
    
  }
}
