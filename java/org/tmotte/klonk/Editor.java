package org.tmotte.klonk;
import java.awt.Toolkit;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.datatransfer.DataFlavor;
import java.awt.dnd.DnDConstants;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.ActionEvent;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.FocusListener;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.io.File;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.ListIterator;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JScrollPane;
import javax.swing.event.CaretEvent;
import javax.swing.event.CaretListener;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.edit.Spaceable;
import org.tmotte.klonk.edit.UndoEvent;
import org.tmotte.klonk.edit.UndoListener;
import org.tmotte.klonk.io.FileMetaData;
import org.tmotte.klonk.io.KFileIO;

/** Enhances MyTextArea with secondary editing features. Also adds file-specific information. */
public class Editor {


  //Dependencies:
  private EditorListener editListener;
  private Setter<Throwable> failHandler;
  private Setter<KeyEvent> externalKeyListener;
  private CurrentOS currentOS;

  //Purely private:
  private MyTextArea jta;
  private MyDocumentListener docListener=new MyDocumentListener();
  private List<Integer> marks;
  private String encoding=FileMetaData.UTF8;
  private boolean encodingNeedsBOM=false;
  private String lineBreaker;
  private boolean autoTrimOnSave=false;
  private boolean used=false;
  private boolean unsavedChanges=false;
  private String title="Untitled";
  private File file;
  private Toolkit toolkit=Toolkit.getDefaultToolkit();


  /////////////////////////
  // Construct & Config: //
  /////////////////////////

  public Editor(
      CurrentOS currentOS, Setter<Throwable> failHandler,
      EditorListener editListener, Setter<KeyEvent> keyListener, UndoListener undoL,
      String lineBreaker, boolean wordWrap, boolean autoTrim
    ) {
    this.currentOS=currentOS;
    this.failHandler=failHandler;
    this.editListener=editListener;
    this.externalKeyListener=keyListener;

    this.lineBreaker=lineBreaker;
    jta=new MyTextArea(currentOS);
    jta.setDragEnabled(false);
    JScrollPane jsp=jta.makeVerticalScrollable();

    // This seem to eliminate the java 8 problem for Mac OSX with
    // vertical scrollbar jitter.
    // https://bugs.openjdk.java.net/browse/JDK-8147994
    if (currentOS.isOSX)
      jsp.setHorizontalScrollBarPolicy(jsp.HORIZONTAL_SCROLLBAR_ALWAYS);

    jta.addUndoListener(undoL);
    jta.addUndoListener(myUndoListener);

    setWordWrap(wordWrap);
    setAutoTrim(autoTrim);
    setEvents(currentOS);
  }

  public void setTabsOrSpaces(int tabsOrSpaces) {
    jta.setTabsOrSpaces(tabsOrSpaces==TabAndIndentOptions.INDENT_TABS);
  }
  public int getTabsOrSpaces() {
    return jta.getTabsOrSpaces()
      ?TabAndIndentOptions.INDENT_TABS
      :TabAndIndentOptions.INDENT_SPACES;
  }
  public void setTabAndIndentOptions(TabAndIndentOptions taio) {
    jta.setTabSize(taio.tabSize);
    jta.setIndentOnHardReturn(taio.indentOnHardReturn);
    jta.setTabIndentsLine(taio.tabIndentsLine);
    jta.setIndentSpaces(taio.buildIndentSpaces());
  }
  public void setWordWrap(boolean wordWrap) {
    jta.setLineWrap(wordWrap);
    jta.setWrapStyleWord(wordWrap);
  }
  public void setAutoTrim(boolean b){
    autoTrimOnSave=b;
  }
  public void setFont(FontOptions options) {
    jta.setFont(options.getFont());
    jta.setForeground(options.getColor());
    jta.setBackground(options.getBackgroundColor());
    jta.setCaretColor(options.getCaretColor());
    jta.setCaretWidth(options.getCaretWidth());
  }
  public void setLineBreaker(String lb) {
    this.lineBreaker=lb;
  }
  public String getLineBreaker() {
    return lineBreaker;
  }
  public String getTitle() {
    return title;
  }
  public void setTitle(String s) {
    title=s;
  }


