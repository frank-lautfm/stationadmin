/**
 * 
 */
package de.emjoy.stationadmin.test.format;

import java.util.Date;

import org.junit.Assert;
import org.junit.Test;

import de.stationadmin.base.track.RegisteredTrack;
import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.track.format.ExtendedTrackFormat;
import de.stationadmin.base.track.format.ExtendedTrackFormat.TrackDetailLevel;

/**
 * @author Frank
 *
 */
public class TestExtendedTitleFormat {

  @Test
  public void testSimpleTitle() throws Exception {

    BasicTrack t = new BasicTrack();
    t.setArtist("Foo & the Bars");
    t.setTitle("What a foo");
    t.setId(123);
    t.setLength(180);
    t.setType(BasicTrack.TYPE_MUSIC);

    ExtendedTrackFormat fmt = new ExtendedTrackFormat();
    String str = fmt.toString(t);

    BasicTrack t2 = fmt.fromString(str);
    Assert.assertEquals("id", t.getId(), t2.getId());
    Assert.assertEquals("artist", t.getArtist(), t2.getArtist());
    Assert.assertEquals("title", t.getTitle(), t2.getTitle());
    Assert.assertEquals("length", t.getLength(), t2.getLength());
    Assert.assertEquals("type", t.getType(), t2.getType());

  }

  @Test
  public void testExtendedTitle() throws Exception {

    RegisteredTrack t = new RegisteredTrack();
    t.setArtist("Foo & the Bars");
    t.setTitle("What a foo");
    t.setId(123);
    t.setLength(180);
    t.setType(BasicTrack.TYPE_MUSIC);
    t.setAlbum("Foobar");
    t.setGenre("FooMucke");
    t.setOwnTrack(true);
    t.setPrivateTrack(false);
    t.setYear(2011);
    t.setUploadDate(new Date(System.currentTimeMillis()));

    ExtendedTrackFormat fmt = new ExtendedTrackFormat(TrackDetailLevel.FULL);

    {
      String str = fmt.toString(t);
      RegisteredTrack t2 = (RegisteredTrack) fmt.fromString(str);
      assertEquals(t, t2);
    }

    {
      t.setAlbum(null);
      t.setGenre(null);
      String str = fmt.toString(t);
      RegisteredTrack t2 = (RegisteredTrack) fmt.fromString(str);
      assertEquals(t, t2);
    }

    {
      t.setUploadDate(null);
      t.setYear(0);
      String str = fmt.toString(t);
      RegisteredTrack t2 = (RegisteredTrack) fmt.fromString(str);
      assertEquals(t, t2);
    }

  }

  private static void assertEquals(RegisteredTrack t, RegisteredTrack t2) {
    Assert.assertEquals("id", t.getId(), t2.getId());
    Assert.assertEquals("artist", t.getArtist(), t2.getArtist());
    Assert.assertEquals("title", t.getTitle(), t2.getTitle());
    Assert.assertEquals("length", t.getLength(), t2.getLength());
    Assert.assertEquals("type", t.getType(), t2.getType());
    Assert.assertEquals("album", t.getAlbum(), t2.getAlbum());
    Assert.assertEquals("genre", t.getGenre(), t2.getGenre());
    Assert.assertEquals("year", t.getYear(), t2.getYear());
    Assert.assertEquals("upload date", t.getUploadDate(), t2.getUploadDate());
    Assert.assertEquals("own", t.isOwnTrack(), t2.isOwnTrack());
    Assert.assertEquals("private", t.isPrivateTrack(), t2.isPrivateTrack());

  }

}
