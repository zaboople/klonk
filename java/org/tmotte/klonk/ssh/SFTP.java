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
 * SFTP in jsch is so flaky that you cannot even use synchronized's to keep it from destroying itself.
 * It will release your locks. 
 */
class SFTP {
  
  private SSH ssh;
  private ChannelSftp channel;
  
  SFTP(SSH ssh) {
    this.ssh=ssh;
  }
  
  ////////////////////////////////
  // PACKAGE-PRIVATE FUNCTIONS: //
  ////////////////////////////////
  
  
  InputStream getInputStream(String file) throws Exception {
    try {
      return getChannel(file).get(file);
    } finally {
      ssh.unlock();
    }
  }
  OutputStream getOutputStream(String file) throws Exception {    
    try {
      return getChannel(file).put(file);
    } finally {
      ssh.unlock();
    }
  }
  boolean isDirectory(String file) {
    ssh.logger.set("SFTP.isDirectory "+Thread.currentThread().hashCode()+" "+file);
    try {      
      ChannelSftp ch=getChannel(file);
      Boolean res=ch.stat(file).isDir();
      return res;
    } catch (Exception e) {
      try {close();} catch (Exception e2) {}//FIXME log that
      if (canIgnore(e, file))
        return false;
      else
        throw new RuntimeException("Failed to get stat on: "+file, e);
    } finally {
      ssh.logger.set("SFTP.isDirectory "+Thread.currentThread().hashCode()+" "+file+" COMPLETE ");
      ssh.unlock();
    }
  }
  
  private static String[] noFiles={};
  String[] listFiles(String file) {
    //ssh.logger.set("SFTP.listFiles "+Thread.currentThread().hashCode()+" "+file);
    ChannelSftp ch=getChannel(file);
    try {
      List vv=ch.ls(file);
      String[] values=new String[vv.size()];
      int count=0;
      if (vv!=null)
        for(int ii=0; ii<vv.size(); ii++){  
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
      try {close();} catch (Exception e2) {}//FIXME log that      
      if (canIgnore(e, file))
        return noFiles;
      else
        throw new RuntimeException("Failed to get stat on: "+file, e);
    } finally {
      //ssh.logger.set("SFTP.listFiles "+Thread.currentThread().hashCode()+" "+file+" COMPLETE ");
      ssh.unlock();
    }
  }

  void close() throws Exception {
    if (channel!=null && channel.isConnected()){
      channel.disconnect();
      channel=null;
    }
  }
  
  ////////////////////////
  // PRIVATE FUNCTIONS: //
  ////////////////////////

  /**
   * As we overload the connection, random stuff blows up but we can ignore it. 
   */
  private boolean canIgnore(Throwable e, String file) {
    //FIXME what about "no such file"?
    e=getCause(e);
    String msg=e.getMessage();
    if ((e instanceof java.lang.InterruptedException) || 
        (e instanceof java.io.InterruptedIOException) ||
         (msg!=null && msg.contains("Permission denied"))
        ){
      ssh.logger.set("SFTP: "+e+" ..."+file);
      return true;
    }
    else
      return false;
  }
  private Throwable getCause(Throwable e) {
    Throwable e1=e.getCause();
    return e1==null ?e :getCause(e1);
  }

  
  private ChannelSftp getChannel(String file) {
    ssh.lock(file);
    try {
      if (channel==null) channel=makeChannel();
    } catch (Exception e) {
      ssh.unlock();
      throw new RuntimeException(e);
    }
    return channel;
  }

  private ChannelSftp makeChannel() throws Exception {
    Session session=ssh.getSession();
    session.setConfig("compression.s2c", "zlib@openssh.com,zlib,none");//FIXME doesnt work otherwise
    session.setConfig("compression.c2s", "zlib@openssh.com,zlib,none");
    ChannelSftp c=(ChannelSftp)session.openChannel("sftp");
    //setBulkRequests() seems to help... not sure. Might be making things worse:
    c.setBulkRequests(1);
    c.connect();      
    return c;
  }    
 
}
