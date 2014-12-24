package org.tmotte.klonk.ssh;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.Channel;
import com.jcraft.jsch.ChannelSftp;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;

class SFTP {
  
  private SSH ssh;
  private ChannelSftp channel;
  SFTP(SSH ssh) {
    this.ssh=ssh;
  }
  InputStream getInputStream(String file) throws Exception {
    return getChannel().get(file);
  }
  OutputStream getOutputStream(String file) throws Exception {    
    return getChannel().put(file);
  }
  void close() throws Exception {
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
 
}
