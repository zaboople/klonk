package org.tmotte.klonk.ssh;
import com.jcraft.jsch.*;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.PrintStream;
import org.tmotte.klonk.config.msg.UserNotify;

public class SSHExec {
  
  private SSH ssh;
  private MeatCounter mc;
  protected UserNotify userNotify;  
  
  SSHExec(SSH ssh, MeatCounter mc, UserNotify userNotify) {
    this.ssh=ssh;
    this.mc=mc;
    this.userNotify=userNotify;
  }
  
  SSHExecResult exec(String command, boolean alertFail) {
    StringBuilder out=new StringBuilder();
    ByteArrayOutputStream err=new ByteArrayOutputStream(512);
    int result=exec(command, out, err);
    if (err.size()>0 || result!=0)
      try {
        out.append(err.toString("utf-8"));
        result=1;
        String bad="SSH Failure: "+out.toString();
        if (alertFail)
          userNotify.alert(bad);
        mylog(bad);
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    return new SSHExecResult(result==0, out.toString());
  }
  private int exec(String command, Appendable out, Appendable err) throws Exception {
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
   * @param sshErr DO NOT PASS SYSTEM.ERR/OUT TO THIS, IT WILL GET CHEWED TO PIECES (i.e. it will be closed by 
   *        the jsch library).
   * @return The output (typically 0,1,2) of the unix command, or -1 if we could not get a connection.
   */
  private int exec(String command, Appendable out, OutputStream sshErr) {
    mylog(Thread.currentThread().hashCode()+" "+command);
    ChannelExec channel=getChannel(command);
    if (channel==null)
      return -1;
    try {
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
        //mylog(ssh.hashCode()+" "+command+" complete "+result);
        return result;
      } catch (Exception e) {
        closeOnFail();
        throw new RuntimeException(e);
      } finally {
        releaseChannel(channel);
        //Doing this will cause output to be lost which doesn't make sense. Let's blame the compiler:
        //in.close(); 
      }
    } catch (Exception e) {
      throw new RuntimeException("Failed to execute: "+command, e);
    } 
  }


  private ChannelExec channel;
  private ChannelExec getChannel(String cmd)  {
    mc.lock(cmd);
    try {
      //if (channel==null) channel=makeChannel();
      return makeChannel();
    } catch (Exception e) {
      mc.unlock();
      throw new RuntimeException(e);
    }
  }
  private void releaseChannel(ChannelExec channel)  {
    try {
      channel.disconnect();
      //this.channel=null;
    } catch (Exception e) {
      throw new RuntimeException(e);
    } finally {
      mc.unlock();
    }
  }  
  private void closeOnFail() {
    if (channel!=null)
      channel.disconnect();
    channel=null;
  }
  private ChannelExec makeChannel() throws Exception{
    Session session=ssh.getSession();
    if (session==null)
      return null;
    return (ChannelExec)session.openChannel("exec");
  }


  private void mylog(String s) {
    userNotify.log("SSHExec: "+s);
  }

  
}
