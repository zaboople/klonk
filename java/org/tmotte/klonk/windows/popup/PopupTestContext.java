package org.tmotte.klonk.windows.popup;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.JFrame;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.BootContext;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.Menus;

/**
 * For testing popups without the overhead of the main application running.
 */
public class PopupTestContext  {

  //DI Components:
  KHome home;
  KLog log;
  KPersist persist;
  JFrame mainFrame;
  Setter<Throwable> fail;
  PopupInfo popupInfo;
  FontOptions fontOptions;
  CurrentOS currentOS=new CurrentOS();



  public KPersist getPersist() {
    if (persist==null)
      persist=new KPersist(getHome(), getFail(), currentOS);
    return persist;
  }
  protected KHome getHome() {
    if (home==null)
      //This could be improved by using a command line argument
      home=new KHome("./test/home");
    return home;
  }
  public JFrame getMainFrame() {
    if (mainFrame==null)
      mainFrame=makeMainFrame();
    return mainFrame;
  }
  public PopupInfo getPopupInfo() {
    if (popupInfo==null)
      popupInfo=new PopupInfo(getMainFrame(), getCurrentOS());
    return popupInfo;
  }
  public FontOptions getFontOptions() {
    if (fontOptions==null)
      fontOptions=getPersist().getFontAndColors();
    return fontOptions;
  }
  public Setter<Throwable> getFail() {
    if (fail==null)
      fail=(Throwable t)->t.printStackTrace(System.err);
    return fail;
  }
  public Image getPopupIcon() {
    return BootContext.getPopupIcon(this);
  }
  public CurrentOS getCurrentOS() {
    return currentOS;
  }

  ////////////////
  // UTILITIES: //
  ////////////////

  public static JFrame makeMainFrame() {
    BootContext.initLookFeel();
    JFrame mainFrame=new JFrame("Klonk - Test Main Frame");
    KeyAdapter ka=new KeyAdapter() {
      public void keyPressed(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
          System.exit(0);
      }
    };
    mainFrame.addKeyListener(ka);
    mainFrame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        System.exit(0);
      }
    });
    mainFrame.setVisible(true);
    mainFrame.setBounds(new java.awt.Rectangle(400,400,300,300));
    mainFrame.toFront();
    return mainFrame;
  }

}