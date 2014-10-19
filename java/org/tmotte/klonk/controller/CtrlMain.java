package org.tmotte.klonk.controller;
import java.awt.Component;
import java.awt.Container;
import java.awt.Image;
import java.awt.Window;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.Iterator;
import javax.swing.SwingWorker;
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.option.LineDelimiterOptions;
import org.tmotte.klonk.config.option.TabAndIndentOptions;
import org.tmotte.klonk.config.msg.Doer;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.MainDisplay;
import org.tmotte.klonk.config.msg.Getter;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.controller.Recents;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.EditorListener;
import org.tmotte.klonk.edit.UndoEvent;
import org.tmotte.klonk.edit.UndoListener;
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Popups; 
import org.tmotte.klonk.windows.popup.YesNoCancelAnswer;

/** Refer to org.tmotte.klonk.config.Boot for application startup */

public class CtrlMain  {


  /////////////////////
  //                 //
  // INSTANCE STUFF: //
  //                 //
  /////////////////////

  
  //DI-injected stuff:
  private Popups popups;
  private KPersist persist;
  private StatusUpdate statusBar;
  private Fail fail;
  private Doer lockRemover, editorSwitchedListener;
  private MainDisplay layout;

  //Editors list:
  private LinkedList<Editor> editors=new LinkedList<>();
  private Editors editorMgr=new Editors(){
    public Editor getFirst() {return editors.getFirst();}
    public Iterable<Editor> forEach() {return editors;}
    public int size() {return editors.size();}
  };

  //Other components:
  private boolean anyUnsaved=false;
  private boolean thisUnsaved=false;
  private Recents recents;

  //Constructor:
  public CtrlMain(Fail fail, final KPersist persist) {
    this.fail=fail;
    this.persist=persist;
    recents=new Recents(persist); 
  }
  
  /////////////////////////////////
  //DI initialization functions: //
  /////////////////////////////////
  
  // DI SET: //
  
  public void setLayout(MainDisplay layout, StatusUpdate statusBar) {
    this.layout=layout;
    this.statusBar=statusBar;
  }
  public void setPopups(Popups p) {
    this.popups=p;
  }
  public void setListeners(
      Doer lockRemover,
      Doer editorSwitchListener,
      Setter<List<String>> recentFileListener,
      Setter<List<String>> recentDirListener
    ) {  
    this.lockRemover=lockRemover;
    this.editorSwitchedListener=editorSwitchListener;
    this.recents.setFileListener(recentFileListener);
    this.recents.setDirListener(recentDirListener);
  }
  

  // DI GET: //

  public Editors getEditors() {
    return editorMgr;
  }
  public Setter<List<String>> getFileReceiver() {
    return new Setter<List<String>>(){
      public @Override void set(List<String> files) 
        {doLoadAsync(files);}
    };
  }
  public Getter<String> getCurrFileNameGetter() {
    return new Getter<String>() {
      public @Override String get() 
        {return getCurrentFileName();}
    };
  }
  public Doer getAppCloseListener() {
    return new Doer() {
      //This is the application close listener:
      public @Override void doIt() 
        {tryExit();}
    };
  }
  public LineDelimiterListener getLineDelimiterListener() {
    return new LineDelimiterListener() {
      public void setDefault(String defaultDelim) {
        persist.setDefaultLineDelimiter(defaultDelim);
        persist.save();
      }
      public void setThis(String lineB) {
        Editor e=editorMgr.getFirst();
        e.setLineBreaker(lineB);
        if (e.getFile()!=null && !e.hasUnsavedChanges())
          fileSave(false);
      }
    };  
  }
  
  //DI OTHER: //
 
  public void doLoadFiles(String[] args) {
    loadFiles(args);
  }
  private EditorListener editorListener=new EditorListener(){
    public void doCapsLock(boolean state) {
      statusBar.showCapsLock(state);
    }
    public void doCaretMoved(Editor editor, int dotPos) {
      editorCaretMoved(editor, dotPos);
    }
    public void doEditorChanged(Editor e) {
      stabilityChange(e);
    }
    public void fileDropped(File file) {
      loadFile(file);
    }
    public void closeEditor() {
      fileClose(false);
    }
  };

 
  //////////////////////
  //                  //
  //   MENU EVENTS:   //
  //                  //
  //////////////////////


