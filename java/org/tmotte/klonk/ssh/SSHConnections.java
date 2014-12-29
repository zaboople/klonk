package org.tmotte.klonk.ssh;
import java.io.File;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.tmotte.common.text.StringChunker;
import org.tmotte.klonk.config.msg.Setter;

/** This does not establish a connection. It only collects potential connections. Not thread-safe. */
public class SSHConnections {

  private final Map<String, Map<String, SSH>> conns=new HashMap<>();
  private final ConnectionParse parser=new ConnectionParse();
  private Set<String> users=new HashSet<>();
  
  private String knownHosts, privateKeys;
  private IUserPass iUserPass;
  private Setter<String> logger, errorHandler;
  private IFileGet iFileGet=new IFileGet(){
    public File get(String uri) {
      if (is(uri)) return getSSHFile(uri);
      else         return new File(uri);
    }
  };

  /////////////
  // CREATE: //
  /////////////

  public SSHConnections (Setter<String> logger, Setter<String> errorHandler) {
    this.logger=logger;
    this.errorHandler=errorHandler;
  }  
  public SSHConnections withKnown(String hostsFile) {
    this.knownHosts=hostsFile;
    return this;
  }
  public SSHConnections withPrivateKeys(String privateKeysFile) {
    this.privateKeys=privateKeysFile;
    return this;
  }
  public SSHConnections withLogin(IUserPass iUserPass) {
    this.iUserPass=iUserPass;
    return this;
  }
  
  /////////////
  // PUBLIC: //
  /////////////

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
      ssh=new SSH(user, host, logger, errorHandler)
        .withKnown(knownHosts)
        .withPrivateKeys(privateKeys)
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

  ////////////////
  // PROTECTED: //
  ////////////////

  /** This is here only for the connection parser: */
  protected String inferUserForHost(String host) {

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
  
  private Map<String,SSH> getForHost(String host) {
    Map<String,SSH> perHost=conns.get(host);
    if (perHost==null){
      perHost=new HashMap<String,SSH>();
      conns.put(host, perHost);
    }
    return perHost;
  }
  private SSH get(String user, String host) { //FIXME I don't think anything is going to use this
    Map<String,SSH> perHost=getForHost(user);
    return perHost.get(host);
  }
  
}
