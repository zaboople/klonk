package org.tmotte.klonk.windows.popup;
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
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.windows.Positioner;

public class GoToLine {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;
  private Setter<String> alerter;

  // Controls:
  private JDialog win;
  private JTextField jtfRow;
  private JButton btnOK, btnCancel;
  private boolean initialized;

  // State:
  private boolean badEntry=false, cancelled=false;
  private int result=-1;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public GoToLine(PopupInfo pInfo, FontOptions fontOptions, Setter<String> alerter) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.alerter=alerter;
    pInfo.addFontListener(fo -> setFont(fo));
  }
  public int show() {
    init();
    String s=jtfRow.getText();
    if (s!=null && !s.equals("")){
      jtfRow.setCaretPosition(0);
      jtfRow.moveCaretPosition(s.length());
    }
    win.pack();
    Positioner.set(pInfo.parentFrame, win, false);

    result=-1;
    badEntry=true;
    cancelled=false;
    while (badEntry && !cancelled)
      doShow();
    return result;
  }

  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  private void doShow() {
    win.setVisible(true);
    win.toFront();
  }

  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    badEntry=false;
    cancelled=!action;
    win.setVisible(false);
    if (action){
      try {
        result=Integer.parseInt(jtfRow.getText());
      } catch (NumberFormatException e) {
        alerter.set("Value entered is not a valid number ");
        badEntry=true;
        return;
      }
      if (result<=0) {
        alerter.set("Value must be greater than 0");
        badEntry=true;
        return;
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
    win.setResizable(false);
    win.setTitle("Go to line");
    jtfRow=new JTextField();
    jtfRow.setColumns(8);
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
  }

  /////////////

  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=GridBug.HORIZONTAL;
    gb.anchor=GridBug.NORTHWEST;
    gb.add(getInputPanel());
    gb.fill=GridBug.HORIZONTAL;
    gb.weightXY(1);
    gb.addY(getButtons());
    setFont(fontOptions);
  }
  private JPanel getInputPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightXY(0).gridXY(0);
    gb.anchor=GridBug.WEST;

    gb.insets.top=2;
    gb.insets.bottom=2;
    gb.insets.left=5;

    JLabel label=new JLabel("Line # ");
    gb.add(label);
    gb.insets.left=0;
    gb.insets.right=5;
    gb.weightXY(1, 0).setFill(GridBug.HORIZONTAL).addX(jtfRow);

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

    // Pressing enter anywhere means ok:
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

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(()->{
      try {
        PopupTestContext ptc=new PopupTestContext();
        KAlert alerter=new KAlert(ptc.getPopupInfo(), ptc.getFontOptions());
        GoToLine gtl=new GoToLine(ptc.getPopupInfo(), ptc.getFontOptions(), alerter);
        System.out.println(gtl.show());
      } catch (Exception e) {
        e.printStackTrace();
      }
    });
  }
}