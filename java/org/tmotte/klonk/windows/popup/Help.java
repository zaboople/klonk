package org.tmotte.klonk.windows.popup;
import org.tmotte.klonk.config.Kontext;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.io.InputStream;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JWindow;
import org.tmotte.common.io.Loader;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.edit.MyTextArea;

class Help {
  private JFrame parentFrame;
  private JButton btnOK;
  private JDialog win;
  private MyTextArea mta;
  private Container mtaContainer;
  private Fail fail;
  private String homeDir;

  public Help(JFrame parentFrame, Fail fail, String homeDir) {
    this.parentFrame=parentFrame;
    this.fail=fail;
    this.homeDir=homeDir;
    create();
    layout();
    listen();
  }
  public void setFont(FontOptions f) {
    mta.setFont(f.getFont());
    mta.setForeground(f.getColor());
    mta.setBackground(f.getBackgroundColor());
    mta.setCaretColor(f.getCaretColor());
  }
  public void show() {
    Point pt=parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
  }
  
  ////////////////////////
  //  PRIVATE METHODS:  //
  ////////////////////////
  
  private void click() {
    win.setVisible(false);  
  }

  // CREATE/LAYOUT/LISTEN: //
  private void create() {
    win=new JDialog(parentFrame, true);
    
    mta=new MyTextArea();
    mtaContainer=mta.makeVerticalScrollable();
    mta.setEditable(false);
    mta.setLineWrap(true);
    mta.setWrapStyleWord(true);
    String helpText=Loader.loadUTF8String(getClass(), "Help.txt");
    helpText=helpText.replace("$[Home]", homeDir);
    mta.setText(helpText);
    mta.setCaretPosition(0);

    btnOK=new JButton("OK");
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(1, 1);
    gb.fill=gb.BOTH;
    gb.add(mtaContainer);

    gb.weighty=0.0;
    gb.insets.top=5;
    gb.insets.bottom=5;
    gb.fill=gb.NONE;
    gb.addY(btnOK);

    win.pack();

    Rectangle rect=parentFrame.getBounds();
    rect.x+=20; rect.y+=20;
    rect.width=Math.max(rect.width-40, 100);
    rect.height=Math.max(rect.height-40, 100);
    win.setBounds(rect);
  }
  private void listen() {
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        click();
      }
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
    mta.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode()==KeyEvent.VK_TAB)
          btnOK.requestFocusInWindow();
      }
    });
  }
    
  /////////////
  /// TEST: ///
  /////////////
  
  public static void main(String[] args) throws Exception{
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Kontext context=Kontext.getForUnitTest();
        Help help=new Help(
          context.mainFrame, 
          context.fail,
          context.home.getUserHome()
        );
        help.setFont(new FontOptions());
        help.show();
        context.mainFrame.dispose();
      }
    });  
      
  }
  
}