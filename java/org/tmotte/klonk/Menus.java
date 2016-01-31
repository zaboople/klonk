package org.tmotte.klonk;
import java.awt.Component;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import java.util.HashMap;
import java.util.Map;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JSeparator;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;
import javax.swing.event.PopupMenuEvent;
import javax.swing.event.PopupMenuListener;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.swang.MenuUtils;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.controller.CtrlMain;
import org.tmotte.klonk.controller.CtrlFileOther;
import org.tmotte.klonk.controller.CtrlMarks;
import org.tmotte.klonk.controller.CtrlOptions;
import org.tmotte.klonk.controller.CtrlOther;
import org.tmotte.klonk.controller.CtrlSearch;
import org.tmotte.klonk.controller.CtrlSelection;
import org.tmotte.klonk.controller.CtrlUndo;
import org.tmotte.klonk.windows.popup.FindAndReplace;

public class Menus {

  //Various instance variables:
  private Editors editors;
  private Map<JMenuItem,Editor> switchMenuToEditor=new Hashtable<>();
  private MenuUtils mu=new MenuUtils();
  private CurrentOS currentOS;
  private JPanel osxAttachPopupsTo;

  // Used when sorting the Switch menu & recent menus, respectively:
  private List<Editor> switchSortSource=new ArrayList<>(20);
  private Map<JMenu, List<String>> recentSortSources=new HashMap<>();

  //Controllers:
  private CtrlMain ctrlMain;
  private CtrlMarks ctrlMarks;
  private CtrlSelection ctrlSelection;
  private CtrlUndo ctrlUndo;
  private CtrlSearch ctrlSearch;
  private CtrlOptions ctrlOptions;
  private CtrlFileOther ctrlFileOther;
  private CtrlOther ctrlOther;

  //Visual component instances:
  private JMenuBar bar=new JMenuBar();
  private JMenu file, fileReopen, fileOpenFromRecentDir, fileSaveToRecentDir,
                fileFave, fileOpenFromFave, fileSaveToFave,
                search, mark, switcher, undo, select, align, external, options, osxShortcuts, help;
  private JMenuItem fileOpen, fileSave, fileNew, fileSaveAs, fileClose,
                    fileCloseOthers, fileCloseAll,
                    filePrint,
                    fileDocDirExplore, fileClipboardDoc, fileClipboardDocDir,
                    fileOpenFromDocDir, fileOpenFromList, fileOpenFromSSH, fileSaveToDocDir, fileSaveToSSH,
                    fileFaveAddFile, fileFaveAddDir,
                    fileExit,
                    searchFind, searchReplace, searchRepeat, searchRepeatBackwards, searchGoToLine,
                    switchBackToFront, switchFrontToBack, switchNextUnsaved, pSwitchNextUnsaved,
                    markSet, markGoToPrevious, markGoToNext, markClearCurrent, markClearAll,
                    undoUndo, undoRedo,
                    undoToBeginning, undoRedoToEnd, undoClearUndos, undoClearRedos, undoClearBoth,
                    selectUpperCase, selectLowerCase, selectSortLines, selectGetSelectionSize,  selectGetAsciiValues,
                    weirdInsertToAlign, weirdInsertToAlignBelow, weirdBackspaceToAlign, weirdBackspaceToAlignBelow,
                    externalRunBatch,
                    optionTabsAndIndents, optionLineDelimiters, optionFont, optionFontBigger, optionFontSmaller,
                    optionFavorites, optionSSH,
                    osxSwitch, osxOpenFrom, osxSaveTo, osxSelect, osxFavorite, osxReopen,
                    helpAbout, helpShortcut;
  private JCheckBoxMenuItem undoFast, optionAutoTrim, optionWordWrap;
  // These are for osx only:
  private JPopupMenu pswitcher, pOpenFrom, pSaveTo, pSelect, pFavorite, pReopen;

  /////////////////////
  //                 //
  // PUBLIC METHODS: //
  //                 //
  /////////////////////


  /////////////////////
  // INITIALIZATION: //
  /////////////////////

