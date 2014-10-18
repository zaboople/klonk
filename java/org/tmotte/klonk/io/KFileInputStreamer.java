package org.tmotte.klonk.io;
import java.io.InputStream;
public interface KFileInputStreamer {
  public InputStream getInputStream() throws Exception;
}