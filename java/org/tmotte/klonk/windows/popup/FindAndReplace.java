package org.tmotte.klonk.windows.popup;
import java.util.regex.PatternSyntaxException;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
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
import org.tmotte.common.swang.Alerter;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.GridBug;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.StatusNotifier;

class FindAndReplace {

  //Display components:
  JDialog win;
  MyTextArea mtaFind, mtaReplace;
  JComponent contFind, contReplace;
  JCheckBox chkReplace, chkCase, chkReplaceAll, chkConfirmReplace, chkRegex;
  JButton btnFind=new JButton(),
          btnReverse=new JButton(),
          btnCancel=new JButton();
  JLabel lblFind;
  
  //Other windows:
  JFrame parentFrame;
  Alerter alerter;
  YesNoCancel popupYesNo, popupYesNoCancel;
  
  //Blah:
  Fail fail;
  
  //Only false when window has never been shown:
  boolean everShown=false;
  //Other state
  boolean skipReplace=false;
  
  //This is so we can send updates back to the main window ourselves:
  StatusNotifier status;
  
  //Note that these are transient (so to speak) and require single-threaded
  //behavior from the class. 
  MyTextArea target;
  Finder finder=new Finder();



  /////////////////////
  // PUBLIC METHODS: //
  /////////////////////

