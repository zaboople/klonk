package org.tmotte.klonk.ssh;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.HashSet;
import java.util.regex.Pattern;
import org.tmotte.common.text.StringChunker;

/** This does not establish a connection. It only collects potential connections. Not thread-safe. */
public class SSHConnections {

  private final Map<String, Map<String, SSH>> conns=new HashMap<>();
  private final ConnectionParse parser=new ConnectionParse();
  private Set<String> users=new HashSet<>();
  private IUserPass iUserPass;
  private String knownHosts, privateKeys;

  public SSHConnections withKnown(String hosts) {
    this.knownHosts=hosts;
    return this;
  }
  public SSHConnections withPrivateKeys(String privateKeys) {
    this.privateKeys=privateKeys;
    return this;
  }
  public SSHConnections withLogin(IUserPass iUserPass) {
    this.iUserPass=iUserPass;
    return this;
  }

  public boolean is(String uri) {
    return uri!=null && (
      uri.startsWith("ssh:") || 
      uri.startsWith("SSH:")
    );
  }
  public SSHFile getFile(String uri) throws ConnectionParseException {
    return parser.parse(this, uri);
  }
  public String inferUserForHost(String host) {
    
    //If there is only one user so far, assume it's them:
    if (users.size()==1)      
      return users.iterator().next();      
      
    //If there is an existing connection for that host, 
    //use the user for it; else tell them to just log in.
    //Note that when they login, they may use a new password.
    Map<String,SSH> perHost=getForHost(host);
    if (perHost.size()==1)
      return perHost.values().iterator().next().getUser();
    else 
    if (iUserPass!=null && iUserPass.get(null, host, null)){
      String user=iUserPass.getUser();
      SSH ssh=getOrCreate(user, host);
      ssh.withPassword(iUserPass.getPass());//FIXME now we have a problem of not needing a password but asking for it
      return user;
    }
    else
      return null;
  }
  public SSH getOrCreate(String user, String host) {
    Map<String,SSH> perHost=getForHost(host);
    SSH ssh=perHost.get(user);
    if (ssh==null) {
      ssh=new SSH(user, host)
        .withKnown(knownHosts)
        .withPrivateKeys(privateKeys)
        .withIUserPass(iUserPass);
      perHost.put(user, ssh);
    }
    return ssh;
  }
  public SSH get(String user, String host) { //FIXME I don't think anything is going to use this
    Map<String,SSH> perHost=getForHost(user);
    return perHost.get(host);
  }
  public void put(String user, String host, SSH ssh) {
    users.add(user);
    Map<String,SSH> perHost=getForHost(host);
    perHost.put(user, ssh);
  }
  public String toString() {
    return conns.toString();
  }
  protected Set<String> getUsers(){
    return users;
  }
  
  private Map<String,SSH> getForHost(String host) {
    Map<String,SSH> perHost=conns.get(host);
    if (perHost==null){
      perHost=new HashMap<String,SSH>();
      conns.put(host, perHost);
    }
    return perHost;
  }
  
  //////////////
  // TESTING: //
  //////////////

  public static void main(String[] args) throws Exception {
    SSHConnections conns=new SSHConnections();
    for (String s: args) {
      SSHFile f=conns.getFile(s);
      System.out.println(
        "FILE: "+f+" "+(f==null ?"" :f.getSSH())
      );
    }
    System.out.println("\n\n"+conns);
  }
}
