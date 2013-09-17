package org.tmotte.common.swang;
/** Probably not entirely necessarily now that I know about setting the default runtime exception handler in java. */
public interface Fail {
  public void fail(Throwable t);
}