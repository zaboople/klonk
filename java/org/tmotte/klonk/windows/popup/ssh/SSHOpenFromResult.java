package org.tmotte.klonk.windows.popup.ssh;
public class SSHOpenFromResult {
  public final String sshFilename;
  public boolean sudo=false;
  public SSHOpenFromResult(String f, boolean s) {
    this.sshFilename=f;
    this.sudo=s;
  }
  public String toString() {
    return sshFilename+ (sudo ?" SUDO" :"");
  }
}