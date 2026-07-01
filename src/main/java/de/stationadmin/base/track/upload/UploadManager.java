/**
 * 
 */
package de.stationadmin.base.track.upload;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang.StringUtils;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.track.DetailedTrack;
import de.stationadmin.base.track.TrackService;
import de.stationadmin.base.util.AbstractBean;
import de.stationadmin.lfm.backend.InsufficientStorageException;
import de.stationadmin.lfm.backend.QueueStatus;

/**
 * 
 * @author Frank Korf
 * 
 */
public class UploadManager extends AbstractBean {
	private static final Logger log = LogManager.getLogger(UploadManager.class);
	private static long QUEUETATUS_DELAY = 10 * 1000;
	private static final int DEFAULT_QUEUE_LIMIT = 15;
	private UploadProgressListener progressListener = new UploadProgressListener();
	private List<QueuedTrack> queue = Collections.synchronizedList(new ArrayList<QueuedTrack>());
	private List<QueuedTrack> processedTracks = Collections.synchronizedList(new ArrayList<QueuedTrack>());
	private volatile int currentIndex = 0;
	private volatile boolean running = false;
	private volatile boolean stop = false;
	private volatile int numberOfTracksProcessing = 0;

	private TrackService trackService;
	private SessionCtx sessionCtx;
	private TrackProcessingMonitor processingMonitor;
	private boolean throttling;
	private boolean waitingDueToInsufficientSpace;
	private long lastQueueStatusCheck = 0;
	
	private QueueStatus queueStatus;
	
	private boolean slowerUploadEnabled = false;

	public boolean isWaitingDueToInsufficientSpace() {
		return waitingDueToInsufficientSpace;
	}

	public boolean isThrottling() {
		return throttling;
	}

	public UploadManager(TrackService trackService, SessionCtx ctx) {
		super();
		this.trackService = trackService;
		this.sessionCtx = ctx;
		this.loadQueue();
	}

	public boolean add(File file) {
		return add(file, false);
	}

	private static boolean isSupportedAudioFile(File file) {
		String name = file.getName().toLowerCase();
		return name.endsWith(".mp3") || name.endsWith(".aac") || name.endsWith(".m4a") || name.endsWith(".flac");
	}

	public boolean add(File file, boolean forcePrivate) {
		if (file.exists() && !file.isDirectory()) {
			if (isSupportedAudioFile(file) && file.length() < 1024 * 1024 * 65) {
				int oldRemaining = this.getNumberOfRemainingFiles();
				QueuedTrack track = new QueuedTrack(file);
				if (forcePrivate) {
					track.getFile().setPrivateTrack(true);
				}
				this.queue.add(track);

				this.progressListener.add(file);
				this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
				this.saveQueue();
				return true;
			} else if (file.getName().toLowerCase().endsWith(".m3u") || file.getName().toLowerCase().endsWith("m3u")) {
				boolean success = false;
				try {
					List<File> files = readM3u(file, null);
					for (File mp3File : files) {
						if (add(mp3File, forcePrivate)) {
							success = true;
						}
					}
				} catch (IOException e) {
				}
				return success;

			}
		}
		return false;
	}

	private List<File> readM3u(File file, String enc) throws IOException {
		File dir = file.getParentFile();
		String drive = "";
		if (dir.getAbsolutePath().charAt(1) == ':') {
			drive = dir.getAbsolutePath().substring(0, 2);
		}

		Reader reader = null;
		if (enc != null) {
			reader = new BufferedReader(new InputStreamReader(new FileInputStream(file), enc), 1024 * 8);
		} else {
			reader = new BufferedReader(new FileReader(file), 1024 * 8);
		}
		StringBuffer text = new StringBuffer();
		char[] buffer = new char[1024 * 8];
		int chars = 0;
		do {
			chars = reader.read(buffer);
			if (chars > 0) {
				text.append(buffer, 0, chars);
			}
		} while (chars > 0);
		reader.close();

		List<File> files = new ArrayList<File>();
		String[] lines = StringUtils.split(text.toString(), "\n\r");
		for (String line : lines) {
			line = line.trim();
			if (line.length() > 1 && line.charAt(0) != '#') {
				String entryFilename = line;
				if (entryFilename.charAt(0) != '\\' && entryFilename.charAt(0) != '/' && entryFilename.charAt(1) != ':') {
					// entry is relative to m3u file
					entryFilename = dir.getAbsolutePath() + File.separatorChar + entryFilename;
				} else if (drive.length() > 0 && entryFilename.charAt(1) != ':') {
					entryFilename = drive + entryFilename;
				}
				File mp3File = new File(entryFilename);
				files.add(mp3File);
			}
		}

		return files;

	}

