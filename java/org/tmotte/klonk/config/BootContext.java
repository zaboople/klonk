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
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.MainDisplay;
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
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Popups;
import javax.swing.JMenuBar;


/** 
 * This implements a sort-of framework-free IoC/DI (inversion of control/dependency injection) architecture. By
 * default, BootContext will assemble the full array of components for a complete application. Using inheritance
 * &amp; overrides, a test context can be created that provides a more minimalist environment with mocking et al.
 */
public class BootContext {
  //Inputs:
  String[] args;

  //DI Components:
  KHome home;
  KLog log;
  FileListen fileListen;
  KPersist persist;
  CtrlMain ctrlMain;
  JFrame mainFrame;
  MainLayout layout;
  StatusUpdate statusBar;
  Popups popups;
  Menus menus;
  Favorites favorites;
  MainDisplay mainDisplay;
  String processID;
  
  protected BootContext(String [] args){
    this.args=args;
    initLookFeel();
  }
  public KHome getHome() {
    if (home==null) {
      String homeDir=KHome.nameIt(System.getProperty("user.home"), "klonk");
      for (int i=0; i<args.length; i++)
        if (args[i].equals("-home") && i<args.length-1){
          args[i]=null;
          homeDir=args[++i].trim();
          args[i]=null;
        }
      home=new KHome(homeDir);
    }
    return home;
  }
  public KLog getLog() {
    if (log==null)
      log=new KLog(getHome(), getProcessID());
    return log;
  }
  public KPersist getPersist() {
    if (persist==null)
      persist=new KPersist(getHome(), getFail());
    return persist;
  }
  public CtrlMain getMainController() {
    if (ctrlMain==null){
      ctrlMain=new CtrlMain(getFail(), getPersist());
      ctrlMain.setLayout(getMainDisplay(), getStatusBar());
      ctrlMain.setPopups(getPopups());
      ctrlMain.setListeners(
        getLockRemover(), 
        getEditorSwitchListener(),
        getRecentFileListener(),
        getRecentDirListener()
      );
    }
    return ctrlMain;
  }
  public Editors getEditors() {
    return getMainController().getEditors();
  }
  public JFrame getMainFrame() {
    if (mainFrame==null) {
      mainFrame=new JFrame("Klonk");
      mainFrame.setIconImage(getAppIcon());
      mainFrame.setJMenuBar(getMenuBar());
    }
    return mainFrame;
  }
  public StatusUpdate getStatusBar() {
    if (statusBar==null)
      statusBar=getLayout().getStatusBar(); 
    return statusBar;
  }
  public Popups getPopups() {
    if (popups==null)
      popups=new Popups(
        getHome(), getFail()
        ,getMainFrame()
        ,getPersist()
        ,getStatusBar()
        ,getPopupIcon() 
        ,getCurrFileNameGetter()
      );
    return popups;
  }
  public Menus getMenus() {
    if (menus==null) {
      menus=new Menus(getEditors(), getFail());
      menus.setFastUndos(getPersist().getFastUndos())
           .setWordWrap( getPersist().getWordWrap());
      Editors ed=getEditors();
      StatusUpdate sup=getStatusBar();
      Popups pop=getPopups();
      KPersist per=getPersist();
      Favorites fave=getFavorites();
      menus.setControllers(
        getMainController()
        ,new CtrlMarks    (ed, sup)
        ,new CtrlSelection(ed, pop, sup)
        ,new CtrlUndo     (ed, pop, sup, per)
        ,new CtrlSearch   (ed, pop)
        ,new CtrlOptions  (ed, pop, sup, per, fave, getLineDelimiterListener())
        ,new CtrlFileOther(ed, sup, fave)
        ,new CtrlOther    (pop)
      );
    }
    return menus;
  }
  public MainLayout getLayout() {
    if (layout==null) {
      layout=new MainLayout();
      layout.init(getMainFrame());
      layout.setAppCloseListener(getAppCloseListener());
      layout.show(
        getPersist().getWindowBounds(
          new java.awt.Rectangle(10, 10, 300, 300)
        ),
        getPersist().getWindowMaximized()
      );
    }
    return layout;
  }
  public Favorites getFavorites() {
    if (favorites==null)
      favorites=new Favorites(
        persist, 
        getFavoriteFileListener(), 
        getFavoriteDirListener()
      );
    return favorites;
  }
  public FileListen getFileListener() {
    if (fileListen==null) 
      fileListen=new FileListen(getLog(), getProcessID(), getHome());    
    return fileListen;
  }
  public Image getPopupIcon() {
    return getIcon("org/tmotte/klonk/windows/app-find-replace.png");  
  }
  public Image getAppIcon() {
    return getIcon("org/tmotte/klonk/windows/app.png");
  }
  public JMenuBar getMenuBar() {
    return getMenus().getMenuBar()  ;
  }
  public String getProcessID() {
    if (processID==null) {
      String pid=ManagementFactory.getRuntimeMXBean().getName();
      processID=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    }
    return processID;
  }
  

  //////////////////////////////////////////////////////////
  //Interface implementations. 
  //These do not need to be tested, but possibly mocked.
  //All but getFail() will only be invoked once if at all.
  //////////////////////////////////////////////////////////

  protected Fail getFail() {
    return getLog();
  }
  protected LineDelimiterListener getLineDelimiterListener() {
    return getMainController().getLineDelimiterListener();
  }
  protected Doer getAppCloseListener() {
    return getMainController().getAppCloseListener();
  }
  protected Getter<String> getCurrFileNameGetter() {
    return getMainController().getCurrFileNameGetter();
  }
  
  protected MainDisplay getMainDisplay() {
    return getLayout().getMainDisplay();
  }
  protected Doer getLockRemover() {
    return getFileListener().getLockRemover();
  }
  
  protected Setter<List<String>> getFavoriteFileListener() {
    return getMenus().getFavoriteFileListener();
  }
  protected Setter<List<String>> getFavoriteDirListener() {
    return getMenus().getFavoriteDirListener();
  }
  protected Doer getEditorSwitchListener() {
    return getMenus().getEditorSwitchListener();
  }
  protected Setter<List<String>> getRecentFileListener() {
    return getMenus().getRecentFileListener();
  }
  protected Setter<List<String>> getRecentDirListener() {
    return getMenus().getRecentDirListener();
  }

  ////////////////
  // UTILITIES: //
  ////////////////

  protected Image getIcon(String filename) {
    URL url=getClass().getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
  public void initLookFeel() {
    try {
      javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  ///////////////////////
  // BOOT APPLICATION: //
  ///////////////////////
 
 
  public static void bootApplication(final String[] args){

    //Initialize the context object. If we can't get a home directory,
    //we're DOA, no point in logging:
    final BootContext context=new BootContext(args);
    if (!context.getHome().ready)
      return;  
      
    //Find out if the application is already running:
    if (!context.getFileListener().lockOrSignal(args)) {
      context.getLog().log("Klonk is handing off to another process.");
      System.exit(0);
      return;
    }
    
    //After the GUI starts up, all errors go here:
    Thread.setDefaultUncaughtExceptionHandler( 
      new Thread.UncaughtExceptionHandler() {
        public void uncaughtException(Thread t, Throwable e){
          context.getLog().fail(e);
        }
      }
    );
    
    //Now we start doing things:
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        CtrlMain ctrlMain=context.getMainController();
        ctrlMain.doNew();
        ctrlMain.doLoadFiles(args);
        context.getFileListener().startDirectoryListener(ctrlMain.getFileReceiver());
      }
    });
  }
  

}