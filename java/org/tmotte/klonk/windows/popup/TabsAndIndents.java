package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.BorderFactory;
import javax.swing.ButtonGroup;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import org.tmotte.common.io.Loader;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.TabAndIndentOptions;
import org.tmotte.klonk.config.Kontext;

class TabsAndIndents {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  private JFrame parentFrame;
  private JDialog win;
  private boolean ok=false;
  
  private JRadioButton 
    jrbThisTabs=new JRadioButton("Tabs"), 
    jrbThisSpaces=new JRadioButton("Spaces"),
    jrbDefTabs=new JRadioButton("Tabs"), 
    jrbDefSpaces=new JRadioButton("Spaces");
  private JSpinner jspSpacesSize=new JSpinner(new SpinnerNumberModel(1,1,99,1));

  private JSpinner jspTabSize=new JSpinner(new SpinnerNumberModel(0,0,99,1));

  private JCheckBox chkIndentOnHardReturn=new JCheckBox(
      "<html><body>Auto-indent when new line is entered</body></html>" 
    );
  private JRadioButton
    jrbTabIndentsLine=new JRadioButton(
      "<html><body><b>Tab</b> key indents line <br>(use <b>Ctrl</b> - <b>Tab</b> to insert tab)</body></html>"     
    ),
    jrbTabIsTab=new JRadioButton(
        "<html><body><b>Tab</b> key inserts tab character</body></html>" 
    );
  
  private JButton btnOK, btnCancel;
    
  private TabAndIndentOptions options;
  
  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public TabsAndIndents(JFrame parentFrame, Fail fail) {
    this.parentFrame=parentFrame;
    create();
    layout(); 
    listen();
  }
  public boolean show(TabAndIndentOptions options) {
    this.options=options;
    ok=false;
    
    //Set values from input:
    jrbThisTabs.setSelected(options.indentionMode==options.INDENT_TABS);
    jrbThisSpaces.setSelected(options.indentionMode==options.INDENT_SPACES);
    jrbDefTabs.setSelected(options.indentionModeDefault==options.INDENT_TABS);
    jrbDefSpaces.setSelected(options.indentionModeDefault==options.INDENT_SPACES);
    jspSpacesSize.setValue(options.indentSpacesSize);
    jspTabSize.setValue(options.tabSize);
    chkIndentOnHardReturn.setSelected(options.indentOnHardReturn);
    jrbTabIndentsLine.setSelected(options.tabIndentsLine);
    jrbTabIsTab.setSelected(!jrbTabIndentsLine.isSelected());  
    
    //Display:
    Point pt=parentFrame.getLocation();
    win.pack();
    win.setLocation(pt.x+20, pt.y+20);
    win.setVisible(true);
    win.paintAll(win.getGraphics());
    win.toFront();
    return ok;
  }
  
