package org.tmotte.klonk.ssh;

/**
 * This is only thrown when an SSH command fails horribly on the client side. When it fails
 * on the server side, one must look in SSHExecResult to find out if/why.
 */
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