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

  private About about;
  private LineDelimiters kDelims;
  private FontPicker fontPicker;

  //DI resources for constructor:
  private final Setter<String> alerter;
  private final JFrame mainFrame;
  
  ///////////////////////////////////////
  // INITIALIZATION AND CONFIGURATION: //
  ///////////////////////////////////////
  
  public Popups(
      JFrame mainFrame, 
      Setter<String> alerter
    ) {
    this.mainFrame        =mainFrame;
    this.alerter          =alerter;
  }

  public JFrame getMainFrame() {
    return mainFrame;
  }

  ///////////////////////////
  // PUBLIC POPUP METHODS: //
  ///////////////////////////

  public void showHelpAbout() {
    getAbout().show();
  }
  

  public boolean doFontAndColors(FontOptions input) {
    return getFontPicker().show(input);
  }
  
  public void showLineDelimiters(LineDelimiterOptions k, LineDelimiterListener k2){
    getLineDelimiters().show(k, k2);
  }
  
  public Setter<String> getAlerter() {
    return alerter;
  }

   
  ///////////////////////////////////////////
  // PRIVATE getX() LESS FREQUENTLY USED:  //
  ///////////////////////////////////////////
  
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
  private LineDelimiters getLineDelimiters() {
    if (kDelims==null)
      kDelims=new LineDelimiters(mainFrame);
    return kDelims;
  }
}
