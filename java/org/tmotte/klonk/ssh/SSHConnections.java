package org.tmotte.klonk.ssh;
import java.util.HashMap;
import java.util.Map;
import org.tmotte.common.text.StringChunker;
import java.util.regex.Pattern;
import java.util.Set;

//ssh:troym@xshell.xshellz.com:/blahblahblahb/bhb/abhba/habbh
/** This does not establish a connection. It only collects potential connections. Not thread-safe. */
public class SSHConnections {

  private final Map<String, Map<String, SSH>> conns=new HashMap<>();
  private final ConnectionParse parser=new ConnectionParse();

 
  public boolean is(String uri) {
    return uri!=null && (
      uri.startsWith("ssh:") || 
      uri.startsWith("SSH:")
    );
  }
  public SSHFile getFile(String uri) throws ConnectionParseException {
    return parser.parse(this, uri);
  }
  public SSH getOrCreate(String user, String host) {
    Map<String,SSH> perUser=getForUser(user);
    SSH ssh=perUser.get(host);
    if (ssh==null) {
      ssh=new SSH(user, host);
      put(user, host, ssh);
    }
    return ssh;
  }
  public SSH get(String user, String host) {
    Map<String,SSH> perUser=getForUser(user);
    return perUser.get(host);
  }
  public void put(String user, String host, SSH ssh) {
    Map<String,SSH> perUser=getForUser(user);
    perUser.put(host, ssh);
  }
  public String toString() {
    return conns.toString();
  }
  protected Set<String> getUsers(){
    return conns.keySet();
  }
  
  private Map<String,SSH> getForUser(String user) {
    Map<String,SSH> perUser=conns.get(user);
    if (perUser==null){
      perUser=new HashMap<String,SSH>();
      conns.put(user, perUser);
    }
    return perUser;
  }
  
  //////////////
  // TESTING: //
  //////////////

  public static void main(String[] args) throws Exception {
    SSHConnections conns=new SSHConnections();
    for (String s: args) {
      SSHFile f=conns.getFile(s);
      System.out.println("FILE: "+f+" "+f.getSSH());
    }
    System.out.println("\n\n"+conns);
  }
}