	public boolean removeFile(File file) {
		if (this.currentIndex < this.queue.size()
				&& this.queue.get(currentIndex).getFile().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
			this.progressListener.setAbortCurrent(true);
		}
		int oldRemaining = this.getNumberOfRemainingFiles();
		int startIndex = this.running ? this.currentIndex + 1 : this.currentIndex;
		for (int i = startIndex; i < this.queue.size(); i++) {
			if (this.queue.get(i).getFile().getFile().getAbsolutePath().equals(file.getAbsolutePath())) {
				this.queue.remove(i);
				this.progressListener.remove(file);
				this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
				this.saveQueue();
				return true;
			}
		}
		return false;
	}

	/**
	 * Number of files that still need to be uploaded
	 * 
	 * @return
	 */
	public int getNumberOfRemainingFiles() {
		return this.queue.size() - this.currentIndex;
	}

	public QueueStatus getQueueStatus() {
		try {
			if(System.currentTimeMillis() - this.lastQueueStatusCheck  >= QUEUETATUS_DELAY) {			
				this.lastQueueStatusCheck = System.currentTimeMillis();
				this.queueStatus = this.sessionCtx.getServer().getQueueStatus(this.sessionCtx.getStationId());
			}
		} catch (IOException e) {
			if(this.queueStatus == null) {
				// create default entry - shouldn't happen
				this.queueStatus = new QueueStatus();
				this.queueStatus.setUserLimit(DEFAULT_QUEUE_LIMIT);
				this.queueStatus.setStationLimit(DEFAULT_QUEUE_LIMIT);
			}
		}
		return this.queueStatus;
	}

	public void run() throws IOException {
		this.setRunning(true);
		this.stop = false;
		if (this.processingMonitor == null || !this.processingMonitor.isAlive()) {
			this.processingMonitor = new TrackProcessingMonitor();
			this.processingMonitor.start();
		}
		
		int maxQueueLength = DEFAULT_QUEUE_LIMIT;
		
		try {
			while (this.currentIndex < this.queue.size() && !stop) {
				this.progressListener.setAbortCurrent(false);

				int numQueued = (int) (this.queue.stream().filter(x -> x.getStatus() == UploadStatus.PROCESSING).count());
				
				long timeSinceLastCheck = System.currentTimeMillis() - this.lastQueueStatusCheck;
				if(numQueued >= 5 && timeSinceLastCheck < QUEUETATUS_DELAY) {
					this.throttling = true;
					Sleep(QUEUETATUS_DELAY - timeSinceLastCheck);
				}

				QueueStatus status = this.getQueueStatus();
				if(status.getEffectiveLimit() < maxQueueLength) {
					maxQueueLength = Math.max(1, status.getEffectiveLimit());
				}
				else if(status.getEffectiveLimit() > maxQueueLength && maxQueueLength < DEFAULT_QUEUE_LIMIT) {
					maxQueueLength = Math.min(DEFAULT_QUEUE_LIMIT, status.getEffectiveLimit());
					log.info("Set max queue length to " + maxQueueLength);
				}
				
				if (queueStatus.IsOverloaded() || (this.slowerUploadEnabled && numQueued >= 3)) {
					// do not add further tracks until more capacity is available
					this.throttling = true;
					Sleep(1000);
					continue;
				} 

				this.throttling = false;

				QueuedTrack entry = this.queue.get(this.currentIndex);
				entry.setStatus(UploadStatus.UPLOADING);
				fireTrackStatusUpdate();
				try {
					try {
						this.waitingDueToInsufficientSpace = false;
						entry.setResponse(this.trackService.upload(entry.getFile(), progressListener));
						if (progressListener.isAbortCurrent()) {
							entry.setStatus(UploadStatus.ABORTED);
						} else {
							entry.setStatus(UploadStatus.PROCESSING);
						}
						this.numberOfTracksProcessing++;
						fireTrackStatusUpdate();
					} catch (InterruptedIOException e) {
						log.info("upload interrupted");
					}
					this.progressListener.currentUploadCompleted();
					int oldRemaining = this.getNumberOfRemainingFiles();
					this.currentIndex++;
					this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
					this.saveQueue();
				} catch (InsufficientStorageException e) {
					entry.setStatus(UploadStatus.WAITING);
					numQueued = (int) (this.queue.stream().filter(x -> x.getStatus() == UploadStatus.PROCESSING).count());
					maxQueueLength = Math.max(1, numQueued);
					this.waitingDueToInsufficientSpace = true;
					Sleep(1000 * 30);
				}
			}
			this.processingMonitor.abort();
			this.progressListener.reset();
		} finally {
			this.setRunning(false);
		}
	}

