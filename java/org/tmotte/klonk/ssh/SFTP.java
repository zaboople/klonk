package org.tmotte.klonk.ssh;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import com.jcraft.jsch.SftpATTRS;
import java.util.List;
import java.util.ArrayList;


/**
 * SFTP in jsch is not thread-safe. Worse yet, you can try to use synchronized blocks to ameliorate, but it will go right around them.
 * So I have a locking system in place to work around this, and it's pretty good. The only catch is that getInputStream() & getOutputStream()
 * get called from far away and it's hard to lock them down.
 */
class SFTP {

  private SSH ssh;
  private ChannelSftp channel;
  private MeatCounter mc;

  SFTP(SSH ssh, MeatCounter mc) {
    this.ssh=ssh;
    this.mc=mc;
  }

  ////////////////////////////////
  // PACKAGE-PRIVATE FUNCTIONS: //
  ////////////////////////////////


  InputStream getInputStream(String file) throws Exception {
    try {
      return getChannel(file).get(file);
    } finally {
      unlock();
    }
  }
  OutputStream getOutputStream(String file) throws Exception {
    try {
      return getChannel(file).put(file);
    } finally {
      unlock();
    }
  }
  SSHFileAttr getAttributes(String file) {
    ssh.userNotify.log("SFTP.getAttributes "+file);
    try {
      ChannelSftp ch=getChannel(file);
      if (ch==null)
        return null;
      SSHFileAttr sfa=new SSHFileAttr(ch.stat(file));
      ssh.userNotify.log(sfa.toString());
      return sfa;
    } catch (Exception e) {
      try {close();} catch (Exception e2) {ssh.userNotify.log(e2);}
      String msg=e.getMessage();
      if (msg!=null && msg.equals("No such file")){
        ssh.userNotify.log(msg+file);
        return null;
        //This never worked out, just made a mess with ->new folder ->rename in file dialog. Unfortunately
        //it also means that we call this function again & again because we have null and want a value.
        //return new SSHFileAttr();
      }
      else
      if (canIgnore(e, file, "getAttributes"))
        return null;
      else
        throw new RuntimeException("Failed to get stat on: "+file, e);
    } finally {
      //ssh.userNotify.log("SFTP.getAttributes "+Thread.currentThread().hashCode()+" "+file+" COMPLETE ");
      unlock();
    }
  }

  private static String[] noFiles={};
  String[] listFiles(String file) {
    ssh.userNotify.log("SFTP.listFiles "+Thread.currentThread().hashCode()+" "+file);
    ChannelSftp ch=getChannel(file);
    try {
      List<?> vv=ch.ls(file);
      String[] values=new String[vv.size()];
      int count=0;
      if (vv!=null)
        for (int ii=0; ii<vv.size(); ii++){
          Object obj=vv.get(ii);
          if (obj instanceof com.jcraft.jsch.ChannelSftp.LsEntry){
            String s=((com.jcraft.jsch.ChannelSftp.LsEntry)obj).getFilename();
            if (!s.equals(".") && !s.equals("..")){
              count++;
              values[ii]=s;
            }
          }
        }
      String[] realVals=new String[count];
      for (int i=values.length-1; i>-1; i--)
        if (values[i]!=null){
          realVals[count-1]=values[i];
          count--;
        }
      return realVals;
    } catch (Exception e) {
      try {close();} catch (Exception e2) {ssh.userNotify.log(e2);}
      if (!canIgnore(e, file, "listFiles"))
        ssh.userNotify.alert(e, "Failed to list files for: "+file);
      return noFiles;
    } finally {
      //ssh.userNotify.log("SFTP.listFiles "+Thread.currentThread().hashCode()+" "+file+" COMPLETE ");
      unlock();
    }
  }

  void close() throws Exception {
    if (channel!=null) {
      if (channel.isConnected())
        try {channel.disconnect();} catch (Exception e) {}
      channel=null;
    }
  }

  ////////////////////////
  // PRIVATE FUNCTIONS: //
  ////////////////////////

  /**
   * As we overload the connection, random stuff blows up but we can ignore it because
   * this is just the file chooser whacking us over the head and it will be just fine.
   */
  private boolean canIgnore(Throwable e, String file, String function) {
    e=getCause(e);
    String msg=e.getMessage();
    if (
        (e instanceof java.lang.InterruptedException) ||
        (e instanceof java.io.InterruptedIOException) ||
        (e instanceof java.lang.IndexOutOfBoundsException) ||
        (msg!=null && (
          msg.contains("Permission denied") ||
          msg.contains("No such file")      ||
          msg.contains("inputstream is closed")
        ))
      ){
      ssh.userNotify.log("SFTP Hidden fail: "+function+" "+e+" ..."+file);
      ssh.userNotify.log(e);
      return true;
    }
    else if ((e instanceof java.io.IOException) && e.getMessage().equals("Pipe closed")){
      ssh.userNotify.log("SFTP Apparently closed: "+e+" ..."+file);
      try {ssh.close();} catch (Exception e2) {ssh.userNotify.log(e2);}
      return false;
    }
    else
      return false;
  }
  private Throwable getCause(Throwable e) {
    Throwable e1=e.getCause();
    return e1==null ?e :getCause(e1);
  }


  private ChannelSftp getChannel(String file) {
    lock(file);
    try {
      if (channel==null || !channel.isConnected())
        channel=makeChannel();
    } catch (Exception e) {
      unlock();
      throw new RuntimeException(e);
    }
    return channel;
  }

  private ChannelSftp makeChannel() throws Exception {
    Session session=ssh.getSession();
    if (session==null)
      return null;
    session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");//Fails if we don't do this - not that I understand it
    session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
    ChannelSftp c=(ChannelSftp)session.openChannel("sftp");
    //setBulkRequests() seems to help... not sure. Might be making things worse:
    c.setBulkRequests(100);
    c.connect();
    return c;
  }

  private void lock(String file) {
    mc.lock(file);
  }
  private void unlock() {
    mc.unlock();
  }
}
