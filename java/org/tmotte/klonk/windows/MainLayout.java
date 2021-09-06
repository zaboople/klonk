package org.tmotte.klonk.windows;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.msg.MainDisplay;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.option.FontOptions;

public class MainLayout {

  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  // DI stuff:
  private JFrame frame;
  private FontOptions fontOptions;
  private CurrentOS currentOS;
  private Runnable appCloseListener;

  // Main editor window components:
  private JLabel lblRow=new JLabel(),
                 lblCol=new JLabel(),
                 lblMsg=new JLabel(),
                 lblMsgBad=new JLabel();
  private JPanel pnlEditor=new JPanel(),
                 pnlSaveThisAlert=new JPanel(),
                 pnlSaveAlert=new JPanel(),
                 pnlCapsLock=new JPanel(),
                 pnlEncryption=new JPanel(),
                 pStatus=new JPanel();
  private GridBug editorGB=new GridBug(pnlEditor);
  private Color noChangeColor;

  // State:
  private boolean hasStatus=false;
  private StatusUpdate statusUpdate;
  private Set<Component> noFontChange=new HashSet<>();

  public MainLayout(
      PopupInfo pInfo, FontOptions fontOptions, Runnable appCloseListener
    ) {
    this.frame=pInfo.parentFrame;
    this.currentOS=pInfo.currentOS;
    this.fontOptions=fontOptions;
    this.appCloseListener=appCloseListener;
    doEvents();
    layout();
    noFontChange.add(pnlEditor);
    pInfo.addFontListener((FontOptions fo) -> setFont(fo));
  }

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  // DI STUFF //

  public JPanel getMainPanel() {
    return pnlEditor;
  }
  public StatusUpdate getStatusBar() {
    if (statusUpdate==null)
      statusUpdate=new StatusUpdate(){
        public void show(String s)                    {MainLayout.this.showStatus(s, false); }
        public void showBad(String s)                 {MainLayout.this.showStatus(s, true);}
        public void showCapsLock(boolean b)           {MainLayout.this.showCapsLock(b);}
        public void showEncryption(boolean b)         {MainLayout.this.showEncryption(b);}
        public void showNoStatus()                    {MainLayout.this.showNoStatus();}
        public void showRowColumn(int row, int column){MainLayout.this.showRowColumn(row,column);}
        public void showChangeThis(boolean b)         {MainLayout.this.showChangeThis(b);}
        public void showChangeAny(boolean b)          {MainLayout.this.showChangeAny(b);}
        public void showTitle(String title)           {MainLayout.this.showTitle(title);}
      };
    return statusUpdate;
  }
  public MainDisplay getMainDisplay() {
    return new MainDisplay() {
      public Rectangle getBounds() {
        return frame.getBounds();
      }
      public boolean isMaximized() {
        return (frame.getExtendedState() & frame.MAXIMIZED_BOTH) == frame.MAXIMIZED_BOTH;
      }
      public void setEditor(Component c) {
        setCurrentEditor(c);
      }
    };
  }

  public void show(Rectangle rect, boolean maximized) {
    Positioner.fix(rect);
    frame.setBounds(rect);
    if (maximized)
      frame.setExtendedState(frame.MAXIMIZED_BOTH);
    frame.setVisible(true);
  }


  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  // STATUS INFO: //

  private void showTitle(String title) {
    frame.setTitle("Klonk: "+title);
  }
  private void showChangeThis(boolean chg) {
    if (currentOS.isOSX)
      frame.getRootPane().putClientProperty(
        "Window.documentModified", chg ? Boolean.TRUE :Boolean.FALSE
      );
    pnlSaveThisAlert.setBackground(chg ?Color.RED :noChangeColor);
  }
  private void showChangeAny(boolean chg) {
    pnlSaveAlert.setBackground(chg ?Color.BLUE :noChangeColor);
  }

