package org.tmotte.common.text;

public interface ArgHandler {
  /** Return a new index, or -1 to indicate nothing found */
  public int handle(String[] args, int currIndex);
  public String document();
}