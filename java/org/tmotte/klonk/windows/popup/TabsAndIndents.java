package org.tmotte.klonk.windows.popup;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
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
import javax.swing.SpinnerNumberModel;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.Radios;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;

public class TabsAndIndents {

  /////////////////////////
  // INSTANCE VARIABLES: //
  /////////////////////////

  // DI:
  private final PopupInfo pInfo;
  private FontOptions fontOptions;

  // State:
  private boolean ok=false;
  private boolean initialized=false;
  private TabAndIndentOptions options;

  // Controls:
  private JDialog win;
  private JRadioButton jrbThisTabs, jrbThisSpaces, jrbDefTabs, jrbDefSpaces;
  private JCheckBox chkIndentOnHardReturn, chkInferTabIndents;
  private JSpinner jspTabSize;
  private JRadioButton jrbTabIndentsLine, jrbTabIsTab;
  private JRadioButton jrbSpacesSizeCustom, jrbSpacesSizeMatch;
  private JSpinner jspSpacesSize;
  private JLabel lblSpacesSize2;
  private JButton btnOK, btnCancel;

  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public TabsAndIndents(PopupInfo pInfo, FontOptions fontOptions) {
    this.pInfo=pInfo;
    this.fontOptions=fontOptions;
    pInfo.addFontListener(fo -> setFont(fo));
  }

