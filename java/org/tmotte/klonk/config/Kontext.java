package org.tmotte.klonk.config;
import java.net.URL;
import javax.swing.ImageIcon;
import java.awt.Image;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyListener;
import java.awt.event.KeyEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JFrame;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.windows.StatusNotifier;
import org.tmotte.klonk.windows.popup.Popups;

public class Kontext {
  public JFrame mainFrame;
  public Fail fail;
  public Popups popups;
  public StatusNotifier status;
  public KHome home;
  public KPersist persist;
  public Image iconImage, iconImageFindReplace;
  public CurrFileGetter currFileGetter;
  
  public static Kontext getProduction(KHome home, Fail fail, StatusNotifier status, CurrFileGetter getter){
    return new Kontext(home, new JFrame("Klonk"), fail, status, getter);
  }
  public static Kontext getForUnitTest() {

    //Create frame that can die off on ESC or when unit test tells it
    //to die via JFrame.dispose()
    final JFrame k=new JFrame("Klonk");
    k.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent e){
        if (e.getKeyCode()==KeyEvent.VK_ESCAPE)
          System.exit(0);
      }
    });
    k.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        System.exit(0);
      }
    });
    k.setVisible(true);
    k.toFront();

    //Finally return context object with the frame:
    return new Kontext(
      new KHome("./test/home"),
      k,
      new Fail(){
        public void fail(Throwable t){
          t.printStackTrace();
        }
      },
      new StatusNotifier(){
        public void showStatus(String msg) {
          System.out.println("Status: "+msg);
        }
      },
      new CurrFileGetter(){
        public String getFile(){return "-none-";}
      }
    );
  }
  private Kontext(KHome home, JFrame mainFrame, Fail fail, StatusNotifier status, CurrFileGetter getter){
    try {
      javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    this.home=home;
    this.mainFrame=mainFrame;
    this.fail=fail;
    this.status=status;
    this.currFileGetter=getter;
    this.persist=new KPersist(home, fail);
    this.popups=new Popups(this);
    iconImage=getIcon("org/tmotte/klonk/windows/app.png");
    iconImageFindReplace=getIcon("org/tmotte/klonk/windows/app-find-replace.png");
  }
  private Image getIcon(String filename) {
    URL url=getClass().getClassLoader().getResource(filename);
    ImageIcon ii=new ImageIcon(url);
    return ii.getImage();
  }
}