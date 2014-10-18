package org.tmotte.klonk.ssh;
import com.jcraft.jsch.*;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

public class SFTP {
  
  private SSH ssh;
  public SFTP(SSH ssh) {
    this.ssh=ssh;
  }
  public SSH getSSH() {
    return ssh;
  }
  public static void main(String[] args) throws Exception{
 
    SSH ssh=SSH.cmdLine(args);
    Session session=ssh.getSession();
    Channel channel=session.openChannel("sftp");
    channel.connect();
    ChannelSftp sftp=(ChannelSftp)channel;
 

    String str;
    int level=0;
 
    session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");
    session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");

    /*
    int charsRead=0;
    byte[] readBuffer=new byte[1024 * 20];
    try (
        InputStream istrm=sftp.get("/home/zaboople/myfile");
        InputStreamReader istr=new InputStreamReader(istrm, fmData.encoding);
      ) {     
      while ((charsRead=istr.read(readBuffer, 0, readBuffer.length))>0){
        String s=new String(readBuffer, 0, charsRead);
        System.out.print(s);
      }
      istr.close();
    }
    java.io.OutputStream output=sftp.put("/home/zaboople/fromme.txt");
    output.close();
    ssh.close();
    */
  }
 
  
}
