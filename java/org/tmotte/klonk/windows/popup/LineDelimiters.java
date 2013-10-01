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
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.Boot;
import org.tmotte.klonk.config.option.LineDelimiterOptions;

class LineDelimiters {
  public int AllLineDelimiters=-1,
             ThisLineDelimiters=-1;

  private JDialog win;
  private JFrame parentFrame;
  private String[] jcOptions=getOptions();
  private JComboBox<String> jcbDefault=new JComboBox<String>(jcOptions),
                            jcbThis=new JComboBox<String>(jcOptions);
  private JButton   btnDefault=new JButton("Set"),
                    btnThis=new JButton("Change"),
                    btnClose=new JButton("Close");
  
  private LineDelimiterListener listener;
  private LineDelimiterOptions options=new LineDelimiterOptions();
  private Fail failer;

  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  public LineDelimiters(JFrame frame, Fail failer) {
    this.failer=failer;
    parentFrame=frame;
    win=new JDialog(frame, true);
    win.setTitle("Line delimiters");
    
    layout();
    listen();

    win.pack();
  }
  

  public void show(LineDelimiterOptions current, LineDelimiterListener listener) {
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


  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void clickSetDefault() {
    options.setDefault(jcbDefault.getSelectedItem().toString());
    btnDefault.setEnabled(false);
    jcbDefault.requestFocus();
    listener.setDefault(options.defaultOption);
  }
  private void clickSetThis() {
    options.setThisFile(jcbThis.getSelectedItem().toString());
    btnThis.setEnabled(false);
    jcbThis.requestFocus();
    listener.setThis(options.thisFile);
  }
  private void clickClose() {
    win.setVisible(false);
  }

  

  // LAYOUT: //

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
  
  }
  private Component getFirstLabel() {
    GridBug gb=new GridBug(new JPanel());
    gb.add(
      new JLabel("<html>Set <b>default</b> delimiter to: </html>")
    );
    return gb.container;
  }
  private Component getFirstPicker() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0)
      .weightXY(1);
    gb.anchor=gb.CENTER;
    gb.add(jcbDefault);
    gb.insets.left=4;
    gb.addX(btnDefault);
    return gb.container;
  }
  private Component getSecondLabel() {
    GridBug gb=new GridBug(new JPanel());
    gb.anchor=gb.WEST;
    gb.gridy=0;
    gb.add(
      new JLabel("<html>Change <b>this file's</b> delimiter to:</html>")
    );
    return gb.container;
  }
  private Component getSecondPicker() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0);
    gb.weightx=1.0;
    gb.anchor=gb.CENTER;
    gb.add(jcbThis);
    gb.insets.left=4;
    gb.addX(btnThis);
    return gb.container;
  }
  
  // LISTENERS //
  
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
 
 
  
  ///////////
  // TEST: //
  ///////////
 
  public static void main(String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        Popups p=Boot.getPopupsForUnitTest();
        LineDelimiterOptions kdo=new LineDelimiterOptions();
        p.showLineDelimiters(
          kdo,
          new LineDelimiterListener(){ 
            public void setDefault(String i) {System.out.println("Default >"+i+"<");}
            public void setThis(String i)    {System.out.println("This >"+i+"<");   }
          }
        );
      }
    });  

  }
}