package org.tmotte.klonk.ssh.test;
import org.tmotte.klonk.ssh.SSHCommandLine;
import org.tmotte.klonk.ssh.SSHFile;
import org.tmotte.klonk.ssh.SSHConnections;
import org.tmotte.common.text.ArgHandler;
import java.io.File;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.InputStream;
import java.io.InputStreamReader;


public class FileSave {
  public static void main(String[] args) throws Exception {
    SSHCommandLine cmd=new SSHCommandLine(args);
    SSHFile file=cmd.sshFile;
    try {
      char[] readBuffer=new char[1024 * 20];   
      try (
        OutputStream output=file.getOutputStream();
        OutputStreamWriter outw=new OutputStreamWriter(output, "utf-8");
        ){
        for (int i=0; i<100; i++){
          System.out.print(".");
          System.out.flush();
          outw.append("What oh "+i);
          if (i % 11 == 0)
            outw.append("\n");
        }
      }
      System.out.println("File written");    
  
      try (
          InputStream istrm=file.getInputStream();
          InputStreamReader istr=new InputStreamReader(istrm, "utf-8");
        ) {     
        int charsRead=0;
        while ((charsRead=istr.read(readBuffer, 0, readBuffer.length))>0){
          String s=new String(readBuffer, 0, charsRead);
          System.out.print(s);
          System.out.flush();
        }
        istr.close();
      }
      System.out.println("File read");
      file.delete();
    } finally {
      cmd.connections.close();    
    }
  }
}