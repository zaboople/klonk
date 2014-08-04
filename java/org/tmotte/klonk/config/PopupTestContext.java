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
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import org.tmotte.klonk.Menus;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.KLog;
import org.tmotte.klonk.windows.MainLayout;
import org.tmotte.klonk.windows.popup.Popups;

/** 
 * Overrides BootContext methods to create a lightweight environment where
 * we can test dialog windows.
 */
public class PopupTestContext extends BootContext {
  
  public PopupTestContext(String[] args){
    super(args);
  }
  
  public @Override CtrlMain getMainController() {
    return null;
  }
  public @Override KLog getLog() {
    if (log==null)
      log=new KLog(System.out);
    return log;
  }
  public @Override KHome getHome() {
    if (home==null) 
      //This could be improved by using a command line argument
      home=new KHome("./test/home");
    return home;
  }
  public @Override JFrame getMainFrame() {
    if (mainFrame==null) {
      mainFrame=new JFrame("Klonk - Test Main Frame");
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
    }
    return mainFrame;
  }
  public @Override StatusUpdate getStatusBar() {
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
  public @Override Popups getPopups() {
    if (popups==null)
      popups=new Popups(
        home
        ,getFail()
        ,getMainFrame()
        ,getPersist()
        ,getStatusBar()
        ,getPopupIcon() 
        ,getCurrFileNameGetter()
      );
    return popups;
  }
  public @Override Menus getMenus() {
    return null;
  }
  public @Override MainLayout getLayout() {
    return null;
  }
  

  //////////////////////////////////////////////////////////
  //Interface implementations. 
  //////////////////////////////////////////////////////////

  protected Getter<String> getCurrFileNameGetter() {
    return new Getter<String>(){
      public String get(){return "-none-";}
    };
  }
  
}