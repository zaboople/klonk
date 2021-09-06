package org.tmotte.klonk.config.msg;

/**
 * Nowadays this could obviously be replaced with java.util.function.Consumer.
 */
public interface Setter<T> {
  public void set(T value);
}