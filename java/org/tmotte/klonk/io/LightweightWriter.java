package org.tmotte.klonk.io;

public interface LightweightWriter extends java.io.Closeable, java.io.Flushable {
  public void append(CharSequence cs) throws Exception;
}