/**
 * 
 */
package de.stationadmin.base;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.prefs.Preferences;

import org.apache.commons.lang.StringUtils;

import de.stationadmin.base.playlist.shuffle.TagWeight;
import de.stationadmin.base.playlist.shuffle.WordDistributionStrategy;
import de.stationadmin.base.util.AbstractBean;

/**
 * Configuration settings for the application
 * 
 * @author Frank Korf
 */
public class Settings extends AbstractBean {
  private int statisticsRefreshInterval = 3;
  private String statisticsLogFile;
  private String titleLogFile;
  private boolean logTitleWithListeners = false;
  private boolean logRank = false;

  private boolean shuffleProtectFirstJingle = false;
  private int shuffleJingleInterval = 0;
  private WordDistributionStrategy shuffleWordDistributionStrategy = WordDistributionStrategy.RANDOM;
  private int generateMinRandomValue = 100;
  private List<TagWeight> generateGlobalTagWeights = new ArrayList<TagWeight>();
  private Map<String, Integer> generateArtistPreselectTagWeights = new HashMap<String, Integer>();
  private Map<String, Integer> generateArtistPreselectLimits = new HashMap<String, Integer>();

  private List<String> artistNormalizerSeperators = Arrays.asList(" feat");
  private Map<String, String> artistNormalizerAliases = new HashMap<String, String>();

  private boolean autoUpdateCheckDisabled = false;

  private int mp3ExplorerMaxFiles = 500;
  private String mp3Player = null;
  private String mp3Root = null;

  private String backupDirectory;
  private int backupFrequency;

  private boolean logDownloadPermitted = false;
  private boolean logAutodownloadPermitted = false;

  public int getStatisticsRefreshInterval() {
    return statisticsRefreshInterval;
  }

  public void setStatisticsRefreshInterval(int statisticsRefreshInterval) {
    int old = this.statisticsRefreshInterval;
    this.statisticsRefreshInterval = statisticsRefreshInterval;
    this.getPcs().firePropertyChange("statisticsRefreshInterval", old, statisticsRefreshInterval);
  }

  public String getStatisticsLogFile() {
    return statisticsLogFile;
  }

  public void setStatisticsLogFile(String statisticsLogFile) {
    String old = this.statisticsLogFile;
    this.statisticsLogFile = statisticsLogFile;
    this.getPcs().firePropertyChange("statisticsLogFile", old, statisticsLogFile);
  }

  public void copyFrom(Settings settings) {
    this.setStatisticsLogFile(settings.getStatisticsLogFile());
    this.setStatisticsRefreshInterval(settings.getStatisticsRefreshInterval());
    this.setTitleLogFile(settings.getTitleLogFile());
    this.setShuffleJingleInterval(settings.getShuffleJingleInterval());
    this.setShuffleProtectFirstJingle(settings.isShuffleProtectFirstJingle());
    this.setShuffleWordDistributionStrategy(settings.getShuffleWordDistributionStrategy());
    this.setGenerateMinRandomValue(settings.getGenerateMinRandomValue());
    this.setGenerateArtistPreselectLimits(settings.getGenerateArtistPreselectLimits());
    this.setGenerateArtistPreselectTagWeights(settings.getGenerateArtistPreselectTagWeights());
    this.setGenerateGlobalTagWeights(settings.getGenerateGlobalTagWeights());
    this.setAutoUpdateCheckEnabled(settings.isAutoUpdateCheckEnabled());
    this.setMp3ExplorerMaxFiles(settings.getMp3ExplorerMaxFiles());
    this.setLogTitleWithListeners(settings.isLogTitleWithListeners());
    this.setLogRank(settings.isLogRank());
    this.setMp3Player(settings.getMp3Player());
    this.setMp3Root(settings.getMp3Root());
    this.setBackupDirectory(settings.getBackupDirectory());
    this.setBackupFrequency(settings.getBackupFrequency());
    this.setLogAutodownloadPermitted(settings.isLogAutodownloadPermitted());
    this.setLogDownloadPermitted(settings.isLogDownloadPermitted());
    this.setArtistNormalizerAliases(settings.getArtistNormalizerAliases());
    this.setArtistNormalizerSeperators(settings.getArtistNormalizerSeperators());
  }

  public String getTitleLogFile() {
    return titleLogFile;
  }

  public void setTitleLogFile(String titleLogFile) {
    String old = this.titleLogFile;
    this.titleLogFile = titleLogFile;
    this.getPcs().firePropertyChange("titleLogFile", old, titleLogFile);
  }

  /**
   * @return the shuffleProtectFirstJingle
   */
  public boolean isShuffleProtectFirstJingle() {
    return shuffleProtectFirstJingle;
  }

  /**
   * @param shuffleProtectFirstJingle
   *          the shuffleProtectFirstJingle to set
   */
  public void setShuffleProtectFirstJingle(boolean shuffleProtectFirstJingle) {
    boolean old = this.shuffleProtectFirstJingle;
    this.shuffleProtectFirstJingle = shuffleProtectFirstJingle;
    this.firePropertyChange("shuffleProtectFirstJingle", old, shuffleProtectFirstJingle);
  }

