package org.tmotte.klonk.config;
import java.awt.Image;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.List;
import javax.swing.JFrame;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.Popups;

/** 
 * For testing popups without the overhead of the main application running.
 */
public class PopupTestContext  {

  //DI Components:
  KHome home;
  KLog log;
  KPersist persist;
  JFrame mainFrame;
  StatusUpdate statusBar;
  Setter<Throwable> fail;
  
  public PopupTestContext(String[] args){
  }
  
  public KLog getLog() {
    if (log==null)
      log=new KLog(System.out);
    return log;
  }
  public KPersist getPersist() {
    if (persist==null)
      persist=new KPersist(getHome(), getFail());
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
  public Setter<Throwable> getFail() {
    if (fail==null)
      fail=new Setter<Throwable>() {
        public void set(Throwable t){
          t.printStackTrace(System.err);
        }
      };
    return fail;
  }
  public StatusUpdate getStatusBar() {
    if (statusBar==null)
      statusBar=new StatusUpdate(){
        public void show(String msg)   {System.out.println(msg);}
        public void showBad(String msg){System.out.println("***"+msg+"***");}
        public void showCapsLock(boolean b)           {}
        public void showNoStatus()                    {}
        public void showRowColumn(int row, int column){}
        public void showChangeThis(boolean b){}
        public void showChangeAny(boolean b) {}
        public void showTitle(String title)  {}
      };
    return statusBar;
  }
  public Image getPopupIcon() {
    return BootContext.getPopupIcon(this);
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