package org.tmotte.klonk.windows.popup.ssh;
import java.awt.Insets;
import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.ArrayList;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.UserServer;
import org.tmotte.klonk.windows.Positioner;
import org.tmotte.klonk.windows.popup.PopupTestContext;

//FIXME document this sucker in the online help
public class SSHOpenFrom {

  private JFrame parentFrame;
  private JDialog win;  
  private JComboBox<String> jcbPrevious;
  private DefaultComboBoxModel<String> jcbPreviousData;
  private JTextField jtfFile;
  private JButton btnOK, btnCancel;
  private JLabel lblError;
  private JPanel pnlError;
  
  private boolean shownBefore=false;
  private boolean initialized=false;
  private String result=null;
  private boolean failed=false;

  public SSHOpenFrom(JFrame parentFrame) {
    this.parentFrame=parentFrame;
  }  

  private void doShow() {
    win.setVisible(true);
    win.toFront();
  }

  public String show(List<UserServer> recent) {
    init();
    jcbPreviousData.removeAllElements();
    for (UserServer us: recent)
      jcbPreviousData.addElement(us.user+"@"+us.server);
    if (!shownBefore && !jcbPrevious.hasFocus())
      jcbPrevious.requestFocusInWindow();
    Positioner.set(parentFrame, win, shownBefore || (win.getBounds().x>-1 && win.getBounds().y>-1));
    failed=true;
    shownBefore=true;
    lblError.setVisible(false);
    lblError.setText(null);
    while (failed)
      doShow();
    return result;
  }



  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    result=null;
    failed=false;
    if (action){
      String userHost=jcbPrevious.getEditor().getItem().toString().trim();
      String file=jtfFile.getText().trim();
      if (!userHost.equals("") && !file.equals("")) {
        if (!file.startsWith("/"))
          file=":~/"+file;
        else
          file=":"+file;
        result="ssh:"+userHost+file;
      }
      else {
        failed=true;
        lblError.setText("ERROR: User@Host & File are needed");
        lblError.setVisible(true);
      }
    }
    if (!failed)
      win.setVisible(false);      
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////


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
    win.setResizable(true);
    win.setTitle("Open from SSH");
    
    jcbPreviousData=new DefaultComboBoxModel<>();
    jcbPreviousData.removeAllElements();

    jcbPrevious=new JComboBox<>(jcbPreviousData);
    jcbPrevious.setEditable(true);
    jcbPrevious.setMaximumRowCount(KPersist.maxRecent);

    jtfFile=new JTextField();
    jtfFile.setColumns(80);

    pnlError=new JPanel();
    lblError=new JLabel();
    lblError.setText("<none>");
    lblError.setForeground(Color.RED);

    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C); 
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.NONE;
    gb.anchor=gb.NORTHWEST;
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(1,0);
    gb.insets.right=5;
    gb.add(getInputPanel());
    gb.insets.right=0;
    gb.weightXY(1);
    gb.addY(getButtons());
    gb.weightXY(0);
    gb.addY(getErrorPanel());
    win.pack();  
  }
  private JPanel getInputPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;

    gb.insets.top=5;
    gb.insets.bottom=2;
    gb.insets.left=10;    
    gb.add(new JLabel(""));
    gb.addX(new JLabel("User@Host"));
    gb.addX(new JLabel("File"));
    
    gb.insets.top=2;
    gb.insets.left=5;
    gb.setX(0);
    gb.addY(new JLabel("ssh:"));
    gb.insets.left=1;
    gb.addX(jcbPrevious);
    gb.insets.left=5;
    gb.weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;    
    gb.addX(jtfFile);
    
    return jp;
  }
  private JPanel getErrorPanel() {
    JPanel panel=pnlError;
    GridBug gb=new GridBug(panel);
    gb.setInsets(5);
    gb.insets.top=0;
    gb.weightXY(1, 0);
    gb.anchor=gb.WEST;
    gb.add(lblError);  
    return panel;
  }
  private JPanel getButtons() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.add(btnOK);
    gb.addX(btnCancel);
    return panel;
  }
  
  private void listen(){
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
    win.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        click(false);
      }
    });
  }
  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame parentFrame=PopupTestContext.makeMainFrame();
          SSHOpenFrom w=new SSHOpenFrom(parentFrame);
          List<UserServer> uss=new ArrayList<>();

          System.out.println(w.show(uss));

          uss.add(new UserServer("mrderp", "suvuh.suv.com"));
          uss.add(new UserServer("trang", "woi.hoi.org"));
          System.out.println(w.show(uss));

          uss.add(new UserServer("sploong", "twabbada.lob.net"));
          System.out.println(w.show(uss));         
          
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }  

}