  /**
   * @return the shuffleJingleInterval
   */
  public int getShuffleJingleInterval() {
    return shuffleJingleInterval;
  }

  /**
   * @param shuffleJingleInterval
   *          the shuffleJingleInterval to set
   */
  public void setShuffleJingleInterval(int shuffleJingleInterval) {
    int old = this.shuffleJingleInterval;
    this.shuffleJingleInterval = shuffleJingleInterval;
    this.firePropertyChange("shuffleJingleInterval", old, shuffleJingleInterval);
  }

  /**
   * @return the logTitleWithListeners
   */
  public boolean isLogTitleWithListeners() {
    return logTitleWithListeners;
  }

  /**
   * @param logTitleWithListeners
   *          the logTitleWithListeners to set
   */
  public void setLogTitleWithListeners(boolean logTitleWithListeners) {
    boolean old = this.logTitleWithListeners;
    this.logTitleWithListeners = logTitleWithListeners;
    this.firePropertyChange("logTitleWithListeners", old, logTitleWithListeners);
  }

  /**
   * @return the autoUpdateCheckDisabled
   */
  public boolean isAutoUpdateCheckEnabled() {
    return !autoUpdateCheckDisabled;
  }

  /**
   * @param autoUpdateCheckDisabled
   *          the autoUpdateCheckDisabled to set
   */
  public void setAutoUpdateCheckEnabled(boolean autoUpdateCheckEnabled) {
    boolean old = !this.autoUpdateCheckDisabled;
    this.autoUpdateCheckDisabled = !autoUpdateCheckEnabled;
    this.firePropertyChange("autoUpdateCheckEnabled", old, autoUpdateCheckEnabled);
  }

  /**
   * @return the mp3ExplorerMaxFiles
   */
  public int getMp3ExplorerMaxFiles() {
    return mp3ExplorerMaxFiles > 0 ? this.mp3ExplorerMaxFiles : 500;
  }

  /**
   * @param mp3ExplorerMaxFiles
   *          the mp3ExplorerMaxFiles to set
   */
  public void setMp3ExplorerMaxFiles(int mp3ExplorerMaxFiles) {
    int old = this.mp3ExplorerMaxFiles;
    this.mp3ExplorerMaxFiles = mp3ExplorerMaxFiles;
    this.firePropertyChange("mp3ExplorerMaxFiles", old, mp3ExplorerMaxFiles);
  }

  /**
   * @return the mp3Player
   */
  public String getMp3Player() {
    return mp3Player;
  }

  /**
   * @param mp3Player
   *          the mp3Player to set
   */
  public void setMp3Player(String mp3Player) {
    mp3Player = StringUtils.trimToNull(mp3Player);
    String old = this.mp3Player;
    this.mp3Player = mp3Player;
    this.firePropertyChange("mp3Player", old, this.mp3Player);
  }

  /**
   * @return the mp3Root
   */
  public String getMp3Root() {
    return mp3Root;
  }

  /**
   * @param mp3Root
   *          the mp3Root to set
   */
  public void setMp3Root(String mp3Root) {
    mp3Root = StringUtils.trimToNull(mp3Root);
    String old = this.mp3Player;
    this.mp3Root = mp3Root;
    this.firePropertyChange("mp3Root", old, mp3Root);
  }

  /**
   * @return the shuffleWordDistributionStrategy
   */
  public WordDistributionStrategy getShuffleWordDistributionStrategy() {
    return shuffleWordDistributionStrategy != null ? this.shuffleWordDistributionStrategy : WordDistributionStrategy.RANDOM;
  }

  /**
   * @param shuffleWordDistributionStrategy
   *          the shuffleWordDistributionStrategy to set
   */
  public void setShuffleWordDistributionStrategy(WordDistributionStrategy shuffleWordDistributionStrategy) {
    WordDistributionStrategy old = this.shuffleWordDistributionStrategy;
    this.shuffleWordDistributionStrategy = shuffleWordDistributionStrategy;
    this.firePropertyChange("shuffleWordDistributionStrategy", old, shuffleWordDistributionStrategy);
  }

  public boolean isAutologin() {
    return Preferences.userRoot().getBoolean("autologin", false);
  }

  public void setAutologin(boolean autologin) {
    boolean old = this.isAutologin();
    Preferences.userRoot().putBoolean("autologin", autologin);
    this.firePropertyChange("autologin", old, autologin);
  }

  /**
   * @return the backupDirectory
   */
  public String getBackupDirectory() {
    return backupDirectory;
  }

  /**
   * @param backupDirectory
   *          the backupDirectory to set
   */
  public void setBackupDirectory(String backupDirectory) {
    String old = this.backupDirectory;
    this.backupDirectory = backupDirectory;
    this.firePropertyChange("backupDirectory", old, backupDirectory);
  }

  /**
   * @return the backupFrequency
   */
  public int getBackupFrequency() {
    return backupFrequency;
  }

  /**
   * @param backupFrequency
   *          the backupFrequency to set
   */
  public void setBackupFrequency(int backupFrequency) {
    int old = this.backupFrequency;
    this.backupFrequency = backupFrequency;
    this.firePropertyChange("backupFrequency", old, backupFrequency);
  }

