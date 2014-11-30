package org.tmotte.klonk.ssh;
public class WrappedSSHException extends RuntimeException { /** FIXME probably junk */
  Exception wrapped;
  public WrappedSSHException(String message, Exception e) {
    super(message, e);
    this.wrapped=e;
  }
  public Exception getWrapped(){
    return wrapped;
  }  
}