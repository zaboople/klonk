package org.tmotte.klonk.ssh;
public class SSHExecResult {
  final boolean success;
  final String output;
  public SSHExecResult(boolean success, String output){
    this.success=success;
    this.output=output;
    logFail();
  }
  public String toString() {
    return success+" "+output;
  }
  private void logFail() {
    if (!success)
      System.out.println("SSHExec failed: "+output);
  }
}