package org.tmotte.klonk.config;
import java.awt.Image;
import java.lang.management.ManagementFactory;
import java.net.URL;
import java.util.List;
import java.util.regex.Pattern;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.SwingUtilities;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.UserNotify;
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
import org.tmotte.klonk.io.FileListenMemoryMap;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.io.LockInterface;
import org.tmotte.klonk.ssh.IUserPass;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.About;
import org.tmotte.klonk.windows.popup.EncryptionInput;
import org.tmotte.klonk.windows.popup.Favorites;
import org.tmotte.klonk.windows.popup.FindAndReplace;
import org.tmotte.klonk.windows.popup.FileDialogWrapper;
import org.tmotte.klonk.windows.popup.FontPicker;
import org.tmotte.klonk.windows.popup.GoToLine;
import org.tmotte.klonk.windows.popup.KAlert;
import org.tmotte.klonk.windows.popup.LineDelimiters;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.OpenFileList;
import org.tmotte.klonk.windows.popup.Help;
import org.tmotte.klonk.windows.popup.Shell;
import org.tmotte.klonk.windows.popup.TabsAndIndents;
import org.tmotte.klonk.windows.popup.YesNoCancel;
import org.tmotte.klonk.windows.popup.ssh.SSHFileDialogNoFileException;
import org.tmotte.klonk.windows.popup.ssh.SSHFileSystemView;
import org.tmotte.klonk.windows.popup.ssh.SSHFileView;
import org.tmotte.klonk.windows.popup.ssh.SSHLogin;
import org.tmotte.klonk.windows.popup.ssh.SSHOpenFrom;
import org.tmotte.klonk.windows.popup.ssh.SSHOptionPicker;
import org.tmotte.common.swang.MinimumFont;

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

    //Turn off direct 3d on windows, seems to cause exceptions rendering our yes/no/cancel window
    System.setProperty("sun.java2d.d3d", "false");

    //Turn on macintosh menubar, and force apple to ask our windows if they want to quit:
    System.setProperty("apple.laf.useScreenMenuBar", "true");
    System.setProperty("apple.eawt.quitStrategy", "CLOSE_ALL_WINDOWS");

    //Find out if the application is already running:
    if (!context.getLockInterface().lockOrSignal(args)) {
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
            context.getLog().alert(e, "Unexpected error");
        }
      }
    );

    //Starting the GUI up:
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        CtrlMain cm=context.createMainController();
        cm.doNew();
        cm.doLoadFiles(args);
        context.getLockInterface().startListener(cm.getAsyncFileReceiver());
      }
    });
  }


  /////////////////////////////////////////
  // INSTANCE VARIABLES AND CONSTRUCTOR: //
  /////////////////////////////////////////

  private KHome home;
  private LockInterface locker;
  private KLog klog;
  private UserNotify userNotify;
  private CurrentOS currentOS;

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
    currentOS=getCurrentOS();
    home=new KHome(
      argHomeDir!=null
        ?argHomeDir
        :KHome.nameIt(
          System.getProperty("user.home"),
          currentOS.isMSWindows ?"klonk" :".klonk"
        )
    );
    String processID;
    if (currentOS.isOSX) {
      //The RuntimeMXBean trick is so slow on OSX that it's crippling. Stupid workaround:
      processID=String.valueOf(new java.util.Random().nextInt());
    }
    else {
      String pid=ManagementFactory.getRuntimeMXBean().getName();
      processID=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    }
    klog=argStdOut
      ?new KLog(System.out)
      :new KLog(home, processID);
    userNotify=new UserNotify(klog);
    locker=currentOS.isOSX
      ?new FileListenMemoryMap(home, klog)
      :new FileListen(klog, processID, home);
  }
  private UserNotify getLog()   { return userNotify;  }
  private LockInterface getLockInterface() { return locker; }
  private KHome getHome() { return home; }


  //////////////////////////
  // USER INTERFACE BOOT: //
  //////////////////////////

  /**
   * This provides the 2nd layer of the application, focused on the user interface.
   */
  private CtrlMain createMainController() {

    initLookFeel();

    // Basic logging & persistence:
    Setter<Throwable> failHandler=userNotify.getExceptionHandler();
    KPersist persist=new KPersist(home, failHandler, currentOS);
    FontOptions fontOptions=persist.getFontAndColors();

    //Main controller:
    CtrlMain ctrlMain=new CtrlMain(userNotify, persist, currentOS);
    Editors editors=ctrlMain.getEditors();

    //Main frame:
    JFrame mainFrame=new JFrame("Klonk");
    mainFrame.setIconImage(getAppIcon(this));
    PopupInfo popupInfo=new PopupInfo(mainFrame, currentOS);

    // MAIN WINDOW LAYOUT: //
    MainLayout layout=new MainLayout(popupInfo, fontOptions, ctrlMain.getAppCloseListener());
    layout.show(
      persist.getWindowBounds(
        new java.awt.Rectangle(10, 10, 300, 300)
      ),
      persist.getWindowMaximized()
    );
    StatusUpdate statusBar=layout.getStatusBar();


    // POPUP WINDOWS: //

    // Our general purpose hello-ok, yes-no-cancel and yes-no popups;
    // Also backfills alerter into our general-purpose notifier:
    final KAlert alerter=new KAlert(popupInfo, fontOptions);
    YesNoCancel
      yesNoCancel=new YesNoCancel(popupInfo, fontOptions, true),
      yesNo      =new YesNoCancel(popupInfo, fontOptions, false);
    userNotify.setUI(alerter, false);

    // SSH Login:
    IUserPass iUserPass=new SSHLogin(popupInfo, fontOptions, alerter);

    // Open file from list:
    OpenFileList openFileList=new OpenFileList(popupInfo, fontOptions);

    // File dialog + SSH:
    // This gets a special user notifier that schedules thread-safe alerts
    // because most of the work is done without the Swing event dispatch thread:
    UserNotify sshUN=new UserNotify(klog).setUI(alerter, true);
    SSHOptions sshOpts=persist.getSSHOptions();
    SSHConnections sshConns=new SSHConnections(sshUN)
      .withLogin(iUserPass)
      .withOptions(sshOpts);
    FileDialogWrapper fileDialogWrapper=new FileDialogWrapper(
      popupInfo,
      new SSHFileSystemView(sshConns, sshUN),
      new SSHFileView()
    );

    //Search popups:
    FindAndReplace findAndReplace=
      new FindAndReplace(popupInfo, fontOptions, persist.getFastUndos(), alerter, statusBar);
    GoToLine goToLine=new GoToLine(popupInfo, fontOptions, alerter);

    //Shell:
    Shell shell=new Shell(
      popupInfo, fontOptions, persist, fileDialogWrapper,
      getPopupIcon(this), ctrlMain.getCurrFileNameGetter()
    );

    //Various option popups:
    Favorites favorites=new Favorites(popupInfo, fontOptions);
    TabsAndIndents tabsAndIndents=new TabsAndIndents(popupInfo, fontOptions);
    FontPicker fontPicker=new FontPicker(popupInfo, alerter);
    SSHOptionPicker sshOptionPicker=new SSHOptionPicker(popupInfo, fontOptions, fileDialogWrapper);
    LineDelimiters kDelims=new LineDelimiters(popupInfo, fontOptions, s->statusBar.show(s));

    //Help:
    Help help=new Help(popupInfo, fontOptions, home.getUserHome());
    About about=new About(popupInfo, fontOptions);


    // MENUS: //

    Menus menus=new Menus(editors, currentOS, fontOptions);
    menus.setFastUndos(persist.getFastUndos())
          .setWordWrap(persist.getWordWrap())
          .setAutoTrim(persist.getAutoTrim());

    //Menu font options:
    popupInfo.addFontListener(menus.getFontListener());

    //Menu controller settings:
    {
      CtrlFavorites ctrlFavorites=new CtrlFavorites(
        persist, menus.getFavoriteFileListener(), menus.getFavoriteDirListener()
      );
      CtrlOptions ctrlOptions=new CtrlOptions  (
          editors, statusBar, persist, ctrlFavorites,
          ctrlMain.getLineDelimiterListener(), popupInfo.getFontListeners(),
          sshConns, sshOptionPicker, tabsAndIndents, favorites, fontPicker, kDelims
        );
      menus.setControllers(
        ctrlMain
        ,new CtrlMarks(editors, statusBar, menus.getMarkStateListener())
        ,new CtrlSelection(editors, statusBar, alerter)
        ,new CtrlUndo     (editors, popupInfo.getFastUndoListeners(), statusBar, yesNo, persist)
        ,new CtrlSearch(editors, statusBar, findAndReplace, goToLine)
        ,new CtrlFileOther(editors, statusBar, ctrlFavorites, currentOS)
        ,new CtrlOther    (shell, help, about)
        ,ctrlOptions
      );

      // Some necessary post-constructor DI here:
      Runnable
        small=ctrlOptions.getFontSmallerLambda(),
        big=ctrlOptions.getFontBiggerLambda();
      shell.setFontListeners(small, big);
      findAndReplace.setFontListeners(small, big);
    }

    //Attach osx popup items to main window, and assign menus to main window:
    menus.attachPopups(layout.getMainPanel());
    mainFrame.setJMenuBar(menus.getMenuBar());

    // PUSH THINGS BACK TO MAIN CONTROLLER: //
    // AND RETURN:                          //
    ctrlMain.setLayoutAndPopups(
      layout.getMainDisplay(),
      statusBar,
      fileDialogWrapper,
      new SSHOpenFrom(popupInfo, fontOptions),
      openFileList,
      new EncryptionInput(popupInfo, fontOptions, persist),
      yesNoCancel,
      yesNo
    );
    ctrlMain.setListeners(
      locker.getLockRemover(),
      sshConns.getFileResolver(),
      menus.getEditorSwitchListener(),
      menus.getRecentFileListener(),
      menus.getRecentDirListener(),
      menus.getExtraKeyListener()
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
  public static CurrentOS getCurrentOS() {
    return new CurrentOS();
  }
}