	private void Sleep(long ms) {
		try {
			Thread.sleep(ms);
		} catch (InterruptedException ex) {
		}

	}

	private void saveQueue() {
		List<String> list = new ArrayList<String>();
		for (int i = this.currentIndex; i < this.queue.size(); i++) {
			QueuedTrack entry = queue.get(i);
			list.add(entry.getFile().getFile().getAbsolutePath() + "\t" + (entry.getFile().isPrivateTrack() ? "1" : "0"));
		}
		String filename = this.sessionCtx.getStationDirectory() + "uploadqueue.txt";
		try (FileOutputStream out = new FileOutputStream(new File(filename))) {
			IOUtils.writeLines(list, null, out, "UTF-8");
		} catch (IOException e) {
			log.error("unable to write upload queue file", e);
		}
	}

	private void loadQueue() {
		String filename = this.sessionCtx.getStationDirectory() + "uploadqueue.txt";
		if (new File(filename).exists()) {
			try (FileInputStream in = new FileInputStream(filename)) {
				List<String> lines = (List<String>) IOUtils.readLines(in, "UTF-8");
				int old = this.queue.size();
				for (String line : lines) {
					String[] parts = StringUtils.split(line, "\t");
					File file = new File(parts[0]);
					;
					if (file.exists()) {
						QueuedTrack track = new QueuedTrack(file);
						if (parts.length > 1 && parts[1].equals("1")) {
							track.getFile().setPrivateTrack(true);
						}
						this.queue.add(track);
						this.progressListener.add(file);
					}
				}
				this.firePropertyChange("numberOfRemainingFiles", old, this.queue.size());
			} catch (IOException e) {
				log.error("unable to write upload queue file", e);
			}

		}

	}

	private void setRunning(boolean running) {
		boolean old = this.running;
		this.running = running;
		this.firePropertyChange("running", old, running);
	}

	public void stop() {
		this.stop = true;
	}

	/**
	 * @return the progressListener
	 */
	public UploadProgressListener getProgressListener() {
		return progressListener;
	}

	/**
	 * @return the files
	 */
	public List<QueuedTrack> getQueue() {
		return Collections.unmodifiableList(queue);
	}

	/**
	 * @return the files
	 */
	public List<QueuedTrack> getProcessedTracks() {
		return this.processedTracks;
	}

	/**
	 * @return the currentIndex
	 */
	public int getCurrentIndex() {
		return currentIndex;
	}

	/**
	 * @return the running
	 */
	public boolean isRunning() {
		return running;
	}

	/**
	 * @return the trackService
	 */
	public TrackService getTrackService() {
		return trackService;
	}

