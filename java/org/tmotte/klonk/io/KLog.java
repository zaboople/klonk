package org.tmotte.klonk.io;
import java.io.*;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Setter;

public class KLog {
  
  private String pid;
  private final long start=System.nanoTime();
  private KHome home;
  private PrintWriter commandLineWriter;
  private Setter<Throwable> failPopup;
  private Setter<Throwable> failer=new Setter<Throwable>(){
    public void set(Throwable t) {
      log(t);
    }
  };
  
  public KLog(OutputStream os){
    this.commandLineWriter=new PrintWriter(new OutputStreamWriter(os));
    this.pid=pid;
  }
  public KLog(KHome home, String pid){
    this.home=home;
    this.pid=pid;
  }
  public KLog setFailPopup(Setter<Throwable> a) {
    this.failPopup=a;
    return this;
  }

  public Setter<Throwable> exceptionHandler(){
    return failer;
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
  public File getLogFile(){
    return home.nameFile("log.txt");
  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void logError(Throwable e) {
    if (failPopup!=null)
      failPopup.set(e);
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
    File logFile=getLogFile();
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