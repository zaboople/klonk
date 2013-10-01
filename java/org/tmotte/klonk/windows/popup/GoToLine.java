package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;

class GoToLine {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  private JFrame parentFrame;
  private Popups popups;
  private Fail fail;

  private JDialog win;
  private JTextField jtfRow;
  private JButton btnOK, btnCancel;

  private boolean badEntry=false, cancelled=false;
  private int result=-1;
  
  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public GoToLine(JFrame parentFrame, Fail fail, Popups popups) {
    this.parentFrame=parentFrame;
    this.fail=fail;
    this.popups=popups;
    create();
    layout(); 
    listen();
  }
  public int show() {
    String s=jtfRow.getText();
    if (s!=null && !s.equals("")){
      jtfRow.setCaretPosition(0);
      jtfRow.moveCaretPosition(s.length());
    }
    win.pack();
    Positioner.set(parentFrame, win, false);

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
      } catch (Exception e) {
        popups.alert("Value entered is not a valid number ");
        badEntry=true;
        return;
      }
      if (result<=0) {
        popups.alert("Value must be greater than 0");
        badEntry=true;
        return;
      }
    }
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //  
  ///////////////////////////

  private void create(){
    win=new JDialog(parentFrame, true);
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

    JLabel label=new JLabel("Line # ");
    gb.add(label);
    gb.insets.left=0;
    gb.insets.right=5;
    gb.addX(jtfRow);
    
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
  
  /////////////
  /// TEST: ///
  /////////////
  
  public static void main(String[] args) throws Exception {
  }
  
}