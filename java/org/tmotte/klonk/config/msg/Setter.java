package org.tmotte.klonk.config.msg;

/** 
 * As per Getter, I'm a liar, and now I'm even worse. The world's most evil 2-interface framework is born. 
 * Actually it's pretty ordinary, all things considered.
 */
public interface Setter<T> {
  public void set(T value);
}