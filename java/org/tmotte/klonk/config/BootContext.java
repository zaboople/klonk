package org.tmotte.klonk.config;
import java.awt.Image;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.option.SSHOptions;
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
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.IUserPass;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Popups;
import org.tmotte.klonk.windows.popup.FileDialogWrapper;
import org.tmotte.klonk.windows.popup.ssh.SSHLogin;
import org.tmotte.klonk.windows.popup.ssh.SSHFileSystemView;
import org.tmotte.klonk.windows.popup.ssh.SSHFileView;
import org.tmotte.klonk.windows.popup.ssh.SSHFileDialogNoFileException;
import org.tmotte.klonk.windows.popup.KAlert;
import javax.swing.JMenuBar;

/** 
 * This implements a sort-of framework-free IoC/DI (inversion of control/dependency injection) architecture. 
 * <br>
 * There are also a few public-static functions that are made available for testing popup windows without
 * booting the whole application. 
 */
public class BootContext {

  //////////////////
  // STATIC BOOT: //
  //////////////////

  public static void main(String[] args) {
    bootApplication(args);
  }

  private static void bootApplication(final String[] args){

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
          //SSHFileDialogNoFileException is thrown by our nasty
          //SSHFileSystemView and can be ignored:
          if (!(e instanceof SSHFileDialogNoFileException))
            context.getLog().log(e);//FIXME make this blow up when sending an alert
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

  /////////////////////////////////////////
  // INSTANCE VARIABLES AND CONSTRUCTOR: //
  /////////////////////////////////////////

  //Command-line inputs:
  private String argHomeDir;
  private boolean argStdOut=false;

  //DI Components:
  private KHome home;
  private KLog log;
  private FileListen fileListen;
  private KPersist persist;
  private CtrlMain ctrlMain;
  private JFrame mainFrame;
  private MainLayout layout;
  private StatusUpdate statusBar;
  private Popups popups;
  private Menus menus;
  private Favorites favorites;
  private MainDisplay mainDisplay;
  private String processID;
  private SSHConnections sshConns;
  private IUserPass iUserPass;
  private FileDialogWrapper fileDialogWrapper;
  private Setter<String> alerter;
  
  private BootContext(String [] args){
    for (int i=0; i<args.length; i++)
      if (args[i].equals("-home") && i<args.length-1){
        args[i]=null;
        argHomeDir=args[++i].trim();
        args[i]=null;
      }
      else
      if (args[i].equals("-stdout")){
        args[i]=null;
        argStdOut=true;
      }
    initLookFeel();
  }
  
  //////////////////////////////////////////
  // CONCRETE CLASSES:                    //
  // Sure maybe everything should be an   //
  // interface but sometimes that's just  //
  // not worth it:                        //
  //////////////////////////////////////////

  private Popups getPopups() {
    if (popups==null)
      popups=new Popups(
         getHome()
        ,getLog().getExceptionHandler()
        ,getMainFrame()
        ,getPersist()
        ,getStatusBar()
        ,getPopupIcon() 
        ,getCurrFileNameGetter()
        ,getAlerter()
        ,getSSHConnections()
        ,getFileDialog()
      );
    return popups;
  }
  private KHome getHome() {
    if (home==null) 
      home=new KHome(
        argHomeDir!=null
          ?argHomeDir
          :KHome.nameIt(System.getProperty("user.home"), "klonk")
      );
    return home;
  }
  private KLog getLog() {
    if (log==null)
      log=argStdOut
        ?new KLog(System.out)
        :new KLog(getHome(), getProcessID());
    return log;
  }
  private KPersist getPersist() {
    if (persist==null)
      persist=new KPersist(getHome(), getLog().getExceptionHandler());
    return persist;
  }
  private CtrlMain getMainController() {
    if (ctrlMain==null){
      ctrlMain=new CtrlMain(getLog().getExceptionHandler(), getPersist());
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
  private Editors getEditors() {
    return getMainController().getEditors();
  }
  private Menus getMenus() {
    if (menus==null) {
      menus=new Menus(getEditors());
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
  private MainLayout getLayout() {
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
  private Favorites getFavorites() {
    if (favorites==null)
      favorites=new Favorites(
        persist, 
        getFavoriteFileListener(), 
        getFavoriteDirListener()
      );
    return favorites;
  }
  private FileListen getFileListener() {
    if (fileListen==null) 
      fileListen=new FileListen(getLog(), getProcessID(), getHome());    
    return fileListen;
  }
  private SSHConnections getSSHConnections() {
    if (sshConns==null){
      SSHOptions sshOpts=getPersist().getSSHOptions();
      sshConns=new SSHConnections(
          getLog().getLogger(),
          getAlerter()
        )
        .withLogin(getSSHLogin())
        .withKnown(sshOpts.getKnownHostsFilename())
        .withPrivateKeys(sshOpts.getPrivateKeysFilename());        
    }
    return sshConns;
  }
  private FileDialogWrapper getFileDialog() {
    if (fileDialogWrapper==null)
      fileDialogWrapper=new FileDialogWrapper(
        mainFrame, 
        new SSHFileSystemView(sshConns, getLog().getLogger()), 
        new SSHFileView()
      );
    return fileDialogWrapper;
  }  

  /////////////////////////////////////////////
  // PURE INTERFACES, ABSTRACT CLASSES, AND: //
  // VALUE OBJECTS:                          //
  /////////////////////////////////////////////

  //Abstracts:
  public JFrame getMainFrame() {
    if (mainFrame==null) {
      mainFrame=new JFrame("Klonk");
      mainFrame.setIconImage(getAppIcon());
      mainFrame.setJMenuBar(getMenuBar());
    }
    return mainFrame;
  }
  private JMenuBar getMenuBar() {
    return getMenus().getMenuBar();
  }
  
  //Interfaces:
  private Setter<String> getAlerter() {
    if (alerter==null)
      alerter=new KAlert(getMainFrame());
    return alerter;
  }
  private IUserPass getSSHLogin() {
    if (iUserPass==null) 
      iUserPass=new SSHLogin(getMainFrame(), getAlerter());
    return iUserPass;
  }
  private StatusUpdate getStatusBar() {
    if (statusBar==null)
      statusBar=getLayout().getStatusBar(); 
    return statusBar;
  }
  private LineDelimiterListener getLineDelimiterListener() {
    return getMainController().getLineDelimiterListener();
  }
  private Doer getAppCloseListener() {
    return getMainController().getAppCloseListener();
  }
  private Getter<String> getCurrFileNameGetter() {
    return getMainController().getCurrFileNameGetter();
  }
  private MainDisplay getMainDisplay() {
    return getLayout().getMainDisplay();
  }
  private Doer getLockRemover() {
    return getFileListener().getLockRemover();
  }
  private Setter<List<String>> getFavoriteFileListener() {
    return getMenus().getFavoriteFileListener();
  }
  private Setter<List<String>> getFavoriteDirListener() {
    return getMenus().getFavoriteDirListener();
  }
  private Doer getEditorSwitchListener() {
    return getMenus().getEditorSwitchListener();
  }
  private Setter<List<String>> getRecentFileListener() {
    return getMenus().getRecentFileListener();
  }
  private Setter<List<String>> getRecentDirListener() {
    return getMenus().getRecentDirListener();
  }

  //Concrete value objects:
  private Image getPopupIcon() {
    return getPopupIcon(this);
  }
  private Image getAppIcon() {
    return getAppIcon(this);
  }
  private String getProcessID() {
    if (processID==null) {
      String pid=ManagementFactory.getRuntimeMXBean().getName();
      processID=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    }
    return processID;
  }
  
  
  ////////////////
  // UTILITIES: //
  ////////////////


  public static Image getIcon(Object o, String filename) {
    URL url=o.getClass().getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
  public static Image getPopupIcon(Object o) {
    return getIcon(o, "org/tmotte/klonk/windows/app-find-replace.png");  
  }
  public static Image getAppIcon(Object o) {
    return getIcon(o, "org/tmotte/klonk/windows/app.png");
  }

  public static void initLookFeel() {
    try {
      javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  

}