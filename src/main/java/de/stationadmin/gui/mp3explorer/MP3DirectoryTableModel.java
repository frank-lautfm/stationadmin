/**
 * 
 */
package de.stationadmin.gui.mp3explorer;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.swing.SwingUtilities;
import javax.swing.table.AbstractTableModel;

import org.apache.commons.lang.StringUtils;
import org.jaudiotagger.tag.FieldKey;
import org.jaudiotagger.tag.Tag;

import de.stationadmin.base.playlist.Playlist;
import de.stationadmin.base.playlist.Playlist.PlaylistType;
import de.stationadmin.base.playlist.trackimport.MP3TrackImportTask;
import de.stationadmin.base.playlist.trackimport.TrackImportHandler;
import de.stationadmin.base.tag.TagManager;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.util.TrackMetaDataReaderFactory;
import de.stationadmin.gui.TextProvider;

/**
 * @author Frank Korf
 * 
 */
public class MP3DirectoryTableModel extends AbstractTableModel {
  private static final long serialVersionUID = -7824053476633059435L;

  private TextProvider textProvider;
  private List<MP3File> files = new ArrayList<MP3File>();
  private File[] directories;
  private boolean recursive = false;
  private int maxTitles = 500;
  private TrackImportHandler titleImportHandler;
  private int resolveId = 0;

  public MP3DirectoryTableModel(TextProvider textProvider, TrackService titleService, TagManager titleTagService) {
    super();
    this.textProvider = textProvider;
    this.titleImportHandler = new TrackImportHandler(titleService, titleTagService, new Playlist(titleService.getTrackRegistry(), PlaylistType.TEMPORARY), 0);
  }

  /**
   * @see javax.swing.table.TableModel#getColumnCount()
   */
  @Override
  public int getColumnCount() {
    return Column.values().length;
  }

  /**
   * @see javax.swing.table.TableModel#getRowCount()
   */
  @Override
  public int getRowCount() {
    return this.files.size();
  }

  public MP3File getFileAt(int row) {
    return this.files.get(row);
  }

  /**
   * @see javax.swing.table.TableModel#getValueAt(int, int)
   */
  @Override
  public Object getValueAt(int rowIndex, int columnIndex) {
    Column col = Column.values()[columnIndex];
    MP3File file = this.files.get(rowIndex);
    Tag tag = file.tag;
    switch (col) {
    case FILENAME:
      return file.file.getName();
    case ALBUM:
      return tag != null ? new Album(tag) : null;
    case ARTIST:
      return tag != null ? StringUtils.trimToNull(tag.getFirst(FieldKey.ARTIST)) : null;
    case TITLE:
      return tag != null ? StringUtils.trimToNull(tag.getFirst(FieldKey.TITLE)) : null;
    case TRACKNO:
      if (tag != null) {
        String trackStr = tag.getFirst(FieldKey.TRACK);
        if (trackStr != null && !trackStr.trim().isEmpty()) {
          try {
            return Integer.parseInt(trackStr.trim());
          } catch (NumberFormatException e) {
            // ignore malformed track number
          }
        }
      }
      return 0;
    case SIZE:
      return file.size;
    }

    return null;
  }

  enum Column {
    FILENAME, ARTIST, TITLE, TRACKNO, ALBUM, SIZE

  }

  /**
   * @see javax.swing.table.AbstractTableModel#getColumnName(int)
   */
  @Override
  public String getColumnName(int column) {
    Column col = Column.values()[column];
    switch (col) {
    case ARTIST:
      return textProvider.getString("trackviewer.property.artist");
    case TITLE:
      return textProvider.getString("trackviewer.property.title");
    case ALBUM:
      return textProvider.getString("trackviewer.property.album");
    case FILENAME:
      return textProvider.getString("mp3explorer.property.filename");
    case TRACKNO:
      return textProvider.getString("mp3explorer.property.trackno");
    case SIZE:
      return textProvider.getString("mp3explorer.property.size");
    }

    return col.name().toLowerCase();
  }

  private static class Album implements Comparable<Album> {
    int trackNo;
    String name;

    Album(Tag tag) {
      if (tag != null) {
        this.name = tag.getFirst(FieldKey.ALBUM);
        String trackStr = tag.getFirst(FieldKey.TRACK);
        if (trackStr != null && !trackStr.trim().isEmpty()) {
          try {
            this.trackNo = Integer.parseInt(trackStr.trim());
          } catch (NumberFormatException e) {
            // ignore malformed track number
          }
        }
      }
    }

