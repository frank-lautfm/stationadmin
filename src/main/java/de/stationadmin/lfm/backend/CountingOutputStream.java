/**
 * 
 */
package de.stationadmin.lfm.backend;

import java.io.FilterOutputStream;
import java.io.IOException;
import java.io.OutputStream;

class CountingOutputStream extends FilterOutputStream {
  private ProgressListener progressListener;
  private int transferred = 0;

  public CountingOutputStream(final OutputStream out, ProgressListener progressListener) {
    super(out);
    this.progressListener = progressListener;
  }

  public void write(byte[] b, int off, int len) throws IOException {
    out.write(b, off, len);
    this.transferred += len;
    this.progressListener.setCurrentValue(this.transferred);
  }
  

  public void write(int b) throws IOException {
    this.transferred++;
    this.progressListener.setCurrentValue(this.transferred);
  }
}