  ////////////////
  // FILE MENU: //
  ////////////////

  public void doFileOpenDialog() {
    File file=popups.showFileDialog(false);
    if (file!=null)
      loadFile(file);
  }
  public void doNew() {
    newEditor();
  }
  public boolean doSave() {
    return doSave(false);
  }
  public boolean doSave(boolean forceNewFile) {
    return fileSave(forceNewFile);
  }
  public void doFileClose() {
    fileClose(false);
  }
  public void doFileCloseOthers() {
    while (editorMgr.size()>1)
      if (!fileCloseLastFirst(false))
        return;
  }
  public void doFileCloseAll() {
    //Close all but one, asking to save as necessary:
    while (editorMgr.size()>1)
      if (!fileCloseLastFirst(false))
        return;
    //If very last one is just an untitled, ignore and return.
    //Note that since we automatically create an untitled when
    //we close the last one, we have to be careful about an 
    //endless loop:
    if (!editorMgr.getFirst().isUsed())
      return;
    fileClose(false);
  }
  public void doLoadFile(String file) {
    loadFile(new File(file));
  }
 
  // FILE OPEN FROM: //
  public void doOpenFromDocDir() {
    File file;
    if ((file=popups.showFileDialogForDir(false, editorMgr.getFirst().getFile().getParentFile()))!=null)
      loadFile(file);
  }
  public void doOpenFrom(String dir) {
    File file;
    if ((
      file=popups.showFileDialogForDir(false, new File(dir))
      )!=null)
      loadFile(file);
  }
  
  // FILE SAVE TO: //
  public void doSaveToDocDir() {
    File file;
    Editor ed=editorMgr.getFirst();
    if ((file=showFileDialogForSave(null, ed.getFile().getParentFile()))!=null)
      fileSave(ed, file, true);
  }
  public void doSaveTo(String dir) {
    File file;
    if ((
      file=showFileDialogForSave(null, new File(dir)) 
      )!=null)
      fileSave(editorMgr.getFirst(), file, true);
  }
  public void doFileExit() {
    tryExit();
  }
  
  //////////////////
  // SWITCH MENU: //
  //////////////////

  public void doSendFrontToBack(){
    Editor e=editors.removeFirst();
    editors.addLast(e);
    setEditor(editors.getFirst());
  }
  public void doSendBackToFront(){
    Editor e=editors.removeLast();
    editors.addFirst(e);
    setEditor(e);
  }
  public void doSwitchToNextUnsaved() {
    for (int i=1; i<editors.size(); i++){
      Editor e=editors.get(i);
      if (e.hasUnsavedChanges()){
        editorSwitch(e);
        return;
      }
    }
    popups.alert(
      editors.getFirst().hasUnsavedChanges()
        ?"No other files have unsaved changes."
        :"No files have unsaved changes."
    );
  }
  public void doSwitch(Editor editor) {
    editorSwitch(editor);
  }

  

  ///////////////////////
  //                   //
  //  PRIVATE METHODS: //
  //                   //
  ///////////////////////


  private void tryExit() {
    while (editorMgr.size()>0)
      if (!fileCloseLastFirst(true))
        return;
    if (!layout.isMaximized()) {
      persist.setWindowBounds(layout.getBounds());
      persist.setWindowMaximized(false);
    }
    else
      persist.setWindowMaximized(true);
    persist.save();
    lockRemover.doIt();
    System.exit(0);
  }
 
  private String getCurrentFileName() {
    File file=editorMgr.getFirst().getFile();
    return file==null ?null :getFullPath(file);
  }


  //////////////////
  // CARET STATE: //
  //////////////////

  private void editorCaretMoved(Editor e, int caretPos) {
    showCaretPos(e, caretPos);
    statusBar.showNoStatus();
  }
  private void showCaretPos(Editor e) {
    showCaretPos(e, e.getCaretPos());
  }
  private void showCaretPos(Editor e, int caretPos) {
    int rowPos=e.getRow(caretPos);
    statusBar.showRowColumn(rowPos+1, caretPos-e.getRowOffset(rowPos));
  }
  
  ////////////////
  // FILE SAVE: //
  ////////////////

