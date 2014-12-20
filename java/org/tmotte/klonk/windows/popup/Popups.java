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
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.LineDelimiterOptions;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.IUserPass;
import org.tmotte.klonk.windows.popup.ssh.SSHFiles;

/**
 * This is a sort of sublayer in our DI/IoC setup. Rather than creating 
 * all the possible UI components at boot, controllers receive an instance
 * of Popups and fetch lazy-initialized components from it. 
 * <br>
 * However: This isn't really that great. Most of these objects are already
 * internally amenable to lazy initialization with low overhead, since they don't
 * extend JFrame or similar ilk, but contain such instead. So I am regularly 
 * moving things backwards up to BootContext (which creates Popups) so that
 * we have one layer instead of two. 
 */
public class Popups {

  //Frequently used popup windows:
  private FindAndReplace findAndReplace;
  private GoToLine goToLinePicker;
  
  //Less frequently used:
  private Help help;
  private About about;
  private LineDelimiters kDelims;
  private TabsAndIndents tabsAndIndents;
  private FontPicker fontPicker;
  private SSHFiles sshFiles;  

  //DI resources for constructor:
  private final KHome home;
  private final Setter<String> alerter;
  private final JFrame mainFrame;
  private final KPersist persist;
  private final StatusUpdate statusBar;
  private final FileDialogWrapper fileDialogWrapper;

  //Other components. Well at least it's just this one:
  private FontOptions fontOptions; 
  
  ///////////////////////////////////////
  // INITIALIZATION AND CONFIGURATION: //
  ///////////////////////////////////////
  
  public Popups(
      KHome home, 
      JFrame mainFrame, 
      KPersist persist, 
      StatusUpdate statusBar, 
      Setter<String> alerter,
      FileDialogWrapper fileDialogWrapper
    ) {
    this.home             =home;
    this.mainFrame        =mainFrame;
    this.statusBar        =statusBar;
    this.persist          =persist;
    this.alerter          =alerter;
    this.fileDialogWrapper=fileDialogWrapper;
  }

  public void setFontAndColors(FontOptions fo) { //FIXME change this to a Setter<FontOptions>
    this.fontOptions=fo;
    if (help!=null)
      help.setFont(fontOptions);
    if (findAndReplace!=null)
      findAndReplace.setFont(fontOptions);
  }
  public JFrame getMainFrame() {
    return mainFrame;
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
  

  public boolean showTabAndIndentOptions(TabAndIndentOptions options) {
    return getTabsAndIndents().show(options);
  }
  public boolean doFontAndColors(FontOptions input) {
    return getFontPicker().show(input);
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
      statusBar.showBad("Line number "+i+" is out of range"); 
  }
  public void showLineDelimiters(LineDelimiterOptions k, LineDelimiterListener k2){
    getLineDelimiters().show(k, k2);
  }
  
  public boolean showSSHOptions(SSHOptions ssho) {
    return getSSHFiles().show(ssho);
  }


  public Setter<String> getAlerter() {
    return alerter;
  }

  
  ///////////////////////////////////////////
  // PRIVATE getX() MORE FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
  
  private GoToLine getGoToLine() {
    if (goToLinePicker==null)
      goToLinePicker=new GoToLine(mainFrame, getAlerter());
    return goToLinePicker;
  }
  private FindAndReplace getFindAndReplace() {
    if (findAndReplace==null){
      findAndReplace=new FindAndReplace(
        mainFrame, getAlerter(), statusBar
      );
      findAndReplace.setFont(getFontOptions());
    }
    return findAndReplace;
  }
  private FileDialogWrapper getFileDialog() {
    return fileDialogWrapper;
  }
  private FontOptions getFontOptions() {
    if (fontOptions==null) 
      fontOptions=persist.getFontAndColors();
    return fontOptions;
  }
  
  ///////////////////////////////////////////
  // PRIVATE getX() LESS FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
  private Help getHelp() {
    if (help==null){
      help=new Help(mainFrame, home.getUserHome());
      help.setFont(fontOptions);
    }
    return help;
  }
  private About getAbout() {
    if (about==null)
      about=new About(mainFrame);
    return about;
  }
  private FontPicker getFontPicker() {
    if (fontPicker==null)
      fontPicker=new FontPicker(mainFrame, getAlerter());
    return fontPicker;
  }
  private SSHFiles getSSHFiles() {
    if (sshFiles==null)
      sshFiles=new SSHFiles(getMainFrame(), getFileDialog());
    return sshFiles;
  }
  private LineDelimiters getLineDelimiters() {
    if (kDelims==null)
      kDelims=new LineDelimiters(mainFrame);
    return kDelims;
  }
  private TabsAndIndents getTabsAndIndents() {
    if (tabsAndIndents==null) 
      tabsAndIndents=new TabsAndIndents(mainFrame);
    return tabsAndIndents;
  }
}
