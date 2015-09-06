package org.tmotte.klonk.ssh;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.LinkedList;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.common.text.StringChunker;
import org.tmotte.klonk.config.msg.UserNotify;

/** This does not establish a connection. It only collects potential connections. Not thread-safe. */
public class SSHConnections {
  
  private final Map<String, Map<String, SSH>> conns=new HashMap<>();
  private final ConnectionParse parser=new ConnectionParse();
  private Set<String> users=new HashSet<>();
  
  private String knownHosts, privateKeys, openSSHConfig;
  private String defaultFilePerms, defaultDirPerms;
  private IUserPass iUserPass;
  private UserNotify userNotify;
  
  private IFileGet iFileGet=new IFileGet(){
    public File get(String uri) {
      if (is(uri)) return getSSHFile(uri);
      else         return new File(uri);
    }
  };

  /////////////
  // CREATE: //
  /////////////

  public SSHConnections (UserNotify userNotify) {
    this.userNotify=userNotify;
  }  
  public SSHConnections withOptions(SSHOptions opts){
    this.knownHosts=opts.getKnownHostsFilename();
    this.privateKeys=opts.getPrivateKeysFilename();
    this.defaultFilePerms=opts.getDefaultFilePermissions();
    this.defaultDirPerms=opts.getDefaultDirPermissions();
    this.openSSHConfig=opts.getOpenSSHConfigFilename();
    for (Map<String,SSH> map: conns.values())
      for (SSH ssh: map.values()) 
        configure(ssh);
    return this;
  }
  public SSHConnections withLogin(IUserPass iUserPass) {
    this.iUserPass=iUserPass;
    return this;
  }
  
  /////////////
  // PUBLIC: //
  /////////////

  public void close(String host) {
    Map<String, SSH> perHost=conns.get(host);
    for (SSH ssh: perHost.values())
      ssh.close();
  }
  public void close() throws Exception {
    for (String host: conns.keySet()){
      Map<String,SSH> forHost=conns.get(host);
      for (String user: forHost.keySet())
        forHost.get(user).close();
    }
    try {
    } catch (Exception e) {
      e.printStackTrace();
    }
  }
  public boolean is(String uri) {
    return uri!=null && (
      uri.startsWith("ssh:") || 
      uri.startsWith("SSH:")
    );
  }
  public SSHFile getSSHFile(String uri) {
    return parser.parse(this, uri);
  }
  public SSH getOrCreate(String user, String host) {
    Map<String,SSH> perHost=getForHost(host);
    SSH ssh=perHost.get(user);
    if (ssh==null) {
      ssh=
        configure(new SSH(user, host, userNotify))
        .withIUserPass(iUserPass);
      if (!ssh.verifyConnection())
        return null;
      perHost.put(user, ssh);
      users.add(user);
    }
    return ssh;
  }
  public String toString() {
    return conns.toString();
  }
  public IFileGet getFileResolver() {
    return iFileGet;
  }
  public List<String> getConnectedHosts() {
    List<String> h=new LinkedList<>();
    for (String s: conns.keySet()){
      Map<String, SSH> zzz=conns.get(s);
      boolean is=false;
      for (String u: zzz.keySet())
        if (!is && zzz.get(u).isConnected())
          is=true;
      if (is) h.add(s);
    }
    return h;
  }

  //////////////////////
  // PACKAGE PRIVATE: //
  //////////////////////

  /** This is here only for the connection parser: */
  String inferUserForHost(String host) {

    //If there is only one user so far, assume it's them:
    if (users.size()==1)      
      return users.iterator().next();      
      
    //If there is an existing connection for that host, 
    //use the user for it; else tell them to just log in.
    //Note that when they login, they may use a new password.
    Map<String,SSH> perHost=getForHost(host);
    if (perHost.size()==1){
      SSH ssh=perHost.values().iterator().next();
      String s=ssh.getUser();
      if (s!=null) 
        return s;
    }

    if (iUserPass!=null && iUserPass.get(null, host, false, privateKeys==null)){ 
      String user=iUserPass.getUser();
      SSH ssh=getOrCreate(user, host);
      String pass=iUserPass.getPass();
      if (pass!=null && !pass.trim().equals(""))
        ssh.withPassword(pass);
      return user;
    }
    else
      return null;
  }
  
  ////////////////////////
  // PRIVATE FUNCTIONS: //
  ////////////////////////

  private SSH configure(SSH ssh) {
    ssh
      .withKnown(knownHosts)
      .withPrivateKeys(privateKeys)
      .withOpenSSHConfig(openSSHConfig)      
      .withDefaultPerms(defaultFilePerms, defaultDirPerms);
    return ssh;
  }
  private Map<String,SSH> getForHost(String host) {
    Map<String,SSH> perHost=conns.get(host);
    if (perHost==null){
      perHost=new HashMap<String,SSH>();
      conns.put(host, perHost);
    }
    return perHost;
  }
  
}
