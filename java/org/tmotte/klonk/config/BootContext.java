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
public class BootContext {
  KHome home;
  Fail fail;
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
  
  protected BootContext(){
  }
  public BootContext(KHome home, Fail fail, FileListen listen) {
    this.home=home;
    this.fail=fail;
    this.fileListen=listen;
  }
  public KPersist getPersist() {
    if (persist==null)
      persist=new KPersist(home, getFail());
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
      mainFrame.setIconImage(Boot.getAppIcon());
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
        home, getFail()
        ,getMainFrame()
        ,getPersist()
        ,getStatusBar()
        ,Boot.getPopupIcon() 
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
    return fileListen;
  }
  

  //////////////////////////////////////////////////////////
  //Interface implementations. 
  //These do not need to be tested, but possibly mocked.
  //All but getFail() will only be invoked once if at all.
  //////////////////////////////////////////////////////////

  protected Fail getFail() {
    return fail;
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
  protected JMenuBar getMenuBar() {
    return getMenus().getMenuBar()  ;
  }

}