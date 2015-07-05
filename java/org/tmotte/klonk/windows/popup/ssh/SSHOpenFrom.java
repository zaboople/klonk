package org.tmotte.klonk.windows.popup.ssh;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.tmotte.klonk.windows.Positioner;

public class SSHOpenFrom {

  private JFrame parentFrame;
  private JDialog win;  

  private JComboBox<String> jcbPrevious;
  private DefaultComboBoxModel<String> jcbPreviousData;
  private JTextField jtfRow;
  private JButton btnOK, btnCancel;
  private boolean shownBefore=false;
  private boolean initialized=false;

  public void show() {
    init();
    if (!jcbPrevious.hasFocus())
      jcbPrevious.requestFocusInWindow();
    Positioner.set(parentFrame, win, shownBefore || (win.getBounds().x>-1 && win.getBounds().y>-1));
    shownBefore=true;
    win.setVisible(true);
    win.toFront();
  }


  private void init() {
    if (!initialized) {
      create();
      layout(); 
      listen();    
      initialized=true;
    }
  }
  private void create(){
    jcbPreviousData=new DefaultComboBoxModel<>();
  
  }
  private void layout(){
  }
  private void listen(){
  }
  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }  

}

