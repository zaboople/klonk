package org.tmotte.klonk.config;
import java.awt.Image;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.msg.Editors;
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
 * There are two layers, an initial layer and a user interface layer. There is also some public-static stuff
 * here for convenience.
 */
public class BootContext {

  //////////////////
  // STATIC BOOT: //
  //////////////////

  public static void main(final String[] args) {

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
    
    //Starting the GUI up:
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        CtrlMain cm=context.createMainController();
        cm.doNew();
        cm.doLoadFiles(args);
        context.getFileListener().startDirectoryListener(cm.getFileReceiver());
      }
    });
  }


  /////////////////////////////////////////
  // INSTANCE VARIABLES AND CONSTRUCTOR: //
  /////////////////////////////////////////

  private KHome home;
  private KLog log;
  private FileListen fileListen;
 
  /**
   * This gives us the 1st layer of the application, enough to 
   * get thru a minimal boot and die informatively when things go wrong:
   */
  private BootContext(String [] args){
    String argHomeDir=null;
    boolean argStdOut=false;
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
    home=new KHome(
      argHomeDir!=null
        ?argHomeDir
        :KHome.nameIt(System.getProperty("user.home"), "klonk")
    );
    String pid=ManagementFactory.getRuntimeMXBean().getName();
    String processID=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    log=argStdOut
      ?new KLog(System.out)
      :new KLog(home, processID);
    fileListen=new FileListen(log, processID, home);    
  } 
  private KHome getHome() { return home; }
  private KLog getLog()   { return log;  }
  private FileListen getFileListener() { return fileListen; }

  
  //////////////////////////
  // USER INTERFACE BOOT: //
  //////////////////////////

  /**
   * This provides the 2nd layer of the application, focused on the user interface.
   */
  private CtrlMain createMainController() {

    initLookFeel();
  
    // Basic logging & persistence:
    Setter<Throwable> failHandler=log.getExceptionHandler();    
    KPersist persist=new KPersist(home, failHandler);
    FontOptions editorFont=persist.getFontAndColors();

    //Main controller:
    CtrlMain ctrlMain=new CtrlMain(failHandler, persist);
    Editors editors=ctrlMain.getEditors();


    // MAIN WINDOW LAYOUT: //

    JFrame mainFrame=new JFrame("Klonk");
    mainFrame.setIconImage(getAppIcon(this));
    MainLayout layout=new MainLayout(mainFrame, ctrlMain.getAppCloseListener());
    layout.show(
      persist.getWindowBounds(
        new java.awt.Rectangle(10, 10, 300, 300)
      ),
      persist.getWindowMaximized()
    );    
    StatusUpdate statusBar=layout.getStatusBar();


    // POPUP WINDOWS: //

    //Our general purpose hello-ok, yes-no-cancel and yes-no popups:
    KAlert alerter=new KAlert(mainFrame);    
    YesNoCancel 
      yesNoCancel=new YesNoCancel(mainFrame, true),
      yesNo      =new YesNoCancel(mainFrame, false);        

    //SSH Login:
    IUserPass iUserPass=new SSHLogin(mainFrame, alerter);

    //File dialog + SSH:
    SSHOptions sshOpts=persist.getSSHOptions();
    SSHConnections sshConns=new SSHConnections(log.getLogger(), alerter)
      .withLogin(iUserPass)
      .withKnown(sshOpts.getKnownHostsFilename())
      .withPrivateKeys(sshOpts.getPrivateKeysFilename());
    FileDialogWrapper fileDialogWrapper=new FileDialogWrapper(
      mainFrame, 
      new SSHFileSystemView(sshConns, log.getLogger()), 
      new SSHFileView()
    );
  
    //Search popups:
    FindAndReplace findAndReplace=
      new FindAndReplace(mainFrame, alerter, statusBar, editorFont);
    GoToLine goToLine=new GoToLine(mainFrame, alerter);

    //Shell:
    Shell shell=new Shell(
      mainFrame, failHandler, persist, fileDialogWrapper, 
      getPopupIcon(this), ctrlMain.getCurrFileNameGetter()
    );

    //Various option popups:
    Favorites favorites=new Favorites(mainFrame, editorFont);
    TabsAndIndents tabsAndIndents=new TabsAndIndents(mainFrame);
    FontPicker fontPicker=new FontPicker(mainFrame, alerter);
    SSHOptionPicker sshOptionPicker=new SSHOptionPicker(mainFrame, fileDialogWrapper);
    LineDelimiters kDelims=new LineDelimiters(mainFrame);
    
    //Help:
    Help help=new Help(mainFrame, home.getUserHome(), editorFont);
    About about=new About(mainFrame);
    

    // MENUS: //
    
    Menus menus=new Menus(editors);  
    menus.setFastUndos(persist.getFastUndos())
          .setWordWrap(persist.getWordWrap());
    {
      //This cannot be an array because of "generic array creation" compiler fail:
      List<Setter<FontOptions>> fontListeners=new java.util.ArrayList<>(10);
      fontListeners.add(shell.getFontListener());
      fontListeners.add(favorites.getFontListener());
      fontListeners.add(help.getFontListener());
      fontListeners.add(findAndReplace.getFontListener());
  
      CtrlFavorites ctrlFavorites=new CtrlFavorites(
        persist, menus.getFavoriteFileListener(), menus.getFavoriteDirListener()
      );
      menus.setControllers(
        ctrlMain
        ,new CtrlMarks    (editors, statusBar)
        ,new CtrlSelection(editors, statusBar, alerter)
        ,new CtrlUndo     (editors, statusBar, yesNo, persist)
        ,new CtrlSearch   (editors, statusBar, findAndReplace, goToLine)
        ,new CtrlFileOther(editors, statusBar, ctrlFavorites)
        ,new CtrlOther    (shell, help, about)
        ,new CtrlOptions  (
          editors, statusBar, persist, ctrlFavorites, 
          ctrlMain.getLineDelimiterListener(), fontListeners,
          sshOptionPicker, tabsAndIndents, favorites, fontPicker, kDelims
        )
      );
    }
    mainFrame.setJMenuBar(menus.getMenuBar());    

    // PUSH THINGS BACK TO MAIN CONTROLLER: //
    // AND RETURN:                          //

    ctrlMain.setLayoutAndPopups(
      layout.getMainDisplay(), 
      statusBar,
      alerter, 
      fileDialogWrapper, 
      yesNoCancel, 
      yesNo
    );
    ctrlMain.setListeners(
      fileListen.getLockRemover(), 
      sshConns.getFileResolver(),
      menus.getEditorSwitchListener(),
      menus.getRecentFileListener(),
      menus.getRecentDirListener()
    );
    return ctrlMain;
  }

  
  ///////////////////////////////////////
  // STATIC UTILITIES:                 //
  // These are public because they are //
  // used in testing:                  //
  ///////////////////////////////////////

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