package org.tmotte.klonk.edit;
import javax.swing.JScrollBar;
import java.awt.AWTKeyStroke;
import java.awt.Container;
import java.awt.Font;
import java.awt.event.ActionEvent;
import java.awt.event.InputEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.geom.Rectangle2D;
import java.awt.KeyboardFocusManager;
import java.awt.Point;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.JViewport;
import javax.swing.JMenuItem;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.Element;
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.common.swang.KeyMapper;

@SuppressWarnings("this-escape")
public class MyTextArea extends JTextArea {
  private static final long serialVersionUID = 1L;

  ////////////////////
  // INSTANCE DATA: //
  ////////////////////


  //GUI components:
  private JPopupMenu menu=new JPopupMenu();
  private JMenuItem mnuUndo, mnuRedo, mnuCut, mnuCopy, mnuPaste, mnuSelectAll;
  private JScrollPane jsp;
  private transient MyCaret myCaret=new MyCaret();

  //Undo components:
  private String preUndoSelected=null;
  private transient Undo undos=new Undo();
  private boolean suppressUndoRecord=false;
  private boolean forceDoubleUp=false;
  private boolean doubleUndo=false;
  private int doubleUpCount=0;
  private LinkedList<UndoListener> undoListeners;
  private String specialOSXTemp=null;

  //Configuration:
  private boolean fastUndos=false;
  private boolean tabsNotSpaces=false;
  private boolean indentOnHardReturn=false;
  private boolean tabIndentsLine=false;
  private String indentSpaces="  ";
  private int indentSpacesLen=indentSpaces.length();
  private transient CurrentOS currentOS;

  ///////////////////
  // CONSTRUCTORS: //
  ///////////////////