  ////////////
  // Marks: //
  ////////////

  public int doSetMark() {
    return setMark();
  }
  public int doMarkGoToPrevious() {
    return goToPreviousMark(true);
  }
  public int doMarkGoToNext() {
    return goToNextMark();
  }
  public void doClearMarks(){
    marks.clear();
  }
  public int doMarkClearCurrent() {
    return clearCurrentMark();
  }
  public int getPreviousMark() {
    return goToPreviousMark(false);
  }
  public boolean hasMarks(){
    return marks!=null && marks.size()>0;
  }
  public int getMarkCount() {
    return marks==null ?0 :marks.size();
  }


  /////////////////////////////
  // Text area pass-through: //
  /////////////////////////////

  /** Didn't want to do this but FindAndReplace needs it. Meh.*/
  public MyTextArea getTextArea() {
    return jta;
  }
  public Container getContainer() {
    return jta.getContainer();
  }
  public void requestFocus(){
    jta.requestFocus();
    // This is to fix an OSX bug where the font renders wrong when we switch
    // editors
    if (currentOS.isOSX)
      jta.repaint();
  }
  public boolean isAnythingSelected() {
    return jta.isAnythingSelected();
  }
  public String getSelection() {
    return jta.getSelectedText();
  }
  public int getRow(int offset) {
    try {
      return jta.getLineOfOffset(offset);
    } catch (Exception e) {
      failHandler.set(e);
      return -1;
    }
  }
  public int getRowOffset(int row) {
    try {
      return jta.getLineStartOffset(row);
    } catch (Exception e) {
      failHandler.set(e);
      return -1;
    }
  }
  public int getCaretPos() {
    return jta.getCaret().getDot();
  }

  public void undo() {
    jta.undo();
  }
  public void redo() {
    jta.redo();
  }
  public void undoToBeginning() {
    jta.undoToBeginning();
  }
  public void redoToEnd() {
    jta.redoToEnd();
  }
  public boolean hasUndos() {
    return jta.hasUndos();
  }
  public boolean hasRedos() {
    return jta.hasRedos();
  }
  public void setFastUndos(boolean fast) {
    jta.setFastUndos(fast);
  }
  public void clearUndos() {
    jta.clearUndos();
  }
  public void clearRedos() {
    jta.clearRedos();
  }


  ///////////////////////////
  // Other public methods: //
  ///////////////////////////

  public File getFile() {
    return file;
  }
  public boolean sameFile(File other) {
    return file!=null && file.equals(other);
  }
  public boolean isUsed() {
    return used;
  }
  public boolean hasUnsavedChanges() {
    return unsavedChanges;
  }

  public void doUpperCase(){
    replaceSelection(jta.getSelectedText().toUpperCase());
  }
  public void doLowerCase(){
    replaceSelection(jta.getSelectedText().toLowerCase());
  }
  public void loadFile(File file, String defaultLineBreaker) throws Exception {
    doLoadFile(file, defaultLineBreaker);
  }
  public void saveFile(File file) throws Exception {
    doSaveFile(file);
  }
  public boolean doSortLines() {
    return sortLines();
  }
  public void doInsertToAlign(boolean above) {
    insertToAlign(above);
  }
  public void doBackspaceToAlign(boolean above) {
    deleteToAlign(above);
  }

  //////////////////////
  //                  //
  // PRIVATE METHODS: //
  //                  //
  //////////////////////

  // MARKS: //

