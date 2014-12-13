package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.common.text.ArgHandler;
import java.io.File;

public class TestTilde {
  public static void main(String[] args) throws Exception {
    SSHCommandLine scl=new SSHCommandLine(args);
    System.out.println(scl.ssh);
    System.out.println("BAM! ~ is: "+scl.ssh.getTildeFix());
    scl.ssh.close();
  }
  private static void mylog(String msg) {
    System.out.println("test.FilesListing: "+msg);
  }

}