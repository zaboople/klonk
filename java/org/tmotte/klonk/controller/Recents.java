package org.tmotte.klonk.controller;
import java.io.File;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.UserServer;
import org.tmotte.klonk.ssh.SSHFile;

/** This is a secondary controller; it is invoked by other controllers. */
class Recents {

  //DI resources:
  private KPersist persist;
  private Setter<List<String>> recentFileListener, recentDirListener;

  //Private data:
  private ArrayList<String> recentDirs, recentFiles;
  private ArrayList<UserServer> recentSSHConns;

  //Initialize:
  Recents(KPersist persist) {
    this.persist=persist;
    recentDirs    =new ArrayList<>(KPersist.maxRecent);
    recentFiles   =new ArrayList<>(KPersist.maxRecent);
    recentSSHConns=new ArrayList<>(KPersist.maxRecent);
    persist.getRecent(recentFiles, recentDirs);
    persist.getRecentSSH(recentSSHConns);
  }
  void setFileListener(Setter<List<String>> recentFileListener) {
    this.recentFileListener=recentFileListener;
    recentFileListener.set(recentFiles);
  }
  void setDirListener(Setter<List<String>> recentDirListener) {
    this.recentDirListener=recentDirListener;
    recentDirListener.set(recentDirs);
  }

  //Data retrieval:
  String getFirstDir()  {return recentDirs.get(0);}
  boolean hasDirs()     {return recentDirs.size()>0;}
  List<UserServer> getRecentSSHConns(){
    return recentSSHConns;
  }

  //Events:
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
    if (recentFiles.size()>KPersist.maxRecent)
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
    //Directory:
    {
      File parent=file.getParentFile();
      if (parent==null)
        return;
      String path=ControllerUtils.getFullPath(parent);
      int i=recentDirs.indexOf(path);
      if (i>-1) recentDirs.remove(i);
      else
      if (recentDirs.size()>KPersist.maxRecent)
        recentDirs.remove(recentDirs.size()-1);
      recentDirs.add(0, path);
      recentDirListener.set(recentDirs);
      persist.setRecentDirs(recentDirs);
    }

    //SSH host/server:
    {
      SSHFile ssf=SSHFile.cast(file);
      if (ssf!=null){
        String user=ssf.getSSH().getUser(),
               host=ssf.getSSH().getHost();
        if (recentSSHConns==null)
          recentSSHConns=new ArrayList<UserServer>(KPersist.maxRecent);
        boolean isFirst=false;
        for (int i=0; i<recentSSHConns.size(); i++) {
          UserServer us=recentSSHConns.get(i);
          if (us.server.equals(host) && us.user.equals(user)){
            if (!(isFirst = i==0))
              recentSSHConns.remove(i);
            break;
          }
        }
        if (!isFirst) {
          recentSSHConns.add(0, new UserServer(user, host));
          if (recentSSHConns.size()>KPersist.maxRecent)
            recentSSHConns.remove(recentSSHConns.size()-1);
          persist.setRecentSSH(recentSSHConns);
        }
      }
    }
  }
}
