package org.tmotte.klonk.config;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.KLog;
import org.tmotte.klonk.Klonk;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.windows.popup.Popups;

/** 
 * This implements a sort-of framework-free IoC/DI (inversion of control/dependency injection) architecture. Boot
 * can either assemble the full array of components for a complete application, and boot it; or it can assemble
 * a minimal set that will support unit testing, primarily for the different dialog windows.
 */
public class Boot {

  public static void main(String[] args) {
    bootApplication(args);
  }

  public static void bootApplication(String[] args){

    /*
      1. Do the preliminary setup:
    */
    
    // KHome:
    String homeDir=KHome.nameIt(System.getProperty("user.home"), "klonk");
    for (int i=0; i<args.length; i++)
      if (args[i].equals("-home") && i<args.length-1){
        args[i]=null;
        homeDir=args[++i].trim();
        args[i]=null;
      }
    KHome home=new KHome(homeDir);
    if (!home.ready)
      return;

    // KLog:
    String pid=ManagementFactory.getRuntimeMXBean().getName();
    pid=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    final KLog log=new KLog(home, pid);
    
    // FileListen:
    final FileListen fileListen=new FileListen(log, pid, home);
    if (!fileListen.lockOrSignal(args)) {
      log.log("Klonk is handing off to another process.");
      System.exit(0);
      return;
    }

    // It is helpful to do this as soon as possible:
    Thread.setDefaultUncaughtExceptionHandler( 
      new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread t, Throwable e){
          log.fail(e);
        }
      }
    );
    initLookFeel();
    
    
    /* 
       2. Now make the whole crazy pile of things that talk to things. Regrettably
          there are some cyclical dependencies, and down within Klonk there is 
          some not-so-loose coupling, but this will do:
    */
    final KPersist persist=new KPersist(home, log);
    final Klonk klonk=new Klonk(
      log, persist,
      new Doer(){
        public @Override void doIt() 
          {fileListen.removeLock();}
      }
    );
    JFrame frame=new JFrame("Klonk");
    frame.setIconImage(getIcon("org/tmotte/klonk/windows/app.png", klonk.getClass()));
    PopupContext context=new PopupContext(
      home, log, frame, persist,
      new Setter<String>() {
        public @Override void set(String msg) 
          {klonk.showStatus(msg);}
      }, 
      new Getter<String>() {
        public @Override String get() 
          {return klonk.getCurrentFileName();}
      }
    );    

    
    //3. Boot into swing:
    log.log("Starting up swing...");
    klonk.startSwing(args, context.mainFrame, context.popups);
    
    
    //4. Listen for files from other app instances:
    fileListen.startDirectoryListener(
      new Setter<List<String>>(){
        public @Override void set(List<String> files) 
          {klonk.doLoadAsync(files);}
      }
    );
  }
  
  
  public static PopupContext getForUnitTest() {

    //Create frame that can die off on ESC or when unit test tells it
    //to die via JFrame.dispose()
    initLookFeel();
    final JFrame frame=new JFrame("Klonk");
    frame.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
          System.exit(0);
      }
    });
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        System.exit(0);
      }
    });
    frame.setVisible(true);
    frame.toFront();

    //Return a custom context. At some point we might change the
    //home directory assumption to be an input to this function,
    //but for now it's fine:
    KHome home=new KHome("./test/home");
    KLog log=new KLog(System.out);
    return new PopupContext(
      home, log, frame, 
      new KPersist(home, log), 
      new Setter<String>(){
        public void set(String msg) {
          System.out.println("Status: "+msg);
        }
      },
      new Getter<String>(){
        public String get(){return "-none-";}
      }
    );
  }
  
  

  ////////////////
  // UTILITIES: //
  ////////////////

  private static void initLookFeel() {
    try {
      javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  static Image getIcon(String filename, Class cl) {
    URL url=cl.getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
}