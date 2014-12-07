package org.tmotte.klonk.ssh;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.nio.file.Path;
import org.tmotte.common.text.StringChunker;
import java.util.List;
import java.util.LinkedList;

public class SSHFile extends File {
  
  public static SSHFile cast(File f) {
    if (f instanceof SSHFile)
      return (SSHFile)f;
    return null;    
  }
 

  protected SSHFile parent;
  protected boolean isDir=false;  
  private final SSH ssh;
  private String name;
  
  private boolean knows=false;

  
  public SSHFile(SSH ssh, SSHFile parent, String name) {
    super(parent, name); 
    this.name=name;
    this.ssh=ssh;
    this.parent=parent;   
  }
  protected SSHFile(SSH ssh, SSHFile parent, String name, boolean isDir) {
    this(ssh, parent, name);
    this.knows=true;
    this.isDir=isDir;
  }


  public SSH getSSH(){
    return ssh;
  }
  
  public @Override String getName() {
    return name;
  }
  /*Tests whether this abstract pathname is absolute. FIXME what happens with ~? */
  public @Override boolean isAbsolute() {
    return true;
  }
  /*Tests whether the file named by this abstract pathname is a hidden file.*/
  public @Override boolean isHidden(){
    return false;
  }  

  public @Override boolean exists() {
    SSHExecResult res=ssh.exec("ls -lda "+getSystemPath());
    knows=true;
    isDir=res.success && res.output.startsWith("d");
    return res.success;
  }
  public @Override boolean isFile() {
    return !isDirectory();
  }
  public @Override boolean isDirectory() {
    if (!knows)
      exists();
    return isDir;
  }
  
  private static String[] noFiles={};
  
  /*Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.*/
  public @Override String[] list(){
    String absolutePath=getSystemPath();
    SSHExecResult res=ssh.exec("ls --file-type -1 "+absolutePath); //FIXME quote the file

    //Fail, for whatever reason including nonexistence:
    if (!res.success) {
      mylog("Failed to list: "+res.output);
      return noFiles;
    }
      
    //List files & dirs:
    StringChunker sc=new StringChunker(res.output);
    List<String> lFiles=new LinkedList<>();
    while (sc.find("\n"))
      lFiles.add(sc.getUpTo().trim());
    String last=sc.getRest().trim();
    if (!last.equals(""))
      lFiles.add(last);
      
    //This happens when it's not a directory, it's a file and you just ls'd it anyhow:
    if (lFiles.size()==1 && lFiles.get(0).equals(absolutePath))
      return noFiles;
      
    //Copy into results:
    String[] files=new String[lFiles.size()];
    for (int i=0; i<lFiles.size(); i++) 
      files[i]=lFiles.get(i);
    return files;
  }
  /*Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.*/
  public @Override File[] listFiles(){
    String[] fs=list();
    File[] files=new File[fs.length];
    for (int i=0; i<fs.length; i++){
      String f=fs[i];
      boolean endsWithSlash=f.endsWith("/");      
      files[i]=new SSHFile(
        ssh, 
        this, 
        endsWithSlash 
          ?f.substring(0, f.length()-1)
          :f, 
        endsWithSlash
      );
    }
    return files;
  }
  public @Override String getAbsolutePath(){
    return getSystemPath();
  }
  /** This will get called when deciding what icon to display in a JFileChooser. Just warning about that. */
  public @Override String getCanonicalPath() {
    return getSystemPath();
  }
  /*Converts this abstract pathname into a pathname string.*/
  public @Override String getPath() {
    return getSystemPath();
  }

  /** Includes the ssh://name@host: in the path */
  public String getNetworkPath() {
    String user=getUser(), host=getHost();
    StringBuilder sb=new StringBuilder(20);
    sb.append("ssh://");
    sb.append(user==null ?""  :user+"@");
    sb.append(host==null ?":" :host+":");
    getSystemPath(sb);
    return sb.toString();
  }
  /** Just the path on the remote system, not including ssh/server/user info */
  public String getSystemPath() {
    StringBuilder sb=new StringBuilder();
    getSystemPath(sb);
    return sb.toString();
  }
  private void getSystemPath(StringBuilder sb) {
    if (parent!=null){
      parent.getSystemPath(sb);
      if (!parent.name.equals("/")) 
        sb.append("/");
    }
    sb.append(getName());
  }


  /*Compares two abstract pathnames lexicographically.*/
  public @Override int compareTo(File other){
    return getAbsolutePath().compareTo(other.getAbsolutePath()); 
  }

  private String getUser() {
    return ssh==null 
      ?null
      :ssh.getUser();
  }
  private String getHost() {
    return ssh==null 
      ?null
      :ssh.getHost();
  }
  
  
  public @Override SSHFile getParentFile() {
    return parent;
  }

