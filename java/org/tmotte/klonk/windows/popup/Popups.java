package org.tmotte.klonk.windows.popup;
import java.util.List;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.config.Kontext;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.LineDelimiterOptions;
import org.tmotte.klonk.config.TabAndIndentOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.StatusNotifier;

public class Popups {

  //Frequently used popup windows:
  private KAlert kAlert;
  private YesNoCancel yesNoCancel;
  private YesNoCancel yesNo;
  private FindAndReplace findAndReplace;
  private FileDialog fileDialog;
  private JFileChooser fileChooser;
  private GoToLine goToLinePicker;
  private Shell shell;
  
  //Less frequently used:
  private Help help;
  private About about;
  private LineDelimiters kDelims;
  private TabsAndIndents tabsAndIndents;
  private FontPicker fontPicker;
  private Favorites favorites;

  //Other components:
  private Kontext context;
  private FontOptions fontOptions;


  ///////////////////////////////////////
  // INITIALIZATION AND CONFIGURATION: //
  ///////////////////////////////////////
  
  public Popups(Kontext context) {
    this.context=context;
  }

  public void setFontAndColors(FontOptions fo) {
    this.fontOptions=fo;
    if (help!=null)
      help.setFont(fontOptions);
    if (findAndReplace!=null)
      findAndReplace.setFont(fontOptions);
    if (favorites!=null)
      favorites.setFont(fontOptions);
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
      yesNoCancel=new YesNoCancel(context.mainFrame, true);
    return yesNoCancel.show(message);
  }
  public boolean askYesNo(String message) {
    if (yesNo==null)
      yesNo=new YesNoCancel(context.mainFrame, false);
    return yesNo.show(message).isYes();
  }

  public boolean showTabAndIndentOptions(TabAndIndentOptions options) {
    return getTabsAndIndents().show(options);
  }
  public boolean doFontAndColors(FontOptions fontOptions) {
    return getFontPicker().show(fontOptions);
  }
  
  public boolean showFavorites(List<String> favoriteFiles, List<String> favoriteDirs) {
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
      context.status.showStatus("Go to line cancelled.");
    else
    if (!target.goToLine(i-1))
      context.status.showStatus("Line number "+i+" is out of range");
  }
  public void showLineDelimiters(LineDelimiterOptions k, LineDelimiterListener k2){
    getLineDelimiters().show(k, k2);
  }
  public void showShell() {
    getShell().show();
  }
  
  // FILE DIALOGS: //
  
  public File showFileDialog(boolean forSave) {
    return showFileDialog(forSave, null);
  }
  public File showFileDialog(boolean forSave, File startFile) {
    return showFileDialog(forSave, startFile, null);
  }
  public File showFileDialogForDir(boolean forSave, File startDir) {
    return showFileDialog(forSave, null, startDir);
  }

  //////////////////////////////////////
  // SEMI-PRIVATE POSITIONING TRICKS: //
  //////////////////////////////////////

  static void position(Window parent, Window popup) {
    position(parent, popup, false);
  }
  static void position(Window parent, Window popup, boolean unless) {
    if (!unless) {
      Rectangle pt2=parent.getBounds();
      popup.setLocation(pt2.x+pt2.width-popup.getWidth(), pt2.y+20);
    }
    Dimension dim=Toolkit.getDefaultToolkit().getScreenSize();    
    Point p=popup.getLocation();
    boolean badX=p.x<0 || p.x>dim.width,
            badY=p.y<0 || p.y>dim.height;
    
    if (badX||badY){
      if (badX) p.x=0;
      if (badY) p.y=0;
      popup.setLocation(p);
    }
  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////


  private File showFileDialog(boolean forSave, File startFile, File startDir) {
    if (true) {
      //File chooser sucks but not as bad as it used to when in native mode:
      if (fileChooser==null)
        fileChooser=new JFileChooser();
      if (startFile!=null){
        if (startFile.isDirectory()){
          startDir=startFile;
        }
        else {
          fileChooser.setSelectedFile(startFile);
          startDir=startFile.getParentFile();
        }
      }
      if (startDir!=null)
        fileChooser.setCurrentDirectory(startDir);
      int returnVal=forSave
        ?fileChooser.showSaveDialog(context.mainFrame)
        :fileChooser.showOpenDialog(context.mainFrame);
      if (returnVal==fileChooser.APPROVE_OPTION)
        return fileChooser.getSelectedFile();
      else
        return null;
    }
    else
      try {
        //Unused; Broken on MS Windows XP. If you do "save as" and the old file
        //name is longer than the new one, the last characters from the old
        //file get appended to the new one. I tried everything and it was
        //unfixable.
        if (fileDialog==null)
          fileDialog=new FileDialog(context.mainFrame);
        FileDialog fd=fileDialog;
        if (startFile!=null)
          fd.setFile(startFile.getCanonicalPath().trim());
        if (startDir!=null) 
          fd.setDirectory(startDir.getCanonicalPath());
        fd.setTitle(forSave ?"Save" :"Open");
        fd.setMode(forSave ?fd.SAVE :fd.LOAD);
        fd.setVisible(true);
        
        File[] f=fd.getFiles();
        if (f==null || f.length==0)
          return null;
        return f[0];
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
  }
  
  ///////////////////////////////////////////
  // PRIVATE getX() MORE FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
  
  private GoToLine getGoToLine() {
    if (goToLinePicker==null)
      goToLinePicker=new GoToLine(context.mainFrame, context.fail, this);
    return goToLinePicker;
  }
  private KAlert getAlerter() {
    if (kAlert==null)
      kAlert=new KAlert(context.mainFrame);
    return kAlert;
  }
  private FindAndReplace getFindAndReplace() {
    if (findAndReplace==null){
      findAndReplace=new FindAndReplace(
        context.mainFrame, context.fail, getAlerter(), context.status
      );
      findAndReplace.setFont(fontOptions);
    }
    return findAndReplace;
  }
  private Shell getShell() {
    if (shell==null) {
      shell=new Shell(
        context.mainFrame, context.fail, context.popups, 
        context.iconImageFindReplace, context.persist, context.currFileGetter
      );
      shell.setFont(fontOptions);
    }
    return shell;
  }
  
  ///////////////////////////////////////////
  // PRIVATE getX() LESS FREQUENTLY USED:  //
  ///////////////////////////////////////////

  private Help getHelp() {
    if (help==null){
      help=new Help(context.mainFrame, context.fail, context.home.getUserHome());
      help.setFont(fontOptions);
    }
    return help;
  }
  private About getAbout() {
    if (about==null)
      about=new About(context.mainFrame, context.fail);
    return about;
  }
  private FontPicker getFontPicker() {
    if (fontPicker==null)
      fontPicker=new FontPicker(context.mainFrame, context.fail, this);
    return fontPicker;
  }
  private Favorites getFavorites() {
    if (favorites==null){
      favorites=new Favorites(context.mainFrame, context.fail, this);
      favorites.setFont(fontOptions);
    }
    return favorites;
  }
  private LineDelimiters getLineDelimiters() {
    if (kDelims==null)
      kDelims=new LineDelimiters(
        context.mainFrame, context.fail
      );
    return kDelims;
  }
  private TabsAndIndents getTabsAndIndents() {
    if (tabsAndIndents==null) 
      tabsAndIndents=new TabsAndIndents(context.mainFrame, context.fail);
    return tabsAndIndents;
  }
}