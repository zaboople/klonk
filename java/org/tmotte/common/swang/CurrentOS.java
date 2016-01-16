package org.tmotte.common.swang;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;

public class CurrentOS {
  public final boolean isOSX;
  public final boolean isMSWindows;
  public CurrentOS(){
    String os=System.getProperty("os.name").toLowerCase();
    isMSWindows=os!=null && os.indexOf("windows")>-1;
    isOSX=os.indexOf("mac os")>-1;
  }
  public CurrentOS(boolean isOSX, boolean isMSWindows) {
    this.isOSX=isOSX;
    this.isMSWindows=isMSWindows;
  }
  public void fixEnterKey(JButton jb, Action action) {
    if (isOSX)
      jb.addKeyListener(
        new KeyAdapter() {
          public void keyPressed(KeyEvent e){
            if (e.getKeyCode()==e.VK_ENTER) {
              e.consume();
              action.actionPerformed(new ActionEvent(jb, 1, "faked"));
            }
          }
        }
      );
  }
}