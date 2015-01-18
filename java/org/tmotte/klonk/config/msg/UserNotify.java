package org.tmotte.klonk.config.msg;
import java.io.PrintWriter;
import org.tmotte.klonk.io.KLog;

public class UserNotify {

  ////////////////////////
  // PRIVATE VARIABLES: //
  ////////////////////////

  private KLog klog;
  private Setter<String> alerter;
  private boolean ensureThreadSafeUI=false;
  private Setter<Throwable> allPurposeExceptionHandler=new Setter<Throwable>(){
    public void set(Throwable t) {
      alert(t);
    }      
  };

  ////////////////////////
  // CONSTRUCTION & DI: //
  ////////////////////////
  
  public UserNotify(java.io.OutputStream out) {
    this(new KLog(out));
  }
  public UserNotify(KLog klog) {
    this.klog=klog;
  }
  public UserNotify setUI(Setter<String> alerter) {
    return setUI(alerter, false);
  }
  public UserNotify setUI(Setter<String> alerter, boolean ensureThreadSafe) {
    this.alerter=alerter;
    this.ensureThreadSafeUI=ensureThreadSafe;
    return this;
  }
  public Setter<Throwable> getExceptionHandler(){
    return allPurposeExceptionHandler;
  }
  
  //////////////
  // LOGGING: //
  //////////////
  
  public void log(String s){
    klog.log(s);
  }
  public void log(Throwable e){
    klog.log(e);
  }
  public void log(Throwable e, String s){
    klog.log(e, s);
  }
  
  //////////////////
  // POPUP-ALERT: //
  //////////////////
  
  public void alert(final String s) {
    if (alerter==null) 
      log("UserNotify: alerter is missing, message was: "+s);
    else
    if (ensureThreadSafeUI)
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          alerter.set(s);
        }
      });
    else
      alerter.set(s);
  }
  public void alert(Throwable t, final String s) { 
    log(t);
    alert(s+" (see log for details) "+t);
  }
  public void alert(Throwable e) {
    log(e);
    alert("Internal error, see log for details: "+e.getMessage());
  }
  
}