  public FindAndReplace(JFrame parentFrame, Fail fail, Alerter alerter, StatusNotifier status) {
    this.parentFrame=parentFrame;
    this.status=status;
    this.fail=fail;
    this.alerter=alerter;
    create();
    layout();
    listen();
    enableReplace();
  }
  public void setFont(FontOptions f) {
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
  public void doFind(MyTextArea mta, boolean replace) {
    setupForShow(mta, replace);
    show();
    while (finder.lastError!=null) {
      alerter.show(finder.lastError);
      show();
    }
  }
  public synchronized void repeatFindReplace(MyTextArea mta, boolean forwards) {
    this.target=mta;
    finder.lastError=null;
    skipReplace=true;
    if (everShown) {
      find(forwards);
      //This could technically happen after a failure where user slaps ESC
      //and doesn't fix their mistake, then does a repeat
      if (finder.lastError!=null){
        alerter.show(finder.lastError);
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

  private void setupForShow(MyTextArea mta, boolean replace) {
    this.target=mta;
    chkReplace.setSelected(replace);
    //Previous line does not trigger an event, so:
    enableReplace();
    mtaFind.requestFocusInWindow();
    //Do all the rectangle/point/dimension stuff:
    Popups.position(parentFrame, win, everShown);
    everShown|=true;
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
      boolean replaceOn=chkReplace.isSelected(),
              replaceAll=chkReplaceAll.isSelected();
      skipReplace&=replaceOn;
      String searchFor=mtaFind.getText();
      boolean foundOnce=false;
      if (replaceAll && !getYesNoReplaceAllWindow().show().isYes())
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
          .find(searchFor, forwards, chkCase.isSelected(), chkRegex.isSelected());
        if (!found) {
          if (!foundOnce) status.showStatus("Not found");
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
        status.showStatus("Found at file position: "+(pos));
        if (replaceOn)
          if (!mustConfirmReplace  || confirmReplace(pos, endPos)) {
            String replacement=finder.replaceResult;
            target.betterReplaceRange(replacement, pos, endPos);
            status.showStatus("Replaced");
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
  
  private static class Finder {
    //Results:
    private int location=-1, locationEnd=-1;
    private String replaceResult;
    private String lastError;
    //Inputs held as instance variables for convenience:
    private Document doc;
    private int offset;
    private boolean replaceOn;
    private String replaceWith;
    //Pattern & matcher preserved for multiple invocations:
    private Pattern pattern;
    private Matcher matcher;

    public int getEnd(){return locationEnd;}
    public int getStart() {return location;}
    
    public void reset() {
      pattern=null;
      matcher=null;
    }
    public Finder setDocument(Document doc, int offset) {
      this.doc=doc;
      this.offset=offset;
      return this;
    }
    public Finder setReplace(boolean replaceOn, String replaceWith) {
      this.replaceOn=replaceOn;
      this.replaceWith=replaceWith;
      return this;
    }
    
    public boolean find(
        String searchFor, 
        boolean forwards, 
        boolean caseSensitive, 
        boolean regex
      ) {
      replaceResult=null;
      location=-1;
      locationEnd=-1;

      String searchIn;
      try {
        searchIn=forwards
          ?doc.getText(offset, doc.getLength()-offset)
          :doc.getText(0, offset);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
      if (regex)
        findRegex(  searchFor, searchIn, forwards, caseSensitive);
      else
        findRegular(searchFor, searchIn, forwards, caseSensitive);
      return location!=-1;
    }
    private void findRegular(String searchFor, String searchIn, boolean forwards, boolean caseSensitive) {
      if (caseSensitive) 
        location=forwards 
          ?searchIn.indexOf(searchFor)
          :searchIn.lastIndexOf(searchFor);
      else {
        //We search both low & up case because apparently
        //there are character sets where this is the only thing
        //that works. Turkish or something. Whatever.
        searchFor=searchFor.toLowerCase();
        String searchInLow=searchIn.toLowerCase();
        location=forwards ?searchInLow.indexOf(searchFor)
                          :searchInLow.lastIndexOf(searchFor);
        searchFor=searchFor.toUpperCase();
        String searchInHi=searchIn.toUpperCase();
        int loc =forwards ?searchInHi.indexOf(searchFor)
                          :searchInHi.lastIndexOf(searchFor);
        if (location==-1 || (loc!=-1 && loc<location))
          location=loc;
      }
      locationEnd=location==-1 ?-1 :location+searchFor.length();
      replaceResult=replaceWith;
    }
    private void findRegex(String searchFor, String searchIn, boolean forwards, boolean caseSensitive) {
      if (pattern==null){
        int flags=Pattern.MULTILINE|Pattern.DOTALL;
        if (!caseSensitive)
          flags|=Pattern.CASE_INSENSITIVE;
        try {
          pattern=Pattern.compile(searchFor, flags);
        } catch (PatternSyntaxException e) {
          lastError="Regex syntax is wrong: "+e.getMessage();
          return;            
        }
        matcher=pattern.matcher(searchIn);
      }
      else
        matcher.reset(searchIn);
      if (matcher.find()){
      
        //This is kooky but we have to go all the way
        //from start to end to get last match with regex.
        do {
          location=matcher.start();
          locationEnd=matcher.end();
        } while (!forwards && matcher.find());
        
        if (replaceOn) {
          
          //Follow thru on the kooky part above:
          if (!forwards)
            matcher.find(location);
            
          //Getting the replacement is dumb. We have no choice but to let it give
          //us all the crap we don't need so we can grab the good part at the end.
          StringBuffer sb=new StringBuffer();
          try {
            matcher.appendReplacement(sb, replaceWith);
          } catch (IllegalArgumentException e) {
            lastError="Replacement syntax is wrong (typically caused by $ characters):\n"
                      +e.getMessage();
            return;
          } catch (IndexOutOfBoundsException e) {
            lastError="Replacement syntax is wrong (typically caused by $ characters):\n"
                      +e.getMessage();
            return;
          }
          replaceResult=sb.substring(location);
        }
      }
    }
    
  }//end inner class
  
  
  private boolean confirmReplace(int startPos, int endPos) throws Exception {
    YesNoCancel asker=getYesNoWindow();
    Point caretPoint=target.getVisualCaretPosition();
    Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();    
    int top, left;
    {
      int spaceTop=80,
          cy=caretPoint.y,
          askHeight=asker.getHeight();
      top=cy+askHeight+spaceTop<dim.height
        ?cy+spaceTop
        :cy-(spaceTop+askHeight);
    }
    {
      int spaceLeft=80,
          cx=caretPoint.x,
          askWidth=asker.getWidth();
      left=cx+askWidth+spaceLeft<dim.width
        ?cx+spaceLeft
        :cx-(spaceLeft+askWidth);
    }
    if (top<0)  top=0;
    if (left<0) left=0;
    return asker.show(left, top).isYes();
  }



  //////////////////////////////////////////
  //                                      //
  // CREATE/LAYOUT/LISTEN FROM HERE DOWN: //
  //                                      //
  //////////////////////////////////////////

  private void create() {
    Font fontBold=new JLabel().getFont().deriveFont(Font.BOLD);
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
    

    //Bottom buttons:
    btnFind.setText("Find");
    btnFind.setFont(fontBold);
    btnReverse.setText("Find Reverse");
    btnReverse.setFont(fontBold);
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
    MyTextArea mta=new MyTextArea(); 
    mta.setRows(3);//This doesn't work right because we set the font different.
    mta.setLineWrap(false);
    mta.setWrapStyleWord(false);
    return mta;
  }
  private YesNoCancel getYesNoWindow() {
    if (popupYesNo==null) {
      popupYesNo=new YesNoCancel(parentFrame, false);
      popupYesNo.setMessage("Replace selection?");
      popupYesNo.setupForFindReplace();
    }
    return popupYesNo;
  }
  private YesNoCancel getYesNoReplaceAllWindow() {
    if (popupYesNoCancel==null) {
      popupYesNoCancel=new YesNoCancel(parentFrame, false);
      popupYesNoCancel.setMessage("Replace all now?");
    }
    return popupYesNoCancel;
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
  

  /////////////
  // EVENTS: //
  /////////////
  
  private void listen() {
    chkReplace.addActionListener(chkReplaceListener);
    chkReplaceAll.addActionListener(chkReplaceAllListener);
    doButtonEvents(btnFind,    buttonListener, KeyMapper.key(KeyEvent.VK_F3));
    doButtonEvents(btnFind,    buttonListener, KeyMapper.key(KeyEvent.VK_ENTER));
    doButtonEvents(btnReverse, buttonListener, KeyMapper.key(KeyEvent.VK_F3, InputEvent.SHIFT_DOWN_MASK));
    doButtonEvents(btnCancel,  buttonListener, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btnCancel,  buttonListener, KeyMapper.key(KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK));
    
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
  
  /** When replace is checked, enable disable stuff: */
  private ActionListener chkReplaceListener=new ActionListener() {
    public void actionPerformed(ActionEvent event) {
      enableReplace();
      if (chkReplace.isSelected())
        mtaReplace.requestFocusInWindow();
    }
  };
  /** When Replace All is checked: */
  private ActionListener chkReplaceAllListener=new ActionListener() {
    public void actionPerformed(ActionEvent event) {
      enableReplace();
    }
  };
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