    /**
     * @see java.lang.Comparable#compareTo(java.lang.Object)
     */
    @Override
    public int compareTo(Album o) {
      if (o == null) {
        return -1;
      }
      int result = StringUtils.trimToEmpty(this.name).compareToIgnoreCase(StringUtils.trimToEmpty(o.name));
      if (result == 0) {
        result = Integer.valueOf(trackNo).compareTo(o.trackNo);
      }
      return result;
    }

    public String toString() {
      return this.name;
    }
  }

  /**
   * @return the directories
   */
  public File[] getDirectories() {
    return directories;
  }

  /**
   * @param directories
   *          the directories to set
   */
  public void setDirectories(File[] directories) throws TooManyTitlesException {
    this.directories = directories;
    this.refresh();
  }

  /**
   * @return the recursive
   */
  public boolean isRecursive() {
    return recursive;
  }

  /**
   * @param recursive
   *          the recursive to set
   */
  public void setRecursive(boolean recursive) throws TooManyTitlesException {
    if (this.recursive != recursive) {
      this.recursive = recursive;
    }
    this.refresh();
  }

  private void refresh() throws TooManyTitlesException {
    Map<String, MP3File> resolved = new HashMap<String, MP3File>();
    for (MP3File file : this.files) {
      resolved.put(file.getFile().getAbsolutePath(), file);
    }
    this.files.clear();
    try {
      if (this.directories != null) {
        LinkedList<File> dirs = new LinkedList<File>();
        dirs.addAll(Arrays.asList(this.directories));
        while (!dirs.isEmpty()) {
          File directory = dirs.removeFirst();
          File[] files = directory.listFiles();
          if (files != null) {
            for (File file : files) {
              if (file.isDirectory()) {
                if (this.recursive) {
                  dirs.add(file);
                }
              } else if (TrackMetaDataReaderFactory.isSupportedAudioFile(file)) {
                MP3File entry = resolved.get(file.getAbsolutePath());
                if (entry == null) {
                  entry = new MP3File();
                  entry.setFile(file);
                  MP3TrackImportTask t = new MP3TrackImportTask(file);
                  t.resolve();
                  entry.tag = t.getTag();
                }
                this.files.add(entry);
                if (this.files.size() >= this.maxTitles) {
                  throw new TooManyTitlesException();
                }
              }
            }
          }
        }
      }
    } finally {
      this.resolveStatus();
      this.fireTableDataChanged();
    }

  }

  private void resolveStatus() {
    if (this.files.size() > 0) {
      this.resolveId++;
      Thread t = new Thread() {

        @Override
        public void run() {
          int myResolveId = resolveId; // used to detect if another thread has started
          for (int i = 0; myResolveId == resolveId && i < files.size(); i++) {
            MP3File file = files.get(i);
            if (file.getStatus() == TrackStatus.UNRESOLVED) {
              titleImportHandler.clear();
              titleImportHandler.add(new MP3TrackImportTask(file.getFile(), file.getTag()));
              titleImportHandler.resolveTags();
              titleImportHandler.resolveTracksLocal();
              if (titleImportHandler.isEverythingResolved()) {
                file.setStatus(TrackStatus.IN_LOCAL_POOL);
                // tm.setTitle(importHandler.getTasks().get(0).getTrackLibraryTitle());
              }
            }
          }
          if (myResolveId == resolveId) {
            SwingUtilities.invokeLater(new Runnable() {

              @Override
              public void run() {
                fireTableRowsUpdated(0, files.size() - 1);
              }
            });
          }
        }

      };
      t.setPriority(Thread.MIN_PRIORITY);
      t.start();

    }
  }

  /**
   * @return the maxTitles
   */
  protected int getMaxTitles() {
    return maxTitles;
  }

  /**
   * @param maxTitles
   *          the maxTitles to set
   */
  protected void setMaxTitles(int maxTitles) {
    this.maxTitles = maxTitles;
  }

  /*
   * (non-Javadoc)
   * 
   * @see javax.swing.table.AbstractTableModel#getColumnClass(int)
   */
  @Override
  public Class<?> getColumnClass(int columnIndex) {
    Column col = Column.values()[columnIndex];
    switch (col) {
    case ARTIST:
    case TITLE:
    case FILENAME:
      return String.class;
    case ALBUM:
      return Album.class;
    case TRACKNO:
      return Integer.class;
    case SIZE:
      return Float.class;
    }
    return String.class;
  }
}