  ////////////////////////
  //                    //
  //  PRIVATE METHODS:  //
  //                    //
  ////////////////////////

  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    if (action) {
      options.indentOnHardReturn=chkIndentOnHardReturn.isSelected();
      options.tabIndentsLine    =jrbTabIndentsLine.isSelected();
      options.tabSize         =Integer.parseInt(jspTabSize.getValue().toString());
      options.indentSpacesSize=Integer.parseInt(jspSpacesSize.getValue().toString());
      options.indentionMode=jrbThisTabs.isSelected() 
        ?options.INDENT_TABS
        :options.INDENT_SPACES;
      options.indentionModeDefault=jrbDefTabs.isSelected() 
        ?options.INDENT_TABS
        :options.INDENT_SPACES;
    }
    win.setVisible(false);  
  }
  private JPanel getBorder(JPanel jp) {
    jp.setBorder(BorderFactory.createBevelBorder(javax.swing.border.BevelBorder.RAISED));
    return jp;
  }
  
  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////
  
  private void create() {
    win=new JDialog(parentFrame, true);
    //win.setResizable(false);
    win.setTitle("Tabs & indents");
    btnOK    =new JButton("OK");
    btnOK.setMnemonic(KeyEvent.VK_K);
    btnCancel=new JButton("Cancel");
    btnCancel.setMnemonic(KeyEvent.VK_C);
  }
  
  
  private void layout() {
    GridBug gb=new GridBug(win);
    gb.gridy=0;
    gb.weightXY(0);
    gb.fill=gb.HORIZONTAL;
    gb.anchor=gb.NORTHWEST;
    gb.add(getTabSizePanel());
    makeSeparator(gb);
    gb.addY(getIndentionModePanel());
    makeSeparator(gb);
    gb.addY(getAutoIndentPanel());
    makeSeparator(gb);
    gb.addY(getButtonPanel());
  }
  private void makeSeparator(GridBug gb) {
    gb.insets.left=5; gb.insets.right=5;
    JSeparator j=new JSeparator(JSeparator.HORIZONTAL);
    gb.addY(j);
    gb.insets.left=0; gb.insets.right=0;
  }
  private JPanel getIndentionModePanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightx=1;
    gb.anchor=gb.WEST;
    gb.gridXY(0);

    gb.insets.top=5;
    gb.insets.bottom=2;
    gb.insets.left=5;
    gb.insets.right=5;

    //Add label:
    gb.gridwidth=2;
    JLabel label=new JLabel("<html><b>Indention: Tabs or Spaces</b></html>");
    gb.add(label);
    
    //Add buttons:
    gb.gridwidth=1;
    gb.insets.top=2;
    gb.addY(getIndentionRadiosPanel(), getIndentionSpinPanel());
    return jp;
  }
  private Container getIndentionRadiosPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.anchor=gb.WEST;
    gb.weightx=1.0;
    gb.gridy=gb.gridx=0;
    gb.add(getIndentionRadiosPanel("This file:", jrbThisTabs, jrbThisSpaces));
    gb.insets.left=7;
    gb.addX(getIndentionRadiosPanel("Default:" , jrbDefTabs,  jrbDefSpaces));
    return gb.container;
  }
  private Container getIndentionRadiosPanel(String label, JRadioButton one, JRadioButton two){
    GridBug gb=new GridBug(new JPanel());
    gb.anchor=gb.WEST;
    gb.weightx=1.0;
    gb.gridy=gb.gridx=0;
    gb.add(new JLabel(label));
    gb.addY(one);
    gb.addY(two);
    return gb.container;
  }
  private JPanel getIndentionSpinPanel() {
    JPanel allPanel=new JPanel();
    GridBug allBug=new GridBug(allPanel);
    allBug.anchor=allBug.WEST;
    allBug.weightx=1;
    allBug.gridx=allBug.gridy=0;
    allBug.insets.top=15;
    allBug.insets.left=allBug.insets.right=5;
    {
      JPanel panel=new JPanel();
      GridBug gb=new GridBug(panel);
      gb.anchor=gb.WEST;
      gb.weightx=1;
      gb.gridx=0;
      gb.gridy=0;
      gb.add(new JLabel("Use "));
      gb.addX(jspSpacesSize);
      gb.addX(new JLabel(" spaces when"));
      allBug.add(panel);
    }
    allBug.insets.top=0;
    allBug.insets.bottom=5;
    allBug.addY(new JLabel("indenting with spaces"));
    return allPanel;
  }
  private JPanel getTabSizePanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.insets.left=gb.insets.right=5;
    gb.insets.top=5;
    gb.weightx=1.0;
    gb.anchor=gb.WEST;
    gb.gridXY(0);
    gb.add(new JLabel("<html><b>Tab Size</b></html>"));
    gb.insets.bottom=5;
    gb.addY(getTabSizer());
    return jp;
  }
  private JPanel getTabSizer() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.weightx=1.0;
    gb.anchor=gb.WEST;
    gb.gridx=1;
    gb.add(new JLabel("Tabs appear as "));
    gb.addX(jspTabSize);
    gb.addX(new JLabel(" spaces"));
    return jp;
  }
  private Container getAutoIndentPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.weightx=1.0;
    gb.anchor=gb.WEST;
    gb.gridXY(0);
    gb.insets.left=gb.insets.right=5;

    gb.insets.top=5;
    gb.gridwidth=2;
    gb.add(new JLabel("<html><b>Automatic Indention</b></html>"));
    gb.gridwidth=1;
    gb.addY(chkIndentOnHardReturn);
    gb.anchor=gb.NORTHWEST;
    gb.insets.top=10;
    gb.setX(0)
      .addY(jrbTabIndentsLine);
    gb.insets.top=0;
    gb.insets.bottom=5;
    gb.anchor=gb.WEST;
    gb.setX(0)
      .addY(jrbTabIsTab);
    return gb.container;
  }
  private JPanel getButtonPanel() {
    JPanel panel=new JPanel();
    GridBug gb=new GridBug(panel);
    Insets insets=gb.insets;
    insets.top=5;
    insets.bottom=5;
    insets.left=5;
    insets.right=5;

    gb.gridx=0;
    gb.add(btnOK);
    gb.addX(btnCancel);
    return panel;
  }


  private void listen() {
    Action okAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(true);}
    };
    btnOK.addActionListener(okAction);
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));

    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnCancel, cancelAction, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));

    Radios.doUpDownArrows(
      Radios.create(jrbThisTabs,        jrbThisSpaces),
      Radios.create(jrbDefTabs,         jrbDefSpaces),
      Radios.create(jrbTabIndentsLine,  jrbTabIsTab)
    );
  }
  
  /////////////
  /// TEST: ///
  /////////////
  
  public static void main(String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try {
          Kontext context=Kontext.getForUnitTest();
          TabsAndIndents tai=new TabsAndIndents(
            context.mainFrame, context.fail
          );
          TabAndIndentOptions ti=new TabAndIndentOptions();
          ti.indentionMode=ti.INDENT_TABS;
          ti.indentionModeDefault=ti.INDENT_SPACES;
          tai.show(ti);
          context.mainFrame.dispose();
        } catch (Exception e) {
          e.printStackTrace();
        }
      }
    });  
  }
  
}