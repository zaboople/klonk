package org.tmotte.klonk.windows;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.awt.Font;
import java.awt.GraphicsEnvironment;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.net.URL;
import javax.swing.ImageIcon;
import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenuBar;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;

public class MainLayout { 

  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  //Core stuff:
  private JFrame frame;
  private Fail failer;
  private Doer appCloseListener;
  
  //Main editor window components:
  private JLabel lblRow=new JLabel(), 
                 lblCol=new JLabel(), 
                 lblMsg=new JLabel(),
                 lblMsgBad=new JLabel();
  private JPanel pnlEditor=new JPanel(), 
                 pnlSaveThisAlert=new JPanel(),
                 pnlSaveAlert=new JPanel(),
                 pnlCapsLock=new JPanel(),
                 pStatus=new JPanel();

  //Other items:
  private GridBug editorGB=new GridBug(pnlEditor);
  private Color noChangeColor;
  private boolean hasStatus=false;
  

  public MainLayout(JFrame frame, Doer appCloseListener){
    this.frame=frame;
    doEvents();
    layout();
    this.appCloseListener=appCloseListener;
  }
  public void show(Rectangle rect, boolean maximized) {
    //Set location:
    Toolkit toolkit =  Toolkit.getDefaultToolkit();
    Dimension dim = toolkit.getScreenSize();    
    rect.x=Math.max(0, Math.min(rect.x, dim.width-10));
    rect.y=Math.max(0, Math.min(rect.y, dim.height-10));
    rect.width=Math.min(rect.width, dim.width-rect.x);
    rect.height=Math.min(rect.height, dim.height-rect.y);
    frame.setBounds(rect);
    if (maximized)
      frame.setExtendedState(frame.MAXIMIZED_BOTH);
    frame.setVisible(true);
  }
  
  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public void setEditor(Component c, String title) {
    pnlEditor.removeAll();
    editorGB.add(c);
    frame.paintAll(frame.getGraphics());
    showTitle(title);
  }
  public void dispose() {
    frame.dispose();
  }
  public Rectangle getMainWindowBounds() {
    return frame.getBounds();
  }
  public boolean isMaximized() {
    return (frame.getExtendedState() & frame.MAXIMIZED_BOTH) == frame.MAXIMIZED_BOTH;
  }
  public JFrame getFrame() {
    return frame;
  }
  public void toFront() {
    frame.toFront();
  }

  // STATUS INFO: //

  public void showTitle(String title) {
    frame.setTitle("Klonk: "+title);
  }
  public void showChangeThis(boolean chg) {
    pnlSaveThisAlert.setBackground(chg ?Color.RED :noChangeColor);
  }
  public void showChangeAny(boolean chg) {
    pnlSaveAlert.setBackground(chg ?Color.BLUE :noChangeColor);
  }
  
  public void showStatus(String msg) {
    showStatus(msg, false);
  }
  public void showStatus(String msg, boolean isBad) {
    lblMsg.setVisible(!isBad);
    lblMsgBad.setVisible(isBad);
    if (isBad)
      lblMsgBad.setText(msg);
    else
      lblMsg.setText(msg);
    pStatus.paintAll(pStatus.getGraphics());//Because during long save/load operations it doesn't update
    hasStatus=true;
  }
  public void showNoStatus() {
    if (hasStatus){
      lblMsg.setText("");
      lblMsg.setVisible(false);
      lblMsgBad.setVisible(false);
      hasStatus=false;
    }
  }
  public void showRowColumn(int row, int col) {
    lblRow.setText(String.valueOf(row));
    lblCol.setText(String.valueOf(col));
  }
  public void showCapsLock(boolean visible) {
    pnlCapsLock.setVisible(visible);
  }
    

  //////////////////////
  // PRIVATE METHODS: //
  //////////////////////

  private void doEvents() {
    frame.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);  
    frame.addWindowListener(new WindowAdapter() {
      public void windowClosing(WindowEvent e){
        appCloseListener.doIt();
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
    pnlEditor.setBackground(Color.GREEN);

    //Status panel:
    gb.fill=gb.HORIZONTAL;
    gb.weightXY(1,0);
    layoutStatusPanel();
    gb.addY(pStatus);
    pnlCapsLock.setVisible(false);

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
    gb.gridXY(0).weightXY(0,0);
    gb.insets.top=1;
    gb.insets.bottom=1;
    gb.anchor=gb.WEST;
    
    gb.insets.left=3;
    gb.addX(lblRowHi);
    
    gb.insets.left=2;
    gb.addX(lblRow);
    
    gb.insets.left=3;
    gb.addX(lblColHi);
    
    gb.insets.left=2;
    gb.addX(lblCol);
    
    gb.insets.left=10;
    gb.addX(makeSeparator());
  
    gb.insets.left=8;
    gb.addX(layoutCapsLock(font));

    gb.weightx=1;
    gb.insets.left=10;
    gb.addX(makeMsgPanel());
    
  }
  private Container layoutCapsLock(Font font) {
    JLabel lblCapsLock=new JLabel();
    lblCapsLock.setFont(font);
    lblCapsLock.setText("CAPS LOCK");
    lblCapsLock.setForeground(Color.RED);

    GridBug gb=new GridBug(pnlCapsLock);
    gb.gridXY(0).weightXY(0);

    gb.addX(lblCapsLock);

    gb.weightx=1;
    gb.insets.left=9;
    gb.addX(makeSeparator());
    
    return pnlCapsLock;
  }
  
  
  private JSeparator makeSeparator() {
    JSeparator sep=new JSeparator(SwingConstants.VERTICAL);
    Dimension prefer=new Dimension(2,10);
    sep.setPreferredSize(prefer);
    sep.setMinimumSize(prefer);
    sep.setMaximumSize(prefer);
    return sep;
  }
  private Container makeMsgPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.gridXY(0).weightXY(0);
    gb.addX(lblMsg).addX(lblMsgBad);
    return gb.container;
  }

  
}
