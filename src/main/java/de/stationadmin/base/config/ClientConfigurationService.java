package de.stationadmin.base.config;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;

import de.stationadmin.base.Service;
import de.stationadmin.base.SessionCtx;
import de.stationadmin.base.Settings;
import de.stationadmin.lfm.backend.ResourceNotFoundException;

public class ClientConfigurationService implements Service {
  private static final String tsfile = "clientconfigts";
  private static final Logger log = Logger.getLogger(ClientConfigurationService.class);
  private static final String dateFormat = "yyyy-MM-dd HH:mm:ss";
  private SessionCtx sessionCtx;
  private List<ClientConfigurationSource> sources = new ArrayList<>();
  private Date timestamp;
  private Settings settings;

  public ClientConfigurationService(SessionCtx ctx, Settings settings) {
    this.sessionCtx = ctx;
    this.settings = settings;
  }

  public boolean isSupported() {
    return !sessionCtx.isDJOnly() && this.settings.isSaveClientSettings();
  }

  public void register(ClientConfigurationSource source) {
    this.sources.add(source);
  }

  /**
   * Writes the latest configuration to the server
   * 
   * @throws IOException
   */
  public void write() throws IOException {
    if (!isSupported()) {
      return;
    }
    ClientConfiguration cfg = new ClientConfiguration();
    for (ClientConfigurationSource src : sources) {
      src.collectClientConfiguration(cfg);
    }
    sessionCtx.getServer().updateClientConfiguration(sessionCtx.getStationId(), cfg, ClientConfiguration.class);
    updateTimestamp(cfg.getTimmestamp());
  }

  public boolean isClientConfigurationAvailableOnServer() {
    try {
      sessionCtx.getServer().getClientConfiguration(sessionCtx.getStationId(), ClientConfiguration.class);
      return true;
    } catch (IOException e) {
      return false;
    }
  }

  /**
   * Reads the latest configuration from server
   * 
   * @throws IOException
   */
  public void read() throws IOException {
    if (!isSupported()) {
      return;
    }
    log.info("Reading client configuration from server");
    try {
      ClientConfiguration cfg = sessionCtx.getServer().getClientConfiguration(sessionCtx.getStationId(), ClientConfiguration.class);
      for (ClientConfigurationSource src : sources) {
        src.applyClientConfiguration(cfg);
      }
      updateTimestamp(cfg.getTimmestamp());
    } catch (ResourceNotFoundException e) {
      // no configuration available
    }
  }

  private void updateTimestamp(Date ts) {
    this.timestamp = ts;
    String tsStr = new SimpleDateFormat(dateFormat).format(ts);
    String filename = sessionCtx.getStationDirectory() + tsfile;
    try (FileOutputStream out = new FileOutputStream(new File(filename))) {
      IOUtils.write(tsStr, out, "UTF-8");
    } catch (Exception e) {
      log.error("Unable to save time stamp to file", e);
    }

  }

  public boolean isUpToDate() {
    try {
      ClientConfiguration cfg = sessionCtx.getServer().getClientConfiguration(sessionCtx.getStationId(), ClientConfiguration.class);
      return timestamp != null && (timestamp.getTime() >= cfg.getTimmestamp().getTime() || cfg.getTimmestamp().getTime() - timestamp.getTime() <= 1000);
    } catch (Exception e) {
      return true;
    }
  }

  @Override
  public void load() throws IOException {
    if (!isSupported()) {
      return;
    }
    // read time stamp from file
    String filename = sessionCtx.getStationDirectory() + tsfile;
    if (new File(filename).exists()) {
      List<String> lines;
      try (FileInputStream input = new FileInputStream(new File(filename))) {
        lines = IOUtils.readLines(input, "UTF-8");
        timestamp = new SimpleDateFormat(dateFormat).parse(lines.get(0));
      } catch (Exception e) {
        log.error("Unable to load time stamp from file", e);
      }
    }
    // run update if necessary
    if (!isUpToDate()) {
      read();
    }
  }

  @Override
  public void synchronize() throws IOException {
    read();
  }

  @Override
  public void close() {
  }

  @Override
  public void initBackgroundTasks() {
  }

}
