package org.tmotte.klonk.windows.popup;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Segment;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.Positioner;

public class FindAndReplace {

  //Display components:
  JDialog win;
  private MyTextArea mtaFind, mtaReplace;
  private JComponent contFind, contReplace;
  private JCheckBox chkReplace, chkCase, chkReplaceAll, chkConfirmReplace, chkRegex, chkRegexMultiline;
  private JButton btnFind, btnReverse, btnCancel;
  private JLabel lblFind;

  //Fonts:
  private Font fontBold, fontNormal;
  private FontOptions fontOptions;
  private final Setter<FontOptions> fontListener=new Setter<FontOptions>(){
    public void set(FontOptions fo){setFont(fo);}
  };

  //Other windows. Yes we technically violate our singleton sort-of-a-rule here, creating
  //extra instances of YesNoCancel
  private JFrame parentFrame;
  private YesNoCancel popupAskReplace, popupAskReplaceAll;

  //Internal state:
  private boolean everShown=false;
  private boolean skipReplace=false;
  private boolean initialized=false;

  //This is so we can send updates back to the main window ourselves, as well
  //as to an alert popup:
  private Setter<String> alerter;
  private StatusUpdate statusBar;

  //Note that these are transient (so to speak) and require single-threaded
  //behavior from the class.
  private MyTextArea target;
  private Finder finder=new Finder();

  //Injected for MyTextArea:
  private CurrentOS currentOS;



  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public FindAndReplace(
      JFrame parentFrame,
      Setter<String> alerter,
      StatusUpdate statusBar,
      CurrentOS currentOS,
      FontOptions fontOptions
    ) {
    this.parentFrame=parentFrame;
    this.statusBar=statusBar;
    this.alerter=alerter;
    this.currentOS=currentOS;
    this.fontOptions=fontOptions;
  }
  public Setter<FontOptions> getFontListener() {
    return fontListener;
  }
  public void doFind(MyTextArea target)    {doFind(target, false);}
  public void doReplace(MyTextArea target) {doFind(target, true);}
  public synchronized void repeatFindReplace(MyTextArea mta, boolean forwards) {
    init();
    this.target=mta;
    finder.lastError=null;
    skipReplace=true;
    if (everShown) {
      find(forwards);
      //This could technically happen after a failure where user slaps ESC
      //and doesn't fix their mistake, then does a repeat
      if (finder.lastError!=null){
        alerter.set(finder.lastError);
        show();
      }
    }
    else
      doFind(mta, forwards);
  }

  //////////////////////
  //                  //
  // PRIVATE METHODS: //
  //                  //
  //////////////////////

  private void doFind(MyTextArea mta, boolean replace) {
    init();
    this.target=mta;
    chkReplace.setSelected(replace);
    //Previous line does not trigger an event, so:
    enableReplace();
    mtaFind.requestFocusInWindow();
    //Do all the rectangle/point/dimension stuff:
    Positioner.set(parentFrame, win, everShown);
    everShown|=true;
    show();
    while (finder.lastError!=null) {
      alerter.set(finder.lastError);
      show();
    }
  }
  private void show() {
    finder.lastError=null;
    finder.reset();
    skipReplace=false;
    win.setVisible(true);
  }



  /////////////////////////
  // Find/Replace Logic: //
  /////////////////////////


