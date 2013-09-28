package org.tmotte.klonk.controller;
import java.util.ArrayList;
import java.io.File;
import java.util.List;
import java.util.LinkedList;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Setter;

/** This is a secondary controller; it is invoked by other controllers. */
class Recents {

  //DI resources:
  private KPersist persist;
  private Setter<List<String>> recentFileListener, recentDirListener;

  //Private data:
  private ArrayList<String> recentDirs, recentFiles;
  
  Recents(KPersist persist) {
    this.persist=persist;
    recentDirs    =new ArrayList<>(persist.maxRecent);
    recentFiles   =new ArrayList<>(persist.maxRecent);
    persist.getRecent(recentFiles, recentDirs);
  }
  public void setFileListener(Setter<List<String>> recentFileListener) {
    this.recentFileListener=recentFileListener;
    recentFileListener.set(recentFiles);
  }
  public void setDirListener(Setter<List<String>> recentDirListener) {
    this.recentDirListener=recentDirListener;
    recentDirListener.set(recentDirs);
  }


  String getFirstDir()  {return recentDirs.get(0);}
  boolean hasDirs()     {return recentDirs.size()>0;}
  void recentFileSavedNew(File file, File oldFile) {
    if (oldFile!=null && !oldFile.equals(file) && oldFile.exists())
      //This is when we save-as and thus discard an existing file.
      recentFileClosed(oldFile);
    recentFileRemoveDirAdd(file);
  }
  void recentFileLoaded(File file) {
    recentFileRemoveDirAdd(file);
  }
  void recentFileClosed(File file){
    String path=ControllerUtils.getFullPath(file);
    for (int i=recentFiles.size()-1; i>=0; i--)
      if (recentFiles.get(i).equals(path))
        recentFiles.remove(i);
    recentFiles.add(0, path);
    if (recentFiles.size()>persist.maxRecent)
      recentFiles.remove(recentFiles.size()-1);
    persist.setRecentFiles(recentFiles);
    recentFileListener.set(recentFiles);
    File f=file.getParentFile();
    addToRecentDirectories(file);
  }

  ////////////////
  // INTERNALS: //
  ////////////////

  private void recentFileRemoveDirAdd(File file) {
    int i=recentFiles.indexOf(ControllerUtils.getFullPath(file));
    if (i>-1) {
      recentFiles.remove(i);
      recentFileListener.set(recentFiles);
      persist.setRecentFiles(recentFiles);
    }
    addToRecentDirectories(file);  
  }
  private void addToRecentDirectories(File file) {
    file=file.getParentFile();
    if (file==null)
      return;
    String path=ControllerUtils.getFullPath(file);
    int i=recentDirs.indexOf(path);
    if (i>-1) recentDirs.remove(i);
    else
    if (recentDirs.size()>persist.maxRecent)
      recentDirs.remove(recentDirs.size()-1);
    recentDirs.add(0, path);
    recentDirListener.set(recentDirs);
    persist.setRecentDirs(recentDirs);
  }


  
}