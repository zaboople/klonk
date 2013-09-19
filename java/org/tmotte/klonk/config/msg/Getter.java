package org.tmotte.klonk.config.msg;

/** 
 * OK I lied. We're not framework-free. There's this thing. Worse yet, I'm seriously considering "Setter". 
 * Here's to the world's almost tiniest framework.
 */
public interface Getter<T> {
  public T get();
}