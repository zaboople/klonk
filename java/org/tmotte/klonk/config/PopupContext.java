package org.tmotte.klonk.config;
import java.awt.Image;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.windows.popup.Popups;

public class PopupContext {
  public KHome home;
  public Fail fail;
  public JFrame mainFrame;
  public Popups popups;
  public KPersist persist;
  public Image iconImageFindReplace;
  public StatusUpdate statusBar;


  public PopupContext(
      KHome home, KLog log, JFrame mainFrame, KPersist persist, StatusUpdate statusBar, 
      Getter<String> currFileGetter
    ){
    this.home=home;
    this.mainFrame=mainFrame;
    this.fail=log;
    this.persist=persist;
    this.statusBar=statusBar;
    this.iconImageFindReplace=Boot.getIcon("org/tmotte/klonk/windows/app-find-replace.png", getClass());
    this.popups=new Popups(this, currFileGetter);
    
    popups.setFontAndColors(persist.getFontAndColors());
    log.setFailPopup(popups.getFailPopup());
  }
  
  //Legacy:
  public static PopupContext getForUnitTest() {
    return Boot.getForUnitTest();
  }
}