  public Menus(Editors editors, CurrentOS currentOS) {
    this.editors=editors;
    this.currentOS=currentOS;
    create();
  }
  public void setControllers(
       CtrlMain ctrlMain
      ,CtrlMarks ctrlMarks
      ,CtrlSelection ctrlSelection
      ,CtrlUndo ctrlUndo
      ,CtrlSearch ctrlSearch
      ,CtrlFileOther ctrlFileOther
      ,CtrlOther ctrlOther
      ,CtrlOptions ctrlOptions
    ){
    this.ctrlMain=ctrlMain;
    this.ctrlMarks=ctrlMarks;
    this.ctrlSelection=ctrlSelection;
    this.ctrlUndo=ctrlUndo;
    this.ctrlSearch=ctrlSearch;
    this.ctrlOptions=ctrlOptions;
    this.ctrlFileOther=ctrlFileOther;
    this.ctrlOther=ctrlOther;
  }
  public JMenuBar getMenuBar() {
    return bar;
  }
  /** This is a workaround for osx making it to hard to keyboard your way to a top menu. */
  public void attachPopups(JPanel pnlEditor) {
    if (currentOS.isOSX)
      osxAttachPopupsTo=pnlEditor;
  }
  public Doer getEditorSwitchListener() {
    return new Doer() {
      public void doIt() {editorChange();}
    };
  }
  public Setter<List<String>> getRecentFileListener() {
    return new Setter<List<String>>(){
      public void set(List<String> files) {setRecentFiles(files);}
    };
  }
  public Setter<List<String>> getRecentDirListener() {
    return new Setter<List<String>>(){
      public void set(List<String> dirs){setRecentDirs(dirs);}
    };
  }
  public Setter<List<String>> getFavoriteFileListener() {
    return new Setter<List<String>>(){
      public void set(List<String> files) {setFavoriteFiles(files);}
    };
  }
  public Setter<List<String>> getFavoriteDirListener() {
    return new Setter<List<String>>(){
      public void set(List<String> dirs){setFavoriteDirs(dirs);}
    };
  }



  /////////////////////////////////////////////
  // RECENT & FAVORITE FILES/DIRS SAVE/OPEN: //
  /////////////////////////////////////////////

  public Menus setFavorites(Collection<String> favoriteFiles, Collection<String> favoriteDirs) {
    setFavoriteFiles(favoriteFiles);
    setFavoriteDirs(favoriteDirs);
    return this;
  }
  public void setFavoriteFiles(Collection<String> startList) {
    setFavorites(startList, fileFave, pFavorite, reopenListener, 2);
  }
  public void setFavoriteDirs(Collection<String> startList) {
    setFavorites(startList, fileOpenFromFave, openFromListener, 0);
    setFavorites(startList, fileSaveToFave,   saveToListener, 0);
  }
  public void setRecentFiles(List<String> startList) {
    setRecent(startList, fileReopen, pReopen, reopenListener);
  }
  public void setRecentDirs(List<String> startList) {
    setRecent(startList, fileOpenFromRecentDir, openFromListener);
    setRecent(startList, fileSaveToRecentDir,   saveToListener);
  }

  public Menus setFastUndos(boolean fast) {
    undoFast.setState(fast);
    return this;
  }
  public Menus setWordWrap(boolean w) {
    optionWordWrap.setState(w);
    return this;
  }
  public Menus setAutoTrim(boolean w) {
    optionAutoTrim.setState(w);
    return this;
  }
  public void editorChange() {
    Editor e=editors.getFirst();
    showHasMarks(e.hasMarks());
    showHasFile(e.getFile()!=null);
    setSwitchMenu();
  }


  //////////////////////
  //                  //
  // PRIVATE METHODS: //
  //                  //
  //////////////////////

  private void showHasFile(boolean has) {
    if (fileDocDirExplore!=null)
      fileDocDirExplore.setEnabled(has);
    fileFaveAddFile.setEnabled(has);
    fileFaveAddDir.setEnabled(has);
    fileClipboardDoc.setEnabled(has);
    fileClipboardDocDir.setEnabled(has);
    fileOpenFromDocDir.setEnabled(has);
    fileSaveToDocDir.setEnabled(has);
  }

  private void showHasMarks(boolean has) {
    markGoToPrevious.setEnabled(has);
    markGoToNext.setEnabled(has);
    markClearCurrent.setEnabled(has);
    markClearAll.setEnabled(has);
  }

