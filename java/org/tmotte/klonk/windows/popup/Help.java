package org.tmotte.klonk.windows.popup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
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
import javax.swing.JTextPane;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.ScrollPaneConstants;
import org.tmotte.common.io.Loader;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.PopupInfo;

public class Help {

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;
  private String homeDir;

  // Controls:
  private JButton btnOK;
  private JDialog win;
  private JTextPane jtp;
  private JScrollPane jsp;
  private Container mtaContainer;

  // State:
  private boolean initialized;

  public Help(PopupInfo pInfo, FontOptions fontOptions, String homeDir) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    this.homeDir=homeDir;
    pInfo.addFontListener(fo -> setFont(fo));
  }
  public void show() {
    show(null);
  }
  public void show(Rectangle bounds) {
    init();
    Point pt=pInfo.parentFrame.getLocation();
    if (bounds!=null)
      win.setBounds(bounds);
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
  }


  ////////////////////////
  //  PRIVATE METHODS:  //
  ////////////////////////

  private void click() {
    win.setVisible(false);
  }

  private void init() {
    if (!initialized) {
      create();
      layout();
      listen();
      initialized=true;
    }
  }

  // CREATE/LAYOUT/LISTEN: //
  private void create() {
    win=new JDialog(pInfo.parentFrame, true);

    jtp=new JTextPane();
    jtp.setEditable(false);
    jtp.setBorder(null);
    jtp.setOpaque(false);
    jtp.setContentType("text/html");

    jtp.addHyperlinkListener(new HyperlinkListener() {
      @Override public void hyperlinkUpdate(final HyperlinkEvent evt) {
        if (HyperlinkEvent.EventType.ACTIVATED == evt.getEventType()){
          String desc = evt.getDescription();
          if (desc == null || !desc.startsWith("#")) return;
          desc = desc.substring(1);
          jtp.scrollToReference(desc);
        }
      }
    });

    String helpText=Loader.loadUTF8String(getClass(), "Help.html");
    helpText=helpText.replace("$[Home]", homeDir);
    jtp.setText(helpText);
    jsp=new JScrollPane(jtp);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    jsp.getVerticalScrollBar().setUnitIncrement(16);
    if (pInfo.currentOS.isOSX)
      jsp.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_ALWAYS);

    //Force the stupid thing to scroll to top:
    jtp.setCaretPosition(0);

    btnOK=new JButton("OK");
  }
  private void layout(){
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(1, 1);
    gb.fill=GridBug.BOTH;
    gb.add(jsp);

    gb.weighty=0.0;
    gb.insets.top=5;
    gb.insets.bottom=5;
    gb.fill=GridBug.NONE;
    gb.addY(btnOK);

    setFont(fontOptions);

    Rectangle rect=pInfo.parentFrame.getBounds();
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
    KeyMapper.easyCancel(btnOK, okAction);
    pInfo.currentOS.fixEnterKey(btnOK, okAction);
    jtp.addKeyListener(new KeyAdapter() {
      public void keyPressed(KeyEvent ke) {
        if (ke.getKeyCode()==KeyEvent.VK_TAB)
          btnOK.requestFocusInWindow();
      }
    });
  }

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception{
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        PopupTestContext ptc=new PopupTestContext();
        Help h=new Help(
          ptc.getPopupInfo(), ptc.getFontOptions(), "."
        );
        h.show(new Rectangle(800,400));
      }
    });
  }

}