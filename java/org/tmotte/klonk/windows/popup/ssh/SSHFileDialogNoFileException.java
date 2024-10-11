package org.tmotte.klonk.windows.popup.ssh;
/**
 * This is so we can throw an exception to force the FileDialog to
 * back off and bail, then catch it and accept it as ok and be quiet
 * instead of barking.
 */
public class SSHFileDialogNoFileException extends RuntimeException {
  private static final long serialVersionUID = 1L;
  public SSHFileDialogNoFileException() {
    super();
  }
}