  /**
   * Doing a removeAll() call messes up our popup menus that derive from the JMenu
   * so we preserve the title JLabel & separator here.
   */
  private void cleanMenu(JMenu menuX, JPopupMenu menuP) {
    Component[] cs=
      menuP==null
        ?null
        :menuP.getComponents();
    menuX.removeAll();
    if (menuP!=null)
      for (int i=0; i<cs.length && i<2; i++)
        menuP.add(cs[i]);
  }
  private void setRecent(List<String> startList, JMenu menuX, Action listener) {
    setRecent(startList, menuX, null, listener);
  }
  private void setRecent(List<String> startList, JMenu menuX, JPopupMenu menuP, Action listener) {
    int size=startList.size();
    cleanMenu(menuX, menuP);
    menuX.setEnabled(size>0);
    int easyLen=3;

    //Build first menu, a quick-list of recent files:
    for (int i=0; i<Math.min(easyLen, size); i++)
      menuX.add(mu.doMenuItem(startList.get(i), listener));

    //Build second menu, a longer sorter list of all files you have open:
    if (size>easyLen){

      // Build & sort list of names:
      List<String> list=recentSortSources.get(menuX);
      if (list==null) {
        list=new ArrayList<>(16);
        recentSortSources.put(menuX, list);
      }
      list.clear();
      for (String s: startList)
        list.add(s);
      Collections.sort(list);

      // Now add them:
      menuX.addSeparator();
      for (String s: list)
        menuX.add(mu.doMenuItem(s, listener));
    }
  }
  private void setFavorites(
      Collection<String> startList, JMenu menuX, Action listener, int skipLast
    ) {
    setFavorites(startList, menuX, null, listener, skipLast);
  }
  private void setFavorites(
      Collection<String> startList, JMenu menuX, JPopupMenu menuP, Action listener, int skipLast
    ) {

    // Collect a list of items to put back:
    JMenuItem[] skip=null;
    if (skipLast>0) {
      skip=new JMenuItem[skipLast];
      int count=menuX.getItemCount();
      int start=count-skipLast;
      for (int i=start; i<count; i++)
        skip[i-start]=menuX.getItem(i);
    }

    cleanMenu(menuX, menuP);

    // Add everything:
    for (String s: startList)
      menuX.add(mu.doMenuItem(s, listener));

    // Put things back or enable/disable if empty:
    if (skipLast>0){
      menuX.addSeparator();
      for (JMenuItem i: skip)
        menuX.add(i);
    }
    else
      menuX.setEnabled(menuX.getItemCount()>0);
  }

  //////////////////
  // SWITCH MENU: //
  //////////////////

  private void setSwitchMenu() {
    switcher.removeAll();
    switchMenuToEditor.clear();
    int easyLen=3;

    // Build first menu, a quick/short list of recent files:
    int i=-1, emin=Math.min(easyLen, editors.size());
    for (Editor e: editors.forEach()){
      ++i;
      if (i>=emin)
        break;
      makeSwitchMenuItem(switcher,  e, i==0, i==1, -1);
    }

    // Build second menu (containing all items) and/or popup switcher menu:
    boolean secondSet=editors.size()>emin;
    if (secondSet)
      switcher.addSeparator();
    if (secondSet || pswitcher!=null){

      // Rebuild sorted list of editors:
      switchSortSource.clear();
      for (Editor e: editors.forEach())
        switchSortSource.add(e);
      Collections.sort(switchSortSource, switchSorter);

      // Clear popup of all but first 2 and last 2 items:
      // (We could do it this way this with our main switcher but never bothered)
      if (pswitcher!=null) {
        int size=pswitcher.getComponentCount();
        for (int ps=2; ps<size-2; ps++)
          pswitcher.remove(2);
      }

      // Add to whichever menu needs it. Be careful: You must
      // make a new menu item each time, EVEN when adding it to
      // different menus:
      Editor firstEditor=editors.getFirst();
      int count=-1;
      for (Editor e: switchSortSource) {
        count++;
        if (secondSet)
          makeSwitchMenuItem(switcher,  e, e==firstEditor, false, -1);
        if (pswitcher!=null)
          makeSwitchMenuItem(pswitcher, e, e==firstEditor, false, 2+count);
      }
    }

    //Build third menu, which swaps current for the other:
    if (editors.size()>1){
      switcher.addSeparator();
      switcher.add(switchFrontToBack);
      switcher.add(switchBackToFront);
      switcher.add(switchNextUnsaved);
    }

    fileCloseOthers.setEnabled(editors.size()>1);
  }
  /** Only called by setSwitchMenu() */
  private void makeSwitchMenuItem(
      javax.swing.JComponent c, Editor e, boolean checked, boolean f12, int index
    ) {
    JMenuItem jmi=
      checked
        ?mu.doMenuItemCheckbox(e.getTitle(), switchListener)
        :mu.doMenuItem(
          e.getTitle(), switchListener, -1,
          f12
            ?KeyMapper.key(KeyEvent.VK_F12,0)
            :null
        );
    switchMenuToEditor.put(jmi, e);
    if (index>0)
      c.add(jmi, index);
    else
      c.add(jmi);
  }
  /** Also only used by setSwitchMenu() */
  private static Comparator<Editor> switchSorter=new Comparator<Editor> () {
    public int compare(Editor e1, Editor e2) {
      return e1.getTitle().compareTo(e2.getTitle());
    }
  };

