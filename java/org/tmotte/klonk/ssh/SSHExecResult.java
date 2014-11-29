package org.tmotte.klonk.ssh;
public class SSHExecResult {
  final boolean success;
  final String output;
  public SSHExecResult(boolean success, String output){
    this.success=success;
    this.output=output;
  }
  public String toString() {
    return success+" "+output;
  }
}