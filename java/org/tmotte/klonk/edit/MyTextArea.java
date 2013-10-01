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
import javax.swing.KeyStroke;
import javax.swing.ScrollPaneConstants;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Element;
import javax.swing.text.JTextComponent;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.common.swang.KeyMapper;

public class MyTextArea extends JTextArea {

  ////////////////////
  // INSTANCE DATA: //
  ////////////////////


  //GUI components:
  private JPopupMenu menu=new JPopupMenu();
  private JMenuItem mnuUndo, mnuRedo, mnuCut, mnuCopy, mnuPaste, mnuSelectAll;
  private JScrollPane jsp;

  //Undo components:
  private String preUndoSelected=null;
  private Undo undos=new Undo();
  private boolean suppressUndoRecord=false;
  private boolean forceDoubleUp=false;
  private boolean doubleUndo=false;
  private int doubleUpCount=0;
  private LinkedList<UndoListener> undoListeners;

  //Configuration:
  private boolean fastUndos=false;
  private boolean tabsOrSpaces=false;
  private boolean indentOnHardReturn=false;
  private boolean tabIndentsLine=false;
  private String indentSpaces="  ";
  private int indentSpacesLen=indentSpaces.length();
  
  ///////////////////
  // CONSTRUCTORS: //
  ///////////////////
  
