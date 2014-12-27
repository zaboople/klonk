package org.tmotte.klonk.ssh;
class SSHExecResult {
  final boolean success;
  final String output;
  SSHExecResult(boolean success, String output){
    this.success=success;
    this.output=output;
    logFail();
  }
  public String toString() {
    return success+" "+output;
  }
  private void logFail() {
  }
}