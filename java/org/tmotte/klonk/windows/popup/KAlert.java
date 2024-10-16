package org.tmotte.klonk.windows.popup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.geom.Rectangle2D;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextPane;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.text.StackTracer;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;

/**
 * Always has one button - OK - and a message. Can display
 * larger, more complex messages unlike YesNoCancel.
 */
public class KAlert implements Setter<String> {

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;

  // Controls:
  private JDialog win;
  private JTextPane errorLabel;
  private JButton ok;
  private Setter<Throwable> errorHandler;
  private double msgMaxLineHeight, msgMaxLineWidth;


  // State:
  private boolean initialized=false;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public KAlert(PopupInfo pInfo, FontOptions fontOptions) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    pInfo.addFontListener(fo -> setFont(fo));
  }
  /** Implements Setter interface */
  public @Override void set(String message){
    show(message.toString());
  }
  public Setter<Throwable> getErrorHandler(){
    if (errorHandler==null)
      errorHandler=(Throwable error) -> fail(error);
    return errorHandler;
  }
  public void show(String message) {
    init();

    // Set label boundaries based on message:
    // (note: JEditorPane does not wrap words properly, so this is STILL an approximation)
    String[] lines=message.split("\n");
    Rectangle parentDim=pInfo.parentFrame.getBounds();
    double
      maxWidth=Math.min(parentDim.width-60, msgMaxLineWidth),
      maxHeight=parentDim.height-60;
    double width=0;
    {
      Graphics graphics=errorLabel.getGraphics();
      FontMetrics fm=graphics.getFontMetrics();
      for (String s: lines)
        width+=fm.getStringBounds(s, graphics).getWidth();
      width+=fm.getStringBounds("123", graphics).getWidth();
      if (width==0) width=100;
      else
      if (width>maxWidth) width=maxWidth;
    }
    double height=lines.length==1
      ?msgMaxLineHeight * 1.2
      :(lines.length + 1) * msgMaxLineHeight;
    {
      if (height==0) height=100;
      else
      if (height>maxHeight) height=maxHeight;
    }
    errorLabel.setPreferredSize(new Dimension((int)width, (int)height));
    errorLabel.setText(message);

    //System.out.println("LineCount "+lines.length);
    //System.out.println("Parent bounds: "+parentDim);
    //System.out.println("Width "+width+" Height "+height);

    win.pack();
    ok.requestFocusInWindow();
    Point pt=pInfo.parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.toFront();
  }

  public void fail(Throwable e) {
    init();
    show(null, e);
  }

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void init() {
    if (!initialized){
      create();
      layout();
      listen();
      initialized=true;
    }
  }
  private void create() {
    win=new JDialog(pInfo.parentFrame, true);

    errorLabel=new JTextPane();
    errorLabel.setEditable(false);
    errorLabel.setBorder(null);
    errorLabel.setOpaque(false);
    errorLabel.setText("ABCDEFG\naaa\n\neee\neifif");
    ok=new JButton("OK");
  }
  private void layout() {
    GridBug gb=new GridBug(win.getContentPane());
    gb.insets.left=10;gb.insets.right=10;
    gb.insets.top=10; gb.insets.bottom=10;
    gb.gridXY(0).weightXY(1);
    gb.fill=GridBug.BOTH;
    gb.addY(errorLabel);
    gb.weightXY(0);
    gb.fill=GridBug.NONE;
    gb.insets.top=5;
    gb.insets.bottom=10;
    gb.addY(ok);

    win.pack();
    setFont(fontOptions);
  }
  private void listen() {
    Action actions=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        win.setVisible(false);
      }
    };
    ok.addActionListener(actions);
    ok.setMnemonic(KeyEvent.VK_K);
    pInfo.currentOS.fixEnterKey(ok, actions);
    KeyMapper.easyCancel(ok, actions);
  }

  private void show(String message, Throwable e) {
    if (message==null)
      message="Whoops... ";
    show(getAlert(message, e));
  }
  private String getAlert(String message, Throwable e) {
    return message+"\n\n"+StackTracer.getStackTrace(e).replaceAll("\t", "    ");
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null && errorLabel!=null && errorLabel.getGraphics()!=null){
      fontOptions.getControlsFont().set(win);
      {
        FontMetrics fm=errorLabel.getGraphics().getFontMetrics();
        StringBuilder sb=new StringBuilder(" ");
        for (char c='A'; c<='Z'; c++) sb.append(c);
        for (char c='a'; c<='z'; c++) sb.append(c);
        while (sb.length()<120)
          for (char c='0'; c<='9'; c++) sb.append(c);
        Rectangle2D rect=fm.getStringBounds(sb.toString(), errorLabel.getGraphics());
        msgMaxLineHeight=rect.getHeight();
        msgMaxLineWidth=rect.getWidth();
      }
    }
  }


  ///////////
  // TEST: //
  ///////////

  public static void main(String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          PopupTestContext ptc=new PopupTestContext();
          ptc.getMainFrame().setBounds(new java.awt.Rectangle(200,200,1200,800));

          KAlert ka=new KAlert(ptc.getPopupInfo(), ptc.getFontOptions());
          ka.show("Small warning thing okay.");
          System.out.println("Max line height "+ka.msgMaxLineHeight+" max line width "+ka.msgMaxLineWidth);
          ka.show("Small warning thing okay but larger.");
          ka.show("Small warning thing okay but it's just a bit larger than before.");
          try {throwTest();} catch (Exception f) {ka.fail(f);}
          ka.show("This is simply the most weird thing anybody's seen in a while, in these parts."
                 +" I'm not sure what you folks are into but you won't be dissatisfied, I "
                 +"assure you");
          ka.show(
            "I dislike you and everything and whatever and so on and howdy and man oh man am I tired "
           +"and this is going to be such a gigantically long sentence that it has no business fitting "
           +"in a window like this it's just too big and it goes on forever keep trying it's almost "
           +"long enough ok now you're cooking just a little more lordy that's enough. Blah blah blah blah"
           +"blah blah blah.\n\n"
           +"Continuing with even more text about nothing in particular, yet something easily understood, "
           +"or perhaps only in theory so, having been written by someone of questionable mental integrity "
           +"and perspective who is not to be trusted with writing sentences of such length or really "
           +"any sentences at all, not to mention sentences that could be misinterpreted as say, a declaration "
           +"of war upon a sovereign nation, should the author happen to be president or secretary of state "
           +"or some other high-ranking position in a heavily armed nation-state foolish enough to elect "
           +"or appoint him. Let's hope that doesn't happen."
          );
          ka.show("I dislike you");
          ka.fail(new RuntimeException("I had a smoochy smooch"));
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