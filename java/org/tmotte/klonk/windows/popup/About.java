package org.tmotte.klonk.windows.popup;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import java.util.Properties;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollBar;
import javax.swing.JScrollPane;
import javax.swing.JTextPane;
import javax.swing.JWindow;
import javax.swing.ScrollPaneConstants;
import org.tmotte.common.io.Loader;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.text.StackTracer;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.windows.Positioner;

public class About {

  // DI:
  private PopupInfo pInfo;
  private FontOptions fontOptions;

  // Controls:
  private JDialog win;
  private JTextPane jtpLicense, jtpVersion, jtpJavaVersion;
  private JScrollPane jspLicense;
  private JButton btnOK;

  // State:
  private boolean initialized=false;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public About(PopupInfo pInfo, FontOptions fontOptions) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    pInfo.addFontListener(fo -> setFont(fo));
  }

  public void show() {
    init();
    Positioner.set(pInfo.parentFrame, win);
    btnOK.requestFocusInWindow();
    win.pack();
    win.setVisible(true);
    win.toFront();
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
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

  private void create(){
    win=new JDialog(pInfo.parentFrame, true);
    win.setTitle("About Klonk");
    win.setPreferredSize(new Dimension(400,400));

    jtpLicense=new JTextPane();
    jtpVersion=new JTextPane();
    jtpJavaVersion=new JTextPane();
    btnOK=new JButton();

    JTextPane[] jtps={jtpLicense, jtpVersion, jtpJavaVersion};
    for (JTextPane jtp: jtps) {
      jtp.setEditable(false);
      jtp.setBorder(null);
      jtp.setOpaque(false);
    }
    Font font=jtpVersion.getFont().deriveFont(Font.BOLD, 14);
    jtpVersion.setFont(font);
    jtpJavaVersion.setFont(font);

    jtpLicense.setContentType("text/html");
    Properties props=new Properties();
    try (java.io.InputStream is=getClass().getResourceAsStream("About-Version-Number.txt");) {
      props.load(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    String number=props.getProperty("VERSION.KLONK");
    String
      license=Loader.loadUTF8String(getClass(), "About-License.html"),
      version=Loader.loadUTF8String(getClass(), "About-Version.txt"),
      javaVersion="Running under Java version: "+System.getProperty("java.version");
    license=license.replaceAll("<meta.*?>", "");
    version=version.replaceAll("\\$version", number);
    jtpLicense.setText(license);
    jtpVersion.setText(version);
    jtpJavaVersion.setText(javaVersion);
    jspLicense=new JScrollPane(jtpLicense);
    jspLicense.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);
    //Force the stupid thing to scroll to top:
    jtpLicense.setCaretPosition(0);

    btnOK.setText("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
  }
  private void layout(){
    GridBug gb=new GridBug(win.getContentPane());

    gb.insets.left=3;gb.insets.right=3;
    gb.gridXY(0).weightXY(0);

    gb.insets.top=10;
    gb.insets.bottom=10;
    gb.weightx=1;
    gb.fill=gb.HORIZONTAL;
    gb.add(jtpVersion);

    gb.insets.top=0;
    gb.addY(jtpJavaVersion);

    gb.insets.top=0;
    gb.weightXY(1);
    gb.fill=gb.BOTH;
    gb.addY(jspLicense);

    gb.weightXY(0);
    gb.fill=gb.NONE;
    gb.insets.top=5;
    gb.insets.bottom=10;
    gb.addY(btnOK);

    setFont(fontOptions);

  }

  private void listen(){
    Action actions=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        win.setVisible(false);
      }
    };
    btnOK.addActionListener(actions);
    pInfo.currentOS.fixEnterKey(btnOK, actions);
    KeyMapper.easyCancel(btnOK, actions);
  }


  ///////////
  // TEST: //
  ///////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        PopupTestContext ptc=new PopupTestContext();
        new About(ptc.getPopupInfo(), ptc.getFontOptions()).show();
      }
    });
  }

}