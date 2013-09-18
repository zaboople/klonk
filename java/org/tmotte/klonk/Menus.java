package org.tmotte.klonk;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import org.tmotte.common.swang.Fail;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MenuUtils;

public class Menus {

  //Various instance variables:
  private Fail failer;
  private final Klonk klonk;
  private Map<JMenuItem,Editor> switchMenuToEditor=new Hashtable<>();
  private MenuUtils mu=new MenuUtils();
  private int maxRecent;

  //Visual component instances:
  private JMenuBar bar=new JMenuBar();
  private JMenu file, fileReopen, fileOpenFromRecentDir, fileSaveToRecentDir, 
                fileFave, fileOpenFromFave, fileSaveToFave,
                search, mark, switcher, undo, select, align, external, options, help;
  private JMenuItem fileOpen, fileSave, fileNew, fileSaveAs, fileClose, 
                    fileCloseOthers, fileCloseAll,
                    filePrint,              
                    fileDocDirWindows, fileClipboardDoc, fileClipboardDocDir, 
                    fileOpenFromDocDir, fileSaveToDocDir,
                    fileFaveAddFile, fileFaveAddDir,
                    fileExit,
                    searchFind, searchReplace, searchRepeat, searchRepeatBackwards, searchGoToLine,
                    switchBackToFront, switchFrontToBack,
                    markSet, markGoToPrevious, markGoToNext, markClearCurrent, markClearAll,
                    undoUndo, undoRedo,
                    undoToBeginning, undoRedoToEnd, undoClearUndos, undoClearRedos, undoClearBoth,
                    selectUpperCase, selectLowerCase, selectSortLines, selectGetSelectionSize,  selectGetAsciiValues, 
                    weirdInsertToAlign, weirdInsertToAlignBelow, weirdBackspaceToAlign, weirdBackspaceToAlignBelow,
                    externalRunBatch,
                    optionTabsAndIndents, optionLineDelimiters, optionFont, optionFavorites,
                    helpAbout, helpShortcut;
  private JCheckBoxMenuItem undoFast, optionWordWrap;


  /////////////////////
  //                 //
  // PUBLIC METHODS: //
  //                 //
  /////////////////////


  /////////////////////
  // INITIALIZATION: //
  /////////////////////
  
  public Menus(final Klonk klonk, Fail failer) {
    this.klonk=klonk;
    this.failer=failer;
    create();
  }
  public Menus setMaxRecent(int i) {
    maxRecent=i;
    return this;
  }
  public JMenuBar getMenuBar() {
    return bar;
  }

  //////////////////
  // SWITCH MENU: //
  //////////////////
  
  public void setSwitchMenu(List<Editor> startList) {
    switcher.removeAll();
    switchMenuToEditor.clear();
    int easyLen=3;
    
    //Build first menu, a quick-list of recent files:
    for (int i=0; i<Math.min(easyLen, startList.size()); i++) {
      Editor e=startList.get(i);
      JMenuItem j=i==0
        ?mu.doMenuItemCheckbox(e.title, switchListener)   
        :mu.doMenuItem(
          e.title, switchListener, -1, 
          i==1 ?KeyMapper.key(KeyEvent.VK_F12,0) :null
        );
      switchMenuToEditor.put(j, e);
      switcher.add(j);
    }
    
    //Build second menu, a longer sorter list of all files you have open:
    if (startList.size()>easyLen){
      switcher.addSeparator();
      Editor first=startList.get(0);
      List<Editor> list=new ArrayList<Editor>(startList.size());
      for (Editor e: startList)
        list.add(e);
      Collections.sort(list, switchSorter);
      for (int i=0; i<list.size(); i++) {
        Editor e=list.get(i);
        JMenuItem j=e==first
          ?mu.doMenuItemCheckbox(e.title, switchListener)   
          :mu.doMenuItem(e.title, switchListener);
        switchMenuToEditor.put(j, e);
        switcher.add(j);
      }
    }
    
    //Build third menu, which swaps current for the other:
    if (startList.size()>1){
      switcher.addSeparator();
      switcher.add(switchFrontToBack);
      switcher.add(switchBackToFront);
    }
    
    fileCloseOthers.setEnabled(startList.size()>1);
  }
  private static Comparator<Editor> switchSorter=new Comparator<Editor> () {
    public int compare(Editor e1, Editor e2) {
      return e1.title.compareTo(e2.title);
    }
  };

