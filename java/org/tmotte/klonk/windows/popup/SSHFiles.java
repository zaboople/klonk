package org.tmotte.klonk.windows.popup;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.windows.Positioner;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.config.option.SSHOptions;


/**
 * Needs:
 * - Pick auth file 
 * - Pick host keys file
 * - Kill connection?
 */
public class SSHFiles {

  private JFrame parentFrame;
  private FileDialogWrapper fdw;
  
  private JTextField jtfKnownHosts=new JTextField();
  private JTextField jtfPrivateKeys=new JTextField();  
  private JButton btnKnownHosts, btnPrivateKeys;
  
  private JDialog win;
  private JButton btnOK, btnCancel;

  private boolean result=false;

  public SSHFiles(
     JFrame parentFrame, 
     FileDialogWrapper fdw
    ) {
    this.parentFrame=parentFrame;
    this.fdw=fdw;
    create();
    layout(); 
    listen();
  }  
  public boolean show(SSHOptions options) {
    result=false;
    doShow();
    return result;
  }
  private void doShow() {
    Positioner.set(parentFrame, win);
    win.setVisible(true);
    win.toFront();
  }


  private String showFileDialog(JTextField jtf) {
    String old=jtf.getText();
    File file=new File(old);
    file=fdw.show(false, file, null);
    if (file==null)
      return null;
    else
      try {
        return file.getCanonicalPath();
      } catch (Exception e) {
        throw new RuntimeException("Could not obtain file: "+file);
      }
  }
  
  private void create(){
    win=new JDialog(parentFrame, true);
    win.setResizable(false);
    win.setTitle("SSH Configuration");
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
    btnKnownHosts=new JButton("...");
    btnPrivateKeys=new JButton("...");
    jtfKnownHosts.setColumns(45);
    jtfPrivateKeys.setColumns(45);
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.NONE;
    gb.anchor=gb.NORTHWEST;
    gb.add(getInputPanel());
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(1);
    gb.addY(getButtons());
    win.pack();
  }
  private JPanel getInputPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    gb.insets.top=2;
    gb.insets.bottom=2;
    gb.insets.left=5;

    {
      gb.add(new JLabel("Known hosts"));
      gb.addX(jtfKnownHosts);
      gb.addX(btnKnownHosts);
    }
    {
      gb.setX(0);
      gb.addY(new JLabel("Private key(s)"));
      gb.addX(jtfPrivateKeys);
      gb.addX(btnPrivateKeys);
    }
    
    return jp;
  }
  private JPanel getButtons() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.gridx=0;
    gb.add(btnOK);
    gb.addX(btnCancel);
    return panel;
  }
  

  private void listen() {
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));
    
    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
  }
  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    result=false;
    win.setVisible(false);  
    if (action){
      result=true;
    }
  }
  

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          PopupTestContext ptc=new PopupTestContext(args);
          JFrame parentFrame=ptc.getMainFrame();
          SSHFiles pop=new SSHFiles(parentFrame, new FileDialogWrapper(parentFrame));
          SSHOptions sopt=new SSHOptions();
          //FIXME put default values in
          pop.show(sopt);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }  
  
}