  private boolean debugMarks() {
    return true;
  }
  private int setMark() {
    int i=jta.getCaretPosition();
    if (marks==null)
      marks=new LinkedList<Integer>();
    //Insert mark into our sorted list.
    //Not going to do a fancy algorithm because it just isn't important and
    //there won't be that many marks.
    int index=0;
    for (ListIterator<Integer> iterNums=marks.listIterator(); iterNums.hasNext();){
      index++;
      int toCompare=iterNums.next();
      if (toCompare==i)
        return -1;
      if (toCompare>i){
        iterNums.previous();
        iterNums.add(i);
        return index;
      }
    }
    marks.add(i);
    return index+1;
  }
  private int goToPreviousMark(boolean moveCursor) {
    if (marks==null || marks.size()==0)
      return -1;
    int currPos=jta.getCaretPosition();
    int count=marks.size()+1;
    for (ListIterator<Integer> iterNums=marks.listIterator(marks.size()); iterNums.hasPrevious();){
      count--;
      final int toCompare=iterNums.previous();
      if (toCompare<currPos || (toCompare==currPos && count==1)){
        if (moveCursor) {
          jta.scrollCaretToMiddle(toCompare);
          jta.highlightCaret(toCompare);
        }
        return count;
      }
    }
    return -1;
  }
  private int goToNextMark() {
    if (marks==null || marks.size()==0)
      return -1;
    int currPos=jta.getCaretPosition();
    int count=0;
    for (ListIterator<Integer> iterNums=marks.listIterator(); iterNums.hasNext();){
      count++;
      final int toCompare=iterNums.next();
      if (toCompare>currPos || (toCompare==currPos && count==marks.size())){
        jta.scrollCaretToMiddle(toCompare);
        jta.highlightCaret(toCompare);
        return count;
      }
    }
    return -1;
  }
  private int clearCurrentMark() {
    int i=jta.getCaretPosition();
    for (ListIterator<Integer> iterNums=marks.listIterator(); iterNums.hasNext();){
      int n=iterNums.next();
      if (n>i)
        return -1;
      else
      if (n==i){
        iterNums.remove();
        return marks.size();
      }
    }
    return -1;
  }

  /**
   * Invoked on document change. This might be pushing the limit on performance,
   * since it's going to be invoked every time you type a character in. Oh well, so
   * far it's not a problem.
   */
  private void updateMarks(int start, int len, boolean insert) {
    if (marks==null || marks.size()==0)
      return;
    int end=start+len;
    boolean nowBefore=false;
    for (ListIterator<Integer> li=marks.listIterator(); li.hasNext();) {
      int a=li.next();
      if ((nowBefore|=start<a)) {
        if (insert)
          li.set(a+len);
        else
        if (end>a)
          li.set(start);
        else
          li.set(a-len);
      }
    }
    debugMarks();
  }

  // SORT: //

  private boolean sortLines() {
    try {
      Caret c=jta.getCaret();
      int dot=c.getDot(), mark=c.getMark();
      int startSel=dot<=mark ?dot  :mark,
          endSel  =dot<=mark ?mark :dot;
      int rowFirst=jta.getLineOfOffset(startSel),
          rowLast =jta.getLineOfOffset(endSel);
      if (rowFirst==rowLast)
        return false;
      List<String> lines=new ArrayList<>(1+rowLast-rowFirst);
      int veryFirst=-1, veryLast=-1;

      for (int r=rowFirst; r<=rowLast; r++){
        int begin=jta.getLineStartOffset(r),
            end  =jta.getLineEndOffset(r);
        if (r==rowFirst)  veryFirst=begin;
        String line=jta.getText(begin, end-begin);
        if (r!=rowLast || !line.equals("\n")) {
          lines.add(line);
          veryLast =end;
        }
      }
      int lineCount=lines.size();
      boolean newLineAtEnd=lines.get(lineCount-1).endsWith("\n");

      //Now sort:
      java.util.Collections.sort(lines, lineSorter);
      StringBuilder sb=new StringBuilder(Math.max(2+veryLast-veryFirst, 2));
      for (int i=0; i<lineCount; i++){
        String s=lines.get(i);
        if (!newLineAtEnd) {
          boolean hasNewLine=s.endsWith("\n");
          if (i==lineCount-1) {
            if (hasNewLine)
              s=s.substring(0, s.length()-1);
          }
          else
          if (!hasNewLine)
            s+="\n";
        }
        sb.append(s);
      }
      jta.betterReplaceRange(sb.toString(), veryFirst, veryLast);
    } catch (Exception e) {
      failHandler.set(e);
    }
    return true;
  }
  private static Comparator<String> lineSorter=new Comparator<String>(){
    public int compare(String a, String b){
      return a.compareTo(b);
    }
  };


