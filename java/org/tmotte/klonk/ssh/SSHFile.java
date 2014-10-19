package org.tmotte.klonk.ssh;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.nio.file.Path;

public class SSHFile extends File {
  
  public final static SSHFile SLASH(SSH ssh){
    return new SSHFile(ssh, null, "/", true);
  }
  public static SSHFile cast(File f) {
    if (f instanceof SSHFile)
      return (SSHFile)f;
    //System.out.println("WARNING NOT SSH "+f);
    return null;    
  }
 

  protected SSHFile parent;
  protected boolean isDir=false;  
  private final SSH ssh;
  private String name;

  
  public SSHFile(SSH ssh, SSHFile parent, String name, boolean isDir) {
    super(parent, name); 
    this.name=name;
    this.ssh=ssh;
    this.parent=parent;   
    this.isDir=isDir;
  }
  public SSH getSSH(){
    return ssh;
  }
  public String getHost() {
    return ssh.getHost();
  }
  public String getUser() {
    return ssh.getUser();
  }
  
  /*Tests whether this abstract pathname is absolute.*/
  public @Override boolean isAbsolute() {
    return true;
  }
  /*Tests whether the file named by this abstract pathname is a hidden file.*/
  public @Override boolean isHidden(){
    return false;
  }  

  public @Override boolean isFile() {
    return !isDir;
  }
  public @Override boolean isDirectory() {
    return isDir;
  }
  public @Override SSHFile getParentFile() {
    return parent;
  }
  public @Override boolean exists() {
    return true; //FIXME
  }
  private void getAbsolutePath(StringBuilder sb) {
    if (parent!=null){
      parent.getAbsolutePath(sb);
      if (!parent.name.equals("/")) 
        sb.append("/");
    }
    sb.append(getName());
  }
  public @Override String getAbsolutePath(){
    StringBuilder sb=new StringBuilder(10);
    getAbsolutePath(sb);
    return sb.toString();
  }
  public String getNetworkPath() {
    StringBuilder sb=new StringBuilder("ssh://");
    getAbsolutePath(sb);
    return sb.toString();
  }
  public @Override String getCanonicalPath() {
    return getAbsolutePath();
  }
  public @Override String	getName() {
    return name;
  }
  /*Converts this abstract pathname into a pathname string.*/
  public @Override String	getPath() {
    //System.out.println("SSHFile.getPath: FIXME "+getName());
    return getAbsolutePath();
  }
  /*Compares two abstract pathnames lexicographically.*/
  public @Override int compareTo(File other){
    return getAbsolutePath().compareTo(other.getAbsolutePath()); 
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
    return getAbsolutePath().hashCode();
  }
  
  public @Override String toString() {
    return getAbsolutePath();
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
    System.out.println("FIXME SSHFile.length()");
    return 100;
  }
  
  
  
  
  
  /*Returns a java.nio.file.Path object constructed from the this abstract path.*/
  public @Override Path toPath() {
    System.out.println("SSHFile.toPath");
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
    System.out.println("SSHFile.canExecute");
    return super.canExecute();
  }  
  /*Tests whether the application can read the file denoted by this abstract pathname.*/
  public @Override boolean canRead() {
    System.out.println("SSHFile.canRead");
    return super.canRead();
  }  
  /*Tests whether the application can modify the file denoted by this abstract pathname.*/
  public @Override boolean canWrite() {
    System.out.println("SSHFile.canWrite");
    return super.canWrite();
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
    System.out.println("SSHFile.getAbsoluteFile: FIXME "+getName());
    return this;  
  }
  
  
  /*Returns the time that the file denoted by this abstract pathname was last modified.*/
  public @Override long	lastModified() {
    System.out.println("SSHFile.lastModified: FIXME "+this);
    return 1;
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
  /*Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.*/
  public @Override String[] list(){
    throw new UnsupportedOperationException();
  }
  /** 
   * Returns an array of strings naming the files and directories in the directory denoted by this 
   * abstract pathname that satisfy the specified filter.
   */
  public @Override String[] list(FilenameFilter filter){
    throw new UnsupportedOperationException();
  }
  /*Returns an array of abstract pathnames denoting the files in the directory denoted by this abstract pathname.*/
  public @Override File[] listFiles(){
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
            "/",
            true
          ), 
          "home", 
          true
        ), 
        "zaboople", 
        true
      );
    System.out.println(file.getAbsolutePath());
  }
}