  private void showStatus(String msg) {
    showStatus(msg, false);
  }
  private void showStatus(String msg, boolean isBad) {
    lblMsg.setVisible(!isBad);
    lblMsgBad.setVisible(isBad);
    if (isBad)
      lblMsgBad.setText(msg);
    else
      lblMsg.setText(msg);
    pStatus.paintAll(pStatus.getGraphics());//Because during long save/load operations it doesn't update
    hasStatus=true;
  }
  private void showNoStatus() {
    if (hasStatus){
      lblMsg.setText("");
      lblMsg.setVisible(false);
      lblMsgBad.setVisible(false);
      hasStatus=false;
    }
  }
  private void showRowColumn(int row, int col) {
    lblRow.setText(String.valueOf(row));
    lblCol.setText(String.valueOf(col));
  }
  private void showCapsLock(boolean visible) {
    pnlCapsLock.setVisible(visible);
  }
  private void showEncryption(boolean visible) {
    pnlEncryption.setVisible(visible);
  }
  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    fontOptions.getControlsFont().set(frame, noFontChange);
  }


  private void setCurrentEditor(Component c) {
    pnlEditor.removeAll();
    editorGB.add(c);
    frame.paintAll(frame.getGraphics());
  }

  private void doEvents() {
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
    frame.addWindowListener(new WindowAdapter() {
      public @Override void windowClosing(WindowEvent e){
        appCloseListener.run();
      }
    });
  }

  /**
   * @param rect The boundaries of the main window.
   */
  private void layout() {

    //Set up editor panel:
    editorGB.gridXY(0).weightXY(1);
    editorGB.fill=editorGB.BOTH;

    //Set up rest of layout:
    GridBug gb=new GridBug(frame.getContentPane());

    //Top panel:
    gb.gridXY(0)
      .weightXY(1, 0);
    gb.fill=gb.HORIZONTAL;
    gb.addY(makeTopPanel());

    //Editor panel:
    gb.fill=gb.BOTH;
    gb.weightXY(1)
      .addY(pnlEditor);

    //Status panel:
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(1,0);
    layoutStatusPanel();
    gb.addY(pStatus);
    pnlCapsLock.setVisible(false);

    setFont(fontOptions);

    //Must be packed BEFORE set location step!
    frame.pack();
  }
  private JPanel makeTopPanel(){
    JPanel blah=new JPanel();
    noChangeColor=pnlSaveThisAlert.getBackground();
    Dimension prefer=new Dimension(15,2);
    pnlSaveThisAlert.setPreferredSize(prefer);
    pnlSaveThisAlert.setMaximumSize(prefer);
    pnlSaveThisAlert.setMinimumSize(prefer);
    pnlSaveAlert.setPreferredSize(prefer);
    pnlSaveAlert.setMaximumSize(prefer);
    pnlSaveAlert.setMinimumSize(prefer);

    GridBug gb=new GridBug(blah);
    gb.fill=gb.VERTICAL;
    gb.weightXY(0).gridXY(0);
    gb.anchor=gb.WEST;
    gb.add(pnlSaveThisAlert);
    gb.weightx=1; //Last in line gets the 1 always
    gb.gridx++;
    gb.addX(pnlSaveAlert);

    return blah;
  }
  private void layoutStatusPanel() {
    Font font=lblRow.getFont().deriveFont(Font.BOLD);
    JLabel lblRowHi=new JLabel(),
           lblColHi=new JLabel();
    lblRowHi.setFont(font);
    lblRowHi.setText("Row:");
    lblRow.setFont(font);
    lblRow.setText("1");
    lblColHi.setFont(font);
    lblColHi.setText(" Col:");
    lblCol.setFont(font);
    lblCol.setText("1");
    lblMsg.setFont(font);
    lblMsgBad.setFont(font);
    lblMsgBad.setForeground(Color.RED);

    GridBug gb=new GridBug(pStatus);
    pStatus.setVisible(true);
    gb.gridXY(0)
      .weightXY(0,0)

      .insets(1, 0, 1, 3)
      .anchor(gb.WEST)
      .addX(lblRowHi)

      .insetLeft(2)
      .addX(lblRow)
      .insetLeft(3)
      .addX(lblColHi)

      .insetLeft(2)
      .addX(lblCol)

      .insets(3,0,3,9)
      .fill(gb.VERTICAL)
      .addX(makeSeparator())

      .insets(0,0,0,8)
      .addX(layoutCapsLock(font))

      .insetLeft(8)
      .addX(layoutEncryption(font))

      .weightX(1)
      .insetLeft(10)
      .addX(makeMsgPanel())
      ;
  }
  private Container layoutCapsLock(Font font) {
    JLabel lbl=new JLabel();
    lbl.setFont(font);
    lbl.setText("CAPS LOCK");
    lbl.setForeground(Color.RED);
    GridBug gb=new GridBug(pnlCapsLock);
    gb
      .gridXY(0)
      .weightXY(0)
      .addX(lbl)
      .weightX(1)
      .insets(3,0,3,9)
      .fill(gb.VERTICAL)
      .addX(makeSeparator());
    return pnlCapsLock;
  }
  private Container layoutEncryption(Font font) {
    JLabel lbl=new JLabel();
    lbl.setFont(font);
    lbl.setText("ENCRYPTED");
    lbl.setForeground(Color.MAGENTA);
    GridBug gb=new GridBug(pnlEncryption);
    return gb.gridXY(0)
      .weightXY(0)
      .addX(lbl)
      .weightX(1)
      .insetLeft(9)
      .insets(3,0,3,9)
      .fill(gb.VERTICAL)
      .addX(makeSeparator())
      .getContainer();
  }


  private JSeparator makeSeparator() {
    return new JSeparator(SwingConstants.VERTICAL);
  }
  private Container makeMsgPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0).weightXY(0);
    gb.addX(lblMsg).addX(lblMsgBad);
    return gb.container;
  }

}
