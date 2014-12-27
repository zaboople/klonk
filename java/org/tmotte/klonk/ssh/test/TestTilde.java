package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.klonk.ssh.SSH;
import org.tmotte.common.text.ArgHandler;
import java.io.File;

public class TestTilde {
  public static void main(String[] args) throws Exception {
    testFinally();
  }
  
  
  private static void test(String[] args) throws Exception {
    SSHCommandLine cmd=new SSHCommandLine(args); 
    SSH ssh=cmd.ssh;
    System.out.println(ssh);
    System.out.println("BAM! ~ is: "+ssh.getTildeFix());
    ssh.close();
  }
  
  
  private static void testFinally() throws Exception {
    try {
      if (true)
        throw new Exception("BANG");
    } catch (Exception e) {
      throw new RuntimeException("RETHROW FROM CATCH", e);
    } finally {
      System.out.println("NO MATTER WHAT");
    }
  }

}