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
import javax.swing.SwingUtilities;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.controller.CtrlFileOther;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.controller.CtrlMarks;
import org.tmotte.klonk.controller.CtrlOptions;
import org.tmotte.klonk.controller.CtrlOther;
import org.tmotte.klonk.controller.CtrlSearch;
import org.tmotte.klonk.controller.CtrlSelection;
import org.tmotte.klonk.controller.CtrlUndo;
import org.tmotte.klonk.controller.Favorites;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.windows.MainLayout;
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

  public static void bootApplication(final String[] args){

    /* 1. Do the preliminary setup: */
    
    // KHome:
    String homeDir=KHome.nameIt(System.getProperty("user.home"), "klonk");
    for (int i=0; i<args.length; i++)
      if (args[i].equals("-home") && i<args.length-1){
        args[i]=null;
        homeDir=args[++i].trim();
        args[i]=null;
      }
    final KHome home=new KHome(homeDir);
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
    
    /* 2. Now get all the swing details going: */
    
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        startSwing(args, home, log, fileListen);
      }
    });
  }

  private static void startSwing(String[] args, KHome home, KLog log, FileListen fileListen) {
    //Persist:
    final KPersist persist=new KPersist(home, log);

    //Main controller. 
    final CtrlMain ctrlMain=new CtrlMain(log, persist);
    Editors editors=ctrlMain.getEditors();

    //Main Frame:
    JFrame frame=new JFrame("Klonk");
    frame.setIconImage(getAppIcon());

    //Layout; display starts here:
    final MainLayout layout=new MainLayout(frame, ctrlMain.getAppCloseListener());
    layout.show(
      persist.getWindowBounds(
        new java.awt.Rectangle(10, 10, 300, 300)
      ),
      persist.getWindowMaximized()
    );
    StatusUpdate statusBar=layout.getStatusBar(); 

    //Popups:
    Popups popups=new Popups(
        home, log, frame, persist, statusBar
        ,getPopupIcon() 
        ,ctrlMain.getCurrFileNameGetter()
      );

    //Menus & Controllers & JMenuBar:
    final Menus menus=new Menus(editors, log);
    menus.setFastUndos(persist.getFastUndos())
         .setWordWrap(persist.getWordWrap());
    Favorites favorites=new Favorites(
      persist, menus.getFavoriteFileListener(), menus.getFavoriteDirListener()
    );
    menus.setControllers(
       ctrlMain
      ,new CtrlMarks    (editors, statusBar)
      ,new CtrlSelection(editors, popups, statusBar)
      ,new CtrlUndo     (editors, popups, statusBar, persist)
      ,new CtrlSearch   (editors, popups)
      ,new CtrlOptions  (editors, popups, statusBar, persist, favorites, ctrlMain.getLineDelimiterListener())
      ,new CtrlFileOther(editors, statusBar, favorites)
      ,new CtrlOther    (popups)
    );
    frame.setJMenuBar(menus.getMenuBar());

    //Now we loop back to ctrlMain to fill in its circular dependencies:
    //(Menus currently has access to ctrlMain, so it could that part itself:)
    ctrlMain.setLayout(layout.getMainDisplay(), statusBar);
    ctrlMain.setPopups(popups);
    ctrlMain.setListeners(
      fileListen.getLockRemover(),
      menus.getEditorSwitchListener(),
      menus.getRecentFileListener(),
      menus.getRecentDirListener()
    );
    
    //Now start a new editor and loading files:
    ctrlMain.doNew();
    ctrlMain.doLoadFiles(args);
    fileListen.startDirectoryListener(ctrlMain.getFileReceiver());
  }
  
  
  public static Popups getPopupsForUnitTest() {

    //Create frame that can die off on ESC or when unit test tells it
    //to die via JFrame.dispose()
    initLookFeel();
    final JFrame frame=new JFrame("Klonk");
    KeyAdapter ka=new KeyAdapter() {
      public void keyPressed(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
          System.exit(0);
      }
    };
    frame.addKeyListener(ka);
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        System.exit(0);
      }
    });
    frame.setVisible(true);
    frame.setBounds(new java.awt.Rectangle(100,100,300,300));
    frame.toFront();

    //Return a custom context. At some point we might change the
    //home directory assumption to be an input to this function,
    //but for now it's fine:
    KHome home=new KHome("./test/home");
    KLog log=new KLog(System.out);
    return new Popups (
      home, log, frame, 
      new KPersist(home, log), 
      new StatusUpdate(){
        public void show(String msg)   {System.out.println(msg);}
        public void showBad(String msg){System.out.println("***"+msg+"***");}
        public void showCapsLock(boolean b)           {}
        public void showNoStatus()                    {}
        public void showRowColumn(int row, int column){}
        public void showChangeThis(boolean b){}
        public void showChangeAny(boolean b) {}
        public void showTitle(String title)  {}
      },
      getPopupIcon(),
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
  static Image getPopupIcon() {
    return getIcon("org/tmotte/klonk/windows/app-find-replace.png");  
  }
  static Image getAppIcon() {
    return getIcon("org/tmotte/klonk/windows/app.png");
  }
  
  static Image getIcon(String filename) {
    URL url=Boot.class.getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
}