  /*Tests this abstract pathname for equality with the given object.*/
  public @Override boolean equals(Object obj){
    if (obj==null || !(obj instanceof SSHFile))
      return false;
      
    SSHFile other=(SSHFile)obj;
    if (!this.getName().equals(other.getName()))
      return false;
    else
    if (other.parent==null)
      return this.parent==null;
    else
    if (this.parent==null)
      return false;
    else
      return this.parent.equals(other.parent);
  }
  /*Computes a hash code for this abstract pathname.*/
  public @Override int hashCode(){
    return getNetworkPath().hashCode();
  }
  
  public @Override String toString() {
    return getNetworkPath();
  }
  /*Returns the canonical form of this abstract pathname.*/
  public @Override File	getCanonicalFile(){
    return this;  
  }
  
  
  ///////////////////////////////
  // NOT PROPERLY IMPLEMENTED: //
  ///////////////////////////////


  /*Returns the length of the file denoted by this abstract pathname.*/
  public @Override long length(){
    mylog("FIXME SSHFile.length()");
    return 100;
  }
  
  
  
  
  
  /*Returns a java.nio.file.Path object constructed from the this abstract path.*/
  public @Override Path toPath() {
    mylog("toPath");
    return super.toPath();
  }  
  /*Creates the directory named by this abstract pathname.*/
  public @Override boolean mkdir(){
    throw new UnsupportedOperationException();
  }
  /*Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.*/
  public @Override boolean mkdirs(){
    throw new UnsupportedOperationException();
  }
  /*Renames the file denoted by this abstract pathname.*/
  public @Override boolean renameTo(File dest){
    throw new UnsupportedOperationException();
  }  

  /*Tests whether the application can execute the file denoted by this abstract pathname.*/
  public @Override boolean canExecute() {
    mylog("canExecute");
    return super.canExecute();
  }  
  /*Tests whether the application can read the file denoted by this abstract pathname.*/
  public @Override boolean canRead() {
    mylog("canRead");
    return super.canRead();
  }  
  /*Tests whether the application can modify the file denoted by this abstract pathname.*/
  public @Override boolean canWrite() {
    mylog("canWrite");
    return true;
  }
  /*Atomically creates a new, empty file named by this abstract pathname if and only if a file with this name does not yet exist.*/
  public @Override boolean createNewFile(){
    throw new RuntimeException("Not supported yet.");
  }
  /*Deletes the file or directory denoted by this abstract pathname.*/
  public @Override boolean delete(){
    throw new RuntimeException("Not supported yet.");
  }
  /*Returns the absolute form of this abstract pathname.*/
  public @Override File	getAbsoluteFile(){
    mylog("getAbsoluteFile: FIXME "+getName());
    return this;  
  }
  
  
  /*Returns the time that the file denoted by this abstract pathname was last modified.*/
  public @Override long	lastModified() {
    mylog("lastModified: FIXME "+this);
    return 1;
  }
  
  private void mylog(String s) {
    System.out.println("SSHFile:"+s);
  }
  
  ////////////////////////
  //  APPARENTLY JUNK:  //
  ////////////////////////

  /*Requests that the file or directory denoted by this abstract pathname be deleted when the virtual machine terminates.*/
  public @Override void deleteOnExit(){
    throw new UnsupportedOperationException();
  }
  /*Returns the number of unallocated bytes in the partition named by this abstract path name.*/
  public @Override long	getFreeSpace(){
    throw new UnsupportedOperationException();
  }
  /*Returns the size of the partition named by this abstract pathname.*/
  public @Override long	getTotalSpace() {
    throw new UnsupportedOperationException();
  }
  /*Returns the number of bytes available to this virtual machine on the partition named by this abstract pathname.*/
  public @Override long	getUsableSpace(){
    throw new UnsupportedOperationException();
  }

  /** 
   * Returns an array of strings naming the files and directories in the directory denoted by this 
   * abstract pathname that satisfy the specified filter.
   */
  public @Override String[] list(FilenameFilter filter){
    throw new UnsupportedOperationException();
  }
  /** 
   *  Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this 
   *  abstract pathname that satisfy the specified filter.
   */
  public @Override File[] listFiles(FileFilter filter){
    throw new UnsupportedOperationException();
  }
  /**
   *  Returns an array of abstract pathnames denoting the files and directories in the directory denoted by this 
   *  abstract pathname that satisfy the specified filter.
   */
  public @Override File[] listFiles(FilenameFilter filter){
    throw new UnsupportedOperationException();
  }

  /////////////
  //  TEST:  //
  /////////////
  
  public static void main(String[] args) throws Exception {
    SSHFile file=
      new SSHFile(
        null, 
        new SSHFile(
          null, 
          new SSHFile(
            null, 
            null, 
            "/"
          ), 
          "home"
        ), 
        "zaboople"
      );
    System.out.println(file.getAbsolutePath());
  }
}