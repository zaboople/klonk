package org.tmotte.klonk.windows.popup.ssh;
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
import javax.swing.JCheckBox;
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
import org.tmotte.klonk.windows.popup.FileDialogWrapper;
import org.tmotte.klonk.windows.popup.PopupTestContext;
import org.tmotte.klonk.config.option.SSHOptions;


/**
 * FIXME Kill connection?
 */
public class SSHOptionPicker {

  private JFrame parentFrame;
  private FileDialogWrapper fdw;
  
  private JCheckBox 
    jcbKnownHosts, 
    jcbPrivateKeys;
  private JTextField 
    jtfKnownHosts, 
    jtfPrivateKeys;  
  private JButton 
    btnKnownHosts, 
    btnPrivateKeys;

  private JDialog win;
  private JButton btnOK, btnCancel;
  private boolean initialized;

  private boolean result=false;

  public SSHOptionPicker(JFrame parentFrame, FileDialogWrapper fdw) {
    this.parentFrame=parentFrame;
    this.fdw=fdw;
  }  
  public boolean show(SSHOptions options) {
    init();
    result=false;
    {
      String knownHosts=ifEmpty(options.getKnownHostsFilename());
      if (knownHosts!=null)
        jtfKnownHosts.setText(knownHosts);
      jcbKnownHosts.setSelected( knownHosts !=null);
      String privateKeys=ifEmpty(options.getPrivateKeysFilename());
      if (privateKeys!=null)
        jtfPrivateKeys.setText(privateKeys);
      jcbPrivateKeys.setSelected( privateKeys !=null);
    }
    setVisible();
    doShow();
    if (result) {
      options.setKnownHostsFilename(
        jcbKnownHosts.isSelected()
          ?ifEmpty(jtfKnownHosts)
          :""
      );
      options.setPrivateKeysFilename(
        jcbPrivateKeys.isSelected()
          ?ifEmpty(jtfPrivateKeys)
          :""
      );    
    }
    return result;
  }
  /** For looping */
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
  
  /////////////
  // CREATE: //
  /////////////
  
  private void init() {
    if (!initialized) {
      create();
      layout(); 
      listen();    
      initialized=true;
    }
  }
  private void create(){
    win=new JDialog(parentFrame, true);
    win.setTitle("SSH Configuration");

    jcbKnownHosts=new JCheckBox("Known hosts");
    jtfKnownHosts=new JTextField();
    jtfKnownHosts.setColumns(45);
    btnKnownHosts=new JButton("...");

    jcbPrivateKeys=new JCheckBox("Private keys");
    jtfPrivateKeys=new JTextField();  
    jtfPrivateKeys.setColumns(45);
    btnPrivateKeys=new JButton("...");

    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.HORIZONTAL;
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

    gb.add(jcbKnownHosts);
    
    gb.weightx=1;
    gb.fill=gb.HORIZONTAL;    
    gb.addX(jtfKnownHosts);    
    gb.fill=gb.NONE;
    gb.weightx=0;
    
    gb.addX(btnKnownHosts);

    gb.setX(0);
    gb.addY(jcbPrivateKeys);
    
    gb.weightx=1;
    gb.fill=gb.HORIZONTAL;
    gb.addX(jtfPrivateKeys);
    gb.fill=gb.NONE;
    gb.weightx=0;

    gb.addX(btnPrivateKeys);
    
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
  
  /////////////
  // LISTEN: //
  /////////////

  private void listen() {
    listenOKCancel();

    //Checkboxes enable text/file browsing:
    Action jcbAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {setVisible();}
    };
    jcbKnownHosts.addActionListener(jcbAction);
    jcbPrivateKeys.addActionListener(jcbAction);
    
    //GetFiles
    listenFileSelector(jtfKnownHosts, btnKnownHosts);
    listenFileSelector(jtfPrivateKeys, btnPrivateKeys);    
  }
  private void listenFileSelector(final JTextField jtf, JButton btn) {
    Action act=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        String s=showFileDialog(jtf);
        if (s!=null)
          jtf.setText(s);
      }
    };
    btn.addActionListener(act);
  }
  private void listenOKCancel() {
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
    result=action;
    win.setVisible(false);  
  }
  private void setVisible() {
    boolean kh=jcbKnownHosts.isSelected(),
            pk=jcbPrivateKeys.isSelected();
    jtfKnownHosts.setEnabled(kh);
    btnKnownHosts.setEnabled(kh);
    jtfPrivateKeys.setEnabled(pk);
    btnPrivateKeys.setEnabled(pk);
  }
  
  ////////////////
  // UTILITIES: //
  ////////////////
  
  private String ifEmpty(JTextField jtf) {
    return ifEmpty(jtf.getText());
  }
  private String ifEmpty(String s) {
    return s==null || s.trim()==""
      ?null
      :s.trim();
  }

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame parentFrame=PopupTestContext.makeMainFrame();
          SSHOptionPicker pop=new SSHOptionPicker(parentFrame, new FileDialogWrapper(parentFrame));
          SSHOptions sopt=new SSHOptions();
          //FIXME put default values in
          if (pop.show(sopt))
            System.out.println(sopt);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }  
  
}
