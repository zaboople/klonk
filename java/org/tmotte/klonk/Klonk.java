package org.tmotte.klonk;
import java.awt.Graphics;
import java.awt.Component;
import java.awt.Container;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.event.ActionEvent;
import java.awt.event.KeyEvent;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import javax.swing.JFrame;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.SwingUtilities;
import org.tmotte.common.swang.KeyMapper;
import org.tmotte.common.text.DelimitedString;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.config.Kontext;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.LineDelimiterOptions;
import org.tmotte.klonk.config.TabAndIndentOptions;
import org.tmotte.klonk.edit.UndoEvent;
import org.tmotte.klonk.edit.UndoListener;
import org.tmotte.klonk.io.FileListen;
import org.tmotte.klonk.io.Printing;
import org.tmotte.klonk.windows.AppCloseListener; 
import org.tmotte.klonk.windows.MainLayout; 
import org.tmotte.klonk.windows.StatusNotifier; 
import org.tmotte.klonk.windows.popup.LineDelimiterListener;
import org.tmotte.klonk.windows.popup.Popups; 
import org.tmotte.klonk.windows.popup.YesNoCancelAnswer;

public class Klonk {


  /////////////////////
  //                 //
  // INSTANCE STUFF: //
  //                 //
  /////////////////////

  //The most essential stuff:
  private FileListen fileListen;
  private KLog log;
  private KHome home;
  
  //Main GUI components:
  private LinkedList<Editor> editors;
  private MainLayout layout;
  private Menus menus;
  private Popups popups;
  
  //Configuration stuff:
  private KPersist persist;
  private int defaultLineBreaker;
  private boolean wordWrap=false,
                  fastUndos=true;
  private static int maxRecent=15;
  private ArrayList<String> 
    recentDirs, recentFiles, favoriteFiles, favoriteDirs, recentCommands;
  private TabAndIndentOptions taio;
  private FontOptions fontOptions;
  
 
  private AppCloseListener myAppCloseListener=new AppCloseListener() {
    public void tryClose() {tryExitSystem();}
  };
  

  /////////////////////
  //                 //
  // PUBLIC METHODS: //
  //                 //
  /////////////////////


  //////////
  // RUN: //
  //////////
  
  public static void main(final String[] args) {
    final Klonk klonk=new Klonk();
    //Don't bother starting swing if we're going to exit immediately;
    if (!klonk.initializeAndCheck(args)){
      if (klonk.home.ready)
        klonk.log.log("Klonk is handing off to another process.");
      System.exit(0);
      return;
    }
    //Start up swing:
    klonk.startup(args);
  }
  public KHome getHome() {
    return home;
  }
  public boolean isAnythingSelected() {
    return editors.get(0).isAnythingSelected();
  }
  public void doLoadFile(File file) {
    loadFile(file);
  }

  //////////////////////
  //                  //
  //   MENU EVENTS:   //
  //                  //
  //////////////////////


  ////////////////
  // FILE MENU: //
  ////////////////

  public void doFileOpenDialog() {
    File file=recentDirs.size()>0
      ?popups.showFileDialogForDir(false, new File(recentDirs.get(0)))
      :popups.showFileDialog(false);
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
    while (editors.size()>1)
      if (!fileCloseLastFirst(false))
        return;
  }
  public void doFileCloseAll() {
    //Close all but one, asking to save as necessary:
    while (editors.size()>1)
      if (!fileCloseLastFirst(false))
        return;
    //If very last one is just an untitled, ignore and return.
    //Note that since we automatically create an untitled when
    //we close the last one, we have to be careful about an 
    //endless loop:
    if (!editors.getFirst().used)
      return;
    fileClose(false);
  }
  public void doPrint() {
    if (Printing.print(editors.getFirst().getTextArea()))
      showStatus("Print job scheduled");
    else
      showStatusBad("Action cancelled");
  }
  

  public void doLoadFile(String file) {
    loadFile(new File(file));
  }
  
