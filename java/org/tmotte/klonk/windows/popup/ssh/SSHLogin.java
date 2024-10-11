package org.tmotte.klonk.windows.popup.ssh;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.ssh.IUserPass;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.windows.popup.KAlert;
import org.tmotte.klonk.windows.popup.PopupTestContext;

public class SSHLogin implements IUserPass {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;
  private Setter<String> alerter;

  // Controls:
  private JTextField jtfUsername;
  private JPasswordField jpfPass;
  private JDialog win;
  private JButton btnOK, btnCancel;
  private JLabel lblHost, lblError, lblErrorText;

  // State:
  private boolean ok=false, badEntry=false, initialized=false;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public SSHLogin(PopupInfo pInfo, FontOptions fontOptions, Setter<String> alerter) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.alerter=alerter;
    pInfo.addFontListener(fo -> setFont(fo));
  }
  public @Override String getUser() {
    return ok
      ?jtfUsername.getText().trim()
      :null;
  }
  public @Override String getPass() {
    return ok
      ?new String(jpfPass.getPassword())
      :null;
  }
  public @Override boolean get(String user, String host, boolean authFail, boolean needsPassword) {
    return show(user, host, authFail, needsPassword);
  }
  public boolean show(String user, String host, boolean authFail, boolean needsPassword) {

    //Initialize:
    init();
    if (user!=null)
      jtfUsername.setText(user);
    jpfPass.setText("");
    jpfPass.setEnabled(needsPassword);
    lblHost.setText(host);
    lblError.setVisible(authFail);
    lblErrorText.setVisible(authFail);
    lblErrorText.setText(authFail ?"User/password failed" :"");

    //Position:
    win.pack();
    Point pt=pInfo.parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);

    //Display cycle: No we don't need a loop, we just
    //leave the screen up until we pass validation. This is because
    //we display the error inline and not in another pop-up:
    badEntry=true;
    ok=true;
    win.pack();
    if (user!=null && jpfPass.isEnabled())
      jpfPass.requestFocusInWindow();
    win.setVisible(true);
    win.toFront();
    return ok;
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
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

  private void create() {
    win=new JDialog(pInfo.parentFrame, true);
    win.setTitle("SSH Login");
    jtfUsername=new JTextField();
    jtfUsername.setColumns(20);
    jpfPass=new JPasswordField();
    jpfPass.setColumns(20);
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
    lblHost=new JLabel("                                             ");
    lblHost.setFont(lblHost.getFont().deriveFont(Font.BOLD));
    lblError=new JLabel("Error: ");
    lblError.setForeground(Color.RED);
    lblErrorText=new JLabel("");
    lblErrorText.setForeground(Color.RED);
  }


  private void layout() {
    GridBug gb=new GridBug(win);
    gb.weightXY(0).gridXY(0);
    gb.addY(makeInputs());
    gb.addY(makeButtonPanel());

    setFont(fontOptions);
  }
  private JPanel makeInputs() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=GridBug.CENTER;
    gb.fill=GridBug.HORIZONTAL;
    gb.anchor=GridBug.WEST;
    gb.setX(0).setY(0);

    gb.insets.top=5;
    gb.insets.bottom=1;
    gb.insets.left=5;
    gb.insets.right=5;

    gb.weightXY(0, 0);
    gb.setX(0).addY(new JLabel("Host: "));
    gb.weightXY(1, 0);
    gb.addX(lblHost);

    gb.weightXY(0, 0);
    gb.setX(0).addY(new JLabel("User name: "));
    gb.weightXY(1, 0);
    gb.addX(jtfUsername);

    gb.weightXY(0, 0);
    gb.setX(0).addY(new JLabel("Password: "));
    gb.weightXY(1, 0);
    gb.addX(jpfPass);

    gb.weightXY(0, 0);
    gb.setX(0).addY(lblError);
    gb.weightXY(1, 0);
    gb.addX(lblErrorText);

    return jp;
  }
  private JPanel makeButtonPanel() {
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
    KeyMapper.easyCancel(btnCancel, cancelAction);
    win.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        click(false);
      }
    });

  }


  /** @param action true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    badEntry=false;
    if (ok &&
        (
        jtfUsername.getText().trim().equals("")
        ||
        (
          jpfPass.isEnabled()==true
          &&
          new String(jpfPass.getPassword()).trim().equals(""))
        )
      ){
      badEntry=true;
      lblError.setVisible(true);
      lblErrorText.setText("User name & password are required");
      lblErrorText.setVisible(true);
      win.pack();
    }
    else
      win.setVisible(false);
  }

  private String debugVals(){
    return " badEntry:"+badEntry+" ok "+ok;
  }

  private void debug(String val) {
    System.out.println("DEBUG "+val);
  }

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        PopupTestContext ptc=new PopupTestContext();
        KAlert ka=new KAlert(ptc.getPopupInfo(), ptc.getFontOptions());
        SSHLogin sh=new SSHLogin(ptc.getPopupInfo(), ptc.getFontOptions(), ka);
        testWindow(sh, "aname", "test1.youknowthat.server.danglblangdingdongwhat.com", false, false);
        testWindow(sh, "aname", "test2.who.perv.com", true, true);
        testWindow(sh, "aname", "needs.password.only.com", false, true);
        testWindow(sh, null, "test3.bleagh.com", false, true);
      }
    });
  }
  private static void testWindow(
      SSHLogin sh, String user, String host, boolean authFail, boolean needsPassword
    ) {
    System.out.println("RESULT: "+sh.show(user, host, authFail, needsPassword));
    System.out.println("user/pass: "+sh.getUser()+" "+sh.getPass());
  }

}