  public boolean show(TabAndIndentOptions options) {
    init();
    this.options=options;
    ok=false;

    //Set values from input:
    jspTabSize.setValue(options.tabSize);

    jrbThisTabs.setSelected(options.indentionMode==options.INDENT_TABS);
    jrbThisSpaces.setSelected(options.indentionMode==options.INDENT_SPACES);
    jrbDefTabs.setSelected(options.indentionModeDefault==options.INDENT_TABS);
    jrbDefSpaces.setSelected(options.indentionModeDefault==options.INDENT_SPACES);

    jrbSpacesSizeCustom.setSelected(!options.indentSpacesSizeMatchTabs);
    jrbSpacesSizeMatch.setSelected(options.indentSpacesSizeMatchTabs);
    jspSpacesSize.setValue(options.indentSpacesSize > 0 ?options.indentSpacesSize :1);
    jspSpacesSize.setEnabled(!options.indentSpacesSizeMatchTabs);

    chkIndentOnHardReturn.setSelected(options.indentOnHardReturn);
    chkInferTabIndents.setSelected(options.inferTabIndents);
    jrbTabIndentsLine.setSelected(options.tabIndentsLine);
    jrbTabIsTab.setSelected(!jrbTabIndentsLine.isSelected());

    //Display:
    Point pt=pInfo.parentFrame.getLocation();
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

  private void setFont(FontOptions fo) {
    this.fontOptions=fo;
    if (win!=null){
      fontOptions.getControlsFont().set(win);
      win.pack();
    }
  }

  /** action=true means OK, false means Cancel */
  private void click(boolean action) {
    ok=action;
    if (action) {
      options.indentOnHardReturn=chkIndentOnHardReturn.isSelected();
      options.inferTabIndents=chkInferTabIndents.isSelected();
      options.tabIndentsLine    =jrbTabIndentsLine.isSelected();
      options.tabSize         =Integer.parseInt(jspTabSize.getValue().toString());
      options.indentSpacesSizeMatchTabs=jrbSpacesSizeMatch.isSelected();
      options.indentSpacesSize=options.indentSpacesSizeMatchTabs
        ?options.tabSize
        :Integer.parseInt(jspSpacesSize.getValue().toString());
      options.indentionMode=jrbThisTabs.isSelected()
        ?options.INDENT_TABS
        :options.INDENT_SPACES;
      options.indentionModeDefault=jrbDefTabs.isSelected()
        ?options.INDENT_TABS
        :options.INDENT_SPACES;
    }
    win.setVisible(false);
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////

  private void init() {
    if (!initialized) {
      create();
      layout();
      listen();
      initialized=true;
    }
  }
  private void create() {
    jrbThisTabs  =new JRadioButton("Tabs");
    jrbThisSpaces=new JRadioButton("Spaces");
    jrbDefTabs   =new JRadioButton("Tabs");
    jrbDefSpaces =new JRadioButton("Spaces");
    jspSpacesSize=new JSpinner(new SpinnerNumberModel(1,1,99,1));
    jspTabSize   =new JSpinner(new SpinnerNumberModel(0,0,99,1));

    chkIndentOnHardReturn=new JCheckBox(
      "<html><body>Auto-indent when new line is entered</body></html>"
    );
    chkInferTabIndents=new JCheckBox(
      "<html><body>Infer tab-indents from file</body></html>"
    );
    jrbTabIndentsLine=new JRadioButton(
      "<html><body><b>Tab</b> key indents line <br>(use <b>Ctrl</b> - <b>Tab</b> to insert tab)</body></html>"
    );
    jrbTabIsTab=new JRadioButton(
        "<html><body><b>Tab</b> key inserts tab character</body></html>"
    );
    jrbSpacesSizeMatch=new JRadioButton("Match \"Tab Size\" (see above)");
    jrbSpacesSizeCustom=new JRadioButton("Use ");

    win=new JDialog(pInfo.parentFrame, true);
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
    gb.addY(getTabsOrSpacesPanel());
    makeSeparator(gb);
    gb.addY(getAutoIndentPanel());
    makeSeparator(gb);
    gb.addY(getButtonPanel());

    setFont(fontOptions);
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

  private void makeSeparator(GridBug gb) {
    gb.insets.left=5; gb.insets.right=5;
    JSeparator j=new JSeparator(JSeparator.HORIZONTAL);
    gb.addY(j);
    gb.insets.left=0; gb.insets.right=0;
  }

  private JPanel getTabsOrSpacesPanel() {
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

    //Add inputs:
    gb.gridwidth=1;
    gb.insets.top=2;
    gb.addY(getTabsOrSpacesRadiosPanel());
    gb.insets.top=2;
    gb.addY(
      new JLabel("<html><b>Indenting with spaces:</b></html>"),
      getSpacesSizePanel()
    );
    return jp;
  }
  private Container getTabsOrSpacesRadiosPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.anchor=gb.NORTHWEST;
    gb.weightx=1.0;
    gb.gridy=gb.gridx=0;
    gb.add(getTabsOrSpacesRadiosPanel("This file:", jrbThisTabs, jrbThisSpaces, new JPanel()));
    gb.insets.left=7;
    gb.addX(getTabsOrSpacesRadiosPanel("Default:" , jrbDefTabs,  jrbDefSpaces, chkInferTabIndents));
    return gb.container;
  }
  private Container getTabsOrSpacesRadiosPanel(String label, JRadioButton one, JRadioButton two, JComponent three){
    GridBug gb=new GridBug(new JPanel());
    gb.anchor=gb.NORTHWEST;
    gb.weightx=1.0;
    gb.gridy=gb.gridx=0;
    gb.add(new JLabel(label));
    gb.addY(one);
    gb.addY(two);
    gb.addY(three);
    return gb.container;
  }
  private JPanel getSpacesSizePanel() {
    JPanel allPanel=new JPanel();
    GridBug allBug=new GridBug(allPanel);
    allBug.anchor=allBug.NORTHWEST;
    allBug.weightx=1;
    allBug.gridx=allBug.gridy=0;
    allBug.insets.top=0;
    allBug.insets.left=allBug.insets.right=5;
    allBug.insets.bottom=0;

    allBug.addY(jrbSpacesSizeMatch);
    {
      JPanel panel=new JPanel();
      GridBug gb=new GridBug(panel);
      gb.anchor=gb.WEST;
      gb.weightx=1;
      gb.gridx=0;
      gb.gridy=0;
      gb.add(jrbSpacesSizeCustom);
      gb.addX(jspSpacesSize);
      gb.addX(lblSpacesSize2=new JLabel(" spaces when indenting with spaces"));
      allBug.addY(panel);
    }
    return allPanel;
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

    // Enter will click OK from anywhere on the window:
    KeyMapper.accel(btnOK, okAction, KeyMapper.key(KeyEvent.VK_ENTER));

    Action cancelAction=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {click(false);}
    };
    btnCancel.addActionListener(cancelAction);
    KeyMapper.easyCancel(btnCancel, cancelAction);

    jrbSpacesSizeCustom.addChangeListener((javax.swing.event.ChangeEvent e)->{
        boolean custom=jrbSpacesSizeCustom.isSelected();
        jspSpacesSize.setEnabled(custom);
    });

    Radios.doUpDownArrows(
      Radios.create(jrbThisTabs,        jrbThisSpaces),
      Radios.create(jrbDefTabs,         jrbDefSpaces),
      Radios.create(jrbTabIndentsLine,  jrbTabIsTab),
      Radios.create(jrbSpacesSizeMatch, jrbSpacesSizeCustom)
    );
  }

  /////////////
  /// TEST: ///
  /////////////

  public static void main(final String[] args) throws Exception {
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        TabAndIndentOptions ti=new TabAndIndentOptions();
        ti.indentionMode=ti.INDENT_TABS;
        ti.indentionModeDefault=ti.INDENT_SPACES;
        PopupTestContext ptc=new PopupTestContext();
        new TabsAndIndents(ptc.getPopupInfo(), ptc.getFontOptions()).show(ti);
        System.out.println(ti);
      }
    });
  }

}