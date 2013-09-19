package org.tmotte.klonk.config;
import java.awt.Image;
import java.util.List;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.KLog;
import org.tmotte.klonk.Klonk;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.windows.popup.Popups;

public class PopupContext {
  public KHome home;
  public Fail fail;
  public JFrame mainFrame;
  public Popups popups;
  public KPersist persist;
  public Image iconImageFindReplace;
  public Setter<String> statusBar;
  public Getter<String> currentFileGetter;

  public PopupContext(
      KHome home, KLog log, JFrame mainFrame, KPersist persist,
      Setter<String> statusBar, Getter<String> currFileGetter
    ){
    this.home=home;
    this.mainFrame=mainFrame;
    this.fail=log;
    this.persist=persist;
    this.statusBar=statusBar;
    iconImageFindReplace=Boot.getIcon("org/tmotte/klonk/windows/app-find-replace.png", getClass());
    popups=new Popups(
      mainFrame, home, fail, persist, statusBar, currFileGetter, iconImageFindReplace
    );
    log.setFailPopup(popups.getFailPopup());
  }
  public static PopupContext getForUnitTest() {
    return Boot.getForUnitTest();
  }
}