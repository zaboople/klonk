package org.tmotte.klonk.io;
import java.io.*;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.config.KHome;

public class KLog implements org.tmotte.common.swang.Fail {
  
  String pid;
  final long start=System.nanoTime();
  Fail failPopup;
  KHome home;
  PrintWriter commandLineWriter;
  
  public KLog(OutputStream os){
    this.commandLineWriter=new PrintWriter(new OutputStreamWriter(os));
    this.pid=pid;
  }
  public KLog(KHome home, String pid){
    this.home=home;
    this.pid=pid;
  }
  public KLog setFailPopup(Fail a) {
    this.failPopup=a;
    return this;
  }

  /** Fulfills Fail interface: */
  public void fail(Throwable t) {
    if (failPopup!=null)
      failPopup.fail(t);
    logError(t);
  }
  public void log(String s) {
    logPlain(pid+": "+(System.currentTimeMillis())+" "+s);
  }
  public void error(String s) {
    log("ERROR: "+s);
  }
  public void log(Throwable e) {
    logError(e);
  }
  public void log(Throwable e, String s) {
    error(s);
    logError(e);
  }


  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void logError(Throwable e) {
    e.printStackTrace();
    PrintWriter pw=getWriter();
    if (pw!=null)
      try {
        e.printStackTrace(pw);
        pw.flush();
      } finally {
        pw.close();
      }
  }
  private void logPlain(String s) {
    PrintWriter pw=getWriter();
    if (pw!=null)
      try {
        pw.println(s);
        pw.flush();
      } finally {
        close(pw);
      }
  }
  private void close(PrintWriter p){
    if (commandLineWriter!=null)
      return;
    p.close();
  }
  private PrintWriter getWriter() {
    if (commandLineWriter!=null)
      return commandLineWriter;
    File logFile=home.nameFile("log.txt");
    try {
      return new PrintWriter(new OutputStreamWriter(new FileOutputStream(logFile, true)));
    } catch (Exception e) {
      System.err.println("Could not create log file "+logFile.getAbsolutePath());
      e.printStackTrace();
      return null;
    }
  }

  ///////////
  // TEST: //
  ///////////
  
  public static void main(String[] args) {
  }
}