  private boolean fileSave(boolean forceNewFile) {
    Editor e=editorMgr.getFirst();
    
    //1. Get file:
    boolean newFile=true;
    File file=null, oldFile=e.getFile();
    if (forceNewFile || oldFile==null) {
      File dir=oldFile==null && recents.hasDirs()
        ?new File(recents.getFirstDir())
        :null;
      if ((file=showFileDialogForSave(oldFile, dir))==null){
        statusBar.showBad("File save cancelled");
        return false;
      }
    }
    else {
      file=oldFile;
      newFile=false;
    }

    //2. Save file:
    return fileSave(e, file, newFile);
  }
  private boolean fileSave(Editor e, File file, boolean newFile) {
    File oldFile=newFile ?e.getFile() :null;
    try {
      statusBar.show("Saving...");
      e.saveFile(file);
    } catch (Exception ex) {
      fail.fail(ex);
      statusBar.showBad("Save failed.");
      return false;
    }
    fileIsSaved(e, file, oldFile, newFile);
    persist.checkSave();
    return true;
  }
  private File showFileDialogForSave(File f, File dir) {
    File file;
    if (f!=null)
      file=popups.showFileDialog(true,  f);
    else
    if (dir!=null)
      file=popups.showFileDialogForDir(true, dir);
    else
      file=popups.showFileDialog(true);
    if (file==null)
      return null;
    for (Editor e: editorMgr.forEach()) 
      if (e.sameFile(file)){
        popups.alert("File is already open in another window; close it first: "+e.getTitle());
        return null;
      }
    //This is only needed if we are using JFileChooser. The AWT chooser will result in a question
    //courtesy of the OS: 
    if (file.exists()) {
      if (!popups.askYesNo("Replace file "+file.getAbsolutePath()+"?"))
        return null;
      else
        file.delete();
    }
    return file;
  }

  /////////////////
  // FILE CLOSE: //
  /////////////////

  private boolean fileCloseLastFirst(boolean forExit) {
    if (editorMgr.size()>1){
      //This forces the app to close the most recently accessed
      //files last, so that they are the most likely to appear in the reopen menu:
      Editor e=editors.removeLast();
      editors.addFirst(e);
      setEditor(e);
    }
    return fileClose(forExit);
  }

  private boolean fileClose(boolean forExit) {
    Editor e=editorMgr.getFirst();
    if (e.hasUnsavedChanges()) {
      YesNoCancelAnswer result=popups.askYesNoCancel("Save changes to: "+e.getTitle()+"?");
      if (result.isYes()){
        if (!fileSave(false))
          return false;
      }
      else
      if (result.isCancel())
        return false;
      //"No" means we do nothing
    }
    if (e.getFile()!=null) 
      recents.recentFileClosed(e.getFile());
    editors.remove(0);
    statusBar.show("Closed: "+e.getTitle());
    if (editorMgr.size()>0)
      setEditor(editorMgr.getFirst());
    else
    if (!forExit)
      newEditor();
    return true;
  }

  ////////////////
  // FILE LOAD: //
  ////////////////

  private void doLoadAsync(final List<String> fileNames) { //FIXME this uses Swing. Bad.
    //Do nothing in the doInBackground() thread, since it is not
    //a GUI thread. Do it all in done(), which is sync'd to GUI events,
    //IE EventDispatch.
    new SwingWorker<String, Object>() {
      @Override public String doInBackground() {return "";}
      @Override protected void done(){
        try {
          loadFiles(fileNames);
        } catch (Exception e) {
          fail.fail(e);
        }
        fileNames.clear();
      }
    }.execute();
  }
  
