package org.tmotte.klonk.windows.popup.ssh;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.ssh.SSHExec;
import org.tmotte.klonk.ssh.SSHFile;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.config.msg.UserNotify;
import java.io.ByteArrayOutputStream;

/** 
  Meant to be used with FileDialogWrapper. This is actually a FileSystemView that can do both local 
  files and ssh files.
*/
public class SSHFileSystemView extends FileSystemView {

  private final FileSystemView defaultView=FileSystemView.getFileSystemView();
  private final ByteArrayOutputStream sshErr=new ByteArrayOutputStream(1024*24);
  private final StringBuilder sshOut=new StringBuilder();
  private final SSHConnections conns;
  private final UserNotify userNotify;

  private final static File[] noFiles=new File[]{};
  
  public SSHFileSystemView(SSHConnections conns, UserNotify userNotify) {
    super();
    this.conns=conns;
    this.userNotify=userNotify;
  }
  
  /* This gets called when you just bang in a path name or press enter on  a path name. */
  public @Override File createFileObject(String path){
    String logstring="createFileObject() ";
    mylog(logstring+path);
    if (conns.is(path)){
      SSHFile file=conns.getSSHFile(path);
      if (file!=null && !file.getSSH().verifyConnection()) 
        file=null;
      if (file==null)
        throw new SSHFileDialogNoFileException();
      mylog(logstring+"Returning file for "+path+": "+file+" is dir "+(file==null ?null :file.isDirectory()));
      return file;
    }
    else
      return defaultView.createFileObject(path);
  }

  public @Override File[] getFiles(File fdir, boolean useFileHiding){
    //mylog("getFiles() "+fdir+" "+fdir.getClass());
    SSHFile dir=cast(fdir);
    if (dir==null)
      return defaultView.getFiles(fdir, useFileHiding);
    return dir.listFiles();
  }
  /* Returns true if the file (directory) can be visited. */
  public @Override Boolean isTraversable(File f){
    if (cast(f)==null)
      return defaultView.isTraversable(f);
    mylog("isTraversable "+f);
    return f.isDirectory();
  }
  /* Used by UI classes to decide whether to display a special icon for drives or partitions, e.g. */
  public @Override boolean isDrive(File dir){
    //mylog("Is drive "+dir+" "+dir.getClass());
    if (cast(dir)==null)
      return defaultView.isDrive(dir);
    return dir.getParentFile()==null;
  }
  /* Is dir the root of a tree in the file system, such as a drive or partition. */
  public @Override boolean isFileSystemRoot(File dir){
    //mylog("isfsroot "+dir);
    if (cast(dir)==null)
      return defaultView.isFileSystemRoot(dir);
    return dir.getParentFile()==null;
  }

  /* Creates a new File object for f with correct behavior for a file system root directory.*/
  protected File createFileSystemRoot(File f){
    mylog("createFileSystemRoot: "+f);
    File ff=cast(f);
    if (ff==null)
      return super.createFileSystemRoot(f);
    else
      while (ff.getParentFile()!=null) //probably wrong
        ff=ff.getParentFile();
    return ff;
  }
  /* On Windows, a file can appear in multiple folders, other than its parent directory in the filesystem. */
  public @Override boolean isParent(File folder, File file){
    //mylog("isParent() "+folder+" "+file);
    SSHFile sFolder=cast(folder), sFile=cast(file);   
    if (folder==null && file==null)
      return defaultView.isParent(folder, file);
    else
    if (folder!=null)
      return folder.equals(file);
    else
      return false;    
  }
  public @Override File createNewFolder(File containingDir){
    mylog("createNewFolder: "+containingDir);
    SSHFile dir=cast(containingDir);
    if (dir==null) 
      try {return defaultView.createNewFolder(containingDir);} catch (Exception e) {throw new RuntimeException(e);}
    else {
      File f=new SSHFile(dir.getSSH(), dir, "New-Folder");
      int i=1;
      while (f.exists())
        f=new SSHFile(dir.getSSH(), dir, "New-Folder"+(i++));
      f.mkdir();
      return f;
    }
  }
  /* Returns a File object constructed in dir from the given filename. */
  public @Override File createFileObject(File fdir, String filename) {
    mylog("createFileObject(): Create file object for dir: "+fdir+" name "+filename);
    SSHFile dir=cast(fdir);
    if (dir==null)
      return defaultView.createFileObject(fdir, filename);
    return new SSHFile(
      dir.getSSH(), 
      dir,
      filename
    );
  }

