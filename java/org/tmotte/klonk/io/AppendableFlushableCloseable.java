package org.tmotte.klonk.io;

public interface AppendableFlushableCloseable extends java.io.Closeable, java.io.Flushable {
  public void append(CharSequence cs) throws Exception;
}