  public void doDocumentDirectoryMSWindows() {
    try{
      Runtime.getRuntime().exec(
        "explorer "+editors.get(0).file.getParent()
      );
    } catch (Exception e) {
      log.fail(e);
    }
  }
  
  // FILE-CLIPBOARD: //
  public void doClipboardDoc() {
    Editor e=editors.get(0);
    toClipboard(e.file.getAbsolutePath());
  }
  public void doClipboardDocDir() {
    Editor e=editors.get(0);
    toClipboard(e.file.getParentFile().getAbsolutePath());
  }
  
  // FAVORITES: //
  public void doAddCurrentToFaveDirs(){
    Editor ed=editors.getFirst();
    String s=getFullPath(ed.file.getParentFile());
    favoriteDirs.add(s);
    menus.setFavoriteDirs(favoriteDirs);
    showStatus("\""+s+"\" added to favorite directories.");
  }
  public void doAddCurrentToFaveFiles(){
    Editor ed=editors.getFirst();
    String s=getFullPath(ed.file);
    favoriteFiles.add(s);
    menus.setFavoriteFiles(favoriteFiles);
    showStatus("\""+s+"\" added to favorite files.");
  }

  // FILE OPEN FROM/SAVE TO: //
  public void doOpenFromDocDir() {
    File file;
    if ((file=popups.showFileDialogForDir(false, editors.get(0).file.getParentFile()))!=null)
      loadFile(file);
  }
  public void doOpenFrom(String dir) {
    File file;
    if ((
      file=popups.showFileDialogForDir(false, new File(dir))
      )!=null)
      loadFile(file);
  }
  public void doSaveToDocDir() {
    File file;
    Editor ed=editors.getFirst();
    if ((file=popups.showFileDialogForDir(true, ed.file.getParentFile()))!=null)
      fileSave(ed, file, true);
  }
  public void doSaveTo(String dir) {
    File file;
    if ((
      file=showFileSaveDialog(null, new File(dir)) 
      )!=null)
      fileSave(editors.get(0), file, true);
  }
  public void doFileExit() {
    tryExitSystem();
  }
  
  //////////////////
  // SEARCH MENU: //
  //////////////////
  