  ///////////////////
  // QUESTIONABLE: //
  ///////////////////

  public @Override File getChild(File parent, String fileName) {
    SSHFile dir=cast(parent);
    if (dir==null)
      return defaultView.getChild(parent, fileName);    
    mylog("getChild() "+parent+" : "+fileName);
    fileName=fileName.trim();
    if (fileName.equals("."))
      return parent;
    else
    if (fileName.equals(".."))
      return parent.getParentFile();
    else
      return new SSHFile(dir.getSSH(), dir, fileName);
  }
  
  public @Override File getHomeDirectory() {
    return defaultView.getHomeDirectory();
  }
  /* Returns the parent directory of dir. */
  public @Override File getParentDirectory(File fdir){
    File dir=cast(fdir);
    if (dir==null)
      return defaultView.getParentDirectory(fdir);    
    mylog("getParentDirectory: "+fdir);
    return dir.getParentFile();
  }
  /* Name of a file, directory, or folder as it would be displayed in a system file browser. */
  public @Override String getSystemDisplayName(File f){
    //mylog("get system display name "+f);
    if (cast(f)==null)
      return defaultView.getSystemDisplayName(f);
    return f.getName();
  }

  /* Used by UI classes to decide whether to display a special icon for a computer node, e.g. */
  public @Override boolean isComputerNode(File dir){
    if (cast(dir)==null)
      return defaultView.isComputerNode(dir);
    return false;
  }


  /* Determines if the given file is a root in the navigatable tree(s). */
  public @Override boolean isRoot(File f) {
    if (cast(f)==null)
      return defaultView.isRoot(f);
    else 
      return f.getParentFile()==null;
  }

  public @Override javax.swing.Icon getSystemIcon(File f){
    SSHFile sf=cast(f);
    return defaultView.getSystemIcon(
      sf==null 
        ?f
        :new File(sf.getSystemPath())
    );
  }

  ////////////////////////
  // Private utilities: //
  ////////////////////////

  private static SSHFile cast(File f) {
    return SSHFile.cast(f);
  }
  private void mylog(String s) {
    if (true)
      userNotify.log("SSHFileSystemView."+s);
  }

 
  /////////////////
  // TOTAL CRAP: //
  /////////////////

  /* Type description for a file, directory, or folder as it would be displayed in a system file browser. */
  public @Override String getSystemTypeDescription(File f){
    String s=defaultView.getSystemTypeDescription(f);
    System.out.println(s+" "+f);
    return s;
  }
  /* Returns all root partitions on this system.*/
  public @Override File[] getRoots(){
    return defaultView.getRoots();
  }
  /* Return the user's default starting directory for the file chooser.*/
  public @Override File getDefaultDirectory() {
    return defaultView.getDefaultDirectory();
  }
  /* Used by UI classes to decide whether to display a special icon for a floppy disk.*/
  public @Override boolean isFloppyDrive(File dir){
    if (cast(dir)==null)
      return defaultView.isFloppyDrive(dir);
    return false;
  }
  /* Returns whether a file is hidden or not. */
  public @Override boolean isHiddenFile(File f){
    if (cast(f)==null)
      return defaultView.isHiddenFile(f);
    return false;
  }
  /* Checks if f represents a real directory or file as opposed to a special folder such as "Desktop". */
  public @Override boolean isFileSystem(File f){
    if (cast(f)==null)
      return defaultView.isFileSystem(f);  
    return true;
  }
  
}