  ///////////////////////////
  // CREATE/LAYOUT/LISTEN: //
  ///////////////////////////

  //Unlike a lot of other create/layout/listens, this is almost all-in-one,
  //since everything is generic and we can chain function calls like an
  //absolute lunatic, which is fun.
  private void create() {
    bar.setBorderPainted(false);

    file=mu.doMenu(bar, "File", KeyEvent.VK_F);

    //FILE MENU SECTION 1:
    mu.add(
       file
      ,fileOpen =mu.doMenuItem(
        "Open...", fileListener, KeyEvent.VK_O,
        KeyMapper.keyByOS(KeyEvent.VK_O)
      )
      ,fileNew  =mu.doMenuItem(
        "New",     fileListener, KeyEvent.VK_N,
        KeyMapper.keyByOS(KeyEvent.VK_N)
      )
      ,fileSave =mu.doMenuItem(
        "Save",    fileListener, KeyEvent.VK_S,
        KeyMapper.keyByOS(KeyEvent.VK_S)
      )
      ,fileSaveAs=mu.doMenuItem(
        "Save as...",   fileListener, KeyEvent.VK_A,
        currentOS.isOSX
          ?KeyMapper.key(KeyEvent.VK_S, InputEvent.META_DOWN_MASK, InputEvent.SHIFT_DOWN_MASK)
          :null
      )
      ,fileClose =mu.doMenuItem(
        "Close",   fileListener, KeyEvent.VK_C,
        currentOS.isOSX
          ?KeyMapper.key(KeyEvent.VK_W, InputEvent.META_DOWN_MASK)
          :KeyMapper.key(KeyEvent.VK_F4, InputEvent.CTRL_DOWN_MASK)
      )
      ,fileCloseOthers=mu.doMenuItem(
        "Close others",   fileListener, KeyEvent.VK_L
      )
      ,fileCloseAll=mu.doMenuItem(
        "Close all",   fileListener, KeyEvent.VK_E
      )
    );

    //FILE MENU SECTION 2 (PRINT):
    file.addSeparator();
    mu.add(
       file
      ,filePrint =mu.doMenuItem(
        "Print", fileListener, KeyEvent.VK_P,
        KeyMapper.key(KeyEvent.VK_P, InputEvent.CTRL_DOWN_MASK)
      )
    );

    //FILE MENU SECTION 3:
    file.addSeparator();
    if (currentOS.isMSWindows || currentOS.isOSX)
      file.add(fileDocDirExplore =mu.doMenuItem(
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


    //FILE MENU SECTION 4:
    file.addSeparator();
    //Used for popups:
    JMenu
      fileOpenFrom=mu.doMenu("Open from", KeyEvent.VK_F),
      fileSaveTo=mu.doMenu("Save to", KeyEvent.VK_V);
    mu.add(
      file
      ,mu.add(
        fileOpenFrom
        ,fileOpenFromDocDir   =mu.doMenuItem(
          "Current document directory", fileListener, KeyEvent.VK_C
        )
        ,fileOpenFromRecentDir=mu.doMenu(
          "Recent directory", KeyEvent.VK_R
        )
        ,fileOpenFromFave     =mu.doMenu(
          "Favorite directory", KeyEvent.VK_F
        )
        ,fileOpenFromList     =
          currentOS.isOSX
            ?mu.doMenuItem(
              "List...", fileListener, KeyEvent.VK_F,
              KeyMapper.key(KeyEvent.VK_O, InputEvent.CTRL_DOWN_MASK)
            )
            :null
        ,null
        ,fileOpenFromSSH      =mu.doMenuItem(
          "SSH...", fileListener, KeyEvent.VK_S
        )
      )
      ,mu.add(
        fileSaveTo
        ,fileSaveToDocDir=mu.doMenuItem(
          "Current document directory", fileListener, KeyEvent.VK_C
        )
        ,fileSaveToRecentDir=mu.doMenu(
          "Recent directory", KeyEvent.VK_R
        )
        ,fileSaveToFave=mu.doMenu(
          "Favorite directory", KeyEvent.VK_F
        )
        ,fileSaveToSSH=mu.doMenuItem(
          "SSH...", fileListener, KeyEvent.VK_S
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
    if (currentOS.isOSX) {
      pOpenFrom=makePopup(fileOpenFrom, "Open from:");
      pSaveTo=makePopup(fileSaveTo, "Save to:");
      pFavorite=makePopup(fileFave, "Favorite files:");
      pReopen=makePopup(fileReopen, "Reopen file:");
    }

    //FILE MENU SECTION 5 (EXIT)
    file.addSeparator();
    file.add(fileExit  =mu.doMenuItem(
      "Exit",    fileListener, KeyEvent.VK_X,
      currentOS.isOSX
        ?null
        :KeyMapper.key(KeyEvent.VK_F4, InputEvent.ALT_DOWN_MASK)
    ));


    //SWITCH:
    switcher=mu.doMenu(bar, "Switch",     KeyEvent.VK_W);
    switchFrontToBack=mu.doMenuItem(
      "Send front to back", switchListener, KeyEvent.VK_S, KeyMapper.key(KeyEvent.VK_F11)
    );
    switchBackToFront=mu.doMenuItem(
      "Send back to front", switchListener, KeyEvent.VK_E, KeyMapper.key(KeyEvent.VK_F11, KeyEvent.SHIFT_DOWN_MASK)
    );
    switchNextUnsaved=mu.doMenuItem(
      "Next unsaved file", switchListener, KeyEvent.VK_X
    );
    if (currentOS.isOSX) {
      pswitcher=makeLabelledPopup("Switch:");
      pSwitchNextUnsaved=mu.doMenuItem(
        "Next unsaved file", switchListener
      );
      pswitcher.addSeparator();
      pswitcher.add(pSwitchNextUnsaved);
    }


    //SEARCH:
    search=mu.doMenu(bar, "Search", KeyEvent.VK_S);
    mu.add(
      search
      ,
      searchFind=mu.doMenuItem(
        "Find", searchListener, KeyEvent.VK_F,
        currentOS.isOSX
          ?KeyMapper.key(KeyEvent.VK_F, KeyEvent.META_DOWN_MASK)
          :KeyMapper.key(KeyEvent.VK_F, KeyEvent.CTRL_DOWN_MASK)
      )
      ,
      searchReplace=mu.doMenuItem(
        "Replace", searchListener, KeyEvent.VK_R,
        currentOS.isOSX
          ?KeyMapper.key(KeyEvent.VK_R, KeyEvent.META_DOWN_MASK)
          :KeyMapper.key(KeyEvent.VK_R, KeyEvent.CTRL_DOWN_MASK)
      )
    );
    search.addSeparator();
    mu.add(
      search
      ,
      searchRepeat=mu.doMenuItem(
        "Repeat find/replace", searchListener, KeyEvent.VK_P,
        FindAndReplace.getFindAgainKey(currentOS)
      )
      ,
      searchRepeatBackwards=mu.doMenuItem(
        "...Backwards", searchListener, KeyEvent.VK_B,
        FindAndReplace.getFindAgainReverseKey(currentOS)
      )
    );
    search.addSeparator();
    mu.add(
      search
      ,
      searchGoToLine=mu.doMenuItem(
        "Go to line number...", searchListener, KeyEvent.VK_O,
        currentOS.isOSX
          ?KeyMapper.key(KeyEvent.VK_T, KeyMapper.shortcutByOS())
          :KeyMapper.key(KeyEvent.VK_G, KeyEvent.CTRL_DOWN_MASK)
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
        markItemListener, KeyEvent.VK_C, 
        KeyMapper.key(KeyEvent.VK_F4, KeyEvent.SHIFT_DOWN_MASK)
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
        KeyMapper.keyByOS(KeyEvent.VK_Z)
      )
      ,
      undoRedo=mu.doMenuItem(
        "Redo", undoItemListener, KeyEvent.VK_R,
        KeyMapper.keyByOS(KeyEvent.VK_Z, KeyEvent.SHIFT_DOWN_MASK)
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


    //SELECTION:
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
    if (currentOS.isOSX) {
      pSelect=makePopup(select, "Selection:");
      pSelect.addPopupMenuListener(popupSelectListener);
    }

    //ALIGN:
    mu.add(
      align=mu.doMenu(bar, "Align", KeyEvent.VK_A)
      ,
      weirdInsertToAlign=mu.doMenuItem(
        "Insert spaces to align cursor to above"+(
          currentOS.isOSX
            ?"  ( ^ space)"
            :""
        ),
        alignItemListener, KeyEvent.VK_I,
        KeyMapper.key(KeyEvent.VK_SPACE, KeyEvent.CTRL_DOWN_MASK)
      )
      ,
      weirdInsertToAlignBelow=mu.doMenuItem(
        "Insert spaces to align cursor to below"+(
          currentOS.isOSX
            ?"  (^ ⇧ space)"
            :""
        ),
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
        KeyMapper.keyByOS(KeyEvent.VK_E)
      )
    );

    //OSX SHORTCUTS:
    if (currentOS.isOSX){
      mu.add(
        osxShortcuts=mu.doMenu(bar, "MacOS Shortcuts", 0)
        ,osxOpenFrom=mu.doMenuItem(
          "Open from...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_O, KeyMapper.shortcutByOS(), KeyEvent.SHIFT_DOWN_MASK)
        )
        ,osxSaveTo=mu.doMenuItem(
          "Save to...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_T, KeyMapper.shortcutByOS(), KeyEvent.SHIFT_DOWN_MASK)
        )
        ,osxFavorite=mu.doMenuItem(
          "Favorite files...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_I, KeyMapper.shortcutByOS(), KeyEvent.SHIFT_DOWN_MASK)
        )
        ,osxReopen=mu.doMenuItem(
          "Reopen file...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_R, KeyMapper.shortcutByOS(), KeyEvent.SHIFT_DOWN_MASK)
        )
      );
      osxShortcuts.addSeparator();
      mu.add(
        osxShortcuts
        ,osxSwitch=mu.doMenuItem(
          "Switch menu...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_I, KeyMapper.shortcutByOS())
        )
        ,osxSelect=mu.doMenuItem(
          "Selection menu...", osxShortcutListener, 0,
          KeyMapper.key(KeyEvent.VK_L, KeyMapper.shortcutByOS())
        )
      );
    }


    //OPTIONS:
    mu.add(
      options=mu.doMenu(bar, "Options",   KeyEvent.VK_O)
      ,optionWordWrap=mu.doMenuItemCheckbox(
        "Word wrap", optionListener, KeyEvent.VK_W, false
      )
      ,optionAutoTrim=mu.doMenuItemCheckbox(
        "Auto-trim trailing spaces on save", optionListener, KeyEvent.VK_A, false
      )
      ,
      optionFontBigger=mu.doMenuItem(
        "Make font bigger", optionListener, KeyEvent.VK_B,
        KeyMapper.key(KeyEvent.VK_EQUALS, KeyMapper.shortcutByOS())
      )
      ,
      optionFontSmaller=mu.doMenuItem(
        "Make font smaller", optionListener, KeyEvent.VK_B,
        KeyMapper.key(KeyEvent.VK_MINUS, KeyMapper.shortcutByOS())
      )
    );
    options.addSeparator();
    mu.add(
      options
      ,optionTabsAndIndents=mu.doMenuItem("Tabs & Indents",               optionListener, KeyEvent.VK_T)
      ,optionLineDelimiters=mu.doMenuItem("Line delimiters",              optionListener, KeyEvent.VK_L)
      ,optionFont          =mu.doMenuItem("Font & colors",                optionListener, KeyEvent.VK_F)
      ,optionFavorites     =mu.doMenuItem("Favorite files & directories", optionListener, KeyEvent.VK_V)
      ,optionSSH           =mu.doMenuItem("SSH Options",                  optionListener, KeyEvent.VK_S)
    );

    //HELP:
    mu.add(
      help=mu.doMenu(bar, "Help",   KeyEvent.VK_H)
      ,helpAbout   =mu.doMenuItem(
        "About Klonk",                   helpListener, KeyEvent.VK_A
      )
      ,helpShortcut=mu.doMenuItem(
        "Shortcuts and hidden features", helpListener, KeyEvent.VK_S, KeyMapper.key(KeyEvent.VK_F1)
      )
    );
  }

  ///////////////////////////////////////
  // POPUP MENU CREATION FOR OSX ONLY: //
  ///////////////////////////////////////

  private JPopupMenu makePopup(JMenu from, String label) {
    JPopupMenu menu=from.getPopupMenu();
    labelPopup(menu, label);
    return menu;
  }
  private JPopupMenu makeLabelledPopup(String label) {
    JPopupMenu menu=new JPopupMenu();
    labelPopup(menu, label);
    return menu;
  }
  private void labelPopup(JPopupMenu menu, String label) {
    JLabel lbl=new javax.swing.JLabel(label);
    lbl.setFont(file.getFont());
    lbl.setBorder(
      new javax.swing.border.EmptyBorder(
        new java.awt.Insets(2,10,2,2)
      )
    );
    menu.insert(lbl, 0);
    menu.insert(new JSeparator(javax.swing.SwingConstants.HORIZONTAL), 1);
  }

  //////////////////////
  // EVENT LISTENERS: //
  //////////////////////

  private Action
    fileListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==fileOpen)         ctrlMain.doFileOpenDialog();
        else
        if (s==fileNew)          ctrlMain.doNew();
        else
        if (s==fileSave)         ctrlMain.doSave();
        else
        if (s==fileSaveAs)       ctrlMain.doSave(true);
        else
        if (s==fileClose)        ctrlMain.doFileClose();
        else
        if (s==fileCloseOthers)  ctrlMain.doFileCloseOthers();
        else
        if (s==fileCloseAll)     ctrlMain.doFileCloseAll();
        else
        if (s==filePrint)            ctrlFileOther.doPrint();
        else
        if (s==fileDocDirExplore)    ctrlFileOther.doDocumentDirectoryExplore();
        else
        if (s==fileClipboardDoc)     ctrlFileOther.doClipboardDoc();
        else
        if (s==fileClipboardDocDir)  ctrlFileOther.doClipboardDocDir();
        else
        if (s==fileOpenFromDocDir)   ctrlMain.doOpenFromDocDir();
        else
        if (s==fileOpenFromList)     ctrlMain.doOpenFromList();
        else
        if (s==fileOpenFromSSH)      ctrlMain.doOpenFromSSH();
        else
        if (s==fileSaveToDocDir)     ctrlMain.doSaveToDocDir();
        else
        if (s==fileSaveToSSH)        ctrlMain.doSaveToSSH();
        else
        if (s==fileFaveAddDir)       ctrlFileOther.doAddCurrentToFaveDirs();
        else
        if (s==fileFaveAddFile)      ctrlFileOther.doAddCurrentToFaveFiles();
        else
        if (s==fileExit)             ctrlMain.doFileExit();
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
        ctrlMain.doLoadFile(s.getText());
      }
    }
    ,
    openFromListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        ctrlMain.doOpenFrom(s.getText());
      }
    }
    ,
    saveToListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        JMenuItem s=(JMenuItem) event.getSource();
        ctrlMain.doSaveTo(s.getText());
      }
    }
    ,

    searchListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==searchRepeat)          ctrlSearch.doSearchRepeat();
        else
        if (s==searchFind)            ctrlSearch.doSearchFind();
        else
        if (s==searchReplace)         ctrlSearch.doSearchReplace();
        else
        if (s==searchRepeatBackwards) ctrlSearch.doSearchRepeatBackwards();
        else
        if (s==searchGoToLine)        ctrlSearch.doSearchGoToLine();
        else
          throw new RuntimeException("What");
      }
    }
    ,
    markItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==markSet) {
          if (ctrlMarks.doMarkSet())
            showHasMarks(true);
        }
        else
        if (s==markGoToPrevious)
          ctrlMarks.doMarkGoToPrevious();
        else
        if (s==markGoToNext)
          ctrlMarks.doMarkGoToNext();
        else
        if (s==markClearCurrent) {
          if (ctrlMarks.doMarkClearCurrent())
            showHasMarks(false);
        }
        else
        if (s==markClearAll) {
          ctrlMarks.doMarkClearAll();
          showHasMarks(false);
        }
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
          ctrlMain.doSendBackToFront();
        else
        if (s==switchFrontToBack)
          ctrlMain.doSendFrontToBack();
        else
        if (s==switchNextUnsaved || s==pSwitchNextUnsaved)
          ctrlMain.doSwitchToNextUnsaved();
        else {
          Editor e=switchMenuToEditor.get(s);
          if (e==null)
            throw new RuntimeException("Menus.switchListener(): Null editor in hash");
          ctrlMain.doSwitch(e);
        }
      }
    }
    ,
    undoItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==undoUndo)        ctrlUndo.doUndo();
        else
        if (s==undoRedo)        ctrlUndo.doRedo();
        else
        if (s==undoFast)        ctrlUndo.doUndoFast();
        else
        if (s==undoToBeginning) ctrlUndo.doUndoToBeginning();
        else
        if (s==undoRedoToEnd)   ctrlUndo.doRedoToEnd();
        else
        if (s==undoClearUndos)  ctrlUndo.doClearUndos();
        else
        if (s==undoClearRedos)  ctrlUndo.doClearRedos();
        else
        if (s==undoClearBoth)   ctrlUndo.doClearUndosAndRedos();
      }
    }
    ,
    selectionItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==selectUpperCase)             ctrlSelection.doWeirdUpperCase();
        else
        if (s==selectLowerCase)             ctrlSelection.doWeirdLowerCase();
        else
        if (s==selectSortLines)             ctrlSelection.doWeirdSortLines();
        else
        if (s==selectGetSelectionSize)      ctrlSelection.doWeirdSelectionSize();
        else
        if (s==selectGetAsciiValues)        ctrlSelection.doWeirdAsciiValues();
      }
    }
    ,
    alignItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==weirdInsertToAlign)         editors.getFirst().doInsertToAlign(true);
        else
        if (s==weirdInsertToAlignBelow)    editors.getFirst().doInsertToAlign(false);
        else
        if (s==weirdBackspaceToAlign)      editors.getFirst().doBackspaceToAlign(true);
        else
        if (s==weirdBackspaceToAlignBelow) editors.getFirst().doBackspaceToAlign(false);
      }
    }
    ,
    externalItemListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==externalRunBatch)        ctrlOther.doShell();
        else
          throw new RuntimeException("Unexpected "+s);
      }
    }
    ,
    optionListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==optionWordWrap)        ctrlOptions.doWordWrap();
        else
        if (s==optionAutoTrim)        ctrlOptions.doAutoTrim();
        else
        if (s==optionTabsAndIndents)  ctrlOptions.doTabsAndIndents();
        else
        if (s==optionLineDelimiters)  ctrlOptions.doLineDelimiters();
        else
        if (s==optionFont)            ctrlOptions.doFontAndColors();
        else
        if (s==optionFontBigger)      ctrlOptions.doFontBigger();
        else
        if (s==optionFontSmaller)     ctrlOptions.doFontSmaller();
        else
        if (s==optionFavorites)       ctrlOptions.doFavorites();
        else
        if (s==optionSSH)             ctrlOptions.doSSH();
      }
    }
    ,
    osxShortcutListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object o=event.getSource();
        JPopupMenu jpm;
        if (o==osxOpenFrom) jpm=pOpenFrom;
        else
        if (o==osxSaveTo)   jpm=pSaveTo;
        else
        if (o==osxFavorite) jpm=pFavorite;
        else
        if (o==osxReopen)   jpm=pReopen;
        else
        if (o==osxSelect)   jpm=pSelect;
        else
        if (o==osxSwitch)   jpm=pswitcher;
        else
          throw new RuntimeException("No popup menu matched: "+o);
        jpm.show(osxAttachPopupsTo, 0, 0);
      }
    }
    ,
    helpListener=new AbstractAction() {
      public void actionPerformed(ActionEvent event) {
        Object s=event.getSource();
        if (s==helpShortcut)
          ctrlOther.doHelpShortcuts();
        else
        if (s==helpAbout)
          ctrlOther.doHelpAbout();
      }
    }
    ;

  //////////////////////////////////
  // REAL-TIME ON-SHOW LISTENERS: //
  //////////////////////////////////
  private void enableSelectItems() {
    boolean selected=editors.getFirst().isAnythingSelected();
    selectUpperCase.setEnabled(selected);
    selectLowerCase.setEnabled(selected);
    selectSortLines.setEnabled(selected);
    selectGetSelectionSize.setEnabled(selected);
    selectGetAsciiValues.setEnabled(selected);
  }
  private PopupMenuListener
    popupSelectListener=new PopupMenuListener() {
      public void popupMenuCanceled(PopupMenuEvent e){}
      public void popupMenuWillBecomeInvisible(PopupMenuEvent e){}
      public void popupMenuWillBecomeVisible(PopupMenuEvent e){
        enableSelectItems();
      }
    };
  private MenuListener
    selectListener=new MenuListener(){
      public void menuCanceled(MenuEvent e){}
      public void menuDeselected(MenuEvent e){}
      public void menuSelected(MenuEvent e){
        enableSelectItems();
      }
    }
    ,
    markListener=new MenuListener() {
      public void menuCanceled(MenuEvent e){}
      public void menuDeselected(MenuEvent e){}
      public void menuSelected(MenuEvent e){
        int prev=editors.getFirst().getPreviousMark(),
            count=editors.getFirst().getMarkCount();
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
