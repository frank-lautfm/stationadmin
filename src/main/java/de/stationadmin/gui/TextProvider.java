/**
 * 
 */
package de.stationadmin.gui;

import java.util.Locale;
import java.util.MissingResourceException;
import java.util.ResourceBundle;

import org.apache.commons.lang.StringUtils;
import org.jdesktop.swingx.error.ErrorInfo;
import org.jdesktop.swingx.error.ErrorLevel;

/**
 * Provides access to localized strings
 * 
 * @author Frank
 */
public class TextProvider {
  private Locale locale = Locale.getDefault();
  private ResourceBundle resources = ResourceBundle.getBundle("messagebundle", this.locale);

  public ErrorInfo createErrorInfo(Throwable exception, String msgKey, String... msgParameters) {
    ErrorInfo errorInfo = new ErrorInfo(this.getString("error.title"), this.getString(msgKey, msgParameters), null,
        "general", exception, ErrorLevel.SEVERE, null);
    return errorInfo;
  }

  /**
   * Gets a localized string
   * 
   * @param key
   * @param parameters
   * @return
   */
  public String getString(String key, String... parameters) {
    try {
      String text = this.resources.getString(key);
      for (int i = 0; i < parameters.length; i++) {
        text = StringUtils.replace(text, "{" + i + "}", parameters[i]);
      }
      return text;
    } catch (MissingResourceException e) {
      return key;
    }
  }

  public Locale getLocale() {
    return locale;
  }

  public void setLocale(Locale locale) {
    this.locale = locale;
    this.resources = ResourceBundle.getBundle("messagebundle", this.locale);
  }

}