  /////////////////////////////////////////////
  // RECENT & FAVORITE FILES/DIRS SAVE/OPEN: //
  /////////////////////////////////////////////

  public Menus setFiles(List<String> recentFiles, List<String> recentDirs, 
                        List<String> favoriteFiles, List<String> favoriteDirs) {
    setRecentFiles(recentFiles);
    setRecentDirs(recentDirs);
    setFavoriteFiles(favoriteFiles);
    setFavoriteDirs(favoriteDirs);
    return this;
  }
  public void setFavoriteFiles(List<String> startList) {
    setFavorites(startList, fileFave, reopenListener, 2);
  }
  public void setFavoriteDirs(List<String> startList) {
    setFavorites(startList, fileOpenFromFave, openFromListener, 0);
    setFavorites(startList, fileSaveToFave,   saveToListener, 0);
  }
  public void setRecentFiles(List<String> startList) {
    setRecent(startList, fileReopen, reopenListener);
  }
  public void setRecentDirs(List<String> startList) {
    setRecent(startList, fileOpenFromRecentDir, openFromListener);
    setRecent(startList, fileSaveToRecentDir,   saveToListener);
  }

  public void showHasFile(boolean has) {
    if (fileDocDirWindows!=null)
      fileDocDirWindows.setEnabled(has);
    fileFaveAddFile.setEnabled(has);
    fileFaveAddDir.setEnabled(has);
    fileClipboardDoc.setEnabled(has);
    fileClipboardDocDir.setEnabled(has);
    fileOpenFromDocDir.setEnabled(has);
  }
  public Menus setFastUndos(boolean fast) {
    undoFast.setState(fast);
    return this;
  }
  public Menus setWordWrap(boolean w) {
    optionWordWrap.setState(w);
    return this;
  }
  public Menus showHasMarks(boolean has) {
    markGoToPrevious.setEnabled(has);
    markGoToNext.setEnabled(has);
    markClearCurrent.setEnabled(has);
    markClearAll.setEnabled(has);
    return this;
  }

  //////////////////////
  //                  //
  // PRIVATE METHODS: //
  //                  //
  //////////////////////


  private void setRecent(List<String> startList, JMenu menuX, Action listener) {
    menuX.removeAll();
    menuX.setEnabled(startList.size()>0);
    int easyLen=3;
    
    //Build first menu, a quick-list of recent files:
    for (int i=0; i<Math.min(easyLen, startList.size()); i++) 
      menuX.add(mu.doMenuItem(startList.get(i), listener));
    
    //Build second menu, a longer sorter list of all files you have open:
    if (startList.size()>easyLen){
      menuX.addSeparator();
      List<String> list=new ArrayList<String>(startList.size());
      for (String s: startList)
        list.add(s);
      Collections.sort(list);
      for (String s: list) 
        menuX.add(mu.doMenuItem(s, listener));
    }
  }
  private void setFavorites(List<String> startList, JMenu menuX, Action listener, int skipLast) {
    JMenuItem[] skipped=new JMenuItem[skipLast];
    {
      int count=menuX.getItemCount();
      int start=count-skipLast;
      for (int i=start; i<count; i++)
        skipped[i-start]=menuX.getItem(i);
    }
    menuX.removeAll();
    for (String s: startList) 
      menuX.add(mu.doMenuItem(s, listener));
    if (skipLast>0){
      menuX.addSeparator();
      for (JMenuItem i: skipped)
        menuX.add(i);
    }
    else
      menuX.setEnabled(menuX.getItemCount()>0);
  }

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////
  
