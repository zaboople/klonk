package org.tmotte.klonk.ssh;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

public class SFTP {
  
  private SSH ssh;
  private ChannelSftp channel;
  public SFTP(SSH ssh) {
    this.ssh=ssh;
  }
  public SSH getSSH() {
    return ssh;
  }
  public InputStream getInputStream(String file) throws Exception {
    return getChannel().get(file);
  }
  public OutputStream getOutputStream(String file) throws Exception {    
    return getChannel().put(file);
  }
  public void close() throws Exception {
    if (channel!=null && channel.isConnected())
      channel.disconnect();
  }
  private ChannelSftp getChannel() throws Exception {
    if (channel==null) {
      Session session=ssh.getSession();
      session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");//FIXME doesnt work otherwise
      session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
      channel=(ChannelSftp)session.openChannel("sftp");
      channel.connect();      
    }
    return channel;
  }
  
  ///////////
  // TEST: //
  ///////////

  /*
  public static void main(String[] args) throws Exception{
    SSH ssh=SSH.cmdLine(args);
    if (ssh==null)
      return;
    SFTP sftp=new SFTP(ssh);
    SSHExec exec=new SSHExec(ssh);    
    StringBuffer outStuff=new StringBuffer();
    
    String dir="/home/zaboople/test1";
    if (exec.exec("mkdir -p "+dir, outStuff, outStuff)!=0){
      System.out.println(outStuff.toString());
      return;
    }
 
    char[] readBuffer=new char[1024 * 20];
 
    //fixme WRAP IN for loop of about 1000 tries
    for (int all=0; all<10; all++){
      
      System.out.println("Starting...");
      String subdir=dir+"/test"+all;
      outStuff.setLength(0);
      if (exec.exec("mkdir -p "+subdir, outStuff, outStuff)!=0){
        System.out.println(outStuff.toString());
        return;
      }
      System.out.println("Dir made...");
      
      for (int f=0; f<5; f++) {
        String filename=subdir+"/blah"+f+".txt";
        try (
          java.io.OutputStream output=sftp.getOutputStream(filename);
          OutputStreamWriter outw=new OutputStreamWriter(output, "utf-8");
          ){
          for (int i=0; i<100; i++){
            System.out.print(".");
            System.out.flush();
            outw.append("What oh "+i);
          }
        }
        System.out.println("File written");
        
        try (
            InputStream istrm=sftp.getInputStream(filename);
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
      }
    }
    sftp.close();
    ssh.close();
  }
  */
  
}
