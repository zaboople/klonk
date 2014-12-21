package org.tmotte.klonk.config;
import java.awt.Image;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JMenuBar;
import javax.swing.SwingUtilities;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.MainDisplay;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.controller.CtrlFavorites;
import org.tmotte.klonk.controller.CtrlFileOther;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.controller.CtrlMarks;
import org.tmotte.klonk.controller.CtrlOptions;
import org.tmotte.klonk.controller.CtrlOther;
import org.tmotte.klonk.controller.CtrlSearch;
import org.tmotte.klonk.controller.CtrlSelection;
import org.tmotte.klonk.controller.CtrlUndo;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.ssh.IUserPass;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.About;
import org.tmotte.klonk.windows.popup.Favorites;
import org.tmotte.klonk.windows.popup.FindAndReplace;
import org.tmotte.klonk.windows.popup.FileDialogWrapper;
import org.tmotte.klonk.windows.popup.FontPicker;
import org.tmotte.klonk.windows.popup.GoToLine;
import org.tmotte.klonk.windows.popup.KAlert;
import org.tmotte.klonk.windows.popup.LineDelimiters;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Help;
import org.tmotte.klonk.windows.popup.Popups;
import org.tmotte.klonk.windows.popup.Shell;
import org.tmotte.klonk.windows.popup.TabsAndIndents;
import org.tmotte.klonk.windows.popup.YesNoCancel;
import org.tmotte.klonk.windows.popup.ssh.SSHFileDialogNoFileException;
import org.tmotte.klonk.windows.popup.ssh.SSHFileSystemView;
import org.tmotte.klonk.windows.popup.ssh.SSHFileView;
import org.tmotte.klonk.windows.popup.ssh.SSHLogin;
import org.tmotte.klonk.windows.popup.ssh.SSHOptionPicker;

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
        CtrlMain cm=context.getMainController();
        cm.doNew();
        cm.doLoadFiles(args);
        context.getFileListener().startDirectoryListener(cm.getFileReceiver());
      }
    });
  }

  /////////////////////////////////////////
  // INSTANCE VARIABLES AND CONSTRUCTOR: //
  /////////////////////////////////////////

  //Command-line inputs:
  private String argHomeDir;
  private boolean argStdOut=false;

  //State-control components:
  private String processID;
  private KHome home;
  private KLog log;
  private KPersist persist;
  private FileListen fileListen;
  private CtrlMain ctrlMain;
  private CtrlFavorites ctrlFavorites;

  //Main window components:
  private Menus menus;
  private StatusUpdate statusBar;
  private MainDisplay mainDisplay;
  
  //Popup window components
  private About about;
  private Favorites favorites;
  private FileDialogWrapper fileDialogWrapper;
  private FindAndReplace findAndReplace;
  private FontPicker fontPicker;
  private GoToLine goToLine;
  private Help help;
  private IUserPass iUserPass;
  private JFrame mainFrame;
  private LineDelimiters kDelims;
  private MainLayout layout;
  private Popups popups;
  private SSHConnections sshConns;
  private SSHOptionPicker sshOptionPicker;
  private Setter<String> alerter;
  private Shell shell;
  private TabsAndIndents tabsAndIndents;
  private YesNoCancel yesNo;
  private YesNoCancel yesNoCancel;
  
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
  
  /////////////////////////////////////////////////
  // Stack overflow (and otherwise) prevention:  //
  /////////////////////////////////////////////////

  private java.util.Set<String> checks=new java.util.HashSet<String>();
  /** 
   * Everything should call this during object assembly to prevent circular references
   * from causing unexpected behavior. It will crash the boot process in the event
   * that such happens.
   */
  private BootContext check(String s) {
    if (checks.contains(s))
      throw new RuntimeException("Recursed back to constructor from constructor: "+s);
    checks.add(s);
    return this;
  }
  
  //////////////////////////////////////////
  // CONCRETE CLASSES:                    //
  // Sure maybe everything should be an   //
  // interface but sometimes that's just  //
  // not worth it:                        //
  //////////////////////////////////////////

  private KHome getHome() {
    if (home==null) {
      check("home");
      home=new KHome(
        argHomeDir!=null
          ?argHomeDir
          :KHome.nameIt(System.getProperty("user.home"), "klonk")
      );
    }
    return home;
  }
  private KLog getLog() {
    if (log==null){
      check("log");
      log=argStdOut
        ?new KLog(System.out)
        :new KLog(getHome(), getProcessID());
    }
    return log;
  }
  private KPersist getPersist() {
    if (persist==null){
      check("persist");
      persist=new KPersist(getHome(), getLog().getExceptionHandler());
    }
    return persist;
  }
  private CtrlMain getMainController() {
    if (ctrlMain==null){
      check("ctrlMain");
      ctrlMain=new CtrlMain(
        getLog().getExceptionHandler(), 
        getPersist() 
      );
      ctrlMain.setLayout(
        getMainDisplay(), 
        getStatusBar()
      );
      ctrlMain.setPopups(
        getAlerter(), 
        getFileDialog(),
        getYesNoCancel(),
        getYesNo()
      );
      ctrlMain.setListeners(
        getLockRemover(), 
        getEditorSwitchListener(),
        getRecentFileListener(),
        getRecentDirListener()
      );
    }
    return ctrlMain;
  }
  private SSHConnections getSSHConnections() {
    if (sshConns==null){
      check("sshConns");
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
  private Editors getEditors() {
    return getMainController().getEditors();
  }
  private Menus getMenus() {
    if (menus==null) {
      check("menus");
      menus=new Menus(getEditors());
      menus.setFastUndos(getPersist().getFastUndos())
           .setWordWrap( getPersist().getWordWrap());
      Editors ed=getEditors();
      StatusUpdate sup=getStatusBar();
      menus.setControllers(
        getMainController()
        ,new CtrlMarks    (ed, sup)
        ,new CtrlSelection(ed, sup, getAlerter())
        ,new CtrlUndo     (ed, sup, getYesNo(), getPersist())
        ,new CtrlSearch   (ed, sup, getFindAndReplace(), getGoToLine())
        ,new CtrlFileOther(ed, sup, getCtrlFavorites())
        ,new CtrlOther    (getShell(), getHelp(), getAbout())
        ,new CtrlOptions(
          ed, sup,
          getPersist(), getCtrlFavorites(), getLineDelimiterListener(), getFontListeners(),
          getSSHOptionPicker(), getTabsAndIndents(), getFavorites(), getFontPicker(),
          getPopups()
        )
      );
    }
    return menus;
  }
  private MainLayout getLayout() {
    if (layout==null) {
      check("layout");
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
  private CtrlFavorites getCtrlFavorites() {
    if (ctrlFavorites==null){
      check("ctrlFavorites");
      ctrlFavorites=new CtrlFavorites(
        persist, getFavoriteFileListener(), getFavoriteDirListener()
      );
    }
    return ctrlFavorites;
  }
  private FileListen getFileListener() {
    if (fileListen==null) {
      check("fileListen");
      fileListen=new FileListen(getLog(), getProcessID(), getHome());    
    }
    return fileListen;
  }

  private Favorites getFavorites() {
    if (favorites==null){
      check("favorites");
      favorites=new Favorites(getMainFrame(), getPersist().getFontAndColors());
    }
    return favorites;
  }
  private Popups getPopups() {
    if (popups==null){
      check("popups");
      popups=new Popups(getMainFrame() ,getAlerter());
    }
    return popups;
  }
  private Help getHelp() {
    if (help==null){
      check("help");
      help=new Help(mainFrame, home.getUserHome(), getPersist().getFontAndColors());
    }
    return help;
  }
  private About getAbout() {
    if (about==null){
      check("about");
      about=new About(mainFrame);
    }
    return about;
  }
  private LineDelimiters getLineDelimiters() {
    if (kDelims==null){
      check("kDelims");
      kDelims=new LineDelimiters(getMainFrame());
    }
    return kDelims;
  }  
  private SSHOptionPicker getSSHOptionPicker() {
    if (sshOptionPicker==null){
      check("sshOptionPicker");
      sshOptionPicker=new SSHOptionPicker(getMainFrame(), getFileDialog());
    }
    return sshOptionPicker;
  } 
  private FileDialogWrapper getFileDialog() {
    if (fileDialogWrapper==null){
      check("fileDialogWrapper");
      fileDialogWrapper=new FileDialogWrapper(
        mainFrame, 
        new SSHFileSystemView(getSSHConnections(), getLog().getLogger()), 
        new SSHFileView()
      );
    }
    return fileDialogWrapper;
  }  
  private GoToLine getGoToLine() {
    if (goToLine==null){
      check("goToLine");
      goToLine=new GoToLine(getMainFrame(), getAlerter());
    }
    return goToLine;
  }
  private YesNoCancel getYesNoCancel() {
    if (yesNoCancel==null){
      check("yesNoCancel");
      yesNoCancel=new YesNoCancel(getMainFrame(), true);
    }
    return yesNoCancel;
  }
  private YesNoCancel getYesNo() {
    if (yesNo==null){
      check("yesNo");
      yesNo=new YesNoCancel(mainFrame, false);
    }
    return yesNo;
  }
  private Shell getShell() {
    if (shell==null) {
      check("shell");
      shell=new Shell(
         getMainFrame() 
        ,getLog().getExceptionHandler()
        ,getPersist()
        ,getFileDialog()
        ,getPopupIcon() 
        ,getCurrFileNameGetter()
      );
    }
    return shell;
  }
  private TabsAndIndents getTabsAndIndents() {
    if (tabsAndIndents==null) {
      check("tabsAndIndents");
      tabsAndIndents=new TabsAndIndents(getMainFrame());
    }
    return tabsAndIndents;
  }
  private FontPicker getFontPicker() {
    if (fontPicker==null)
      fontPicker=new FontPicker(getMainFrame(), getAlerter());
    return fontPicker;
  }
  
  

  /////////////////////////////////////////////
  // PURE INTERFACES, ABSTRACT CLASSES, AND: //
  // VALUE OBJECTS:                          //
  /////////////////////////////////////////////

  //Abstracts:
  public JFrame getMainFrame() {
    if (mainFrame==null) {
      check("mainFrame");
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
    if (alerter==null){
      check("alerter");
      alerter=new KAlert(getMainFrame());
    }
    return alerter;
  }
  private IUserPass getSSHLogin() {
    if (iUserPass==null) {
      check("iUserPass");
      iUserPass=new SSHLogin(getMainFrame(), getAlerter());
    }
    return iUserPass;
  }
  private FindAndReplace getFindAndReplace() {
    if (findAndReplace==null){
      check("findAndReplace");
      findAndReplace=new FindAndReplace(
        getMainFrame(), getAlerter(), getStatusBar(), getPersist().getFontAndColors()
      );
    }
    return findAndReplace;
  }  

  // Nested interfaces: For many of these, if the function gets called twice,
  // a new object will be returned each time, but it's not a big deal. We should
  // try to avoid that, however.
  
  private MainDisplay getMainDisplay() {
    return getLayout().getMainDisplay();
  }
  private StatusUpdate getStatusBar() {
    return getLayout().getStatusBar(); 
  }
  private LineDelimiterListener getLineDelimiterListener() {
    return getMainController().getLineDelimiterListener();
  }
  private List<Setter<FontOptions>> getFontListeners() {
    List<Setter<FontOptions>> fl=new java.util.ArrayList<>(10);
    fl.add(getShell().getFontListener());
    fl.add(getFavorites().getFontListener());
    fl.add(getHelp().getFontListener());
    fl.add(getFindAndReplace().getFontListener());
    return fl;
  }  
  private Doer getAppCloseListener() {
    return getMainController().getAppCloseListener();
  }
  private Getter<String> getCurrFileNameGetter() {
    return getMainController().getCurrFileNameGetter();
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