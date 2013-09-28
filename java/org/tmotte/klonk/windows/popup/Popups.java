package org.tmotte.klonk.windows.popup;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.Collection;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.config.PopupContext;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.PopupContext;
import org.tmotte.klonk.config.LineDelimiterOptions;
import org.tmotte.klonk.config.TabAndIndentOptions;
import org.tmotte.klonk.edit.MyTextArea;


/**
 * This is a sort of sublayer in our DI/IoC setup. Boot creates PopupContext creates Popups. Among other things, 
 * Popups is separated because it does lazy initialization.
 */
public class Popups {

  //Frequently used popup windows:
  private KAlert kAlert;
  private YesNoCancel yesNoCancel;
  private YesNoCancel yesNo;
  private FindAndReplace findAndReplace;
  private FileDialogWrapper fileDialogWrapper;
  private GoToLine goToLinePicker;
  private Shell shell;
  
  //Less frequently used:
  private Help help;
  private About about;
  private LineDelimiters kDelims;
  private TabsAndIndents tabsAndIndents;
  private FontPicker fontPicker;
  private Favorites favorites;

  //DI resources:
  private JFrame mainFrame;
  private KHome home;
  private Fail fail;
  private KPersist persist;
  private StatusUpdate statusBar;
  private Getter<String> currFileGetter;
  private Image iconImageFindReplace;    

  //Other components. Well at least it's just this one:
  private FontOptions fontOptions; 
  
  ///////////////////////////////////////
  // INITIALIZATION AND CONFIGURATION: //
  ///////////////////////////////////////
  
  public Popups(PopupContext context, Getter<String> currFileGetter) {
    this.mainFrame=context.mainFrame;
    this.statusBar=context.statusBar;
    this.fail=context.fail;
    this.persist=context.persist;
    this.home=context.home;
    this.iconImageFindReplace=context.iconImageFindReplace;
    this.currFileGetter=currFileGetter;
  }

  public void setFontAndColors(FontOptions fo) {
    this.fontOptions=fo;
    if (help!=null)
      help.setFont(fontOptions);
    if (findAndReplace!=null)
      findAndReplace.setFont(fontOptions);
    if (favorites!=null)
      favorites.setFont(fontOptions);
    if (shell!=null)
      shell.setFont(fontOptions);
  }

  ///////////////////////////
  // PUBLIC POPUP METHODS: //
  ///////////////////////////

  public void showHelp() {
    getHelp().show();
  }
  public void showHelpAbout() {
    getAbout().show();
  }
  
  public Fail getFailPopup() {
    return getAlerter();
  }
  public void alert(String message) {
    getAlerter().show(message);
  }
  public void fail(Throwable message) {
    getAlerter().fail(message);
  }

  public YesNoCancelAnswer askYesNoCancel(String message) {
    if (yesNoCancel==null)
      yesNoCancel=new YesNoCancel(mainFrame, true);
    return yesNoCancel.show(message);
  }
  public boolean askYesNo(String message) {
    if (yesNo==null)
      yesNo=new YesNoCancel(mainFrame, false);
    return yesNo.show(message).isYes();
  }

  public boolean showTabAndIndentOptions(TabAndIndentOptions options) {
    return getTabsAndIndents().show(options);
  }
  public boolean doFontAndColors(FontOptions fontOptions) {
    return getFontPicker().show(fontOptions);
  }
  
  public boolean showFavorites(Collection<String> favoriteFiles, Collection<String> favoriteDirs) {
    return getFavorites().show(favoriteFiles, favoriteDirs);
  }

  public void doFind(MyTextArea target) {
    getFindAndReplace().doFind(target, false);
  }
  public void doReplace(MyTextArea target) {
    getFindAndReplace().doFind(target, true);
  }
  public void repeatFindReplace(MyTextArea target, boolean forwards) {
    getFindAndReplace().repeatFindReplace(target, forwards);
  }
  public void goToLine(MyTextArea target) {
    GoToLine gtl=getGoToLine();
    int i=gtl.show();
    if (i==-1)
      statusBar.showBad("Go to line cancelled.");
    else
    if (!target.goToLine(i-1))
      statusBar.showBad("Line number "+i+" is out of range"); //FIXME needs StatusUpdater here to red out the message
  }
  public void showLineDelimiters(LineDelimiterOptions k, LineDelimiterListener k2){
    getLineDelimiters().show(k, k2);
  }
  public void showShell() {
    getShell().show();
  }
  
  public File showFileDialog(boolean forSave) {
    return showFileDialog(forSave, null);
  }
  public File showFileDialog(boolean forSave, File startFile) {
    return getFileDialog().show(forSave, startFile, null);
  }
  public File showFileDialogForDir(boolean forSave, File startDir) {
    return getFileDialog().show(forSave, null, startDir);
  }


  
  ///////////////////////////////////////////
  // PRIVATE getX() MORE FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
  
  private GoToLine getGoToLine() {
    if (goToLinePicker==null)
      goToLinePicker=new GoToLine(mainFrame, fail, this);
    return goToLinePicker;
  }
  private KAlert getAlerter() {
    if (kAlert==null)
      kAlert=new KAlert(mainFrame);
    return kAlert;
  }
  private FindAndReplace getFindAndReplace() {
    if (findAndReplace==null){
      findAndReplace=new FindAndReplace(
        mainFrame, fail, 
        new Setter<String>(){public void set(String s) {alert(s);}}, 
        statusBar
      );
      findAndReplace.setFont(fontOptions);
    }
    return findAndReplace;
  }
  private Shell getShell() {
    if (shell==null) {
      shell=new Shell(
        mainFrame, fail, this, 
        iconImageFindReplace, persist, currFileGetter
      );
      shell.setFont(fontOptions);
    }
    return shell;
  }
  private FileDialogWrapper getFileDialog() {
    if (fileDialogWrapper==null)
      fileDialogWrapper=new FileDialogWrapper(mainFrame);  
    return fileDialogWrapper;
  }
  
  ///////////////////////////////////////////
  // PRIVATE getX() LESS FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
  private KAlert getKAlert() {
    if (kAlert==null)
      kAlert=new KAlert(mainFrame);
    return kAlert;
  }
  private Help getHelp() {
    if (help==null){
      help=new Help(mainFrame, fail, home.getUserHome());
      help.setFont(fontOptions);
    }
    return help;
  }
  private About getAbout() {
    if (about==null)
      about=new About(mainFrame, fail);
    return about;
  }
  private FontPicker getFontPicker() {
    if (fontPicker==null)
      fontPicker=new FontPicker(mainFrame, fail, this);
    return fontPicker;
  }
  private Favorites getFavorites() {
    if (favorites==null){
      favorites=new Favorites(mainFrame, fail, this);
      favorites.setFont(fontOptions);
    }
    return favorites;
  }
  private LineDelimiters getLineDelimiters() {
    if (kDelims==null)
      kDelims=new LineDelimiters(
        mainFrame, fail
      );
    return kDelims;
  }
  private TabsAndIndents getTabsAndIndents() {
    if (tabsAndIndents==null) 
      tabsAndIndents=new TabsAndIndents(mainFrame, fail);
    return tabsAndIndents;
  }
}