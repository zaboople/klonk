package org.tmotte.klonk.ssh;
import com.jcraft.jsch.SftpATTRS;
import java.io.File;
import java.io.FileFilter;
import java.io.FilenameFilter;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.List;
import java.util.regex.Pattern;
import org.tmotte.common.text.StringChunker;

class SSHFileAttr  {
  
  public final boolean isDirectory;
  public final long size;
  public final boolean exists;
  private final long lastMod;//This is the time without milliseconds

  SSHFileAttr(SftpATTRS orig){
    isDirectory=orig.isDir();
    lastMod=orig.getMTime();
    size=orig.getSize();
    exists=true;
  }
  SSHFileAttr(){
    isDirectory=false;
    this.exists=false;
    this.size=0;
    this.lastMod=0;
  }
  
  long getLastModified() {
    return lastMod*1000;
  }  

  public String toString() {
    return exists+" "+isDirectory+" "+size+" "+lastMod;
  }
}