  public void doSearchFind(){
    popups.doFind(editors.getFirst().getTextArea());
  }
  public void doSearchReplace(){
    popups.doReplace(editors.getFirst().getTextArea());
  }
  public void doSearchRepeat(){
    popups.repeatFindReplace(editors.getFirst().getTextArea(), true);
  }
  public void doSearchRepeatBackwards(){
    popups.repeatFindReplace(editors.getFirst().getTextArea(), false);
  }
  public void doSearchGoToLine() {
    popups.goToLine(editors.getFirst().getTextArea());
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
  public void doSwitch(Editor editor) {
    this.editorSwitch(editor);
  }

  ////////////////
  // MARK MENU: //
  ////////////////
  
  public void doMarkSet() {
    Editor e=editors.getFirst();
    int i=e.doSetMark();
    if (i!=-1){
      showStatus("Mark set");
      menus.showHasMarks(true);
      markStatus.go(i, e.getMarkCount(), true);
    }
    else
      showStatusBad("Mark already set at this position");
  }
  public void doMarkGoToPrevious() {
    Editor e=editors.getFirst();
    int i=e.doMarkGoToPrevious();
    if (i!=-1)
      markStatus.go(i, e.getMarkCount(), false);
    else
      showStatusBad("Cursor is before first mark.");
  }
  public void doMarkGoToNext() {
    Editor e=editors.getFirst();
    int i=e.doMarkGoToNext();
    if (i!=-1)
      markStatus.go(i, e.getMarkCount(), false);
    else
      showStatusBad("Cursor is after last mark.");
  }
  public void doMarkClearCurrent() {
    int i=editors.getFirst().doMarkClearCurrent();
    if (i==-1)
      showStatusBad("Cursor is not on a set mark.");
    else {
      if (i==0)
        menus.showHasMarks(false);
      showStatus("Mark cleared; "+i+" marks left.");
    }
  }
  public void doMarkClearAll() {
    editors.getFirst().doClearMarks();
    menus.showHasMarks(false);
    showStatus("All marks cleared");
  }
  //These two methods are for the menus
  int getPreviousMark() {
    return editors.getFirst().getPreviousMark();
  }
  int getMarkCount() {
    return editors.getFirst().getMarkCount();
  }
  


  ////////////////
  // UNDO MENU: //
  ////////////////
  public void doUndo(){
    editors.getFirst().undo();
  }
  public void doRedo(){
    editors.getFirst().redo();
  }
  public void doUndoToBeginning() {
    editors.get(0).undoToBeginning();
    showStatus("Undone to beginning");
  }
  public void doRedoToEnd() {
    editors.get(0).redoToEnd();
    showStatus("Redone to end");
  }
  public void doUndoFast() {
    persist.setFastUndos(fastUndos=!fastUndos);
    persist.save();
    for (Editor e: editors)
      e.setFastUndos(fastUndos);
  }
  public void doClearUndos() {
    if (popups.askYesNo("Clear undos?")){
      editors.getFirst().clearUndos();
      showStatus("Undo stack cleared");
    }
    else
      showStatusBad("Action cancelled");
  }
  public void doClearRedos() {
    if (popups.askYesNo("Clear redos?")){
      editors.getFirst().clearRedos();
      showStatus("Redo stack cleared");
    }
    else
      showStatusBad("Action cancelled");
  }
  public void doClearUndosAndRedos() {
    if (popups.askYesNo("Clear undos and redos?")){
      editors.getFirst().clearUndos();
      editors.getFirst().clearRedos();
      showStatus("Undos & redos cleared");
    }
    else
      showStatusBad("Action cancelled");
  }
  
  /////////////////////
  // SELECTION MENU: //
  /////////////////////

  public void doWeirdUpperCase() {
    editors.getFirst().doUpperCase();
  }
  public void doWeirdLowerCase() {
    editors.getFirst().doLowerCase();
  }
  public void doWeirdSortLines() {
    if (!editors.getFirst().doSortLines())
      showStatusBad("Only one line was selected for sort");
  }
  public void doWeirdSelectionSize() {
    popups.alert("Selected text size: "+editors.getFirst().getSelection().length());
  }
  public void doWeirdAsciiValues() {
    String text=editors.getFirst().getSelection();
    DelimitedString result=new DelimitedString(" ");
    int len=text.length();
    for (int i=0; i<len; i++)
      result.add((int)text.charAt(i));
    String r=result.toString();
    toClipboard(r);
    if (r.length()>1000)
      r=r.substring(1000)+"...";
    popups.alert("ASCII/UTF-8 values copied to Clipboard as: "+r);
  }
  
  /////////////////
  // ALIGN MENU: //
  /////////////////
    
  public void doWeirdInsertToAlign(boolean above){
    editors.getFirst().doInsertToAlign(above);
  }
  public void doWeirdBackspaceToAlign(boolean above){
    editors.getFirst().doBackspaceToAlign(above);
  }

  ////////////////////
  // EXTERNAL MENU: //
  ////////////////////
  
  public void doShell() {
    popups.showShell(recentCommands, persist.maxRecent);
  }

  //////////////////
  // OPTION MENU: //
  //////////////////


  public void doWordWrap() {
    persist.setWordWrap(wordWrap=!wordWrap);
    for (Editor e: editors)
      e.setWordWrap(wordWrap);;
    persist.save();
  }

  public void doTabsAndIndents(){
    taio.indentionMode=editors.get(0).getTabsOrSpaces();
    if (popups.showTabAndIndentOptions(taio)){
      editors.get(0).setTabsOrSpaces(taio.indentionMode);
      for (Editor e: editors)
        e.setTabAndIndentOptions(taio);
      persist.setTabAndIndentOptions(taio);
      persist.save();
    }
  }
  
  public void doFontAndColors() {
    if (!popups.doFontAndColors(fontOptions))
      return;
    for (Editor e: editors)
      e.setFont(fontOptions);
    popups.setFontAndColors(fontOptions);
  }
  
  public void doFavorites() {
    if (!popups.showFavorites(favoriteFiles, favoriteDirs)) 
      showStatusBad("Changes to favorite files/directories cancelled");
    else {
      menus.setFavoriteFiles(favoriteFiles);
      menus.setFavoriteDirs(favoriteDirs);
      showStatus("Changes to favorite files/directories saved");
    }
  }
 

  // LINE DELIMITERS: //

  public void doLineDelimiters(){
    LineDelimiterOptions k=new LineDelimiterOptions();
    k.defaultOption=persist.getDefaultLineDelimiter();
    k.thisFile=editors.get(0).lineBreaker;
    popups.showLineDelimiters(k, kld);
  }
  private LineDelimiterListener kld=new LineDelimiterListener() {
    public void setDefault(int defaultDelim) {
      persist.setDefaultLineDelimiter(defaultDelim);
      persist.save();
    }
    public void setThis(int lineB) {
      Editor e=editors.get(0);
      e.lineBreaker=lineB;
      if (e.file!=null)
        fileSave(false);
    }
  };
  

  ////////////////
  // HELP MENU: //
  ////////////////
  
  public void doHelpShortcuts() {
    popups.showHelp();
  }
  public void doHelpAbout() {
    popups.showHelpAbout();
  }

  ////////////////////
  // EDITOR EVENTS: //
  ////////////////////
  public void doCapsLock() {
    capsLock();
  }
  public void doCaretMoved(Editor editor, int dotPos) {
    editorCaretMoved(editor, dotPos);
  }
  public void doEditorChanged(Editor e) {
    editorUnstable(e);
  }
  public void doLoadFromListener(final List<String> fileNames) {
    //Do nothing in the doInBackground() thread, since it is not
    //a GUI thread. Do it all in done(), which is sync'd to GUI events,
    //IE EventDispatch.
    new SwingWorker<String, Object>() {
      @Override public String doInBackground() {return "";}
      @Override protected void done(){
        try {
          loadFiles(fileNames);
        } catch (Exception e) {
          log.fail(e);
        }
        fileNames.clear();
      }
    }.execute();
  }


  ///////////////////////
  //                   //
  //  PRIVATE METHODS: //
  //                   //
  ///////////////////////



  ///////////////////////
  // STARTUP/SHUTDOWN: //
  ///////////////////////

  private boolean initializeAndCheck(String[] args) {

    //First, do the absolute minimum setup, which is 
    //to get our home directory and start logging:
    String homeDir=KHome.nameIt(System.getProperty("user.home"), "klonk");
    for (int i=0; i<args.length; i++)
      if (args[i].equals("-home") && i<args.length-1){
        args[i]=null;
        homeDir=args[++i].trim();
        args[i]=null;
      }
    home=new KHome(homeDir);
    if (!home.ready)
      return false;
    String pid=ManagementFactory.getRuntimeMXBean().getName();
    pid=Pattern.compile("[^a-zA-Z0-9]").matcher(pid).replaceAll("");
    log=new KLog(home, pid);

    //Second, set up our file listener and check 
    //and see if we can obtain our mutex:
    fileListen=new FileListen(this, log, pid, home);
    return fileListen.lockOrSignal(args);
  }
  
  private void startup(final String[] args) {
    log.log("Starting up swing...");
    try {
      UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());    
    } catch (Exception e) {
      log.log(e);
    }
    SwingUtilities.invokeLater(new Runnable() {
      public void run() {

        //It is helpful to do this as soon as possible:
        Thread.setDefaultUncaughtExceptionHandler( 
          new Thread.UncaughtExceptionHandler() {
            public void uncaughtException(Thread t, Throwable e){
              log.fail(e);
            }
          }
        );

        //Bootstrap menus, layout so we can get our context object:
        
        //Context
        Kontext context=Kontext.getProduction(
          home, log,
          new StatusNotifier() {public @Override void showStatus(String msg) {layout.showStatus(msg);}}
        );
        persist=context.persist;
        menus=new Menus(Klonk.this, log);
        layout=new MainLayout(context.mainFrame, menus.getMenuBar(), myAppCloseListener, context.iconImage);
        
        //Now follow through on layout:
        layout.show(
          persist.getWindowBounds(
            new java.awt.Rectangle(10, 10, 300, 300)
          ),
          persist.getWindowMaximized()
        );
        popups=context.popups;
        log.setFailPopup(popups.getFailPopup());

        //More persistence stuff:
        recentDirs    =new ArrayList<>(persist.maxRecent);
        recentFiles   =new ArrayList<>(persist.maxRecent);
        favoriteFiles =new ArrayList<>(persist.maxRecent);
        favoriteDirs  =new ArrayList<>(persist.maxRecent);
        recentCommands=new ArrayList<>(persist.maxRecent);
        persist.getFiles(recentFiles, recentDirs, favoriteFiles, favoriteDirs)
               .getCommands(recentCommands);
        wordWrap=persist.getWordWrap();
        fastUndos=persist.getFastUndos();
        defaultLineBreaker=persist.getDefaultLineDelimiter();
        taio=persist.getTabAndIndentOptions();
        fontOptions=persist.getFontAndColors();

        //Popups and menu stuff, uses persistence above:
        popups.setFontAndColors(fontOptions);
        menus.setMaxRecent(persist.maxRecent)
             .setFiles(recentFiles, recentDirs, favoriteFiles, favoriteDirs)
             .setFastUndos(fastUndos)
             .setWordWrap(wordWrap);

        //Create a blank editor:
        editors=new LinkedList<>();
        newEditor();
        
        //Load files. This should come last because it will feed us files
        //async. It will look for files that appeared while we
        //were starting up:
        fileListen.startDirectoryListener();
        loadFiles(args);
        
      }
    });  
  }
  
