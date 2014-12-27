package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.common.text.ArgHandler;
import java.io.File;

public class FileListing {
  public static void main(String[] args) throws Exception {
    SSHCommandLine cmd=new SSHCommandLine(args);
    File file=cmd.sshFile;
    mylog("SSH File: "+file);
    mylog("Is directory: "+file.isDirectory());
    for (File f: file.listFiles())
      mylog("File: "+f+" "+f.isDirectory()); 
    while ((file=file.getParentFile())!=null) {    
      mylog("Parent "+file+" "+file.isDirectory());
      for (File f: file.listFiles())
        mylog("File: "+f+" "+f.isDirectory()); 
    }
    cmd.connections.close();
  }
  private static void mylog(String msg) {
    System.out.println("test.FilesListing: "+msg);
  }
}