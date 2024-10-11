package org.tmotte.klonk.ssh;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;

public class SSHFile extends File {
  private static final long serialVersionUID = 1L;

  ///////////////////////
  // STATIC UTILITIES: //
  ///////////////////////

  public static SSHFile cast(File f) {
    if (f instanceof SSHFile)
      return (SSHFile)f;
    return null;
  }
  private static Pattern blankPattern=Pattern.compile(" ");

  ///////////////////////////////////////
  // INSTANCE VARIABLES / CONSTRUCTOR: //
  ///////////////////////////////////////

  protected SSHFile parent;
  private final transient SSH ssh;
  private String name;
  private transient SSHFileAttr attributes=null;

  public SSHFile(SSH ssh, SSHFile parent, String filename) {
    super(parent, filename);
    this.name=filename;
    this.ssh=ssh;
    this.parent=parent;
  }


  public SSH getSSH(){
    return ssh;
  }
  public InputStream getInputStream() throws Exception {
    return ssh.getInputStream(getTildeFixPath());
  }
  public OutputStream getOutputStream() throws Exception {
    if (!exists())
      ssh.makeFile(getQuotedPath());
    return ssh.getOutputStream(getTildeFixPath());
  }

  public @Override String getName() {
    return name;
  }



  /*Tests whether this abstract pathname is absolute. */
  public @Override boolean isAbsolute() {
    return true;
  }
  /*Tests whether the file named by this abstract pathname is a hidden file.*/
  public @Override boolean isHidden(){
    return false;
  }

  ///////////////////////////
  // LAZY LOAD ATTRIBUTES: //
  ///////////////////////////

  public @Override boolean isFile() {
    refresh();
    return attributes!=null && attributes.exists && !attributes.isDirectory;
  }
  public @Override boolean isDirectory() {
    refresh();
    return attributes!=null && attributes.exists && attributes.isDirectory;
  }
  public @Override boolean exists() {
    refresh();
    return attributes!=null && attributes.exists;
  }
  private void refresh(){
    attributes=ssh.getAttributes(getTildeFixPath().trim());
  }

  //////////////
  // LISTING: //
  //////////////

