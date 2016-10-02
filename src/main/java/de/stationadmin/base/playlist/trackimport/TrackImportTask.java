/**
 * 
 */
package de.stationadmin.base.playlist.trackimport;

import java.util.List;

import de.stationadmin.base.track.BasicTrack;
import de.stationadmin.base.util.AbstractBean;

/**
 * @author Frank Korf
 * 
 */
public abstract class TrackImportTask extends AbstractBean {
	// private File file;
	private String artist;
	private String title;
	private String album;
	private BasicTrack trackLibraryTitle;
	private List<? extends BasicTrack> candidates;
	
	private Status status = Status.OPEN;

	
	/**
	 * Gets the album as resolved from media file tag
	 * @return
	 */
	public String getAlbum() {
		return album;
	}

	/**
	 * Gets the artist as resolved from media file tag
	 * @return
	 */
	public String getArtist() {
		return artist;
	}

	/**
	 * Gets candidates for the track library title - this will only be filled
	 * if multiple titles are available
	 * @return
	 */
	public List<? extends BasicTrack> getCandidates() {
		return candidates;
	}

	/**
	 * Gets a string representation of the source (e. g. filename)
	 * @return
	 */
	public abstract String getSourceString();

	/**
	 * Gets the status of this task
	 * @return
	 */
	public Status getStatus() {
		return status;
	}

	/**
	 * Gets the title name as resolved from media file tag
	 * 
	 * @return
	 */
	public String getTitle() {
		return title;
	}

	/**
	 * Gets the resolved title from the laut.fm track library - this is the
	 * title that will finally be used in the playlist
	 * @return
	 */
	public BasicTrack getTrackLibraryTitle() {
		return trackLibraryTitle;
	}

	public void setAlbum(String album) {
		this.album = album;
	}

	public void setArtist(String artist) {
		this.artist = artist;
	}

	public void setCandidates(List<? extends BasicTrack> candidates) {
		this.candidates = candidates;
	}

	public void setStatus(Status status) {
		Status old = this.status;
		this.status = status;
		this.firePropertyChange("status", old, status);
	}
	
	public void setTitle(String title) {
		this.title = title;
	}

	public void setTrackLibraryTitle(BasicTrack trackLibraryTitle) {
		BasicTrack old = this.trackLibraryTitle;
		this.trackLibraryTitle = trackLibraryTitle;
		this.firePropertyChange("trackLibraryTitle", old, trackLibraryTitle);
	}
	
	public abstract void resolve();

	public enum Status {
		/** task is completely open */
		OPEN,
		/** a matching title has been resolved */
		RESOLVED,
		/** no mp3 tags found */
		NO_TAGS,
    /** no mp3 tags found */
    TAG_READ_ERROR,
		/** searching candidates */
		SEARCHING,
		/** multiple candidates found - user selection required */
		MULTIPLE_CANDIDATES,
		/** no matching title found in track library */
		NO_CANDIDATES
	}

}
