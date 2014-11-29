package org.tmotte.klonk.ssh;
import com.jcraft.jsch.*;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import java.io.ByteArrayOutputStream;

public class SSHExec {
  
  private SSH ssh;
  public SSHExec(SSH ssh) {
    this.ssh=ssh;
  }
  
  public SSH getSSH() {
    return ssh;
  }
  public SSHExecResult exec(String command) throws WrappedSSHException {
    StringBuilder out=new StringBuilder();
    ByteArrayOutputStream err=new ByteArrayOutputStream(512);
    int result=exec(command, out, err);
    if (err.size()>0 || result!=0)
      try {
        out.append(err.toString("utf-8"));
        result=1;
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    return new SSHExecResult(result==0, out.toString());
  }
  public int exec(String command, Appendable out, Appendable err) throws WrappedSSHException {
    ByteArrayOutputStream baos=new ByteArrayOutputStream(512);
    int result=exec(command, out, baos);
    try {
      err.append(baos.toString("utf-8"));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    return result;
  }
  
  /** 
   * @param sshErr DO NOT PASS SYSTEM.ERR/OUT TO THIS, IT WILL GET CHEWED TO PIECES.
   * @return The output (typically 0,1,2) of the unix command, or -1 if we could not get a connection.
   */
  public int exec(String command, Appendable out, OutputStream sshErr) throws WrappedSSHException {
    try {
      Session session=ssh.getSession();
      if (session==null)
        return -1;
      ChannelExec channel=(ChannelExec)ssh.getSession().openChannel("exec");
      channel.setCommand(command);      
      channel.setErrStream(sshErr);
      channel.setOutputStream(null);
      InputStream in=channel.getInputStream();
      channel.connect();
      byte[] tmp=new byte[1024];
      try {
        //This loop will spin rather heavily. 
        while (!channel.isClosed()){
          while (in.available()>0){
            int i=in.read(tmp, 0, 1024);
            if (i<0) break;
            out.append(new String(tmp, 0, i));
          }
          //Doing this will cause output to be lost. I do not know why.
          //try {Thread.sleep(10);} catch (Exception e) {e.printStackTrace();}
        }
        int result=channel.getExitStatus();
        sshErr.flush();
        return result;
      } finally {
        channel.disconnect();  
        //Doing this will cause output to be lost which doesn't make sense. Let's blame the compiler:
        //in.close(); 
      }
    } catch (Exception e) {
      throw new WrappedSSHException("Failed to execute: "+command, e);
    }
  }

  public static void main(String[] args) throws Exception {
    /*
    SSH ssh=SSH.cmdLine(args);
    SSHExec exec=new SSHExec(ssh);
    StringBuilder app=new StringBuilder();
    for (int i=0; i<20; i++) {
      app.setLength(0);
      {
        System.out.println("\n\n");
        int fail=exec.exec("ls --file-type -a -1", app, app);
        System.out.println(fail==0 ?"SUCCESS: " :"FAILED: ");
        System.out.println(app);
      }
      {
        System.out.println("\n\n");
        int fail=exec.exec("ls /; echo '!!!mcshite!!!'", app, app);
        System.out.println(fail==0 ?"SUCCESS: " :"FAILED: ");
        System.out.println(app);
      }
    }
    System.err.flush();
    System.out.flush();
    ssh.close();
    */
  }
  
}
