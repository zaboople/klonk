package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Container;
import java.awt.Insets;
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
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextField;
import javax.swing.JCheckBox;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.io.EncryptionParams;
import org.tmotte.klonk.windows.Positioner;

public class EncryptionInput {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;
  private KPersist persist;

  // Controls:
  private JDialog win;
  private JComboBox<Integer> jcbBits;
  private JPasswordField jpfPass;
  private JTextField jtfPass;
  private JCheckBox chkShowPass, chkPreservePass;
  private JButton btnOK, btnCancel;
  private JLabel lblError, lblEncryptedWarning;
  private boolean initialized;

  // State:
  private boolean badEntry=false, cancelled=false, result=false;
  private EncryptionParams encryptionParams;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public EncryptionInput(
      PopupInfo pInfo,
      FontOptions fontOptions,
      KPersist persist
    ) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.persist=persist;
    pInfo.addFontListener(fo -> setFont(fo));
  }

  public synchronized boolean show(
      EncryptionParams encryptionParams, boolean forEncrypt, String fail
    ) {
    init();
    this.encryptionParams=encryptionParams;
    if (encryptionParams.bits!=0)
      try {
        jcbBits.setSelectedItem(encryptionParams.bits);
      } catch (Exception e) {
        fail=(fail==null ?"" :(fail+" ... "))+e.getMessage();
      }

    lblError.setVisible(fail!=null);
    // Breaks up the error:
    if (fail!=null && fail.length()>70) {
      StringBuilder newFail=new StringBuilder(fail);
      for (int i=70; i<newFail.length(); i+=(70+"<br>".length())) {
        int space=newFail.indexOf(" ", i);
        if (space!=-1)
          newFail.replace(space, space+1, "<br>");
      }
      fail=newFail.toString();
    }
    lblError.setText(fail!=null ?("<html><body><b>Error: "+fail+"</b></body></html>") :"");

    lblEncryptedWarning.setVisible(!forEncrypt);
    chkShowPass.setSelected(persist.getEncryptionShowPass());
    chkPreservePass.setSelected(persist.getEncryptionPreservePass());
    showPass(persist.getEncryptionShowPass(), false);
    win.pack();
    Positioner.set(pInfo.parentFrame, win, true);
    if (jtfPass.isVisible())
      jtfPass.requestFocusInWindow();
    else
      jpfPass.requestFocusInWindow();

    badEntry=true;
    cancelled=false;
    while (badEntry && !cancelled){
      win.setVisible(true);
      win.toFront();
    }
    if (!chkPreservePass.isSelected()) {
      jtfPass.setText("");
      jpfPass.setText("");
    }
    persist.setEncryptionShowPass(chkShowPass.isSelected());
    persist.setEncryptionPreservePass(chkPreservePass.isSelected());
    persist.checkSave();

    return result;
  }

  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////


  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    badEntry=false;
    cancelled=!action;
    win.setVisible(false);
    result=action;
    if (action){
      char[] pass=
        jpfPass.isVisible()
          ?jpfPass.getPassword()
          :jtfPass.getText().toCharArray();
      if (pass.length==0) {
        lblError.setText("<html><body><b>No password entered</b></body></html>");
        lblError.setVisible(true);
        win.pack();
        badEntry=true;
        return;
      }
      else {
        encryptionParams.bits=jcbBits.getItemAt(jcbBits.getSelectedIndex());
        encryptionParams.pass=pass;
      }
    }
  }

  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
  }

  private void showPass(boolean visible, boolean swap) {
    if (swap) {
      if (visible)
        jtfPass.setText(new String(jpfPass.getPassword()));
      else
        jpfPass.setText(jtfPass.getText());
    }
    jtfPass.setVisible(visible);
    jpfPass.setVisible(!visible);
    win.pack();
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
    win=new JDialog(pInfo.parentFrame, true);
    win.setResizable(true);
    win.setTitle("Encryption");

    lblEncryptedWarning=new JLabel(
      "<html><body><b>"
      +"This file appears to be encrypted.<br>Do you want to try to decrypt it?"
      +"</b></body></html>"
    );

    jcbBits=new JComboBox<>();
    jcbBits.addItem(128);
    jcbBits.addItem(192);
    jcbBits.addItem(256);

    jpfPass=new JPasswordField();
    jtfPass=new JTextField();
    jtfPass.setVisible(false);
    chkPreservePass=new JCheckBox(
      "<html><body>Preserve password for this editing session</body></html>"
    );
    chkShowPass=new JCheckBox(
      "<html><body>Show password</body></html>"
    );

    lblError=new JLabel("Error: ");
    lblError.setForeground(Color.RED);

    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);

  }

  /////////////

  private void layout() {
    GridBug gb=new GridBug(win);
    gb
      .gridXY(0)
      .weightXY(0)
      .fill(gb.HORIZONTAL)
      .anchor(gb.NORTHWEST)
      .insets(5, 10, 5, 10)
      .add(getInputPanel())
      .addY(getButtons())
    ;
    setFont(fontOptions);
  }
  private Container getInputPanel() {
    GridBug gb=new GridBug(new JPanel());
    return
      gb.weightXY(1, 1)
        .gridXY(0)
        .anchor(gb.CENTER)
        .insets(5, 5, 10, 5)

        .gridWidth(3)
        .addY(lblEncryptedWarning)

        .fill(gb.HORIZONTAL)
        .insets(2, 2, 2, 2)
        .weightX(0)
        .gridX(0)
        .gridWidth(1)
        .addY(new JLabel("Key size:"))
        .gridWidth(2)
        .addX(jcbBits)

        .setX(0)
        .gridWidth(1)
        .addY(new JLabel("Password:"))
        .weightX(1)
        .addX(jpfPass)
        .addX(jtfPass)

        .setX(0)
        .gridWidth(4)
        .addY(chkShowPass)
        .addY(chkPreservePass)
        .fill(gb.NONE)
        .addY(lblError)

        .getContainer()
      ;
  }
  private JPanel getButtons() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    gb
      .insets(2)
      .gridXY(0,0)
      .add(btnOK)
      .addX(btnCancel);
    return panel;
  }
  private void listen() {

    // Show/hide password:
    chkShowPass.addActionListener(
      new AbstractAction(){
        public void actionPerformed(ActionEvent event) {
          showPass(chkShowPass.isSelected(), true);
        }
      }
    );

    // OK button:
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));

    // Cancel:
    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        click(false);
      }
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.easyCancel(btnCancel, cancelAction);
    win.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        click(false);
      }
    });
  }

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(()->{
      try {
        PopupTestContext ptc=new PopupTestContext();
        EncryptionInput pop=new EncryptionInput(
          ptc.getPopupInfo(), ptc.getFontOptions(), ptc.getPersist()
        );
        EncryptionParams params=new EncryptionParams();
        testShow(pop, params,true, null);
        testShow(pop, params,true, "Key length is disallowed");
        testShow(pop, params,false, null);
        testShow(pop, params,false,
          "Decryption failure: Blargh etc.etc.etc.etc.etc. etc.etc.etc.etc. etc. etc. etc. etc. etc."
         +"Yep No StackTrace trash and all that"
        );
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
  private static void testShow(EncryptionInput ei, EncryptionParams params, boolean forEncrypt, String fail) {
    params.nullify();
    System.out.print("OK? "+ei.show(params, forEncrypt, fail));
    System.out.println(params);
  }
}