  /*Returns an array of strings naming the files and directories in the directory denoted by this abstract pathname.*/
  public @Override String[] list(){
    return ssh.list(getTildeFixPath());
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
        f
      );
    }
    return files;
  }

  /////////////////
  // PATH STUFF: //
  /////////////////

  public @Override String getAbsolutePath(){
    return getNetworkPath();
  }
  /** This will get called when deciding what icon to display in a JFileChooser. Just warning about that. */
  public @Override String getCanonicalPath() {
    return getNetworkPath();
  }
  /*Converts this abstract pathname into a pathname string.*/
  public @Override String getPath() {
    return getNetworkPath();
  }
  /** Just the path on the remote system, not including ssh/server/user info */
  public String getSystemPath() {
    StringBuilder sb=new StringBuilder();
    getSystemPath(sb);
    return sb.toString();
  }
  /*Returns the canonical form of this abstract pathname.*/
  public @Override File	getCanonicalFile(){
    return this;
  }

  // PRIVATE: //

  /** Includes the ssh://name@host: in the path */
  private String getNetworkPath() {
    String user=getUser(), host=getHost();
    StringBuilder sb=new StringBuilder(20);
    sb.append("ssh:");
    sb.append(user==null ?""  :user+"@");
    sb.append(host==null ?":" :host+":");
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
  private String getTildeFixPath(){
    String s=getSystemPath();
    if (s.startsWith("~"))
      s=s.replace("~", ssh.getTildeFix());
    return s;
  }
  private String getQuotedPath(){
    String s=getSystemPath();
    if (s.contains(" "))
      s=blankPattern.matcher(s).replaceAll("\\\\ ");
    return s;
  }

  //////////////
  // COMPARE: //
  //////////////

  /*Compares two abstract pathnames lexicographically.*/
  public @Override int compareTo(File other){
    return getAbsolutePath().compareTo(other.getAbsolutePath());
  }
  private static boolean cmp(Object a, Object b) {
    if (a==null)
      return b==null;
    else
    if (b==null)
      return a==null;
    else
      return a.equals(b);
  }
  /*Tests this abstract pathname for equality with the given object.*/
  public @Override boolean equals(Object obj){
    if (obj==null || !(obj instanceof SSHFile))
      return false;
    SSHFile other=(SSHFile)obj;
    if (!cmp(this.getName(), other.getName()))
      return false;
    if (this.parent==null && other.parent==null)
      return cmp(this.getUser(), other.getUser())
        &&   cmp(this.getHost(), other.getHost());
    return cmp(this.parent, other.parent);
  }
  /*Computes a hash code for this abstract pathname.*/
  public @Override int hashCode(){
    return getNetworkPath().hashCode();
  }
  public @Override String toString() {
    return getNetworkPath();
  }


  private String getUser() {
    return ssh==null  ?null :ssh.getUser();
  }
  private String getHost() {
    return ssh==null  ?null :ssh.getHost();
  }
  private void mylog(String s) {
    ssh.userNotify.log(s);
  }

  ///////////////////////////
  // FILE MODIFICATION:    //
  // mkdir, rename, delete //
  ///////////////////////////

  /*Creates the directory named by this abstract pathname. */
  public @Override boolean mkdir(){
    ssh.uncache(getTildeFixPath());
    return ssh.mkdir(getQuotedPath());
  }
  /*Renames the file denoted by this abstract pathname.*/
  public @Override boolean renameTo(File dest){
    SSHFile sshFile=cast(dest);
    String otherName=sshFile==null
      ?dest.getAbsolutePath()
      :sshFile.getQuotedPath();
    boolean success=ssh.rename(getQuotedPath(), otherName);
    ssh.uncache(getTildeFixPath());
    if (success)
      this.name=dest.getName();
    return success;
  }
  /*Deletes the file or directory denoted by this abstract pathname.*/
  public @Override boolean delete(){
    ssh.uncache(getTildeFixPath());
    return ssh.remove(getQuotedPath());
  }

  //////////////////////
  // OTHER OVERRIDES: //
  //////////////////////

  public @Override SSHFile getParentFile() {
    return parent;
  }
  /*Returns the length of the file denoted by this abstract pathname.*/
  public @Override long length(){
    return attributes==null ?0 :attributes.size;
  }
  /*Returns the time that the file denoted by this abstract pathname was last modified.*/
  public @Override long	lastModified() {
    return attributes==null ?1 :attributes.getLastModified();
  }


  ///////////////////////////////
  // NOT PROPERLY IMPLEMENTED: //
  ///////////////////////////////


  /**
   * Returns a java.nio.file.Path object constructed from the this abstract path.
   * This is a little bit of a problem as Editor uses it to check for "sameness".
   * Not sure why it can't use absolutePath() instead.
   */
  public @Override Path toPath() {
    mylog(getSystemPath()+"toPath");
    return super.toPath();
  }
  /*Tests whether the application can execute the file denoted by this abstract pathname.*/
  public @Override boolean canExecute() {
    mylog("canExecute");
    return super.canExecute();
  }
  /*Tests whether the application can read the file denoted by this abstract pathname.*/
  public @Override boolean canRead() {
    mylog("canRead");
    return true;
    //return super.canRead();
  }
  /*Tests whether the application can modify the file denoted by this abstract pathname.*/
  public @Override boolean canWrite() {
    mylog("canWrite");
    return true;
  }
  /*Returns the absolute form of this abstract pathname.*/
  public @Override File	getAbsoluteFile(){
    mylog("getAbsoluteFile: "+getName());
    return this;
  }



  ////////////////////
  //  UNSUPPORTED:  //
  ////////////////////

  /*Atomically creates a new, empty file named by this abstract pathname if and only if a file with this name does not yet exist.*/
  public @Override boolean createNewFile(){
    throw new UnsupportedOperationException();
  }
  /*Creates the directory named by this abstract pathname, including any necessary but nonexistent parent directories.*/
  public @Override boolean mkdirs(){
    throw new UnsupportedOperationException();
  }
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

}