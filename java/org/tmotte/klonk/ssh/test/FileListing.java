package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHConnections;
import java.io.File;

public class FileListing {
  public static void main(String[] args) throws Exception {
    SSHCommandLine cmd=new SSHCommandLine(args);
    try {
      File file=cmd.sshFile;
      mylog(file);
      System.out.println("Children...");
      for (File f: file.listFiles())
        mylog(f);
      /*
      while ((file=file.getParentFile())!=null) {
        System.out.println("Parent:");
        mylog(file);
        for (File f: file.listFiles()){
          System.out.println("Child:");
          mylog(f);
        }
      }
      */
    } finally {
      cmd.connections.close();
    }
  }
  private static void mylog(File f) {
    long last=f.lastModified();
    System.out.println(
      String.format("test.FilesListing: Dir: %-6s %s %s", ""+f.isDirectory(), new java.util.Date(f.lastModified()), f.getAbsolutePath())
    );
  }
}