  //Unlike a lot of other create/layout/listens, this is almost all-in-one,
  //since everything is generic and we can chain function calls like an
  //absolute lunatic, which is fun.
  private void create() {
    bar.setBorderPainted(false);
    
    file=mu.doMenu(bar, "File", KeyEvent.VK_F);
    
    //File menu section 1:
    mu.add(
       file
      ,fileOpen =mu.doMenuItem("Open...", fileListener, KeyEvent.VK_O)
      ,fileNew  =mu.doMenuItem("New",     fileListener, KeyEvent.VK_N)
      ,fileSave =mu.doMenuItem(
        "Save",    fileListener, KeyEvent.VK_S, 
        KeyMapper.key(KeyEvent.VK_S, InputEvent.CTRL_DOWN_MASK)
      )
      ,fileSaveAs=mu.doMenuItem("Save as...",   fileListener, KeyEvent.VK_A)
      ,fileClose =mu.doMenuItem(
        "Close",   fileListener, KeyEvent.VK_C,
        KeyMapper.key(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK)
      )
      ,fileCloseOthers=mu.doMenuItem(
        "Close others",   fileListener, KeyEvent.VK_L
      )
      ,fileCloseAll=mu.doMenuItem(
        "Close all",   fileListener, KeyEvent.VK_E
      )
    );
    
    //File menu section 2 (print):
    file.addSeparator();
    mu.add(
       file
      ,filePrint =mu.doMenuItem(
        "Print", fileListener, KeyEvent.VK_P,
        KeyMapper.key(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)
      )
    );
    
    //File menu section 3:
    file.addSeparator();
    String os=System.getProperty("os.name");
    if (os!=null && os.toLowerCase().indexOf("windows")>-1)
      file.add(fileDocDirWindows =mu.doMenuItem(
        "Open document's directory", fileListener, KeyEvent.VK_D
      ));
    mu.add(
      file, 
      fileClipboardDoc=mu.doMenuItem(
        "Copy document name to clipboard", fileListener, KeyEvent.VK_Y
      ),
      fileClipboardDocDir=mu.doMenuItem(
        "Copy document directory to clipboard", fileListener, KeyEvent.VK_U
      )
    );

    //File menu section 4:
    file.addSeparator();
    mu.add(
      file
      ,mu.add(
        mu.doMenu("Open from", KeyEvent.VK_F)
        ,fileOpenFromDocDir=mu.doMenuItem(
          "Current document directory", fileListener, KeyEvent.VK_C
        )
        ,fileOpenFromRecentDir=mu.doMenu(
          "Recent directory", KeyEvent.VK_R
        )
        ,fileOpenFromFave=mu.doMenu(
          "Favorite directory", KeyEvent.VK_F
        )
      )
      ,mu.add(
        mu.doMenu("Save to", KeyEvent.VK_V)
        ,fileSaveToDocDir=mu.doMenuItem(
          "Current document directory", fileListener, KeyEvent.VK_C
        )
        ,fileSaveToRecentDir=mu.doMenu(
          "Recent directory", KeyEvent.VK_R
        )
        ,fileSaveToFave=mu.doMenu(
          "Favorite directory", KeyEvent.VK_F 
        )
      )
      ,
      mu.add(
        fileFave  =mu.doMenu("Favorite files", KeyEvent.VK_I)
        ,
        fileFaveAddFile=mu.doMenuItem(
          "Add current file to favorites", fileListener, KeyEvent.VK_A
        )
        ,
        fileFaveAddDir=mu.doMenuItem(
          "Add current directory to favorites", fileListener, KeyEvent.VK_D
        )
      )
      ,
      fileReopen=mu.doMenu("Re-open", KeyEvent.VK_R)
    );

    //File menu section 5 (Exit)
    file.addSeparator();
    file.add(fileExit  =mu.doMenuItem(
      "Exit",    fileListener, KeyEvent.VK_X,
      KeyMapper.key(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK)
    ));


    //SWITCH:    
    switcher=mu.doMenu(bar, "Switch",     KeyEvent.VK_W);
    switchFrontToBack=mu.doMenuItem(
      "Send front to back", switchListener, KeyEvent.VK_S, KeyMapper.key(KeyEvent.VK_F11)
    );
    switchBackToFront=mu.doMenuItem(
      "Send back to front", switchListener, KeyEvent.VK_S, KeyMapper.key(KeyEvent.VK_F11, KeyEvent.SHIFT_DOWN_MASK)
    );

    
    //SEARCH:
    search=mu.doMenu(bar, "Search", KeyEvent.VK_S);
    mu.add(
      search
      ,
      searchFind=mu.doMenuItem(
        "Find", searchListener, KeyEvent.VK_F,
        KeyMapper.key(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
      )
      ,
      searchReplace=mu.doMenuItem(
        "Replace", searchListener, KeyEvent.VK_R,
        KeyMapper.key(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK)
      )
    );
    search.addSeparator();
    mu.add(
      search
      ,
      searchRepeat=mu.doMenuItem(
        "Repeat find/replace", searchListener, KeyEvent.VK_P,
        KeyMapper.key(KeyEvent.VK_F3)
      )
      ,
      searchRepeatBackwards=mu.doMenuItem(
        "...Backwards", searchListener, KeyEvent.VK_B,
        KeyMapper.key(KeyEvent.VK_F3, KeyEvent.SHIFT_DOWN_MASK)
      )
    );
    search.addSeparator();
    mu.add(
      search
      ,
      searchGoToLine=mu.doMenuItem(
        "Go to line number...", searchListener, KeyEvent.VK_O,
        KeyMapper.key(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK)
      )
    );
        
    
    //MARK:
    mark=mu.doMenu(bar, "Mark", markListener, KeyEvent.VK_M);
    mu.add(
      mark,
      markSet=mu.doMenuItem(
        "Set mark", 
        markItemListener, KeyEvent.VK_S, KeyMapper.key(KeyEvent.VK_F4)
      )
      ,
      markGoToPrevious=mu.doMenuItem(
        "Go to previous mark", 
        markItemListener, KeyEvent.VK_G, KeyMapper.key(KeyEvent.VK_F8)
      )
      ,
      markGoToNext=mu.doMenuItem(
        "Go to next mark", 
        markItemListener, KeyEvent.VK_O, KeyMapper.key(KeyEvent.VK_F9)
      )
      ,
      markClearCurrent=mu.doMenuItem(
        "Clear current mark", 
        markItemListener, KeyEvent.VK_C
      )
      ,
      markClearAll=mu.doMenuItem(
        "Clear all marks", 
        markItemListener, KeyEvent.VK_L
      )
    );
    showHasMarks(false);


    //UNDO:
    undo=mu.doMenu(bar, "Undo", KeyEvent.VK_U);
    mu.add(
      undo
      ,
      undoUndo=mu.doMenuItem(
        "Undo", undoItemListener, KeyEvent.VK_U, 
        KeyMapper.key(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK)
      )
      ,
      undoRedo=mu.doMenuItem(
        "Redo", undoItemListener, KeyEvent.VK_R, 
        KeyMapper.key(KeyEvent.VK_Z, KeyEvent.CTRL_DOWN_MASK, KeyEvent.SHIFT_DOWN_MASK)
      )
    );
    undo.addSeparator();
    mu.add(
      undo
      ,
      undoToBeginning=mu.doMenuItem(
        "Undo to beginning", undoItemListener, KeyEvent.VK_D, 
        KeyMapper.key(KeyEvent.VK_F9, KeyEvent.CTRL_DOWN_MASK, KeyEvent.SHIFT_DOWN_MASK)
      )
      ,
      undoRedoToEnd=mu.doMenuItem(
        "Redo to end", undoItemListener, KeyEvent.VK_T, 
        KeyMapper.key(KeyEvent.VK_F12, KeyEvent.CTRL_DOWN_MASK, KeyEvent.SHIFT_DOWN_MASK)
      )
    );
    undo.addSeparator();
    undo.add(
      undoFast=mu.doMenuItemCheckbox(
        "Use fast undos", undoItemListener, KeyEvent.VK_F, false
      )
    );
    undo.addSeparator();
    mu.add(
      undo
      ,
      undoClearUndos=mu.doMenuItem("Clear undo stack", undoItemListener)
      ,
      undoClearRedos=mu.doMenuItem("Clear redo stack", undoItemListener)
      ,
      undoClearBoth=mu.doMenuItem("Clear both", undoItemListener)
    );


    //WEIRD:
    mu.add(
      select=mu.doMenu(bar, "Selection",   selectListener, KeyEvent.VK_E)
      ,selectUpperCase=mu.doMenuItem(
        "Uppercase selection", 
        selectionItemListener, KeyEvent.VK_U
      )
      ,selectLowerCase=mu.doMenuItem(
        "Lowercase selection", 
        selectionItemListener, KeyEvent.VK_L
      )
      ,selectSortLines=mu.doMenuItem(
        "Sort selected lines", 
        selectionItemListener, KeyEvent.VK_S
      )
      ,selectGetSelectionSize=mu.doMenuItem(
        "Get selection size", 
        selectionItemListener, KeyEvent.VK_G
      )
      ,selectGetAsciiValues=mu.doMenuItem(
        "Get ASCII/Unicode value(s) of selection", 
        selectionItemListener, KeyEvent.VK_A
      )
    );

    //ALIGN:
    mu.add(
      align=mu.doMenu(bar, "Align", KeyEvent.VK_A)
      ,
      weirdInsertToAlign=mu.doMenuItem(
        "Insert spaces to align cursor to above", 
        alignItemListener, KeyEvent.VK_I,
        KeyMapper.key(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)
      )
      , 
      weirdInsertToAlignBelow=mu.doMenuItem(
        "Insert spaces to align cursor to below", 
        alignItemListener, KeyEvent.VK_N,
        KeyMapper.key(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK, KeyEvent.SHIFT_DOWN_MASK)
      )
      , 
      weirdBackspaceToAlign=mu.doMenuItem(
        "Backspace to align cursor to above", 
        alignItemListener, KeyEvent.VK_B,
        KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK)
      )
      , 
      weirdBackspaceToAlignBelow=mu.doMenuItem(
        "Backspace to align cursor to below", 
        alignItemListener, KeyEvent.VK_C,
        KeyMapper.key(KeyEvent.VK_BACK_SPACE, KeyEvent.CTRL_DOWN_MASK, KeyEvent.SHIFT_DOWN_MASK)
      )
    );
    
    //EXTERNAL:
    mu.add(
      external=mu.doMenu(bar, "External", KeyEvent.VK_X)
      ,externalRunBatch=mu.doMenuItem(
        "Run batch program", 
        externalItemListener, KeyEvent.VK_R,
        KeyMapper.key(KeyEvent.VK_E, KeyEvent.CTRL_DOWN_MASK)
      )
    );
               
    //OPTIONS:
    mu.add(
      options=mu.doMenu(bar, "Options",   KeyEvent.VK_O)
      ,optionWordWrap      =mu.doMenuItemCheckbox("Word wrap",            optionListener, KeyEvent.VK_W, false)
    );
    options.addSeparator();
    mu.add(
      options
      ,optionTabsAndIndents=mu.doMenuItem("Tabs & Indents",               optionListener, KeyEvent.VK_T)
      ,optionLineDelimiters=mu.doMenuItem("Line delimiters",              optionListener, KeyEvent.VK_L)
      ,optionFont          =mu.doMenuItem("Font & colors",                optionListener, KeyEvent.VK_F)
      ,optionFavorites     =mu.doMenuItem("Favorite files & directories", optionListener, KeyEvent.VK_A)
    );
    
    //HELP:
    mu.add(
      help=mu.doMenu(bar, "Help",   KeyEvent.VK_H)
      ,helpAbout   =mu.doMenuItem("About Klonk",                   helpListener, KeyEvent.VK_A)
      ,helpShortcut=mu.doMenuItem("Shortcuts and hidden features", helpListener, KeyEvent.VK_S)
    );
  }
  private Action
    fileListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==fileOpen)         klonk.doFileOpenDialog();
        else
        if (s==fileNew)          klonk.doNew();
        else
        if (s==fileSave)         klonk.doSave();
        else
        if (s==fileSaveAs)       klonk.doSave(true);
        else
        if (s==fileClose)        klonk.doFileClose();
        else
        if (s==fileCloseOthers)  klonk.doFileCloseOthers();
        else
        if (s==fileCloseAll)     klonk.doFileCloseAll();
        else
        if (s==filePrint)        klonk.doPrint();
        else
        if (s==fileDocDirWindows)    klonk.doDocumentDirectoryMSWindows();
        else
        if (s==fileClipboardDoc)     klonk.doClipboardDoc();
        else
        if (s==fileClipboardDocDir)  klonk.doClipboardDocDir();
        else
        if (s==fileOpenFromDocDir)   klonk.doOpenFromDocDir();
        else
        if (s==fileSaveToDocDir)     klonk.doSaveToDocDir();
        else
        if (s==fileFaveAddDir)       klonk.doAddCurrentToFaveDirs();
        else
        if (s==fileFaveAddFile)      klonk.doAddCurrentToFaveFiles();
        else
        if (s==fileExit)             klonk.doFileExit();
        else
          throw new RuntimeException("Invalid file event "+event);
      }
    }
    ,
    //These have to be individualized because they require the text of the
    //item that fired them, can't detect which.
    reopenListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        klonk.doLoadFile(s.getText());
      }
    }
    ,
    openFromListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        klonk.doOpenFrom(s.getText());
      }
    }
    ,
    saveToListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        klonk.doSaveTo(s.getText());
      }
    }
    ,

    searchListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==searchRepeat)          klonk.doSearchRepeat();
        else
        if (s==searchFind)            klonk.doSearchFind();
        else
        if (s==searchReplace)         klonk.doSearchReplace();
        else
        if (s==searchRepeatBackwards) klonk.doSearchRepeatBackwards();
        else
        if (s==searchGoToLine)        klonk.doSearchGoToLine();
        else
          throw new RuntimeException("What");
      }
    }
    ,
    markItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==markSet)          klonk.doMarkSet();
        else
        if (s==markGoToPrevious) klonk.doMarkGoToPrevious();
        else
        if (s==markGoToNext)     klonk.doMarkGoToNext();
        else
        if (s==markClearCurrent) klonk.doMarkClearCurrent();
        else
        if (s==markClearAll)     klonk.doMarkClearAll();
        else
          throw new RuntimeException("What");
      }
    }
    ,
    switchListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        //Two things can happen, send go back or pick a file:
        if (s==switchBackToFront)
          klonk.doSendBackToFront();
        else
        if (s==switchFrontToBack)
          klonk.doSendFrontToBack();
        else {
          Editor e=switchMenuToEditor.get(s);
          if (e==null)
            failer.fail(new RuntimeException("Menus.switchListener(): Null editor in hash"));
          klonk.doSwitch(e);
        }
      }
    }
    ,
    undoItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==undoUndo)        klonk.doUndo();
        else
        if (s==undoRedo)        klonk.doRedo();
        else
        if (s==undoFast)        klonk.doUndoFast();
        else
        if (s==undoToBeginning) klonk.doUndoToBeginning();
        else
        if (s==undoRedoToEnd)   klonk.doRedoToEnd();
        else
        if (s==undoClearUndos)  klonk.doClearUndos();
        else
        if (s==undoClearRedos)  klonk.doClearRedos();
        else
        if (s==undoClearBoth)   klonk.doClearUndosAndRedos();
      }
    }
    ,
    selectionItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==selectUpperCase)             klonk.doWeirdUpperCase();
        else
        if (s==selectLowerCase)             klonk.doWeirdLowerCase();
        else
        if (s==selectSortLines)             klonk.doWeirdSortLines();
        else
        if (s==selectGetSelectionSize)      klonk.doWeirdSelectionSize();
        else
        if (s==selectGetAsciiValues)        klonk.doWeirdAsciiValues();
      }
    }
    , 
    alignItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==weirdInsertToAlign)         klonk.doWeirdInsertToAlign(true);
        else
        if (s==weirdInsertToAlignBelow)    klonk.doWeirdInsertToAlign(false);
        else
        if (s==weirdBackspaceToAlign)      klonk.doWeirdBackspaceToAlign(true);
        else
        if (s==weirdBackspaceToAlignBelow) klonk.doWeirdBackspaceToAlign(false);
      }
    }
    , 
    externalItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==externalRunBatch)        klonk.doShell();
        else
          throw new RuntimeException("Unexpected "+s);
      }
    }
    , 
    optionListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==optionWordWrap)        klonk.doWordWrap();
        else
        if (s==optionTabsAndIndents)  klonk.doTabsAndIndents();
        else
        if (s==optionLineDelimiters)  klonk.doLineDelimiters();
        else
        if (s==optionFont)            klonk.doFontAndColors();
        else
        if (s==optionFavorites)       klonk.doFavorites();
      }
    }
    , 
    helpListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==helpShortcut)
          klonk.doHelpShortcuts();
        else
        if (s==helpAbout)
          klonk.doHelpAbout();
      }
    }
    ;
  private MenuListener 
    selectListener=new MenuListener(){
      public void menuCanceled(MenuEvent e){}
      public void menuDeselected(MenuEvent e){}
      public void menuSelected(MenuEvent e){
        boolean selected=klonk.isAnythingSelected();
        selectUpperCase.setEnabled(selected);
        selectLowerCase.setEnabled(selected);
        selectSortLines.setEnabled(selected);
        selectGetSelectionSize.setEnabled(selected);
        selectGetAsciiValues.setEnabled(selected);
      }
    }
    ,
    markListener=new MenuListener() {
      public void menuCanceled(MenuEvent e){}
      public void menuDeselected(MenuEvent e){}
      public void menuSelected(MenuEvent e){
        int prev=klonk.getPreviousMark(),
            count=klonk.getMarkCount();
        if (count==0){
          markGoToPrevious.setText("Go to previous mark");
          markGoToNext.setText("Go to next mark");
        }
        else {
          markGoToPrevious.setText(
            "Go to previous mark - "+(prev==-1 ?"None" :prev+" of "+count)
          );
          markGoToNext.setText(
            "Go to next mark - "+(prev==count ?"None" :(prev+1)+" of "+count)
          );
        }
      }
    }
  ;   
}
