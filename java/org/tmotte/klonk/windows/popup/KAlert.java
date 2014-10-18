package org.tmotte.klonk.windows.popup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.text.StackTracer;
import org.tmotte.klonk.config.PopupTestContext;
import org.tmotte.klonk.config.msg.Setter;

/**
 * Always has one button - OK - and a message. Can display
 * larger, more complex messages unlike YesNoCancel.
 */
class KAlert implements Fail, Setter<String> {

  private JDialog win;
  private JLabel msgLabel=new JLabel();
  private JTextPane errorLabel=new JTextPane();
  private JFrame parentFrame;
  private JButton ok=new JButton("OK");

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////
  
  public KAlert(JFrame frame) {
    parentFrame=frame;
    win=new JDialog(frame, true);

    errorLabel.setEditable(false); // as before
    errorLabel.setBorder(null);       
    errorLabel.setOpaque(false);

  
    ok.addActionListener(btnActions);
    ok.setMnemonic(KeyEvent.VK_K);
    KeyMapper.accel(ok, btnActions, KeyEvent.VK_ESCAPE);
    KeyMapper.accel(ok, btnActions, KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK);
    
    GridBug gb=new GridBug(win.getContentPane());
    
    gb.insets.left=10;gb.insets.right=10;
    gb.insets.top=10; gb.insets.bottom=10;
    gb.gridXY(0).weightXY(1);
    gb.add(msgLabel);
    gb.fill=gb.BOTH;
    gb.addY(errorLabel);
    gb.weightXY(0);
    gb.fill=gb.NONE;
    gb.insets.top=5;
    gb.insets.bottom=10;
    gb.addY(ok);
    msgLabel.setVisible(false);
  }
  /** Implements Setter interface */
  public @Override void set(String message){
    show(message.toString());
  }
  public void show(String message) {
    errorLabel.setSize(new Dimension(0,0));
    errorLabel.setPreferredSize(null);
    errorLabel.setText(message);
    win.pack();
    int wide=errorLabel.getPreferredSize().width,
        high=errorLabel.getSize().height;
    if (wide>500){
      double dwide=wide;
      dwide/=500d;
      int increment=(int)Math.round(dwide);
      high=high*(increment+(increment> 5 ?2 :1));
      errorLabel.setPreferredSize(new Dimension(500, high));
      win.pack();
    }
    show();
  }

  /** Implements Fail interface: */
  public void fail(Throwable e) {
    show(null, e);
  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  
  private void show(String message, Throwable e) {
    if (message==null)
      message="Whoops... ";
    message=getAlert(message, e);
    errorLabel.setPreferredSize(null);
    errorLabel.setSize(new Dimension(0,0));
    errorLabel.setText(message);
    win.pack();
    show();
  }
  private String getAlert(String message, Throwable e) {
    return message+"\n\n"+StackTracer.getStackTrace(e).replaceAll("\t", "    ");
  }
  private void show() {
    Point pt=parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.toFront();
  }
  private Action btnActions=new AbstractAction() {
    public void actionPerformed(ActionEvent event) {
      win.setVisible(false);
    }
  };
  
  ///////////
  // TEST: //
  ///////////

  public static void main(String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          JFrame frame=PopupTestContext.makeMainFrame();
          KAlert ka=new KAlert(frame);
          try {throwTest();} catch (Exception f) {ka.fail(f);}
          ka.show("This is simply the most pornographic thing anybody's seen in a while, in these parts."
                 +" I'm not sure what kind of smut you folks are into but you won't be dissatisfied, I "
                 +"assure you");
          ka.show(
            "I dislike you and everything and whatever and so on and howdy and man oh man am I tired "
           +"and this is going to be such a gigantically long sentence that it has no business fitting "
           +"in a window like this it's just too big and it goes on forever keep trying it's almost " 
           +"long enough ok now you're cooking just a little more lordy that's enough. Blah blah blah blah"
           +"blah blah blah."
           +"I dislike you and everything and whatever and so on and howdy and man oh man am I tired "
           +"and this is going to be such a gigantically long sentence that it has no business fitting "
           +"in a window like this it's just too big and it goes on forever keep trying it's almost " 
           +"long enough ok now you're cooking just a little more lordy that's enough. Blah blah blah blah"
           +"blah blah blah."
          );
          ka.show("I dislike you");
          ka.fail(new RuntimeException("I had a smoochy smooch"));
          frame.dispose();
          return;
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }
  private static void throwTest() {
    new Runnable() {
      public void run() {
        new Runnable() {
          public void run() {
            new Runnable() {
              public void run() {
                new Runnable() {
                  public void run() {throw new RuntimeException("hork");}
                }.run();
              }
            }.run();
          }
        }.run();
      }
    }.run();
  }
}