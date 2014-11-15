package org.tmotte.klonk.windows.popup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import java.awt.FocusTraversalPolicy;
import org.tmotte.klonk.config.PopupTestContext;

class YesNoCancel {
  private int yesOrNoOrCancel=-1;
  private JDialog win;
  private JLabel msgLabel;
  private JFrame parentFrame;
  private JButton btnYes, btnNo, btnCancel;

  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  public YesNoCancel(JFrame frame) {
    this(frame, true);
  }
  public YesNoCancel(JFrame frame, boolean haveCancel) {
    parentFrame=frame;
    create(haveCancel);
    layout();
    listen();
  }
  public void setMessage(String msg) {
    msgLabel.setText(msg);
    win.pack();
  }
  
  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public YesNoCancelAnswer show() {
    return show(null);
  }
  public YesNoCancelAnswer show(String message) {
    Point pt=parentFrame.getLocation();
    return show(pt.x+20, pt.y+20, message);
  }
  public YesNoCancelAnswer show(int x, int y) {
    return show(x, y, null);
  }
  public YesNoCancelAnswer show(int x, int y, String message) {
    if (message!=null)
      setMessage(message);
    win.setLocation(x, y);
    win.setVisible(true);
    btnYes.requestFocusInWindow();
    win.toFront();
    return new YesNoCancelAnswer(yesOrNoOrCancel);
  }
  public int getHeight() {
    return win.getHeight();
  }
  public int getWidth() {
    return win.getWidth();
  }
  public void setupForFindReplace() {
    doF3Stuff();
  }
  

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void create(boolean doCancel) {
    win=new JDialog(parentFrame, true);
    msgLabel=new JLabel();
    btnYes=new JButton("Yes");
    btnYes.setMnemonic(KeyEvent.VK_Y);
    btnNo=new JButton("No");
    btnNo.setMnemonic(KeyEvent.VK_N);
    if (doCancel){
      btnCancel=new JButton("Cancel");
      btnCancel.setMnemonic(KeyEvent.VK_C);
    }
  }

  private void layout() {
    Container cont=win.getContentPane();
    GridBug grid=new GridBug(cont);

    grid.gridXY(0);
    Insets insets=grid.insets=new Insets(20, 20, 20, 20);

    insets.top=15;
    grid.add(msgLabel);

    JPanel p=layoutButtons();
    insets.left=10;
    insets.right=10;
    insets.top=insets.bottom=0;
    grid.weightx=1.0;
    grid.addY(p);
  }
  
  private JPanel layoutButtons() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=0;
    insets.bottom=15;
    insets.left=5;
    insets.right=5;
    
    gb.gridXY(0);
    gb.add(btnYes);
    gb.addX(btnNo);
    if (btnCancel!=null)
      gb.addX(btnCancel);
    return panel;
  }

  /////////////
  // EVENTS: //
  /////////////

  
  private void listen() {
    listen(btnYes,    YesNoCancelAnswer.YES);
    listen(btnNo,     YesNoCancelAnswer.NO);
    listen(btnCancel, YesNoCancelAnswer.CANCEL);
  }
  private void listen(JButton button, int action) {
    if (button!=null) {
      KeyMonster km=new KeyMonster();
      km.result=action;
      button.addKeyListener(km);
      button.addActionListener(btnActions);
    }
  }
  private Action btnActions=new AbstractAction() {
    public void actionPerformed(ActionEvent event) {
      Object o=event.getSource();
      if (o==btnYes)
        click(YesNoCancelAnswer.YES);
      else
      if (o==btnNo)
        click(YesNoCancelAnswer.NO);
      else
      if (o==btnCancel)
        click(YesNoCancelAnswer.CANCEL);
    }
  };
      
  
  private class KeyMonster extends KeyAdapter {
    int result;
    public void keyPressed(KeyEvent e){
      int code=e.getKeyCode();
      if (code==KeyEvent.VK_ESCAPE) 
        click(YesNoCancelAnswer.CANCEL);
      else
      if (code==KeyEvent.VK_W && KeyMapper.ctrlPressed(e)) 
        click(YesNoCancelAnswer.CANCEL);
      else
      if (code==KeyEvent.VK_ENTER) 
        click(result);
      else
      if (code==KeyEvent.VK_Y) 
        click(YesNoCancelAnswer.YES);
      else
      if (code==KeyEvent.VK_N) 
        click(YesNoCancelAnswer.NO);
      else
      if (code==KeyEvent.VK_C && btnCancel!=null) 
        click(YesNoCancelAnswer.CANCEL);
      else
      if (code==KeyEvent.VK_LEFT || code==KeyEvent.VK_KP_LEFT){
        Component c=win.getFocusOwner();
        if (arrow(c, btnNo, btnYes)    ||
            arrow(c, btnYes, btnCancel, btnNo)||
            arrow(c, btnCancel, btnNo)){}
      }
      else
      if (code==KeyEvent.VK_RIGHT || code==KeyEvent.VK_KP_RIGHT){
        Component c=win.getFocusOwner();
        if (arrow(c, btnNo, btnCancel, btnYes)||
            arrow(c, btnYes, btnNo)   ||
            arrow(c, btnCancel, btnYes)){}
      }
    }
  };
  private boolean arrow(Component c, JButton btnWas, JButton... btnIs) {
    if (c==btnWas) 
      for (JButton b: btnIs)
        if (b!=null) {
          b.requestFocusInWindow();
          return true;
        }
    return false;
  }
  private void doF3Stuff() {
    btnYes.setText("Yes (F3)");
    win.pack();
    KeyMapper.accel(btnYes, btnActions, KeyEvent.VK_F3);
    KeyMapper.accel(btnYes, btnActions, KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK);
  }
  private void click(int result) {
    win.setVisible(false);  
    yesOrNoOrCancel=result;
  }
 
 
  ///////////
  // TEST: //
  ///////////
 
  public static void main(String[] args) {
    final boolean doCancel=Boolean.parseBoolean(args[0]);
    final String message=args[1];
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        JFrame m=PopupTestContext.makeMainFrame();
        YesNoCancel ky=new YesNoCancel(m, doCancel);
        System.out.println(ky.show(message));
      }
    });  
  }
}