package de.stationadmin.base.playlist;

import java.util.Map;

import javax.xml.bind.annotation.XmlRootElement;

import org.codehaus.jackson.annotate.JsonIgnoreProperties;

/**
 * Meta data for registered shuffle script
 * @author fkorf
 *
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@XmlRootElement(name = "ShuffleScript")
public class ShuffleScriptMeta {
  public static final String BASIC = "basic";
  public static final String BUCKET = "bucket";
  public static final String STATIONADMIN = "StationAdmin";
  public static final String BLOCKSELECT = "BlockSelect";

  private String key;
  private String defaultVersion;
  private String optsKey;
  private String file;
  private Map<String, Object> defaultOpts;
  private boolean supportsGlobalOpts;
  
  public ShuffleScriptMeta() {
    
  }
  
  public ShuffleScriptMeta(String key, String defaultVersion, String optsKey, String file, Map<String, Object> defaultOpts, boolean useGlobalOpts) {
    super();
    this.key = key;
    this.defaultVersion = defaultVersion;
    this.optsKey = optsKey;
    this.file = file;
    this.defaultOpts = defaultOpts;
    this.supportsGlobalOpts = useGlobalOpts;
  }


  /**
   * Base key for identifying the script
   * @return
   */
  public String getKey() {
    return key;
  }

  /**
   * Base key for identifying the script
   * @param key
   */
  public void setKey(String key) {
    this.key = key;
  }

  /**
   * Default version that will be used when storing the script
   * @return
   */
  public String getDefaultVersion() {
    return defaultVersion;
  }
  
  /**
   * Default version that will be used when storing the script
   * @param defaultVersion
   */
  public void setDefaultVersion(String defaultVersion) {
    this.defaultVersion = defaultVersion;
  }

  /**
   * Key used to assign the options configuration panel (if any)
   * @return
   */
  public String getOptsKey() {
    return optsKey;
  }
  
  /**
   * Key used to assign the options configuration panel (if any)
   * @param optsKey
   */
  public void setOptsKey(String optsKey) {
    this.optsKey = optsKey;
  }
  
  /**
   * Local JavaSript file (if any)
   * @return
   */
  public String getFile() {
    return file;
  }
  
  /**
   * Local JavaSript file (if any)
   * @param file
   */
  public void setFile(String file) {
    this.file = file;
  }
  
  /**
   * Default options for this script
   * @return
   */
  public Map<String, Object> getDefaultOpts() {
    return defaultOpts;
  }
  
  /**
   * Default options for this script
   * @param defaultOpts
   */
  public void setDefaultOpts(Map<String, Object> defaultOpts) {
    this.defaultOpts = defaultOpts;
  }

  public boolean isSupportsGlobalOpts() {
    return supportsGlobalOpts;
  }

  public void setSupportsGlobalOpts(boolean useGlobalOpts) {
    this.supportsGlobalOpts = useGlobalOpts;
  }


}
