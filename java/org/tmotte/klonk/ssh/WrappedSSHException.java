package org.tmotte.klonk.ssh;
public class WrappedSSHException extends Exception {
  Exception wrapped;
  public WrappedSSHException(String message, Exception e) {
    super(message, e);
    this.wrapped=e;
  }
  public Exception getWrapped(){
    return wrapped;
  }  
}