  // ALIGN: //


  private boolean deleteToAlign(boolean above) {
    try {
      int cpos;
      {
        Caret c=jta.getCaret();
        int dot=c.getDot(), mark=c.getMark();
        cpos=dot<=mark ?dot  :mark;
      }

      int rowNum=jta.getLineOfOffset(cpos);
      int currLineStartOffset=jta.getLineStartOffset(rowNum);
      int colIndex=cpos-currLineStartOffset;
      if (colIndex==0)
        return false;

      //Get first of trailing spaces on current line:
      int currLineLimit=-1;
      {
        String currLineToCursor=jta.getText(currLineStartOffset, colIndex);
        int len=currLineToCursor.length();
        for (int i=len-1; i>=0; i--) {
          char c=currLineToCursor.charAt(i);
          if (c!=' ' && c!='\t')
            break;
          currLineLimit=i;
        }
      }
      if (currLineLimit==-1)
        return false;

      String line=null;
      if (above) {
        int min=Math.max(-1, rowNum-4),
            rr=rowNum-1;
        while (rr>min && line==null)
          line=checkLineForBackAlign(rr--, currLineLimit, colIndex);
      }
      else {
        int max=Math.min(jta.getLineCount(), rowNum+4),
            rr=rowNum+1;
        while (rr<max && line==null)
          line=checkLineForBackAlign(rr++, currLineLimit, colIndex);
      }
      int len=line==null ?0 :line.length();
      int newPos=len<colIndex ?len :Spaceable.getLeft(line);
      if (newPos<currLineLimit)
        newPos=currLineLimit;
      jta.betterReplaceRange(null, currLineStartOffset+newPos, cpos);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private String checkLineForBackAlign(int row, int currLineLimit, int currLinePos) throws Exception {
    int pStart=jta.getLineStartOffset(row);
    int pEnd  =Math.min(pStart+currLinePos, jta.betterLineEndOffset(row));
    int diff  =pEnd-pStart;
    if (diff>=currLineLimit) {
      String temp=trimRight(jta.getText(pStart, diff));
      if (!temp.equals("") && temp.length()>=currLineLimit)
        return temp;
    }
    return null;
  }

  private boolean insertToAlign(boolean above) {
    try {
      int cpos=jta.getCaretPosition();
      int rowNum=jta.getLineOfOffset(cpos);
      int colIndex=cpos-jta.getLineStartOffset(rowNum);

      String line=null;
      if (above) {
        int min=Math.max(-1, rowNum-4),
            rr=rowNum-1;
        while (rr>min && line==null)
          line=checkLineForInsertAlign(rr--, colIndex);
      }
      else {
        int max=Math.min(jta.getLineCount(), rowNum+3),
            rr=rowNum+1;
        while (rr<max && line==null)
          line=checkLineForInsertAlign(rr++, colIndex);
      }

      //Get the bump for the best matching line found and insert:
      int bump=line==null
        ?1
        :Spaceable.getRight(line);
      if (bump==0)
        bump=1;
      StringBuilder sb=new StringBuilder(bump);
      for (int i=0; i<bump; i++) sb.append(" ");
      jta.insert(sb.toString(), cpos);
      return true;
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  private String checkLineForInsertAlign(int row, int currLinePos) throws Exception {
    int pStart=jta.getLineStartOffset(row)+currLinePos,
        pEnd  =jta.betterLineEndOffset(row);
    if (pStart<pEnd) {
      String temp=trimRight(jta.getText(pStart, pEnd-pStart));
      if (temp.length()>0)
        return temp;
    }
    return null;
  }
  private String trimRight(String s) {
    int last=s.length();
    for (int i=last-1; i>=0; i--){
      char t=s.charAt(i);
      if (t==' ' || t=='\t')
        last=i;
      else
        break;
    }
    if (last==0) return "";
    return last<s.length() ?s.substring(0, last) :s;
  }



  //ETC: //

  private void replaceSelection(String text) {
    try {
      Caret c=jta.getCaret();
      final int i=c.getMark(), j=c.getDot();
      jta.betterReplaceRange(
        text, i<j ?i :j, i<j ?j :i
      );
    } catch (Exception e) {
      failHandler.set(e);
    }
  }


  /////////////
  // EVENTS: //
  /////////////

  private void setEvents(CurrentOS currentOS) {

    //File drag & drop:
    jta.setDropTarget(myDropTarget);

    //Gotta listen to the caret so we show the correct
    //row & column, for one thing:
    jta.addCaretListener(new CaretListener(){
      public void caretUpdate(CaretEvent e) {
        editListener.doCaretMoved(Editor.this, e.getDot());
      }
    });

    //Document change listening, for red thing and so on:
    jta.getDocument().addDocumentListener(docListener);

    doCapsCrap();

    //Ctrl-W is an extra file close shortcut:
    if (!currentOS.isOSX)
      KeyMapper.accel(
        jta, "EditorCtrlW",
        new AbstractAction() {
          public void actionPerformed(ActionEvent ae) {
            editListener.closeEditor();
          }
        },
        KeyEvent.VK_W, KeyEvent.CTRL_DOWN_MASK
      );
  }


  private void doCapsCrap(){
    //Caps lock detection: So this is hard. We need to check when the caps button is
    //pressed, and sometimes we get that. But we also need to check when we get focus,
    //in case it changed when we weren't around. But we also need to check every time
    //you press a key, because... Sigh. And it still works halfway. And that depends on
    //operating system. So... sigh.
    Action capsAction=new AbstractAction() {
      public void actionPerformed(ActionEvent ae) {
        checkCapsLock();
      }
    };
    KeyMapper.accel(jta, "EditorCapsLock1", capsAction, KeyEvent.VK_CAPS_LOCK);
    KeyMapper.accel(jta, "EditorCapsLock2", capsAction, KeyEvent.VK_CAPS_LOCK,
                    KeyEvent.SHIFT_DOWN_MASK);
    KeyMapper.accel(jta, "EditorCapsLock3", capsAction, KeyEvent.VK_CAPS_LOCK,
                    KeyEvent.CTRL_DOWN_MASK);
    KeyMapper.accel(jta, "EditorCapsLock3", capsAction, KeyEvent.VK_CAPS_LOCK,
                    KeyEvent.ALT_DOWN_MASK);
    KeyMapper.accel(jta, "EditorCapsLock4", capsAction, KeyEvent.VK_CAPS_LOCK,
                    KeyEvent.SHIFT_DOWN_MASK, KeyEvent.CTRL_DOWN_MASK);
    jta.addFocusListener(
      new FocusAdapter(){
        public void focusGained(FocusEvent e){
          checkCapsLock();
        }
      }
    );
    jta.addKeyListener(myKeyListener);
  }
  private void checkCapsLock() {
    boolean state=toolkit.getLockingKeyState(KeyEvent.VK_CAPS_LOCK);
    editListener.doCapsLock(state);
  }
  KeyAdapter myKeyListener=new KeyAdapter() {
    public void keyReleased(KeyEvent e){
      //No, doing this in keyPressed() doesn't work:
      checkCapsLock();
    }
    public void keyPressed(KeyEvent e){
      externalKeyListener.set(e);
    }
  };


  private DropTarget myDropTarget=new DropTarget() {
    public synchronized void drop(DropTargetDropEvent evt) {
      try {
        evt.acceptDrop(DnDConstants.ACTION_COPY);
        List droppedFiles=(List)evt.getTransferable().getTransferData(DataFlavor.javaFileListFlavor);
        for (Object file : droppedFiles)
          editListener.fileDropped((File)file);
      } catch (Exception ex) {
        failHandler.set(ex);
      }
    }
  };


  /////////////////////////////////////////////////////////////////////////
  // NOTE: MyDocumentListener always gets invoked before myUndoListener. //
  //       This affects the used & unsavedChanges variables.             //
  /////////////////////////////////////////////////////////////////////////

  private class MyDocumentListener implements DocumentListener {
    public void insertUpdate(DocumentEvent de) {
      unsavedChanges=true;
      used|=true;
      updateMarks(de.getOffset(), de.getLength(), true);
      editListener.doEditorChanged(Editor.this);
    }
    public void removeUpdate(DocumentEvent de) {
      unsavedChanges=true;
      used|=true;
      updateMarks(de.getOffset(), de.getLength(), false);
      editListener.doEditorChanged(Editor.this);
    }
    public void changedUpdate(DocumentEvent e) {
      unsavedChanges=true;
      editListener.doEditorChanged(Editor.this);
    }
  }
  private UndoListener myUndoListener=new UndoListener() {
    public void happened(UndoEvent ue) {
      if (unsavedChanges && ue.isUndoSaveStable) {
        unsavedChanges=false;
        editListener.doEditorChanged(Editor.this);
      }
      if ((ue.isNoMoreUndos || ue.isUndoSaveStable) && file==null && jta.getLineCount()==1 && "".equals(jta.getText())){
        used=false;
      }
    }
  };


  ////////////////
  // LOAD/SAVE: //
  ////////////////


  private void doLoadFile(File file, String defaultLineBreaker) throws Exception {
    jta.reset();
    jta.setSuppressUndo(true);
    jta.getDocument().removeDocumentListener(docListener);
    try {
      FileMetaData res=KFileIO.load(jta, file);
      encoding=res.encoding;
      encodingNeedsBOM=res.encodingNeedsBOM;
      lineBreaker=res.delimiter;
      if (lineBreaker==null)
        lineBreaker=defaultLineBreaker;
      setTabsOrSpaces(res.hasTabs ?TabAndIndentOptions.INDENT_TABS
                                  :TabAndIndentOptions.INDENT_SPACES);
    } finally {
      jta.setSuppressUndo(false);
      jta.getDocument().addDocumentListener(docListener);
    }
    used|=true;
    setFile(file);
    unsavedChanges=false;
    jta.setCaretPosition(0);
  }
  private void doSaveFile(File saveToFile) throws Exception {
    if (autoTrimOnSave) {
      int lc=jta.getLineCount();
      for (int line=0; line<lc; line++) {
        int
          end=jta.getLineEndOffset(line),
          start=jta.getLineStartOffset(line);
        if (line < lc-1)
          end-=1;
        String lineStr=jta.getText(start, end-start);
        if (lineStr.endsWith(" ")) {
          while (lineStr.endsWith(" "))
            lineStr=lineStr.substring(0, lineStr.length()-1);
          jta.betterReplaceRange(lineStr, start, end);
        }
      }
    }
    used|=true;
    KFileIO.save(jta, saveToFile, lineBreaker, encoding, encodingNeedsBOM);
    if (saveToFile!=file)
      setFile(saveToFile);
    unsavedChanges=false;
    jta.setSaved();
  }
  private void setFile(File newFile) {
    file=newFile;
    try {
      title=file.getCanonicalPath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}
