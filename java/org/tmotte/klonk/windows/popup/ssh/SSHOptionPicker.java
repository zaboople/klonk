package org.tmotte.klonk.windows.popup.ssh;
import java.awt.Color;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.List;
import java.util.ArrayList;
import java.util.LinkedList;
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

  private JDialog win;  
  private JCheckBox 
    jcbKnownHosts, 
    jcbPrivateKeys,
    jcbSelectAllConns;
  private JTextField 
    jtfKnownHosts, 
    jtfPrivateKeys;  
  private JButton 
    btnKnownHosts, 
    btnPrivateKeys;
  private JPanel jpConns;
  private GridBug gbConns;
  private JLabel lblOpenConns;
  private List<JCheckBox> listCBConns;
  private static class TripleCheck {
    JCheckBox read=new JCheckBox(" "), write=new JCheckBox(" "), execute=new JCheckBox(" ");
  }
  private TripleCheck tcUser, tcGroup, tcOther;

  private JButton btnOK, btnCancel;

  private boolean initialized;
  private boolean result=false;

  public SSHOptionPicker(JFrame parentFrame, FileDialogWrapper fdw) {
    this.parentFrame=parentFrame;
    this.fdw=fdw;
  }  
  public boolean show(SSHOptions options, final List<String> servers) {
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
      
      boolean kk=servers.size()>0;
      jcbSelectAllConns.setEnabled(kk);
      if (kk) 
        jcbSelectAllConns.setVisible(servers.size()>1);
      lblOpenConns.setForeground(kk ?Color.BLACK :Color.GRAY);
      jpConns.removeAll();
      listCBConns.clear();
      gbConns.setY(0);
      for (String s: servers){
        JCheckBox j=new JCheckBox(s);
        gbConns.addY(j);
        listCBConns.add(j);
      }
      win.pack();
    }
    setVisible();
    doShow();
    if (result) {
      servers.clear();
      for (JCheckBox jcb: listCBConns) 
        if (jcb.isSelected())
          servers.add(jcb.getText());
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
    
    jcbSelectAllConns=new JCheckBox("Select all");
    lblOpenConns=new JLabel("<html><b>Kill open connections:</b></html>");
    jpConns=new JPanel();
    gbConns=new GridBug(jpConns);
    listCBConns=new LinkedList<JCheckBox>();
    
    tcUser=new TripleCheck();
    tcGroup=new TripleCheck();
    tcOther=new TripleCheck();
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.HORIZONTAL;
    gb.anchor=gb.NORTHWEST;
    gb.add(getKnownPrivatePanel());
    
    gb.insets.top=20;
    gb.insets.left=8;
    gb.insets.right=8;
    gb.insets.bottom=4;
    {
      JSeparator j=new JSeparator(JSeparator.HORIZONTAL);
      j.setForeground(new Color(200,200,200));
      gb.addY(j);
    }
    gb.addY(getAccessPanel());
    {
      JSeparator j=new JSeparator(JSeparator.HORIZONTAL);
      j.setForeground(new Color(200,200,200));
      gb.addY(j);
    }    
    gb.insets.top=2;
    gb.insets.left=4;
    gb.insets.right=4;
    gb.insets.bottom=0;

    gb.addY(getKillConnsPanel());
    gb.weightXY(1);
    gb.addY(getButtons());
    win.pack();
  }
  private JPanel getKnownPrivatePanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    gb.insets.top=8;
    gb.insets.bottom=2;

    gb.gridwidth=2;
    gb.insets.left=6;
    gb.add(new JLabel("<html><b>Files:</b></html>"));
    gb.gridwidth=1;
    gb.insets.left=2;

    //Known hosts checkbox+textfield+button:
    gb.insets.left+=2;
    gb.insets.top=4;
    gb.addY(jcbKnownHosts);
    
    gb.weightx=1;
    gb.fill=gb.BOTH;    
    gb.addX(jtfKnownHosts);    
    gb.fill=gb.NONE;
    gb.weightx=0;
    
    gb.insets.right=4;
    gb.addX(btnKnownHosts);
    gb.insets.right=0;

    //Private keys checkbox+textfield+button:
    gb.setX(0);
    gb.insets.top=4;   
    gb.addY(jcbPrivateKeys);
    
    gb.weightx=1;
    gb.fill=gb.BOTH;    
    gb.addX(jtfPrivateKeys);
    gb.fill=gb.NONE;
    gb.weightx=0;

    gb.insets.right=4;
    gb.addX(btnPrivateKeys);
    
    return jp;
  }

  private JPanel getAccessPanel() {
    JPanel jp=new JPanel();
    
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;
    gb.insets.top=4;
    gb.insets.bottom=2;
    gb.insets.left=2;
    gb.weightx=1;
    gb.gridwidth=4;
    gb.add(new JLabel("<html><body><b>Default access for new files</b></body></html>"));    
    gb.gridwidth=1;

    gb.setX(0);  
    gb.weightx=0;
    gb.insets.left=20;
    gb.addY(new JLabel(""));    
    gb.addX(new JLabel("Read   "));
    gb.addX(new JLabel("Write   "));
    gb.weightx=1;
    gb.addX(new JLabel("Execute"));
    gb.setX(0);
    
    gb.weightx=0;
    gb.addY(new JLabel("User"));
    gb.addX(tcUser.read);
    gb.addX(tcUser.write);
    gb.weightx=1;
    gb.addX(tcUser.execute);

    gb.weightx=0;
    gb.setX(0);
    gb.addY(new JLabel("Group"));
    gb.addX(tcGroup.read);
    gb.addX(tcGroup.write);
    gb.weightx=1;
    gb.addX(tcGroup.execute);
        

    gb.weightx=0;
    gb.setX(0);
    gb.addY(new JLabel("Other"));
    gb.addX(tcOther.read);
    gb.addX(tcOther.write);
    gb.weightx=1;
    gb.addX(tcOther.execute);

    return jp;
  }


  private JPanel getKillConnsPanel() {
    JPanel jp=new JPanel();
    //jp.setBackground(Color.GREEN);
    
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;
    gb.insets.top=4;
    gb.insets.bottom=2;
    gb.insets.left=2;

    gb.weightx=1;
    gb.addX(lblOpenConns);
    gb.insets.top=0;
    gb.insets.bottom=0;
    gb.insets.left=0;
    gb.addY(jcbSelectAllConns);
    gb.addY(jpConns);

    gbConns.weightXY(1,0);
    gbConns.anchor=gbConns.WEST;
    
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

    //Check all:
    listenForCheckAll();
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
  private void listenForCheckAll() {
    jcbSelectAllConns.addActionListener(
      new AbstractAction() {
        public void actionPerformed(ActionEvent event) {
          boolean sel=jcbSelectAllConns.isSelected();
          for (JCheckBox b: listCBConns)
            b.setSelected(sel);
        }
      }
    );    
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
  
  ///////////
  // TEST: //
  ///////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame parentFrame=PopupTestContext.makeMainFrame();
          SSHOptionPicker pop=new SSHOptionPicker(parentFrame, new FileDialogWrapper(parentFrame));
          SSHOptions sopt=new SSHOptions();
          List<String> servers=new ArrayList<>();
          
          servers.add("hoopty.doopty.com");
          servers.add("braindamage.waffles.org");
          servers.add("splat.cannon.imbecile.nk");
          debugTestResult(pop.show(sopt, servers), sopt, servers);
          
          servers.clear();
          servers.add("bottom.bong.com");
          debugTestResult(pop.show(sopt, servers), sopt, servers);
          
          servers.clear();
          debugTestResult(pop.show(sopt, servers), sopt, servers);
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }  
  private static void debugTestResult(boolean result, SSHOptions sopt, List<String> servers){
    System.out.println("\n");
    if (result){
      System.out.println(sopt);
      for (String s: servers) 
        System.out.println("Disconnect: "+s);
    }
    else
      System.out.println("Cancelled");
  }
}