  private void tryExitSystem() {
    while (editors.size()>0)
      if (!fileCloseLastFirst(true))
        return;
    if (!layout.isMaximized()) {
      persist.setWindowBounds(layout.getMainWindowBounds());
      persist.setWindowMaximized(false);
    }
    else
      persist.setWindowMaximized(true);
    persist.setFiles(recentFiles, recentDirs, favoriteFiles, favoriteDirs)
           .setCommands(recentCommands)
           .setFontAndColors(fontOptions)
           .save();
    fileListen.removeLock();
    layout.dispose();
    System.exit(0);
  }

  ////////////
  // MARKS: //
  ////////////

  private MarkStatus markStatus=new MarkStatus();
  private class MarkStatus implements Runnable {
    boolean set; int i, count;
    public void go(int i, int count, boolean set) {
      this.i=i;
      this.count=count;
      this.set=set;
      SwingUtilities.invokeLater(this);
    }
    public void run() {
      showStatus("Mark "+i+" of "+count+(set ?" set." :"."));
    }
  }
  
  ////////////////
  // CLIPBOARD: //
  ////////////////

  private void toClipboard(String name) {
    StringSelection stringSelection = new StringSelection(name);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, stringSelection);  
    showStatus("Copied to clipboard: "+name);
  }

  ////////////////
  // FILE SAVE: //
  ////////////////

  private boolean fileSave(boolean forceNewFile) {
    Editor e=editors.get(0);
    
    //1. Get file:
    boolean newFile=true;
    File file;
    if (forceNewFile || e.file==null) {
      File dir=e.file==null && recentDirs.size()>0 
        ?new File(recentDirs.get(0))
        :null;
      if ((file=showFileSaveDialog(e.file, dir))==null){
        showStatusBad("File save cancelled");
        return false;
      }
    }
    else {
      file=e.file;
      newFile=false;
    }

    //2. Save file:
    return fileSave(e, file, newFile);
  }
  private boolean fileSave(Editor e, File file, boolean newFile) {
    File oldFile=newFile ?e.file :null;
    try {
      showStatus("Saving...");
      e.saveFile(file);
    } catch (Exception ex) {
      log.fail(ex);
      showStatusBad("Save failed.");
      return false;
    }
    fileIsSaved(e, file, oldFile, newFile);
    return true;
  }
  private File showFileSaveDialog(File f, File dir) {
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
    for (Editor e: editors) 
      if (e.path!=null && e.path.equals(file.toPath().toAbsolutePath()) && e!=editors.getFirst()){
        popups.alert("File is already open in another window; close it first: "+e.title);
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
    if (editors.size()>1){
      //This forces the app to close the most recently accessed
      //files last, so that they are the most likely to appear in the reopen menu:
      Editor e=editors.removeLast();
      editors.addFirst(e);
      setEditor(e);
    }
    return fileClose(forExit);
  }

  private boolean fileClose(boolean forExit) {
    Editor e=editors.get(0);
    if (e.unsavedChanges) {
      YesNoCancelAnswer result=popups.askYesNoCancel("Save changes to: "+e.title+"?");
      if (result.isYes()){
        if (!fileSave(false))
          return false;
      }
      else
      if (result.isCancel())
        return false;
      //"No" means we do nothing
    }
    if (e.file!=null) 
      recentFileClosed(e.file);
    editors.remove(0);
    showStatus("Closed: "+e.title);
    if (editors.size()>0)
      setEditor(editors.get(0));
    else
    if (!forExit)
      newEditor();
    return true;
  }

  ////////////////
  // FILE LOAD: //
  ////////////////
  
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
    log.log("Klonk.loadFile() "+file);
    layout.toFront();
    if (!file.exists()){
      popups.alert("No such file: "+file);
      return;
    }
    for (int i=0; i<editors.size(); i++) {
      Editor ed=editors.get(i);
      if (ed.file!=null && ed.path.equals(file.toPath().toAbsolutePath())){
        editorSwitch(ed);
        popups.alert("File is already open: "+ed.title);
        return;
      }
    }
    try {
      Editor toUse=null;
      for (int i=0; i<editors.size() && toUse==null; i++) {
        Editor e=editors.get(i);
        if (!e.used) {
          toUse=e;
          editorSwitch(e);
        }
      }
      if (toUse==null)
        toUse=newEditor();
      loadFile(toUse, file);
    } catch (Exception e) {
      log.fail(e);
    }
  }
  private boolean loadFile(Editor e, File file) {
    try {
      showStatus("Loading: "+file+"...");
      e.loadFile(file, defaultLineBreaker);
    } catch (Exception ex) {
      log.fail(ex);
      showStatusBad("Load failed");
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
      this, log, myUndoListener, this.defaultLineBreaker, wordWrap
    ); 
    e.setFastUndos(fastUndos);
    e.title=getUnusedTitle(editors);
    e.setTabAndIndentOptions(taio);
    e.setTabsOrSpaces(taio.indentionModeDefault);
    e.setFont(fontOptions);

    editors.add(0, e);
    setEditor(e); 
    return e;
  }
  private void setEditor(Editor e) {
    layout.setEditor(e.getContainer(), e.title);
    editorChange(e);
    e.requestFocus();
  }
  private void capsLock() {
    boolean state= Toolkit.getDefaultToolkit().getLockingKeyState(KeyEvent.VK_CAPS_LOCK); 
    layout.showCapsLock(state);
  }

  private static String getUnusedTitle(List<Editor> editors) {
    String name="Untitled";
    int incr=0;
    while (isUsedTitle(name, editors))
      name="Untitled "+(++incr);
    return name;
  }
  private static boolean isUsedTitle(String name, List<Editor> editors) {
    for (int i=0; i<editors.size(); i++) 
      if (editors.get(i).title.equals(name))
        return true;
    return false;
  }

  ///////////////////
  // WINDOW STATE: //
  ///////////////////


  private void showStatus(String status) {
    layout.showStatus(status);
  }
  private void showStatusBad(String status) {
    layout.showStatus(status, true);
  }
  private void showStatus(String status, boolean bad) {
    layout.showStatus(status, bad);
  }
  private UndoListener myUndoListener=new UndoListener() {
    public void happened(UndoEvent ue) {
      if (ue.isNoMoreUndos)
        showStatusBad("No more undos.");
      else
      if (ue.isNoMoreRedos)
        showStatusBad("No more redos.");
      else
      if (ue.isUndoSaveStable){
        editorStable(editors.get(0));
      }
    }
  };
  private void editorCaretMoved(Editor e, int caretPos) {
    showCaretPos(e, caretPos);
    layout.showNoStatus();
  }
  private void showCaretPos(Editor e) {
    showCaretPos(e, e.getCaretPos());
  }
  private void showCaretPos(Editor e, int caretPos) {
    int rowPos=e.getRow(caretPos);
    layout.showRowColumn(rowPos+1, caretPos-e.getRowOffset(rowPos));
  }
  private void fileIsSaved(Editor e, File file, File oldFile, boolean newFile) {
    if (newFile){
      editorFileChange(e, file);
      recentFileSavedNew(file, oldFile);
    }
    else
      editorStable(e);
    showStatus(newFile ?"File saved: "+file :"File saved");
  }
  private void fileIsLoaded(Editor e, File file) {
    recentFileLoaded(file);
    editorFileChange(e, file);
    showStatus("File loaded: "+file);
  }
  private void editorFileChange(Editor ed, File file) {
    ed.file=file;
    ed.path=file.toPath().toAbsolutePath();
    ed.title=getFullPath(ed.file);
    editorStable(ed);
    editorChange(ed);
  }
  
  private void editorStable(Editor e) {
    e.used|=true;
    if (e.unsavedChanges)
      showStabilityChange(e, false);
  }
  private void editorUnstable(Editor e){
    e.used|=true;
    if (!e.unsavedChanges)
      showStabilityChange(e, true);
  }
  private void showStabilityChange(Editor e, boolean unsavedChanges) {
    e.unsavedChanges=unsavedChanges;
    layout.showChange(unsavedChanges);
  }
  private void editorChange(Editor e) {
    layout.showChange(e.unsavedChanges);
    layout.showTitle(e.title);
    menus.showHasMarks(e.hasMarks());
    showCaretPos(e);
    menus.showHasFile(e.file!=null);
    menus.setSwitchMenu(editors);
  }
  
  
  ///////////////////
  // RECENT FILES: //
  ///////////////////
  

  private void recentFileSavedNew(File file, File oldFile) {
    if (oldFile!=null && !oldFile.equals(file) && oldFile.exists())
      //This is when we save-as and thus discard an existing file.
      recentFileClosed(oldFile);
    recentFileRemoveDirAdd(file);
  }
  private void recentFileLoaded(File file) {
    recentFileRemoveDirAdd(file);
  }
  private void recentFileClosed(File file){
    String path=getFullPath(file);
    for (int i=recentFiles.size()-1; i>=0; i--)
      if (recentFiles.get(i).equals(path))
        recentFiles.remove(i);
    recentFiles.add(0, path);
    if (recentFiles.size()>persist.maxRecent)
      recentFiles.remove(recentFiles.size()-1);
    menus.setRecentFiles(recentFiles);
    File f=file.getParentFile();
    addToRecentDirectories(file);
  }

  private void recentFileRemoveDirAdd(File file) {
    int i=recentFiles.indexOf(getFullPath(file));
    if (i>-1) {
      recentFiles.remove(i);
      menus.setRecentFiles(recentFiles);
    }
    addToRecentDirectories(file);  
  }
  private void addToRecentDirectories(File file) {
    file=file.getParentFile();
    if (file==null)
      return;
    String path=getFullPath(file);
    int i=recentDirs.indexOf(path);
    if (i>-1) recentDirs.remove(i);
    else
    if (recentDirs.size()>persist.maxRecent)
      recentDirs.remove(recentDirs.size()-1);
    recentDirs.add(0, path);
    menus.setRecentDirs(recentDirs);
  }


  ////////////////
  // UTILITIES: //
  ////////////////

  private String getFullPath(File file) {
    try {
      return file.getCanonicalPath();
    } catch (Exception e) {
      log.fail(e);
      return null;
    }
  }
}
