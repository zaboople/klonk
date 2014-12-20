package org.tmotte.klonk.io;
import java.io.*;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Setter;

public class KLog {
  
  private final long start=System.nanoTime();
  private KHome home;
  private PrintWriter commandLineWriter;
  private Setter<Throwable> failPopup;
  private java.text.SimpleDateFormat sdformat;  
  private Setter<Throwable> failer=new Setter<Throwable>(){
    public void set(Throwable t) {log(t);}
  };
  private Setter<String> ilogger=new Setter<String>(){
    public void set(String s) {log(s);}
  };
  
  //////////////////
  // CONSTRUCTOR: //
  //////////////////
  
  public KLog(OutputStream os){
    this.commandLineWriter=new PrintWriter(new OutputStreamWriter(os));
    sdformat=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS ");
  }
  public KLog(KHome home, String pid){
    this.home=home;
    sdformat=new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS '"+pid+"\t'");
  }
  public KLog setFailPopup(Setter<Throwable> a) {
    this.failPopup=a;
    return this;
  }
  
  /////////////////
  // INTERFACES: //
  /////////////////

  public Setter<Throwable> getExceptionHandler(){
    return failer;
  }
  public Setter<String> getLogger(){
    return ilogger;
  }

  ///////////////////////
  // PUBLIC FUNCTIONS: //
  ///////////////////////
  
  public void log(String s) {
    logPlain(
      sdformat.format(new java.util.Date())+s
    );
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
    //e.printStackTrace();
    PrintWriter pw=getWriter();
    if (pw!=null)
      try {
        e.printStackTrace(pw);
        pw.flush();
      } finally {
        close(pw);
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
    KLog log=new KLog(System.out);
    log.log("BARF");
    System.out.flush();
    
  }
}