  private void loadFiles(String[] args) {
    if (args!=null)
      for (String s: args) {
        if (s==null) continue;
        s=s.trim();
        if (s.equals("")) continue;
        loadFile(s);
      }
  }
  private void loadFiles(List<String> fileNames) {
    for (String s: fileNames) loadFile(s);
  }
  private void loadFile(String fileName) {
    loadFile(new File(fileName));
  }
  /** Makes sure file isn't already loaded, and finds an editor to load it into: **/
  private void loadFile(File file) {
    if (!file.exists()){
      popups.alert("No such file: "+file);
      return;
    }
    for (Editor ed: editorMgr.forEach()) 
      if (ed.sameFile(file)){
        editorSwitch(ed);
        popups.alert("File is already open: "+ed.getTitle());
        return;
      }
    try {
      Editor toUse=null;
      for (Editor e:editorMgr.forEach()) 
        if (!e.isUsed()) {
          toUse=e;
          editorSwitch(e);
          break;
        }
      if (toUse==null)
        toUse=newEditor();
      loadFile(toUse, file);
    } catch (Exception e) {
      fail.fail(e);
    }
  }
  private boolean loadFile(Editor e, File file) {
    try {
      statusBar.show("Loading: "+file+"...");
      e.loadFile(file, persist.getDefaultLineDelimiter());
    } catch (Exception ex) {
      fail.fail(ex);
      statusBar.showBad("Load failed");
      return false;
    }
    fileIsLoaded(e, file);
    return true;
  }


  ////////////////////////
  // EDITOR MANAGEMENT: //
  ////////////////////////

  private void editorSwitch(Editor editor) {
    editors.remove(editors.indexOf(editor));
    editors.add(0, editor);
    setEditor(editor);
  }
  private Editor newEditor(){
    Editor e=new Editor(
      fail, editorListener, myUndoListener, 
      persist.getDefaultLineDelimiter(), persist.getWordWrap()
    ); 
    e.setFastUndos(persist.getFastUndos());
    e.setTitle(getUnusedTitle());
    TabAndIndentOptions taio=persist.getTabAndIndentOptions();
    e.setTabAndIndentOptions(taio);
    e.setTabsOrSpaces(taio.indentionModeDefault);
    e.setFont(persist.getFontAndColors());

    editors.add(0, e);
    setEditor(e); 
    return e;
  }
  private void setEditor(Editor e) {
    layout.setEditor(e.getContainer());
    editorSwitched(e);
    e.requestFocus();
  }

  private String getUnusedTitle() {
    String name="Untitled";
    int incr=0;
    while (isUsedTitle(name))
      name="Untitled "+(++incr);
    return name;
  }
  private boolean isUsedTitle(String name) {
    for (Editor ed:editorMgr.forEach()) 
      if (ed.getTitle().equals(name))
        return true;
    return false;
  }



  //////////////////////////////
  // ULTRA COMPLICATED STATE: //
  //////////////////////////////

  private UndoListener myUndoListener=new UndoListener() {
    public void happened(UndoEvent ue) {
      if (ue.isNoMoreUndos)
        statusBar.showBad("No more undos.");
      else
      if (ue.isNoMoreRedos)
        statusBar.showBad("No more redos.");
    }
  };

  private void fileIsSaved(Editor e, File file, File oldFile, boolean newFile) {
    if (newFile){
      editorSwitched(e);
      recents.recentFileSavedNew(file, oldFile);
    }
    else
      stabilityChange(e);
    statusBar.show(newFile ?"File saved: "+file :"File saved");
  }
  private void fileIsLoaded(Editor e, File file) {
    recents.recentFileLoaded(file);
    editorSwitched(e);
    statusBar.show("File loaded: "+file);
  }
  
  private void stabilityChange(Editor editor) {
    boolean b=editor.hasUnsavedChanges();
    if (thisUnsaved!=b){
      thisUnsaved=b;
      showLights();
    }
  }
  private void showLights() {
    //This adjusts the red/blue lights:
    statusBar.showChangeThis(thisUnsaved);
    if (thisUnsaved) {
      if (!anyUnsaved)
        statusBar.showChangeAny(anyUnsaved=true);
      return;
    }
    else 
    if (anyUnsaved) 
      for (Editor ed: editors)
        if (ed.hasUnsavedChanges())
          return;
    statusBar.showChangeAny(anyUnsaved=false);
  }
  /** Invoked whenever we switch to a different editor, 
      or current editor is used to open a file
      or current editor is saved to a new file. */
  private void editorSwitched(Editor e) {
    thisUnsaved=e.hasUnsavedChanges();
    showLights();
    statusBar.showTitle(e.getTitle());
    showCaretPos(e);
    editorSwitchedListener.doIt();
  }


  ////////////////
  // UTILITIES: //
  ////////////////

  private String getFullPath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (Exception e) {
      fail.fail(e);
      return null;
    }
  }
}