  private void find(boolean forwards) {
    try {
      win.setVisible(false);
      boolean replaceOn=chkReplace.isSelected();
      boolean replaceAll=replaceOn && chkReplaceAll.isSelected();
      skipReplace&=replaceOn;
      String searchFor=mtaFind.getText();
      boolean foundOnce=false;
      if (replaceAll && !getAskReplaceAllWindow().show().isYes())
        return;
      boolean mustConfirmReplace=!replaceAll && chkConfirmReplace.isSelected();
      boolean findAgain=true;
      Caret caret=target.getCaret();
      int mark=caret.getMark(), dot=caret.getDot();
      int offset=((forwards ^ skipReplace) ^ !replaceOn)
         ?Math.min(mark, dot)
         :Math.max(mark, dot);
      while (findAgain) {
        findAgain=false;
        boolean found=finder
          .setDocument(target.getDocument(), offset)
          .setReplace(replaceOn, mtaReplace.getText())
          .find(
            searchFor, forwards, chkCase.isSelected(),
            chkRegex.isSelected(), chkRegexMultiline.isSelected()
          );
        if (!found) {
          if (!foundOnce) statusBar.showBad("Not found");
          return;
        }
        foundOnce|=true;
        int pos=finder.getStart(), endPos=finder.getEnd();
        if (forwards) {
          pos+=offset;
          endPos+=offset;
        }
        if (!replaceAll) {
          target.scrollCaretToMiddle(pos, endPos);
          target.setCaretPosition(pos);
          target.moveCaretPosition(endPos);
        }
        statusBar.show("Found at file position: "+(pos));
        if (replaceOn)
          if (!mustConfirmReplace  || confirmReplace(pos, endPos)) {
            String replacement=finder.replaceResult;
            target.betterReplaceRange(replacement, pos, endPos);
            statusBar.show("Replaced");
            if (replaceAll)
              offset=pos + (forwards ?replacement.length()  :0);
            else
            if (!forwards)
              target.setCaretPosition(pos);//Otherwise next replace will search our replacement
            findAgain=replaceAll;
          }
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }


  private boolean confirmReplace(int startPos, int endPos) throws Exception {
    YesNoCancel asker=getAskReplaceWindow();
    Point caretPoint=target.getVisualCaretPosition();
    Rectangle dim =parentFrame.getBounds();
    int tooLow=dim.y+dim.height,
        tooRight=dim.x+dim.width;
    int top, left;
    {
      int spaceTop=80,
          cy=caretPoint.y,
          askHeight=asker.getHeight();
      top=cy+askHeight+spaceTop<tooLow
        ?cy+spaceTop
        :cy-(spaceTop+askHeight);
    }
    {
      int spaceLeft=80,
          cx=caretPoint.x,
          askWidth=asker.getWidth();
      left=cx+askWidth+spaceLeft<tooRight
        ?cx+spaceLeft
        :cx-(spaceLeft+askWidth);
    }
    if (left<dim.x) left=dim.x;
    if (top <dim.y) top=dim.y;
    return asker.show(left, top).isYes();
  }



  //////////////////////////////////////////
  //                                      //
  // CREATE/LAYOUT/LISTEN FROM HERE DOWN: //
  //                                      //
  //////////////////////////////////////////

  private void init() {
    if (!initialized) {
      create();
      layout();
      listen();
      initialized=true;
    }
  }

  private void create() {
    fontNormal=new JLabel().getFont();
    fontBold=fontNormal.deriveFont(Font.BOLD);
    win=new JDialog(parentFrame, true);
    win.setTitle("Find & Replace");

    //Find label & text box:
    mtaFind=makeTextArea();
    lblFind=new JLabel("Find:");
    lblFind.setFont(fontBold);
    lblFind.setLabelFor(mtaFind);
    contFind=mtaFind.makeVerticalScrollable();

    //Replace checkbox & text area
    chkReplace =new JCheckBox("Replace with:");
    chkReplace.setFont(fontBold);
    mtaReplace=makeTextArea();
    contReplace=mtaReplace.makeVerticalScrollable();

    //Option checkboxes:
    chkCase=new JCheckBox("Case sensitive");
    chkReplaceAll=new JCheckBox("Replace all");
    chkConfirmReplace=new JCheckBox("Confirm replace");
    chkConfirmReplace.setSelected(true);
    chkRegex=new JCheckBox("Regular expression");
    chkRegex.setSelected(false);
    chkRegexMultiline=new JCheckBox("Multi-line + Dot-all");
    chkRegexMultiline.setSelected(true);


    //Bottom buttons:
    btnFind=new JButton();
    btnFind.setText("Find");
    btnFind.setFont(fontBold);
    btnReverse=new JButton();
    btnReverse.setText("Find Reverse");
    btnReverse.setFont(fontBold);
    btnCancel=new JButton();
    btnCancel.setText("Cancel");

    lblFind.setDisplayedMnemonic( KeyEvent.VK_D);
    chkReplace.setMnemonic(       KeyEvent.VK_R);
    chkCase.setMnemonic(          KeyEvent.VK_A);
    chkReplaceAll.setMnemonic(    KeyEvent.VK_P);
    chkConfirmReplace.setMnemonic(KeyEvent.VK_M);
    chkRegex.setMnemonic(         KeyEvent.VK_X);
    btnFind.setMnemonic(          KeyEvent.VK_F);
    btnReverse.setMnemonic(       KeyEvent.VK_V);
    btnCancel.setMnemonic(        KeyEvent.VK_C);

  }
  private MyTextArea makeTextArea() {
    MyTextArea mta=new MyTextArea(currentOS);
    mta.setRows(3);//This doesn't work right because we set the font different.
    mta.setLineWrap(false);
    mta.setWrapStyleWord(false);
    return mta;
  }
  private YesNoCancel getAskReplaceWindow() {
    if (popupAskReplace==null) {
      popupAskReplace=new YesNoCancel(parentFrame, currentOS, false);
      popupAskReplace.setMessage("Replace selection?");
      popupAskReplace.setupForFindReplace();
    }
    return popupAskReplace;
  }
  private YesNoCancel getAskReplaceAllWindow() {
    if (popupAskReplaceAll==null) {
      popupAskReplaceAll=new YesNoCancel(parentFrame, currentOS, false);
      popupAskReplaceAll.setMessage("Replace all now?");
    }
    return popupAskReplaceAll;
  }

  /////////////
  // LAYOUT: //
  /////////////

  private void layout() {
    GridBug gb=new GridBug(win);
    gb.anchor=gb.WEST;
    gb.insets.right=2;
    gb.insets.top=5;

    //Find label:
    gb.insets.left=4;
    gb.fill=gb.NONE;
    gb.gridXY(0);
    gb.weightXY(1, 0);
    gb.add(lblFind);

    //Find textbox:
    gb.insets.left=0;
    gb.insets.right=0;
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.weightXY(1, 0.5);
    gb.addY(contFind);

    //Replace checkbox;
    gb.insets.left=0;
    gb.insets.right=2;
    gb.insets.top=10;
    gb.fill=gb.NONE;
    gb.weightXY(0);
    gb.addY(chkReplace);

    //Replace textbox:
    gb.insets.left=0;
    gb.insets.right=0;
    gb.insets.top=0;
    gb.fill=gb.BOTH;
    gb.weightXY(1, 0.5);
    gb.addY(contReplace);

    //Buttons and otherwise:
    gb.insets.top=5;
    gb.weighty=0;
    gb.insets.bottom=5;
    gb.addY(getBottomPanel());
    win.pack();

    enableReplace();
    setFont();
  }
  private Container getBottomPanel() {
    GridBug gb=new GridBug(new JPanel());

    //Checkboxes:
    gb.gridXY(0).weightXY(0);
    gb.add(getOptionsPanel());

    //Separator:
    JSeparator sep=new JSeparator(SwingConstants.VERTICAL);
    sep.setMinimumSize(new Dimension(2,10));
    gb.fill=gb.VERTICAL;
    gb.weighty=1;
    gb.addX(sep);

    //Buttons:
    gb.weighty=0;
    gb.weightx=1;
    gb.fill=gb.HORIZONTAL;
    gb.addX(getButtonPanel());
    return gb.container;
  }
  private Container getOptionsPanel() {
    GridBug gb=new GridBug(new JPanel());
    gb.insets.left=5; gb.insets.right=5;
    gb.anchor=gb.NORTHWEST;
    gb.gridXY(0).weightXY(0);
    gb.add(chkCase);
    gb.insets.top=-3;
    gb.addY(chkRegex);
    gb.insets.left+=12;
    gb.addY(chkRegexMultiline);
    chkRegexMultiline.setVisible(false);
    gb.insets.left-=12;
    gb.addY(chkConfirmReplace);
    gb.addY(chkReplaceAll);
    return gb.container;
  }
  private Container getButtonPanel() {
    JPanel jp=new JPanel();
    GridBug gb=new GridBug(jp);
    gb.anchor=gb.WEST;
    gb.gridXY(0).weightXY(0);
    gb.insets.left=10;
    gb.add(btnFind);
    gb.addX(btnReverse);
    gb.insets.right=10;
    gb.weightx=1;
    gb.addX(btnCancel);
    return gb.container;
  }

  private void setFont(FontOptions f) {
    this.fontOptions=f;
    setFont();
  }
  private void setFont() {
    FontOptions f=fontOptions;
    if (mtaFind!=null && mtaReplace!=null) {
      mtaFind.setFont(f.getFont());
      mtaFind.setForeground(f.getColor());
      mtaFind.setBackground(f.getBackgroundColor());
      mtaFind.setCaretColor(f.getCaretColor());

      mtaReplace.setFont(f.getFont());
      mtaReplace.setForeground(f.getColor());
      mtaReplace.setBackground(f.getBackgroundColor());
      mtaReplace.setCaretColor(f.getCaretColor());

      //Am doing this because otherwise it doesn't adjust to
      //our desired setting of rows elsewhere because we set
      //font AFTER instantiation.
      win.pack();
    }
  }


  /////////////
  // EVENTS: //
  /////////////

  private void listen() {
    addCheckBoxListeners(chkReplace, chkReplaceAll, chkCase, chkRegex, chkRegexMultiline);
    doButtonEvents(btnFind,     buttonListener, KeyMapper.key(KeyEvent.VK_F3));
    doButtonEvents(btnFind,     buttonListener, KeyMapper.key(KeyEvent.VK_ENTER));
    doButtonEvents(btnReverse,  buttonListener, KeyMapper.key(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
    KeyMapper.easyCancel(btnCancel, buttonListener);
    mtaFind.addFocusListener(new FocusAdapter(){
      public void focusGained(FocusEvent fe) {
        mtaFind.selectAll();
      }
    });
    mtaReplace.addFocusListener(new FocusAdapter(){
      public void focusGained(FocusEvent fe) {
        mtaReplace.selectAll();
      }
    });
    mtaFind.addKeyListener(textAreaListener);
    mtaReplace.addKeyListener(textAreaListener);
  }
  private void doButtonEvents(JButton butt, Action action, KeyStroke key) {
    butt.addActionListener(action);
    KeyMapper.accel(butt, action, key);
  }

  private void addCheckBoxListeners(JCheckBox... chks) {
    ActionListener chkListener=new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        JCheckBox jcb=(JCheckBox)event.getSource();
        if (jcb==chkCase || jcb==chkReplaceAll || jcb==chkRegex) {
          if (jcb.isSelected())
            jcb.setFont(fontBold);
          else
            jcb.setFont(fontNormal);
        }
        if (jcb==chkRegex) {
          chkRegexMultiline.setVisible(chkRegex.isSelected());
        }
        if (jcb==chkReplace) {
          enableReplace();
          if (chkReplace.isSelected())
            mtaReplace.requestFocusInWindow();
        }
        if (jcb==chkReplaceAll) {
          enableReplace();
        }
      }
    };
    for (JCheckBox jcb: chks)
      jcb.addActionListener(chkListener);
  }

  /** Button clicks: */
  private Action buttonListener=new AbstractAction() {
    public void actionPerformed(ActionEvent event) {
      Object o=event.getSource();
      if (o==btnFind)
        find(true);
      else
      if (o==btnReverse)
        find(false);
      else
      if (o==btnCancel)
        win.setVisible(false);
    }
  };
  /** Listening to the textareas for tab & enter keys: */
  private KeyAdapter textAreaListener=new KeyAdapter() {
    public void keyPressed(KeyEvent e){
      final int code=e.getKeyCode();
      if (code==e.VK_TAB) {
        int mods=e.getModifiersEx();
        if (KeyMapper.ctrlPressed(mods))
          ((MyTextArea)e.getSource()).replaceSelection("	");
        else
        if (KeyMapper.shiftPressed(mods)){
          if (e.getSource()==mtaFind)
            btnCancel.requestFocusInWindow();
          else
            chkReplace.requestFocusInWindow();
        }
        else
        if (e.getSource()==mtaFind)
          chkReplace.requestFocusInWindow();
        else
          chkCase.requestFocusInWindow();
        e.consume();
      }
      else
      if (code==KeyEvent.VK_ENTER) {
        int mods=e.getModifiersEx();
        if (KeyMapper.ctrlPressed(mods) || KeyMapper.shiftPressed(mods) || KeyMapper.altPressed(mods))
          ((MyTextArea)e.getSource()).replaceSelection("\n");
        else
          find(true);
        e.consume();
      }
    }
  };
  private void enableReplace() {
    boolean replaceOn=chkReplace.isSelected();
    mtaReplace.setEnabled(replaceOn);
    contReplace.setEnabled(replaceOn);
    chkReplaceAll.setEnabled(replaceOn);
    chkConfirmReplace.setEnabled(replaceOn && !chkReplaceAll.isSelected());
  }


}