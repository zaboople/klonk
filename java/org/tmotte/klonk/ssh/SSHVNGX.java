package org.tmotte.klonk.ssh;
import org.vngx.jsch.*; //FIXME
import org.vngx.jsch.util.*; //FIXME
import org.vngx.jsch.config.SessionConfig;
import java.util.List;
import java.util.HashMap;
import java.util.Map;

public class SSHVNGX {

  //ls --file-type -a -1
  private Session session;
  
  private static void printHostKeys(JSch jsch) throws Exception {
    HostKeyRepository hkr=jsch.getHostKeyRepository();
    List<HostKey> hks=hkr.getHostKeys();
    if (hks!=null){
      System.out.println("Host keys in "+hkr.getKnownHostsRepositoryID());
      for(HostKey hk: hks)
        printHostKey(hk, jsch);
    }  
  }
  private static void printHostKey(HostKey hk, JSch jsch) throws Exception {
    if (hk!=null)
      System.out.println(
        "Host: "+hk.getHost()+" "+
        "Type: "+hk.getType()+" "+
        "Fingerprint: "+hk.getFingerPrint()
      );
  }
  public SSHVNGX(String user, String host, String pass, String knownHosts) throws Exception {
  
    //They think it's a good idea for it to be a singleton, fine:
    JSch jsch=JSch.getInstance();

    //Set known hosts:   
    //jsch.setKnownHosts(knownHosts);
    printHostKeys(jsch);

    //This is unnecessary, but it shows how to set up configuration:
    Map<String,String> configProps=new java.util.HashMap<>();
    configProps.put("StrictHostKeyChecking", "no");
    SessionConfig config=new SessionConfig(configProps);
    
    
    session=jsch.createSession(user, host, 22, config);
    
    session.connect(30000, pass.getBytes()); 
    
    printHostKey(session.getHostKey(), jsch);
    
    Channel channel=session.openChannel("shell");
    channel.setInputStream(System.in);
    channel.setOutputStream(System.out);
    channel.connect(3*1000);
  }
  
  public static void main(String[] args) throws Exception {
    SSHVNGX ssh=new SSHVNGX(args[0], args[1], args[2], args[3]);
  }
}