  public MyTextArea() {
    addMenus();
    addListeners();
    //Removing these keystrokes because they mess up undo, and I wanted one of my menus 
    //to use the first one:
    getInputMap().put(KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK), "none");
    getInputMap().put(KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.SHIFT_DOWN_MASK), "none");
    getInputMap().put(KeyMapper.key(KeyEvent.VK_DELETE,     KeyEvent.CTRL_DOWN_MASK), "none");
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

  public void undo(){
    suppressUndoRecord=true;
    startUndo();
    suppressUndoRecord=false;
  }
  public void redo(){
    suppressUndoRecord=true;
    startRedo();
    suppressUndoRecord=false;
  }
  
  public Point getVisualCaretPosition() throws Exception { 
    //Both of these offsets include the invisible part of the text area. The first
    //is an offset from the top of the text area, the other of course from top of the screen.
    //The latter, however, gets smaller (even negative) as you scroll down, whereas
    //the former gets larger.
    java.awt.Rectangle caretPos=modelToView(getCaretPosition());
    Point mtaPos=getLocationOnScreen();
    return new Point(caretPos.x+mtaPos.x, caretPos.y+mtaPos.y);
  }
  public void scrollCaretToMiddle(int caretPos) {
    doScrollIntoView(caretPos, caretPos);
  }
  public void scrollCaretToMiddle(int caretPos, int newCaretEnd) {
    doScrollIntoView(caretPos, newCaretEnd);
  }
  public boolean goToLine(int line) {
    try {
      if (line>getLineCount())
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
    //Powers of 2 seem to be the best unit increment here:
    JScrollBar jsb=jsp.getVerticalScrollBar();
    jsb.setUnitIncrement(16);
    //Default scroll mode is the "BLIT" scroll mode, which doesn't seem to be as fast.
    JViewport jvp=jsp.getViewport();
    jvp.setScrollMode(jvp.SIMPLE_SCROLL_MODE);
    
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
  public void undoToBeginning() {
    undoAll();
  }
  public void redoToEnd() {
    redoAll();
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
    return tabsOrSpaces;
  }
  public void setTabsOrSpaces(boolean tabs) {
    tabsOrSpaces=tabs;
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
  public void betterReplaceRange(String s, int start, int end) throws Exception {
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
        KeyMapper.key(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK)
      ),
      mnuRedo=MenuUtils.doMenuItem(
        "Redo", myRightClickListener, KeyEvent.VK_R,
        KeyMapper.key(KeyEvent.VK_Z, InputEvent.CTRL_DOWN_MASK|InputEvent.SHIFT_DOWN_MASK)
      )
    );
    menu.addSeparator();
    MenuUtils.add(
      menu, 
      mnuCut=MenuUtils.doMenuItem(
        "Cut", myRightClickListener, KeyEvent.VK_C,
        KeyMapper.key(KeyEvent.VK_X, InputEvent.CTRL_DOWN_MASK)
      ),
      mnuCopy=MenuUtils.doMenuItem(
        "Copy", myRightClickListener, KeyEvent.VK_P,
        KeyMapper.key(KeyEvent.VK_C, InputEvent.CTRL_DOWN_MASK)
      ),
      mnuPaste=MenuUtils.doMenuItem(
        "Paste", myRightClickListener, KeyEvent.VK_P,
        KeyMapper.key(KeyEvent.VK_V, InputEvent.CTRL_DOWN_MASK)
      )
    );
    menu.addSeparator();
    MenuUtils.add(
      menu, 
      mnuSelectAll=MenuUtils.doMenuItem(
        "Select All", myRightClickListener, KeyEvent.VK_T,
        KeyMapper.key(KeyEvent.VK_A, InputEvent.CTRL_DOWN_MASK)
      )
    );
  }
  
  //////////////////
  //              //
  //  LISTENERS:  //
  //              //
  //////////////////
  
  
  private void addListeners() {
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
        undos.doAdd(start, len, getText(start, len), forceDoubleUp);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    int incr=0;
    public void removeUpdate(DocumentEvent de) {
      if (suppressUndoRecord)
        return;
      try {
        int start=de.getOffset(), len=de.getLength();
        if (preUndoSelected==null || len!=preUndoSelected.length())
          throw new RuntimeException(
            "Recorded selection length does not match DocumentEvent; DE start:"+start
           +" DE length:"+len+" selection length:"+(preUndoSelected==null ?0 :preUndoSelected.length())
           +" selection: "+preUndoSelected
          );
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
      //WARNING WARNING WARNING
      //If an if statement intercepts a key code but does not take action,
      //you run the risk of skipping the call to setSelected() at the bottom.
      //This will cause an undo error!
      try {
        final int code=e.getKeyCode();
        setSelected(code==e.VK_DELETE, 
                    code==e.VK_BACK_SPACE || 
                        (code==e.VK_H && KeyMapper.ctrlPressed(e.getModifiersEx()))
                      );        

         if (code==e.VK_RIGHT) {

          //ARROW RIGHT:
          int mods=e.getModifiersEx();
          if (KeyMapper.ctrlPressed(mods)){
            e.consume();
            doControlArrow(false, KeyMapper.shiftPressed(mods));
          }
         
        }
        else
        if (code==e.VK_UP) {
          int mods=e.getModifiersEx();
          if (!KeyMapper.shiftPressed(mods) && !KeyMapper.ctrlPressed(mods)){
            Caret c=getCaret();
            int start=c.getDot(), end=c.getMark();
            if (end<start) {
              setCaretPosition(start);
              moveCaretPosition(end);
            }
          }
        }
        else
        if (code==e.VK_LEFT) {

          //ARROW LEFT:
          int mods=e.getModifiersEx();
          boolean shift=KeyMapper.shiftPressed(mods);
          if (KeyMapper.ctrlPressed(mods)){
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
        if (code==e.VK_CONTEXT_MENU || (
              code==e.VK_F10 && KeyMapper.shiftPressed(e)
            )) { 
            
          //CONTEXT MENU:
          Point p=getCaret().getMagicCaretPosition();
          if (p==null) p=new Point(10,10);
          showMenu(p.x, p.y);
          e.consume();
          
        }
        else
        if (code==e.VK_Z) {
        
          //UNDO/REDO:
          int mods=e.getModifiersEx();
          if (KeyMapper.ctrlPressed(mods)){
            if (KeyMapper.shiftPressed(mods))
              redo();
            else
              undo();
            e.consume();
          }
          
        }
        else
        if (code==e.VK_Y && KeyMapper.ctrlPressed(e)) {
        
          //Another way to REDO:
          redo();
          e.consume();

        }
        else
        if (code==e.VK_ENTER && indentOnHardReturn) {
          
           doIndentOnHardReturn();
           e.consume();
          
        }
        else
        if (code==e.VK_TAB && tabIndentsLine) {
        
          //Tab indent & tab insert:
          int mods=e.getModifiersEx();
          if (KeyMapper.ctrlPressed(mods)){
            Caret c=getCaret();
            int start=c.getDot(), end=c.getMark();
            replaceRange("	", start<=end ?start :end, start<=end ?end :start);
          }
          else
            doTabIndent(KeyMapper.shiftPressed(mods));
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
  private Action myRightClickListener=new AbstractAction(){
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
      int newCaretStartY=modelToView(newCaretStart).y,
          newCaretEndY  =modelToView(newCaretEnd).y;
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
      if (row!=lineCount && endRow==cpos+1) 
        //When there is another row, end of row is 
        //is a linefeed, so you're one behind:
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

  private void doTabIndent(boolean remove) {
    try {
      Caret caret=getCaret();
      
      //Determine current selection, forwards or backwards:
      boolean forwards;
      int startSel, endSel;
      {
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
          

      //Now build up buffer of changes:
      String indentStr=tabsOrSpaces ?"\t" :indentSpaces;
      int indentStrLen=tabsOrSpaces ?1    :indentSpacesLen;
      StringBuilder sb=new StringBuilder(
        2+endPos-startPos+
        (
          remove ?0 :(indentStrLen*(lastRow+1-firstRow))
        )
      );
      boolean anyChange=false;
      int veryLastRow=getLineCount()-1;
      for (int r=firstRow; r<=lastRow; r++){
        int sp=getLineStartOffset(r),
            ep=getLineEndOffset(r);//;
        String lineStr=getText(sp, ep-sp);
        int eolFactor=(r==veryLastRow ?0 :1);
        
        //For partially indented lines, we need to know that:
        int spaceCount=getSpaceCount(lineStr);
        int actualLen=getTabOffBy(spaceCount);
        boolean lenMismatch=actualLen!=indentStrLen;
        
        if (remove) {
          if (lineStr.startsWith(indentStr) || lenMismatch){
            if (lenMismatch) 
              actualLen=indentStrLen-actualLen;
            anyChange=true;
            lineStr=lineStr.substring(actualLen);
            if (r==firstRow && startSel!=sp) {
              if (startSel<=sp+spaceCount)
                startSel=sp+spaceCount-actualLen;
              else
                startSel-=actualLen;
            }
            endSel-=actualLen;
          }  
        }
        else
        if (!lenMismatch && firstRow==lastRow && 
            spaceCount==lineStr.length()-eolFactor && 
            startSel!=ep-eolFactor) 
          //This catches the situation where it looks like you need to indent
          //but you actually have enough spaces already, just after the cursor. 
          //So just move the cursor:
          setCaretPosition(ep-eolFactor);
        else {
          anyChange=true;
          if (r==firstRow && (!anySel || startSel!=sp)) {
            if (startSel<=sp+spaceCount)
              //Moves cursor to end of indention area on single line indent
              //where you're in the middle of the spaces:
              startSel=sp+spaceCount+actualLen;
            else
              startSel+=actualLen;
          }
          endSel+=actualLen;
          sb.append(lenMismatch ?indentStr.substring(0, actualLen) :indentStr);
        }
        sb.append(lineStr);
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
  private int getTabOffBy(int spaceCount) {
    if (tabsOrSpaces)
      return 1;
    if (spaceCount==0)
      return indentSpacesLen;
    if (spaceCount<indentSpacesLen)
      return indentSpacesLen-spaceCount;
    return indentSpacesLen-(spaceCount % indentSpacesLen);
  }
  private int getSpaceCount(String lineStr) {
    int len=lineStr.length();
    int spaceCount=0;
    char check=tabsOrSpaces ?'\t' :' ';
    for (int i=0; i<len; i++)
      if (lineStr.charAt(i)!=check)
        break;
      else
        spaceCount++;
    return spaceCount;
  }
    
  private void doIndentOnHardReturn() {
    try {
      Caret caret=getCaret();
      int cpos=caret.getDot(), mark=caret.getMark();
      int start=cpos<=mark ?cpos :mark,
          end  =cpos<=mark ?mark :cpos;
      if (start!=end)
        setSelected(getText(start, end-start));
      int row=getLineOfOffset(start);
      int rowStart=getLineStartOffset(row);
      String rowText=getText(rowStart, start-rowStart);
      int rowLen=rowText.length();
      StringBuilder newText=new StringBuilder(rowText.length());
      newText.append("\n");
      int i=0;
      char t=tabsOrSpaces ?'	' :' ';
      while (i<rowLen && rowText.charAt(i++)==t)
        newText.append(t);
      replaceRange(newText.toString(), start, end);
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

  private void undoAll(){
    suppressUndoRecord=true;
    UndoStep us;
    while ((us=undos.doUndo())!=null) 
      while (!doUndoReplace(us, null, true)){}
    suppressUndoRecord=false;
    checkUnstable();
  }
  private void redoAll(){
    suppressUndoRecord=true;
    UndoStep us;
    while ((us=undos.doRedo())!=null) 
      while (!doUndoReplace(us, null, false)){}
    suppressUndoRecord=false;
    checkUnstable();
  }
  
  
  private void startUndo(){
    doubleUpCount=0;
    UndoStep st=null;
    do {
      st=continueUndo(st);
      if (st!=null)
        undos.doUndo();
    } while (st!=null && checkUnstableAndFast(st));
  }
  private UndoStep continueUndo(UndoStep old){
    UndoStep st=undos.getUndo();
    boolean did=false;
    if (st==null){
      if (old==null) fireUndoEvent(new UndoEvent().setNoMoreUndos());
    }
    else
      did=doUndoReplace(st, old, true);
    return did ?st :null;
  }

  private void startRedo(){
    doubleUpCount=0;
    UndoStep st=null;
    do {
      st=continueRedo(st);
      if (st!=null)
        undos.doRedo();
    } while (st!=null && checkUnstableAndFast(st));
  }
  private UndoStep continueRedo(UndoStep old){
    UndoStep st=undos.getRedo();
    boolean did=false;
    if (st==null){
      if (old==null) fireUndoEvent(new UndoEvent().setNoMoreRedos());
    }
    else
      did=doUndoReplace(st, old, false);
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
    return (undoOrRedo ^ st.uType==st.ADD)             //XOR trickiness
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