	protected int checkUploadedTracks() {
		int remaining = 0;
		boolean tracksCompleted = false;
		List<QueuedTrack> toBeRemoved = new ArrayList<QueuedTrack>();
		for (QueuedTrack entry : queue) {
			if (entry.getTrack() == null) {
				remaining++;
				if (entry.getResponse() != null) {
					try {
						DetailedTrack track = trackService.getTrack(entry.getResponse().getId());
						if (track == null) {
							log.error("No track retrieved for " + entry.getResponse().getId() + " - upload failed");
							entry.setStatus(UploadStatus.ABORTED); // not the nicest thing, but we need to get rid of this entry
							toBeRemoved.add(entry);
						}
						if (track != null && track.getId() > 0) {
							remaining--;
							entry.setTrack(track);
							entry.setStatus(UploadStatus.COMPLETED);
							System.out.println("processing completed " + entry.getFile().getFile().getName());
							
							// submit local meta data if the server track has none
							if (isMetaDataMissing(track) && entry.getLocalMetaData() != null) {
								try {
									DetailedTrack merged = mergeLocalMetaData(track, entry.getLocalMetaData());
									trackService.updateTrack(merged);
									log.info("Submitted local meta data for track " + track.getId()
											+ ": " + merged.getArtist() + " - " + merged.getTitle());
									entry.setTrack(merged);
								} catch (Exception e) {
									log.warn("Failed to submit local meta data for track " + track.getId(), e);
								}								
							}
							
							addToProcessed(entry);
							this.trackService.getTrackRegistry().registerOwnTrack(entry.getTrack());
							tracksCompleted = true;
						}
					} catch (Exception e) {

					}
				}
			}
		}
		if (toBeRemoved.size() > 0) {
			int oldRemaining = this.getNumberOfRemainingFiles();
			for (QueuedTrack track : toBeRemoved) {
				this.queue.remove(track);
				this.progressListener.remove(track.getFile().getFile());
			}
			this.firePropertyChange("numberOfRemainingFiles", oldRemaining, this.getNumberOfRemainingFiles());
			this.saveQueue();
		}

		if (tracksCompleted) {
			fireTrackCompleted();
			if (remaining == 0 && this.getNumberOfRemainingFiles() == 0) {
				try {
					trackService.saveTracks();
				} catch (Exception e) {
				}
			}
		}

		this.numberOfTracksProcessing = remaining;
		return remaining;
	}

	private void addToProcessed(QueuedTrack track) {
		int numProcessed = this.processedTracks.size();
		processedTracks.add(track);
		this.firePropertyChange("numProcessedTracks", numProcessed, this.processedTracks.size());

	}

	/**
	 * Clears the list of processed tracks
	 */
	public void clearProcessedTracks() {
		int numProcessed = this.processedTracks.size();
		this.processedTracks.clear();
		this.firePropertyChange("numProcessedTracks", numProcessed, this.processedTracks.size());
	}

	private void fireTrackStatusUpdate() {
		this.firePropertyChange("trackStatusUpdate", false, true);
	}

	private void fireTrackCompleted() {
		this.firePropertyChange("trackCompleted", false, true);
	}

	private void clearQueue() {
		queue.clear();
		currentIndex = 0;
		progressListener.setCurrentValue(0);
		progressListener.setMaxValue(0);
	}

	private class TrackProcessingMonitor extends Thread {
		private boolean abort = false;

		public void run() {
			int remaining = 0;
			do {
				try {
					Thread.sleep(1000 * 10);
				} catch (Exception e) {
				}
				remaining = checkUploadedTracks();

			} while (!abort || remaining > 0);

			// if everything is uploaded we can clear the queue
			if (currentIndex == queue.size()) {
				clearQueue();
			}
		}

		public void abort() {
			this.abort = true;
		}
	}

	/**
	 * Number of tracks that are currently processed on the server
	 * 
	 * @return the numTracksProcessing
	 */
	public int getNumberOfTracksProcessing() {
		return numberOfTracksProcessing;
	}

	public boolean isSlowerUploadEnabled() {
		return slowerUploadEnabled;
	}

	public void setSlowerUploadEnabled(boolean slowerUploadEnabled) {
		this.slowerUploadEnabled = slowerUploadEnabled;
	}

	/**
	 * Returns <code>true</code> if the server track has neither artist nor title
	 * set, indicating that meta data should be submitted from the local file.
	 */
	private boolean isMetaDataMissing(DetailedTrack track) {
		return (track.getArtist() == null || track.getArtist().trim().length() == 0)
				&& (track.getTitle() == null || track.getTitle().trim().length() == 0);
	}

	/**
	 * Creates a copy of {@code serverTrack} with artist, title and (if present)
	 * album taken from {@code local}.
	 */
	private DetailedTrack mergeLocalMetaData(DetailedTrack serverTrack, DetailedTrack local) {
		DetailedTrack merged = new DetailedTrack(serverTrack);
		merged.setArtist(local.getArtist());
		merged.setTitle(local.getTitle());
		if (local.getAlbum() != null && local.getAlbum().trim().length() > 0) {
			merged.setAlbum(local.getAlbum());
		}
		return merged;
	}

}
