package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JWindow;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.option.LineDelimiterOptions;

public class LineDelimiters {

  // DI:
  private JFrame parentFrame;
  private CurrentOS currentOS;

  // Controls:
  private JDialog win;
  private String[] jcOptions;
  private JComboBox<String> jcbDefault, jcbThis;
  private JButton btnDefault, btnThis, btnClose;

  // State:
  private LineDelimiterListener listener;
  private boolean initialized=false;

  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  public LineDelimiters(JFrame frame, CurrentOS currentOS) {
    parentFrame=frame;
    this.currentOS=currentOS;
  }


  public void show(LineDelimiterOptions current, LineDelimiterListener listener) {
    init();
    this.listener=listener;
    //These have to come before the other crap or it won't render. Which is stupid.
    jcbDefault.setSelectedIndex(getSelectedOption(current.defaultOption));
    jcbThis.setSelectedIndex(getSelectedOption(current.thisFile));
    btnDefault.setEnabled(false);
    btnThis.setEnabled(false);
    //And now the normal bs:
    Point pt=parentFrame.getLocation();
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
    jcbDefault.requestFocus();
  }


  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void clickSetDefault() {
    btnDefault.setEnabled(false);
    jcbDefault.requestFocus();
    String sel=jcbDefault.getSelectedItem().toString();
    listener.setDefault(LineDelimiterOptions.translateFromReadable(sel));
  }
  private void clickSetThis() {
    btnThis.setEnabled(false);
    jcbThis.requestFocus();
    String sel=jcbThis.getSelectedItem().toString();
    listener.setThis(LineDelimiterOptions.translateFromReadable(sel));
  }
  private void clickClose() {
    win.setVisible(false);
  }


  ///////////////////////////////
  // CREATE / LAYOUT / LISTEN: //
  ///////////////////////////////

  private void init() {
    if (!initialized){
      create();
      layout();
      listen();
      initialized=true;
    }
  }

  private void create() {
    win=new JDialog(parentFrame, true);
    win.setTitle("Line delimiters");
    jcOptions =getOptions();
    jcbDefault=new JComboBox<String>(jcOptions);
    jcbThis   =new JComboBox<String>(jcOptions);
    btnDefault=new JButton("Set");
    btnThis   =new JButton("Change");
    btnClose  =new JButton("Close");
  }
  private void layout() {
    GridBug gb=new GridBug(win.getContentPane());
    gb.gridXY(0);
    gb.anchor=gb.WEST;

    Insets insets=gb.insets;
    insets.right=10;
    insets.left=10;

    //Label for default:
    insets.top=10;
    gb.weightx=1.0;
    gb.add(getFirstLabel());

    //Buttons for default:
    insets.top=3;
    gb.gridwidth=1;
    gb.addY(getFirstPicker());

    //Label for this:
    insets.top=10;
    gb.gridx=0;
    gb.addY(getSecondLabel());

    //Buttons for this:
    insets.top=3;
    insets.bottom=10;
    gb.addY(getSecondPicker());

    //Button for close:
    insets.top=10;
    gb.anchor=gb.CENTER;
    gb.addY(btnClose);

    win.pack();
  }
  private Component getFirstLabel() {
    return getLabel("<html>Set <b>default</b> delimiter to: </html>");
  }
  private Component getFirstPicker() {
    return getPicker(jcbDefault, btnDefault);
  }
  private Component getSecondLabel() {
    return getLabel("<html>Change <b>this file's</b> delimiter to:</html>");
  }
  private Component getSecondPicker() {
    return getPicker(jcbThis, btnThis);
  }
  private Component getLabel(String name) {
    GridBug gb=new GridBug(new JPanel());
    gb.add(new JLabel(name));
    return gb.container;
  }
  private Component getPicker(Component jcb, Component btn) {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightx=1.0;
    gb.insets.top=2;
    gb.anchor=gb.SOUTH;
    gb.add(jcb);
    gb.insets.left=8;
    gb.addX(btn);
    return gb.container;
  }


  private void listen() {
    jcbDefault.addItemListener(
      new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          btnDefault.setEnabled(true);
        }
      }
    );
    jcbThis.addItemListener(
      new ItemListener() {
        public void itemStateChanged(ItemEvent e) {
          btnThis.setEnabled(true);
        }
      }
    );

    Action closeAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        clickClose();
      }
    };
    btnClose.addActionListener(closeAction);
    KeyMapper.accel(btnClose, closeAction, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnClose, closeAction, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
    btnClose.setMnemonic(KeyEvent.VK_C);
    btnDefault.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {clickSetDefault();}
    });
    btnThis.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent e) {clickSetThis();}
    });
  }

  ///////////////////////
  // STATIC UTILITIES: //
  ///////////////////////

  private static int getSelectedOption(String option) {
    if (option.equals(LineDelimiterOptions.CRs))
      return 0;
    else
    if (option.equals(LineDelimiterOptions.LFs))
      return 1;
    else
    if (option.equals(LineDelimiterOptions.CRLFs))
      return 2;
    throw new RuntimeException("I don't know what to do with: <"+option+">");
  }
  private static String[] getOptions() {
    return new String[]{
      LineDelimiterOptions.translateToReadable(LineDelimiterOptions.CRs),
      LineDelimiterOptions.translateToReadable(LineDelimiterOptions.LFs),
      LineDelimiterOptions.translateToReadable(LineDelimiterOptions.CRLFs),
    };
  }


  ///////////
  // TEST: //
  ///////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        final LineDelimiterOptions kdo=new LineDelimiterOptions();
        PopupTestContext ptc=new PopupTestContext();
        new LineDelimiters(
          ptc.makeMainFrame(), ptc.getCurrentOS()
        ).show(
          kdo,
          new LineDelimiterListener(){
            public void setDefault(String i) {
              kdo.defaultOption=i;
              System.out.println(kdo);
            }
            public void setThis(String i)    {
              kdo.thisFile=i;
              System.out.println(kdo);
            }
          }
        );
        System.out.println(kdo);
      }
    });
  }
}