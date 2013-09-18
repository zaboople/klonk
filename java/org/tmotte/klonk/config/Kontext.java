package org.tmotte.klonk.config;
import org.tmotte.klonk.io.FileListen;
import java.awt.Image;
import java.util.regex.Pattern;
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
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.Klonk;
import org.tmotte.klonk.KLog;
import org.tmotte.klonk.windows.popup.Popups;
import java.util.List;

/** 
 * This class implements our framework-free IoC/DI (inversion of control/dependency injection) stuff. 
 * It is roughly analogous to Spring's "Application Context". 
 * In the future this may get split into Kontext & KontextKreator but for now it's maintainable enough as is.
 */
public class Kontext {

  //Doubt it will be needed outside of here:
  private KLog log;

  public JFrame mainFrame;
  public Fail fail;
  public Popups popups;
  public Setter<String> status;
  public KHome home;
  public KPersist persist;
  public Image iconImage, 
               iconImageFindReplace;//I want to make this a "Getter" but no, no. No I don't. Fail fast. Sigh.
  public Getter<String> currFileGetter;
  
  
  public static void bootApplication(String[] args){
    // 1. Figure out our home directory:
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

    // 2. Get our PID & a log:
    String pid=ManagementFactory.getRuntimeMXBean().getName();
    pid=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    final KLog log=new KLog(home, pid);
    
    // 3. Set up our file listener and check 
    //    and see if we can obtain our mutex:
    FileListen fileListen=new FileListen(log, pid, home);
    if (!fileListen.lockOrSignal(args)) {
      log.log("Klonk is handing off to another process.");
      System.exit(0);
      return;
    }

    // 4. It is helpful to do this as soon as possible:
    Thread.setDefaultUncaughtExceptionHandler( 
      new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread t, Throwable e){
          log.fail(e);
        }
      }
    );

    // 5. Now we make the whole crazy pile of things that talk to things:
    final Klonk klonk=new Klonk(log, fileListen);
    fileListen.setFileReceiver(
      new Setter<List<String>>(){
        public @Override void set(List<String> files) 
          {klonk.doLoadAsync(files);}
      }
    );
    Kontext context=new Kontext(
      home, log, new JFrame("Klonk"), 
      new Setter<String>() {
        public @Override void set(String msg) 
          {klonk.showStatus(msg);}
      }, 
      new Getter<String>() {
        public @Override String get() 
          {return klonk.getCurrentFileName();}
      }
    );    

    //6. Boot into swing...
    log.log("Starting up swing...");
    klonk.startSwing(
      args, context.persist, context.mainFrame, context.iconImage, context.popups
    );
  }
  
  
  public static Kontext getForUnitTest() {

    //Create frame that can die off on ESC or when unit test tells it
    //to die via JFrame.dispose()
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

    //Return context with this frame, as well
    //as home set for our test directory instead
    //of the usual:
    KHome home=new KHome("./test/home");
    return new Kontext(
      home,
      new KLog(System.out),
      frame,
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
  
  
  private Kontext(
      KHome home, KLog log, JFrame mainFrame, 
      Setter<String> status, Getter<String> currFileGetter
    ){
    try {
      javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.home=home;
    this.log=log;
    this.mainFrame=mainFrame;
    this.fail=log;
    this.status=status;
    persist=new KPersist(home, fail);
    iconImage           =getIcon("org/tmotte/klonk/windows/app.png");
    iconImageFindReplace=getIcon("org/tmotte/klonk/windows/app-find-replace.png");

    //This is an IoC sublayer, more info in its javadoc:
    popups=new Popups(
      mainFrame, home, fail, persist, status, currFileGetter, iconImageFindReplace
    );
    
    //Reverse dependency stuff (evil but ok):
    log.setFailPopup(popups.getFailPopup());
 }
  
  ////////////////
  // UTILITIES: //
  ////////////////
  
  private Image getIcon(String filename) {
    URL url=getClass().getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
}