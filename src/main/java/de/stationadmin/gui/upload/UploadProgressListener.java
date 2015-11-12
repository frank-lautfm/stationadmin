/**
 * 
 */
package de.stationadmin.gui.upload;

import java.io.File;

import de.stationadmin.lfm.backend.ProgressListener;

/**
 *
 * @author Frank Korf
 *
 */
public class UploadProgressListener implements ProgressListener {
  private volatile int currentFileUploaded;
  private volatile int currentFileMax;
  private volatile int totalMax;
  private volatile int totalUploaded;
  private volatile boolean abortCurrent = false;

  /**
   * @see de.emjoy.stationadmin.raw.ProgressListener#setCurrentValue(int)
   */
  @Override
  public void setCurrentValue(int value) {
    this.currentFileUploaded = value;
  }

  /**
   * @see de.emjoy.stationadmin.raw.ProgressListener#setMaxValue(int)
   */
  @Override
  public void setMaxValue(int max) {
    this.currentFileMax = max;
  }
  
  public void add(File file) {
    this.totalMax += (int)file.length();
  }
  
  public void remove(File file) {
    this.totalMax -= (int)file.length();
  }
  
  public void currentUploadCompleted() {
    this.totalUploaded += this.currentFileMax;
    this.currentFileUploaded = 0;
    this.currentFileMax = 0;
  }

  /**
   * @return the currentFileUploaded
   */
  public int getCurrentFileUploaded() {
    return currentFileUploaded;
  }
  
  public void reset() {
    this.currentFileUploaded = 0;
    this.totalUploaded = 0;
    this.currentFileMax = 0;
    this.totalMax = 0;
  }

  /**
   * @return the currentFileMax
   */
  public int getCurrentFileMax() {
    return currentFileMax;
  }

  /**
   * @return the totalMax
   */
  public int getTotalMax() {
    return totalMax;
  }

  /**
   * @return the totalUploaded
   */
  protected int getTotalUploaded() {
    return totalUploaded + this.currentFileUploaded;
  }

  /**
   * @return the abort
   */
  public boolean isAbortCurrent() {
    return abortCurrent;
  }

  /**
   * @param abort the abort to set
   */
  public void setAbortCurrent(boolean abort) {
    this.abortCurrent = abort;
  }

}