  /**
   * @return the generateMinRandomValue
   */
  public int getGenerateMinRandomValue() {
    return this.generateMinRandomValue > 0 ? this.generateMinRandomValue : 100;
  }

  /**
   * @param generateMinRandomValue
   *          the generateMinRandomValue to set
   */
  public void setGenerateMinRandomValue(int generateMinRandomValue) {
    int old = this.generateMinRandomValue;
    this.generateMinRandomValue = generateMinRandomValue;
    this.firePropertyChange("generateMinRandomValue", old, generateMinRandomValue);
  }

  /**
   * @return the logRank
   */
  public boolean isLogRank() {
    return logRank;
  }

  /**
   * @param logRank
   *          the logRank to set
   */
  public void setLogRank(boolean logRank) {
    boolean old = this.logRank;
    this.logRank = logRank;
    this.firePropertyChange("logRank", old, logRank);
  }

  @Deprecated
  public boolean isLogDownloadPermitted() {
    return logDownloadPermitted;
  }

  public void setLogDownloadPermitted(boolean logDownloadPermitted) {
    boolean old = this.logDownloadPermitted;
    this.logDownloadPermitted = logDownloadPermitted;
    this.firePropertyChange("logDownloadPermitted", old, logDownloadPermitted);
  }

  @Deprecated
  public boolean isLogAutodownloadPermitted() {
    return logAutodownloadPermitted;
  }

  public void setLogAutodownloadPermitted(boolean logAutodownloadPermitted) {
    boolean old = this.logAutodownloadPermitted;
    this.logAutodownloadPermitted = logAutodownloadPermitted;
    this.firePropertyChange("logAutodownloadPermitted", old, logAutodownloadPermitted);
  }

  /**
   * @return the generateGlobalTagWeights
   */
  public List<TagWeight> getGenerateGlobalTagWeights() {
    return generateGlobalTagWeights;
  }

  /**
   * @param generateGlobalTagWeights
   *          the generateGlobalTagWeights to set
   */
  public void setGenerateGlobalTagWeights(List<TagWeight> generateGlobalTagWeights) {
    if (generateGlobalTagWeights == null) {
      generateGlobalTagWeights = new ArrayList<TagWeight>();
    }
    List<TagWeight> old = this.generateGlobalTagWeights;
    this.generateGlobalTagWeights = generateGlobalTagWeights;
    this.firePropertyChange("generateGlobalTagWeights", old, generateGlobalTagWeights);
  }

  /**
   * @return the generateArtistPreselectTagWeights
   */
  public Map<String, Integer> getGenerateArtistPreselectTagWeights() {
    return generateArtistPreselectTagWeights;
  }

  /**
   * @param generateArtistPreselectTagWeights
   *          the generateArtistPreselectTagWeights to set
   */
  public void setGenerateArtistPreselectTagWeights(Map<String, Integer> generateArtistPreselectTagWeights) {
    if (generateArtistPreselectTagWeights == null) {
      generateArtistPreselectTagWeights = new HashMap<String, Integer>();
    }
    Map<String, Integer> old = this.generateArtistPreselectTagWeights;
    this.generateArtistPreselectTagWeights = generateArtistPreselectTagWeights;
    this.firePropertyChange("generateArtistPreselectTagWeights", old, generateArtistPreselectTagWeights);
  }

  /**
   * @return the generateArtistPreselectLimits
   */
  public Map<String, Integer> getGenerateArtistPreselectLimits() {
    return generateArtistPreselectLimits;
  }

  /**
   * @param generateArtistPreselectLimits
   *          the generateArtistPreselectLimits to set
   */
  public void setGenerateArtistPreselectLimits(Map<String, Integer> generateArtistPreselectLimits) {
    if (generateArtistPreselectLimits == null) {
      generateArtistPreselectLimits = new HashMap<String, Integer>();
    }
    Map<String, Integer> old = this.generateArtistPreselectLimits;
    this.generateArtistPreselectLimits = generateArtistPreselectLimits;
    this.firePropertyChange("generateArtistPreselectLimits", old, generateArtistPreselectLimits);
  }

  /**
   * @return the artistNormalizerSeperators
   */
  public List<String> getArtistNormalizerSeperators() {
    if (this.artistNormalizerSeperators == null) {
      this.artistNormalizerSeperators = Arrays.asList(" feat");
    }
    return artistNormalizerSeperators;
  }

  /**
   * @param artistNormalizerSeperators
   *          the artistNormalizerSeperators to set
   */
  public void setArtistNormalizerSeperators(List<String> artistNormalizerSeperators) {
    this.artistNormalizerSeperators = artistNormalizerSeperators;
  }

  /**
   * @return the artistNormalizerAliass
   */
  public Map<String, String> getArtistNormalizerAliases() {
    return artistNormalizerAliases;
  }

  /**
   * @param artistNormalizerAliass
   *          the artistNormalizerAliass to set
   */
  public void setArtistNormalizerAliases(Map<String, String> artistNormalizerAliass) {
    this.artistNormalizerAliases = artistNormalizerAliass;
  }

}