  public MyTextArea(CurrentOS currentOS) {
    addMenus();
    addListeners();
    this.currentOS=currentOS;
    //Removing these keystrokes because they mess up undo, and I wanted one of my menus
    //to use the first one:
    getInputMap().put(KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK), "none");
    getInputMap().put(KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.SHIFT_DOWN_MASK), "none");
    getInputMap().put(KeyMapper.key(KeyEvent.VK_DELETE,     KeyEvent.CTRL_DOWN_MASK), "none");
    setMargin(new java.awt.Insets(2,4,2,4));
    myCaret.setMyWidth(2);
    setCaret(myCaret);
  }
  public MyTextArea addUndoListener(UndoListener ul) {
    if (undoListeners==null)
      undoListeners=new LinkedList<UndoListener>();
    undoListeners.add(ul);
    return this;
  }

  ////////////////////////////////////
  // PUBLIC PROPERTIES AND METHODS: //
  ////////////////////////////////////

  public void setCaretWidth(int w) {
    myCaret.setMyWidth(w);
  }

  public Point getVisualCaretPosition() throws Exception {
    //Both of these offsets include the invisible part of the text area. The first
    //is an offset from the top of the text area, the other of course from top of the screen.
    //The latter, however, gets smaller (even negative) as you scroll down, whereas
    //the former gets larger.
    Rectangle2D caretPos=modelToView2D(getCaretPosition());
    Point mtaPos=getLocationOnScreen();
    int cx=(int)caretPos.getX();
    int cy=(int)caretPos.getY();
    return new Point(cx+mtaPos.x, cy+mtaPos.y);
  }
  public void scrollCaretToMiddle(int caretPos) {
    doScrollIntoView(caretPos, caretPos);
  }
  public void scrollCaretToMiddle(int caretPos, int newCaretEnd) {
    doScrollIntoView(caretPos, newCaretEnd);
  }
  public boolean goToLine(int line) {
    try {
      if (line>getLineCount()-1)
        return false;
      final int r=getLineStartOffset(line), e=getLineEndOffset(line)-1;
      doScrollIntoView(r, r);
      doHighlightEffect(r, r, e);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public void highlightCaret(int caretPos) {
    try {
      int row=getLineOfOffset(caretPos);
      int start=getLineStartOffset(row);
      int end=getLineEndOffset(row);
      doHighlightEffect(caretPos, start, end);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public JScrollPane makeVerticalScrollable(){
    jsp=new JScrollPane(this);
    jsp.setVerticalScrollBarPolicy(ScrollPaneConstants.VERTICAL_SCROLLBAR_ALWAYS);

    // Powers of 2 seem to be the best unit increment here:
    JScrollBar jsb=jsp.getVerticalScrollBar();
    jsb.setUnitIncrement(currentOS.isOSX ?8 :16);

    // Default scroll mode is the "BLIT" scroll mode, which doesn't seem to be as fast.
    JViewport jvp=jsp.getViewport();
    jvp.setScrollMode(JViewport.SIMPLE_SCROLL_MODE);

    return jsp;
  }
  public boolean isAnythingSelected() {
    Caret c=getCaret();
    return c.getDot()!=c.getMark();
  }
  public Container getContainer() {
    return jsp==null ?this :jsp;
  }
  public void setFastUndos(boolean fast) {
    fastUndos=fast;
  }
  public void setSaved() {
    undos.markSave();
  }
  public void setSuppressUndo(boolean s) {
    suppressUndoRecord=s;
  }

  public void undo(){
    undoRedoOnce(true);
  }
  public void redo(){
    undoRedoOnce(false);
  }
  public boolean hasUndos() {
    return undos.hasUndos();
  }
  public boolean hasRedos() {
    return undos.hasRedos();
  }
  public void clearUndos() {
    undos.clearUndos();
  }
  public void clearRedos() {
    undos.clearRedos();
  }
  public boolean undoToHistorySwitch() {
    return undoRedoToHistorySwitch(true);
  }
  public boolean redoToHistorySwitch() {
    return undoRedoToHistorySwitch(false);
  }
  public void undoToBeginning() {
    undoRedoAll(true);
  }
  public void redoToEnd() {
    undoRedoAll(false);
  }
  public void reset() {
    setSuppressUndo(true);
    undos.clearUndos();
    undos.clearRedos();
    undos.markSave();
    setText("");
    setSuppressUndo(false);
  }

  public boolean getTabsOrSpaces() {
    return tabsNotSpaces;
  }
  public void setTabsOrSpaces(boolean tabs) {
    tabsNotSpaces=tabs;
  }
  public void setIndentOnHardReturn(boolean t) {
    indentOnHardReturn=t;
  }
  public void setTabIndentsLine(boolean t) {
    tabIndentsLine=t;
  }
  public void setIndentSpaces(String s) {
    indentSpaces=s;
    indentSpacesLen=s.length();
  }
  public int betterLineEndOffset(int line) {
    try {
      return line<getLineCount()-1
        ?getLineEndOffset(line)-1
        :getLineEndOffset(line);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public void betterReplaceRange(String s, int start, int end) {
    forceDoubleUp=start!=end && s!=null &&  s.length()>0;
    try {
      if (start!=end){
        setSelected(getText(start, end-start));
        replaceRange(s, start, end);
      }
      else
        insert(s, start);
    } catch (Exception e) {
      throw new RuntimeException("Failed to replace from "+start+" "+end, e);
    } finally {
      forceDoubleUp=false;
    }
  }
  public void moveRightOnce() {
    doIndent(false, 1);
  }
  public void moveLeftOnce() {
    doIndent(true, 1);
  }

  /////////////
  // LAYOUT: //
  /////////////

  private void addMenus() {

    //NOTE: The key mnemonics for undo/redo don't necessarily work
    //because they are only enabled when you show the menu. We handle
    //that in our key events. Ctrl-x-a-c-v already work without the menu.

    MenuUtils.add(
      menu,
      mnuUndo=MenuUtils.doMenuItem(
        "Undo", myRightClickListener, KeyEvent.VK_U,
        KeyMapper.keyByOS(KeyEvent.VK_Z)
      ),
      mnuRedo=MenuUtils.doMenuItem(
        "Redo", myRightClickListener, KeyEvent.VK_R,
        KeyMapper.keyByOS(KeyEvent.VK_Z, InputEvent.SHIFT_DOWN_MASK)
      )
    );
    menu.addSeparator();
    MenuUtils.add(
      menu,
      mnuCut=MenuUtils.doMenuItem(
        "Cut", myRightClickListener, KeyEvent.VK_C,
        KeyMapper.keyByOS(KeyEvent.VK_X)
      ),
      mnuCopy=MenuUtils.doMenuItem(
        "Copy", myRightClickListener, KeyEvent.VK_P,
        KeyMapper.keyByOS(KeyEvent.VK_C)
      ),
      mnuPaste=MenuUtils.doMenuItem(
        "Paste", myRightClickListener, KeyEvent.VK_P,
        KeyMapper.keyByOS(KeyEvent.VK_V)
      )
    );
    menu.addSeparator();
    MenuUtils.add(
      menu,
      mnuSelectAll=MenuUtils.doMenuItem(
        "Select All", myRightClickListener, KeyEvent.VK_T,
        KeyMapper.keyByOS(KeyEvent.VK_A)
      )
    );
  }

  //////////////////
  //              //
  //  LISTENERS:  //
  //              //
  //////////////////


  private final void addListeners() {
    addKeyListener(new MyKeyListener());
    getDocument().addDocumentListener(new MyDocumentListener());
    addMouseListener(
      new MouseAdapter() {
        public void mousePressed(MouseEvent e) {maybeShowPopup(e);}
        public void mouseReleased(MouseEvent e) {maybeShowPopup(e);}
        private void maybeShowPopup(MouseEvent e) {
          if (e.isPopupTrigger())
            showMenu(e.getX(), e.getY());
          else
            setSelected(false, false);
        }
      }
    );

    //We can't capture control-tab in our key listener unless we do this, because ctrl-tab is
    //considered a "focus traversal" trigger:
    //Set<AWTKeyStroke> forwardKeys = getFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS);
    //Set<AWTKeyStroke> newForwardKeys = new HashSet<AWTKeyStroke>(forwardKeys);
    //newForwardKeys.add(KeyStroke.getKeyStroke(KeyEvent.VK_ENTER, 0));
    //KeyStroke stroke=KeyStroke.getKeyStroke(KeyEvent.VK_TAB, InputEvent.CTRL_MASK);
    Set<AWTKeyStroke> newForwardKeys = new HashSet<>();
    setFocusTraversalKeys(KeyboardFocusManager.FORWARD_TRAVERSAL_KEYS, newForwardKeys);
  }



  ////////////////////////
  // DOCUMENT LISTENER: //
  ////////////////////////

  private class MyDocumentListener implements DocumentListener {
    //Note: DocumentEvent.getChange() is useless.
    public void insertUpdate(DocumentEvent de) {
      if (suppressUndoRecord)
        return;
      try {
        int start=de.getOffset(), len=de.getLength();
        //System.out.println("INSERT: "+de+" "+start+" "+len);
        String change=getText(start, len);
        if (currentOS.isOSX) {
          //This should probably be done in all cases:
          setSelected(null);
          //Refer to the extensive writeup in removeUpdate about this:
          if (len == 1)
            specialOSXTemp=change;
          else
          if (specialOSXTemp!=null)
            specialOSXTemp=null;
        }
        undos.doAdd(start, len, change, forceDoubleUp);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    int incr=0;
    public void removeUpdate(DocumentEvent de) {
      //We've been there and done that: After it's happened, you CANNOT get hold
      //of the text that was removed. It's too late.
      if (suppressUndoRecord)
        return;
      try {
        int start=de.getOffset(), len=de.getLength();
        //System.out.println("Start: "+start+" len: "+len+ " pre: " +preUndoSelected);
        if (preUndoSelected==null || len!=preUndoSelected.length()) {
          if (preUndoSelected==null && currentOS.isOSX && specialOSXTemp!=null){
            // So on the macintosh, when you press a vowel and hold it,
            // you're given the option to automatically delete the letter that shows
            // and replace with another.
            // The length of the remove will be incorrectly reported in cases
            // where you had text selected; that's already done, the character was inserted,
            // now you're after that, but it will still report that selection size.
            setSelected(specialOSXTemp);
          }
          else
            throw new RuntimeException(
              "Recorded selection length does not match DocumentEvent; DE start:"+start
             +" DE length:"+len+" selection length:"+(preUndoSelected==null ?0 :preUndoSelected.length())
             +" selection: "+preUndoSelected
            );
        }
        undos.doRemove(start, len, preUndoSelected, forceDoubleUp);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public void changedUpdate(DocumentEvent e) {
      throw new RuntimeException(new RuntimeException("Didn't expect a changedUpdate "+e));
    }
  }

  ///////////////////
  // KEY LISTENER: //
  ///////////////////

  private class MyKeyListener extends KeyAdapter {
    public void keyPressed(KeyEvent e){
      try {
        final int code=e.getKeyCode();
        int mods=e.getModifiersEx();

        // Intercept variations on backspace/delete one character:
        setSelected(
          code==KeyEvent.VK_DELETE ||
            (code==KeyEvent.VK_D && KeyMapper.ctrlPressed(mods) && currentOS.isOSX)
          ,
          code==KeyEvent.VK_BACK_SPACE ||
            (code==KeyEvent.VK_H && KeyMapper.ctrlPressed(mods))
        );

        //System.out.println("CODE "+code+" MODS "+mods);
        if (code==KeyEvent.VK_RIGHT) {

          //ARROW RIGHT:
          if (KeyMapper.ctrlPressed(mods) || KeyMapper.optionPressed(mods, currentOS)){
            e.consume();
            doControlArrow(false, KeyMapper.shiftPressed(mods));
          }

        }
        else
        if (code==KeyEvent.VK_UP) {
          if (!KeyMapper.shiftPressed(mods) && !KeyMapper.ctrlPressed(mods)){
            Caret c=getCaret();
            int start=c.getDot(), end=c.getMark();
            if (end<start) {
              e.consume();
              setCaretPosition(start);
              moveCaretPosition(end);
            }
          }
        }
        else
        if (code==KeyEvent.VK_LEFT) {

          //ARROW LEFT:
          boolean shift=KeyMapper.shiftPressed(mods);
          if (KeyMapper.ctrlPressed(mods) || KeyMapper.optionPressed(mods, currentOS)){
            e.consume();
            doControlArrow(true, shift);
          }
          else
          if (!shift) {
            //Breaks out of current selection, to the left, regardless
            //of which side cursor is on:
            Caret c=getCaret();
            int start=c.getDot(), end=c.getMark();
            if (end<start){
              e.consume();
              setCaretPosition(end==0 ?0 :end-1);
            }
          }

        }
        else
        if (code==KeyEvent.VK_CONTEXT_MENU || (
              code==KeyEvent.VK_F10 && KeyMapper.shiftPressed(e)
            )) {

          //CONTEXT MENU:
          Point p=getCaret().getMagicCaretPosition();
          if (p==null) p=new Point(10,10);
          e.consume();
          showMenu(p.x, p.y);

        }
        else
        if (code==KeyEvent.VK_Z) {

          //UNDO/REDO:
          if (KeyMapper.ctrlPressed(mods) || KeyMapper.metaPressed(e, currentOS)){
            if (KeyMapper.shiftPressed(mods))
              redo();
            else
              undo();
            e.consume();
          }

        }
        else
        if (code==KeyEvent.VK_Y && KeyMapper.ctrlPressed(e)) {

          //Another way to REDO:
          redo();
          e.consume();

        }
        else
        if (code==KeyEvent.VK_ENTER && indentOnHardReturn) {

           doIndentOnHardReturn();
           e.consume();

        }
        else
        if (code==KeyEvent.VK_TAB && tabIndentsLine) {

          //Tab indent & tab insert:
          if (KeyMapper.ctrlPressed(mods)){
            Caret c=getCaret();
            int start=c.getDot(), end=c.getMark();
            replaceRange("	", start<=end ?start :end, start<=end ?end :start);
          }
          else
            doIndent(KeyMapper.shiftPressed(mods), tabsNotSpaces ?1 :indentSpacesLen);
          e.consume();

        }
        else
        if (code==KeyEvent.VK_W && KeyMapper.ctrlPressed(e) && currentOS.isOSX)
          // Normally this gives a delete-to-beginning-of-line that
          // nobody knows about
          e.consume();
        else
        if (code==KeyEvent.VK_V && KeyMapper.ctrlPressed(e) && currentOS.isOSX){
          // Pressing ctrl-v on macintosh should paste:
          paste();
          e.consume();
        }
        else
        if (code==KeyEvent.VK_C && KeyMapper.ctrlPressed(e) && currentOS.isOSX){
          // Pressing ctrl-c on macintosh should copy:
          copy();
          e.consume();
        }
        else
        if (code==KeyEvent.VK_X && KeyMapper.ctrlPressed(e) && currentOS.isOSX){
          // Pressing ctrl-x on macintosh should cut:
          cut();
          e.consume();
        }
        else
        if (code==KeyEvent.VK_HOME && currentOS.isOSX){
          // Pressing home on OSX should go to beginning of line
          doHomeEnd(true, KeyMapper.shiftPressed(e), KeyMapper.ctrlPressed(e));
          e.consume();
        }
        else
        if (code==KeyEvent.VK_END && currentOS.isOSX){
          // Pressing end on OSX should go to end of line
          doHomeEnd(false, KeyMapper.shiftPressed(e), KeyMapper.ctrlPressed(e));
          e.consume();
        }
      } catch (Exception ex) {
        throw new RuntimeException(ex);
      }
    }
  }


  ////////////////////////
  // RIGHT-CLICK MENUS: //
  ////////////////////////

  private void showMenu(int x, int y) {
    final Caret caret=getCaret();
    int cpos=caret.getDot(),
        mark=caret.getMark();
    mnuCut.setEnabled(cpos!=mark);
    mnuCopy.setEnabled(cpos!=mark);
    mnuUndo.setEnabled(undos.hasUndos());
    mnuRedo.setEnabled(undos.hasRedos());
    menu.show(MyTextArea.this, x, y);
  }
  private transient Action myRightClickListener=new AbstractAction(){
    public void actionPerformed(ActionEvent event) {
      Object s=event.getSource();
      if (s==mnuUndo)  undo();
      else
      if (s==mnuRedo)  redo();
      else
      if (s==mnuCut)   cut();
      else
      if (s==mnuCopy)  copy();
      else
      if (s==mnuPaste) paste();
      else
      if (s==mnuSelectAll) selectAll();
      else
        throw new RuntimeException("Unexpected: "+s);
    }
  };




  //////////////////////////////
  // CARET/CURSOR MANAGEMENT: //
  // -scrolling               //
  // -bump out of selection   //
  // -ctrl-arrow              //
  // -highlight               //
  //////////////////////////////

  private void betterSetCaretPosition(int start, int end){
    doScrollIntoView(start, end==-1 ?start :end);
    setCaretPosition(start);
    if (end!=-1 && end!=start)
      moveCaretPosition(end);
  }

  private void doHighlightEffect(final int pos, final int startLine, final int endLine) {
    setCaretPosition(startLine);
    moveCaretPosition(endLine);
    //This next thing is sometimes necessary:
    invalidate(); repaint();
    javax.swing.SwingUtilities.invokeLater(new Runnable() {
      public void run() {
        try{Thread.sleep(300);} catch (Exception e) {}
        setCaretPosition(pos);
      }
    });
  }

  private void doScrollIntoView(int newCaretStart, int newCaretEnd) {
    try {
      if (jsp==null)
        return;
      JScrollBar jsb=jsp.getVerticalScrollBar();
      int editorHeight=jsp.getHeight();
      int edgeLimit=editorHeight/3;
      int currTop=jsp.getViewport().getViewPosition().y;
      int currBottom=currTop+editorHeight;
      int newCaretStartY=(int)modelToView2D(newCaretStart).getY(),
          newCaretEndY  =(int)modelToView2D(newCaretEnd).getY();
      int offFromBottom=currBottom-newCaretEndY,
          offFromTop   =newCaretStartY-currTop;

      //Temporarily turning off the adjustment of visible caret because it
      //really doesn't seem helpful. Tends to cause a sudden bump where the
      //thing you're watching for a change suddenly moves elsewhere:
      if (offFromBottom<0)
        //Beyond bottom of viewport:
        jsb.setValue(jsb.getValue()+(-offFromBottom)+edgeLimit);

      else
      if (offFromTop<0)
        jsb.setValue(jsb.getValue()+offFromTop-edgeLimit);

    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void doHomeEnd(boolean home, boolean shiftPressed, boolean ctrlPressed) throws Exception {
    int pos;
    if (home){
      if (ctrlPressed)
        pos=0;
      else
        pos=getLineStartOffset(getLineOfOffset(getCaret().getDot()));
    } else {
      int lineCount=getLineCount();
      if (ctrlPressed)
        pos=getLineEndOffset(lineCount-1);
      else {
        int row=getLineOfOffset(getCaret().getDot());
        pos=getLineEndOffset(row);
        if (getLineCount()-1!=row)
          pos-=1;
      }
    }
    if (shiftPressed)
      moveCaretPosition(pos);
    else
      setCaretPosition(pos);
  }

  private void doControlArrow(boolean left, boolean shift) throws Exception {
    final Caret caret=getCaret();
    int
      //Where the blinky thing is:
      cpos=caret.getDot(),
      //If there is a selection, where it starts,
      //else the same as cpos; keep in mind that
      //it is not necessarily to the right or left
      //of cpos just because this is right-> or left->
      mark=caret.getMark();
    int row=getLineOfOffset(cpos);
    int upTo=0;
    if (left) {
      int startRow=getLineStartOffset(row);
      if (startRow>0 && cpos==startRow)
        startRow=getLineStartOffset(row-1);
      String selectable=getText(startRow, cpos-startRow);
      upTo=Selectable.getLeft(selectable, shift)-selectable.length();
    }
    else {
      int endRow=getLineEndOffset(row),
          lineCount=getLineCount();
      if (row!=lineCount-1 && endRow==cpos+1)
        //When there is another row, end of row is a linefeed, so you're one behind:
        endRow=getLineEndOffset(row+1);
      String selectable=getText(cpos, endRow-cpos);
      upTo=Selectable.getRight(selectable, shift);
    }
    if (upTo!=0){
      if (shift){
        setCaretPosition(mark);
        moveCaretPosition(cpos+upTo);
      }
      else
        setCaretPosition(cpos+upTo);
    }
  }


  //////////////////
  // TAB INDENTS: //
  //////////////////

  private final transient Indenter indenter=new Indenter();
  private void initIndenter(int indentLen) {
    indenter.tabIndents=tabsNotSpaces;
    indenter.tabSize=getTabSize();
    indenter.spaceIndentLen=indentLen;
  }


  private void doIndent(boolean remove, int indentLen) {
    try {

      //Determine current selection, forwards or backwards:
      boolean forwards;
      int startSel, endSel;
      {
        Caret caret=getCaret();
        final int cpos=caret.getDot(),
                  mark=caret.getMark();
        forwards=cpos>=mark;
        startSel=forwards ?mark :cpos;
        endSel  =forwards ?cpos :mark;
      }
      boolean anySel=startSel!=endSel;

      //Get first and last row. Ignore last row
      //if it's an empty line:
      int firstRow=getLineOfOffset(startSel),
          lastRow =getLineOfOffset(endSel);
      if (firstRow!=lastRow) {
        int startLast=getLineStartOffset(lastRow);
        if (getText(startLast, endSel-startLast).equals(""))
          lastRow-=1;
      }
      int startPos=getLineStartOffset(firstRow),
          endPos  =getLineEndOffset(lastRow);
      final int veryLastRow=getLineCount()-1;


      //Now build up buffer of changes:
      StringBuilder sb=new StringBuilder(
        2+endPos-startPos+
        (
          remove ?0 :(indentLen*(lastRow+1-firstRow))
        )
      );


      boolean anyChange=false;
      boolean singleLine=firstRow==lastRow;
      boolean fitToBlock=false;
      initIndenter(indentLen);
      for (int r=firstRow; r<=lastRow; r++){
        int sp=getLineStartOffset(r),
            ep=getLineEndOffset(r);//;
        final int eolFactor=(r==veryLastRow ?0 :1);

        //System.out.println("DEBUG startSel "+startSel+" endSel "+endSel+" sp "+sp+" ep "+ep
        //  +" eolFactor "+eolFactor+" r "+r+" firstRow "+firstRow+" remove "+remove+" singleLine "+singleLine);
        {
          indenter.init(getText(sp, ep-sp));
          fitToBlock|= r==firstRow && indenter.pastBlock > 0;
          if (!remove && !fitToBlock && singleLine  && startSel!=ep-eolFactor && indenter.blank)
            //This catches the situation where it looks like you need to indent
            //but you actually have enough spaces already, just after the cursor.
            //So just move the cursor:
            setCaretPosition(ep-eolFactor);
          else
            indenter.indent(remove, fitToBlock);
        }

        endSel+=indenter.lenChange;
        if (r==firstRow && (indenter.blank || !remove || startSel!=sp)) {
          if (startSel<sp + indenter.endPos)
            //Moves cursor to end of indention area on single line indent
            //where you're in the middle of the spaces:
            startSel=sp+indenter.endPos;
          else
            startSel+=indenter.lenChange;
        }
        anyChange|=indenter.anyChange;
        sb.append(indenter.buffer);
      }
      if (anyChange) {
        betterReplaceRange(sb.toString(), startPos, endPos);
        setCaretPosition(forwards  ?startSel :endSel);
        if (anySel)
          moveCaretPosition(forwards ?endSel   :startSel);
      }
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  private void doIndentOnHardReturn() {
    try {

      // Get start & end of selection; and get previous row's text:
      final int startSel, endSel;
      final String prevRowText;
      {
        Caret caret=getCaret();
        int cpos=caret.getDot(), mark=caret.getMark();
        startSel=cpos<=mark ?cpos :mark;
        endSel  =cpos<=mark ?mark :cpos;
        int rowStart=getLineStartOffset(getLineOfOffset(startSel));
        prevRowText=getText(rowStart, startSel-rowStart);
      }

      // Selected text will be removed, so record it:
      if (startSel!=endSel)
        setSelected(getText(startSel, endSel-startSel));

      // Build up indention text based on previous row's indent:
      int prevLen=prevRowText.length();
      StringBuilder newText=new StringBuilder(prevRowText.length());
      for (int i=0; i<prevLen; i++) {
        char c=prevRowText.charAt(i);
        if (c=='\t' || c==' ')
          newText.append(c);
        else
          break;
      }

      // New line goes at beginning; then we're done, replace:
      initIndenter(indentSpacesLen);
      indenter.repair(newText);
      newText.insert(0, "\n");
      replaceRange(newText.toString(), startSel, endSel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  //////////////
  //  UNDOS:  //
  //////////////

  /** This prepares us for the next change by telling us what was selected
      before something goes bang. */
  private void setSelected(boolean del, boolean back) {
    try {
      String sel=getSelectedText();
      if (sel==null) {
        int cpos=getCaret().getDot();
        if (del)
          sel=getText(cpos, 1);
        else
        if (back && cpos>0)
          sel=getText(cpos-1, 1);
      }
      setSelected(sel);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private void setSelected(String text) {
    preUndoSelected=text;
  }

  private void undoRedoAll(boolean undoOrRedo) {
    suppressUndoRecord=true;
    UndoStep us;
    while ((
            us=undoOrRedo ?undos.doUndo() :undos.doRedo()
          )!=null)
      while (!doUndoReplace(us, null, undoOrRedo)){}
    checkUnstable();
    suppressUndoRecord=false;
  }

  private boolean undoRedoToHistorySwitch(boolean undoOrRedo) {
    if (!(
        undoOrRedo
          ?undos.hasHistorySwitchUndo() :undos.hasHistorySwitchRedo()
      ))
      return false;
    suppressUndoRecord=true;
    boolean first=true;
    boolean skipDoubleUp=false;
    UndoStep step;
    while ((
            step=undoOrRedo ?undos.getUndo() :undos.getRedo()
          )!=null) {
      if (first) {
        first=false;
        skipDoubleUp=step.doubleUp;
      }
      if (step.doubleUp ^ skipDoubleUp)
          break;
      step=undoOrRedo ?undos.doUndo() :undos.doRedo();
      while (!doUndoReplace(step, null, undoOrRedo)){}
    }
    checkUnstable();
    suppressUndoRecord=false;
    return true;
  }

  private void undoRedoOnce(final boolean undoOrRedo) {
    suppressUndoRecord=true;
    doubleUpCount=0;
    UndoStep st=null;
    do {
      st=continueUndoRedoOnce(st, undoOrRedo);
      if (st!=null) {
        if (undoOrRedo)
          undos.doUndo();
        else
          undos.doRedo();
      }
    } while (st!=null && checkUnstableAndFast(st));
    suppressUndoRecord=false;
  }
  private UndoStep continueUndoRedoOnce(final UndoStep old, final boolean undoOrRedo){
    UndoStep st=undoOrRedo ?undos.getUndo() :undos.getRedo();
    boolean did=false;
    if (st==null){
      if (old==null)
        fireUndoEvent(
          undoOrRedo
            ?new UndoEvent().setNoMoreUndosError()
            :new UndoEvent().setNoMoreRedosError()
        );
    }
    else
      did=doUndoReplace(st, old, undoOrRedo);
    return did ?st :null;
  }



  private boolean checkUnstableAndFast(UndoStep newStep) {
    //Agree to fast undo if we aren't in stable state, and
    //either fast undos are on, or we have a doubleUp
    return checkUnstable() && (fastUndos || newStep.doubleUp);
  }
  private boolean checkUnstable() {
    if (undos.isSavedState()){
      fireUndoEvent(new UndoEvent().setUndoSaveStable());
      return false;
    }
    return true;
  }

  /** The "old" parameter is so we can decide whether we've gone fast enough. */
  private boolean doUndoReplace(UndoStep st, UndoStep old, boolean undoOrRedo) {
    doubleUndo=st.doubleUp && old!=null && old.doubleUp && (!st.doubleUpDone || doubleUpCount<1);
    if (doubleUndo)
      doubleUpCount++;
    if (old!=null && !doubleUndo) {
      if (old.len>1 || st.len>1)
        return false;
      if (old.uType!=st.uType)
        return false;
      if (!UndoSimilar.matches(old.text, st.text))
        return false;
    }
    //Now take it a step further. This may still return false
    //because the position change is too far, fast or not:
    return (undoOrRedo ^ st.uType==UndoStep.ADD)             //XOR trickiness
      ?doUndoReplace(old!=null, st.text, st.start, st.start)      //Add text back if undo/remove redo/add
      :doUndoReplace(old!=null, null, st.start, st.start+st.len); //Rip text out  if redo/remove undo/add
  }

  private boolean doUndoReplace(boolean hasPriorUndo, String text, int start, int finish) {
    final Caret caret=getCaret();
    int cpos=caret.getDot(), mark=caret.getMark();
    if (text!=null) {
      //Test add text.. Only if we are very close to the position
      //we are adding to:
      if (!doubleUndo && Math.abs(cpos-start)>1) {
        if (!hasPriorUndo)
          betterSetCaretPosition(start, start);
        return false;
      }
    }
    else {
      //Test remove text.. If the caret is not very close
      //to the text (thus the >1) or if it's more than one
      //character, we make sure the text is selected, so you
      //see what you're about to remove.
      int sel1=mark<cpos ?mark :cpos,
          sel2=mark<cpos ?cpos :mark;
      boolean notYet=
         !doubleUndo && (
           Math.abs(start-sel1)>1
           ||
          (Math.abs(start-finish)>1 && Math.abs(start-sel1) +Math.abs(finish-sel2)>1)
         );
      if (notYet){
        if (!hasPriorUndo)
          betterSetCaretPosition(start, finish);
        return false;
      }
    }

    //Do the deed;
    doScrollIntoView(start, start);
    replaceRange(text, start, finish);

    //Highlight what we just did, if we're adding
    //text and it's more than one character:
    if (text!=null) {
      int len=text.length();
      if (len>1) {
        setCaretPosition(start);
        moveCaretPosition(start+len);
      }
      else
        setCaretPosition(start+len);
    }

    return true;
  }
  private void fireUndoEvent(UndoEvent ue) {
    if (undoListeners!=null)
      for (UndoListener u: undoListeners)
        u.happened(ue);
  }

}
