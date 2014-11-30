package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.common.text.ArgHandler;
import java.io.File;

public class FileListing {
  public static void main(String[] args) throws Exception {
    FileGrab grab=new FileGrab();
    SSHConnections conns=new SSHCommandLine(grab).process(args);
    mylog("File: "+grab.file);
    File file=conns.getFile(grab.file);
    mylog("SSH File: "+file);
    mylog("Is directory: "+file.isDirectory());
    for (File f: file.listFiles())
      mylog("File: "+f);
    conns.close();
  }
  private static void mylog(String msg) {
    System.out.println("test.FilesListing: "+msg);
  }
  private static class FileGrab implements ArgHandler {
    String file;
    public int handle(String[] args, int i) {
      if (args[i].equals("-f"))
        file=args[++i];
      else
        i=-1;
      return i;      
    }
    public String document(){
      return "-f <file>";
    }
  }
}