package org.tmotte.klonk.windows.popup.ssh;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.klonk.ssh.SSHExec;
import org.tmotte.klonk.ssh.SSHFile;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.ConnectionParseException;
import org.tmotte.klonk.ssh.WrappedSSHException;
import org.tmotte.klonk.config.msg.Setter;
import java.io.ByteArrayOutputStream;

/** 
  Meant to be used with FileDialogWrapper. This is actually a FileSystemView that can do both local 
  files and ssh files.
  
  FIXME a lot of stuff being created that only needs to be created once.
  FIXME a lot of functions implemented that probably don't need to be.
  FIXME handle ..'s and .'s
*/
public class SSHFileSystemView extends FileSystemView {

  private final FileSystemView defaultView=FileSystemView.getFileSystemView();
  private final ByteArrayOutputStream sshErr=new ByteArrayOutputStream(1024*24);
  private final StringBuilder sshOut=new StringBuilder();
  private final SSHConnections conns;

  private final static File[] noFiles=new File[]{};
  
  public SSHFileSystemView(SSHConnections conns) {
    super();
    this.conns=conns;
  }
  
  /* This gets called when you just bang in a path name or press enter on  a path name. FIXME */
  public @Override File createFileObject(String path){
    String logstring="SSHFileSystemView.createFileObject() ";
    mylog(logstring+path);
    if (conns.is(path)){
      SSHFile file=conns.getFile(path);
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

  //FIXME what does the user interface do on authentication failure?
  private int _getFiles(SSHFile dir, boolean useFileHiding) throws WrappedSSHException {
    sshErr.reset();
    sshOut.setLength(0);
    mylog("SSHFileSystemView._getFiles(): "+dir.getAbsolutePath());
    //FIXME getting a null pointer on / here
    return dir.getSSH().getExec().exec(
      "ls --file-type -a -1 "+dir.getAbsolutePath(), sshOut, sshErr //FIXME quote dir, at least spaces
    );
  }
  public @Override File[] getFiles(File fdir, boolean useFileHiding){
    mylog("getFiles() "+fdir);
    SSHFile dir=cast(fdir);
    if (dir==null)
      return defaultView.getFiles(fdir, useFileHiding);
    try {
      int res=_getFiles(dir, useFileHiding);
      if (res==-1)
        return noFiles;//No connection
      else
      if (res!=0)
        throw new Exception(sshErr.toString("utf-8"));        
    } catch (WrappedSSHException e) {
      if ("socket is not established".equals(e.getWrapped().getMessage()))
        return noFiles;
      else
        throw new RuntimeException(e);
    } catch (Exception e) {
      throw new RuntimeException("Could not list "+dir, e);
    }
    String sshOutStr=DotStripper.process(sshOut.toString());
    String[] s=sshOutStr.split("\n");
    File[] files=new File[s.length];
    for (int i=0; i<s.length; i++){
      boolean isDir=s[i].endsWith("/");
      String name=isDir ? s[i].substring(0, s[i].length()-1) :s[i];
      files[i]=new SSHFile(dir.getSSH(), dir, name, isDir);
    }
    return files;
  }
  /* Returns true if the file (directory) can be visited. */
  public @Override Boolean isTraversable(File f){
    mylog("isTraversable "+f);
    if (cast(f)==null)
      return defaultView.isTraversable(f);
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
    mylog("SSHFileSystemView.isParent() "+folder+" "+file);
    if (cast(folder)==null || cast(file)==null)
      return defaultView.isParent(folder, file);
    return true;
  }


  ///////////////////
  // QUESTIONABLE: //
  ///////////////////

  /* Returns a File object constructed in dir from the given filename. */
  public @Override File createFileObject(File fdir, String filename) {
    mylog("SSHFileSystemView.createFileObject(): Create file object for dir: "+fdir+" name "+filename);
    SSHFile dir=cast(fdir);
    if (dir==null)
      return defaultView.createFileObject(fdir, filename);
    if (true)
      throw new RuntimeException("FIXME");    
    return new SSHFile(
      dir.getSSH(), 
      new SSHFile(dir.getSSH(), dir.getParentFile(), dir.getName(), true),
      filename,
      filename.endsWith("/")
    );
  }
  public @Override File createNewFolder(File containingDir){
    mylog("SSHFileSystemView.createNewFolder: "+containingDir);
    SSHFile dir=cast(containingDir);
    if (dir==null) 
      try {return defaultView.createNewFolder(containingDir);} catch (Exception e) {throw new RuntimeException(e);}
    else {
      File f=new SSHFile(dir.getSSH(), dir, "New-Folder", true);
      f.mkdir();
      return f;
    }
  }
  public @Override File getChild(File parent, String fileName) {
    mylog("SSHFileSystemView.getChild() "+parent+" "+fileName);
    SSHFile dir=cast(parent);
    if (dir==null)
      return defaultView.getChild(parent, fileName);
    return new SSHFile(dir.getSSH(), dir, fileName, false);
  }
  
  public @Override File getHomeDirectory() {
    return defaultView.getHomeDirectory();
  }
  /* Returns the parent directory of dir. */
  public @Override File getParentDirectory(File fdir){
    mylog("SSHFileSystemView.getParentDirectory: "+fdir);
    File dir=cast(fdir);
    if (dir==null)
      return defaultView.getParentDirectory(fdir);    
    else
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


  /* Determines if the given file is a root in the navigatable tree(s). FIXME watch out for /foo/.. */
  public @Override boolean isRoot(File f) {
    if (cast(f)==null)
      return defaultView.isRoot(f);
    else
      return f.getParentFile()==null;
  }

  ////// CRAP: ////////
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

  ////////////////////////
  // Private utilities: //
  ////////////////////////

  private static SSHFile cast(File f) {
    return SSHFile.cast(f);
  }
  private void mylog(String s) {
    if (true)
      System.out.println(s);
  }
 
  /////////////////
  // TOTAL CRAP: //
  /////////////////

  /* Type description for a file, directory, or folder as it would be displayed in a system file browser. */
  public @Override String getSystemTypeDescription(File f){
    return defaultView.getSystemTypeDescription(f);
  }
  /* Returns all root partitions on this system.*/
  public @Override File[] getRoots(){
    return defaultView.getRoots();
  }
  /*
  public @Override javax.swing.Icon getSystemIcon(File f){
    return defaultView.getSystemIcon(f);
  }
  /* Return the user's default starting directory for the file chooser.*/
  public @Override File getDefaultDirectory() {
    mylog("get Default directory");
    return defaultView.getDefaultDirectory();
  }
  
}
