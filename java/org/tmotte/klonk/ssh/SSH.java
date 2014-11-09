package org.tmotte.klonk.ssh;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;

//FIXME reconnect verify health
public class SSH {

  private JSch jsch=new JSch();
  private Session session;
  private boolean debug;
  private String user, pass, host, knownHosts, privateKeys;
  private boolean connected=false;
  private SFTP sftp;
  private SSHExec exec;
  private IUserPass iUserPass;
   
  ////////////////////
  // INSTANTIATION: //
  ////////////////////
   
  public SSH(String user, String host) {
    this.user=user;
    this.host=host;
  }
  public SSH(String host) {
    this.host=host;
  }
  public SSH withPassword(String pass) {
    this.pass=pass;
    return this;
  }
  public SSH withUser(String user) {
    this.user=user;
    return this;
  }
  public SSH withDebug(boolean debug) {
    this.debug=true;
    return this;
  }
  public SSH withKnown(String hosts) {
    this.knownHosts=hosts;
    return this;
  }
  public SSH withPrivateKeys(String privateKeys) {
    this.privateKeys=privateKeys;
    return this;
  }
  public SSH withIUserPass(IUserPass iUserPass) {
    this.iUserPass=iUserPass;
    return this;
  }
  
  
  //////////
  // USE: //
  //////////

  public String getHost() {
    return host;
  }
  public String getUser() {
    return user;
  }
  public SSHExec getExec() {
    if (this.exec==null)
      this.exec=new SSHExec(this);  
    return exec;
  }
  public SFTP getSFTP() {
    if (this.sftp==null)
      this.sftp=new SFTP(this);  
    return sftp;
  }
  public boolean isConnected(){
    return connected;
  }
  public boolean canConnect() {
    return pass!=null || privateKeys!=null;
  }
  protected Session getSession() throws Exception {
    checkConnect();
    return session;
  }
  private void checkConnect() throws Exception {
    if (!connected)
      connect();
  }
  private SSH connect() throws Exception {
    if (knownHosts!=null) {
      jsch.setKnownHosts(knownHosts);
      if (debug)
        printHostKeys(jsch);
    }

    session=jsch.getSession(user, host, 22);  
    
    if (knownHosts==null)
      session.setConfig("StrictHostKeyChecking", "no");
    
    if (privateKeys!=null) {
      session.setConfig("PreferredAuthentications", "publickey");
      jsch.addIdentity(privateKeys);
    }
    
    System.out.println("FIXME "+iUserPass+" "+pass);
    if (pass!=null){
      session.setPassword(pass);
      pass=null; //Security feature - yes your password is one-time. I'm not letting somebody come get it.
    }
    else
    if (iUserPass!=null && iUserPass.get(user, host)){ //FIXME why null the user just reuse
      withUser(iUserPass.getUser());
      withPassword(iUserPass.getPass());
      session.setPassword(pass);
    }      
    session.setTimeout(20000);
    session.connect(); 
    connected=true;
    return this;
  }
  public void close() throws Exception {
    if (sftp!=null)
      sftp.close();
    session.disconnect();
    connected=false;
  }
  public boolean matches(String user, String host) {
    return 
      (user==null || user.equals(this.user)) && 
      host.equals(this.host);
  }
  public String toString() {
    return "SSH: user: "+user+" host: "+host+" knownHosts: "+knownHosts+" privateKeys: "+privateKeys;
  }

  
  ////////////////////////
  // TESTING/DEBUGGING: //
  ////////////////////////

  private static void printHostKeys(JSch jsch) {
    HostKeyRepository hkr=jsch.getHostKeyRepository();
    HostKey[] hks=hkr.getHostKey();
    if( hks!=null){
      System.out.println("Host keys in "+hkr.getKnownHostsRepositoryID());
      for(HostKey hk: hks)
        System.out.println("Known host: "+debugHostKey(hk, jsch));
    }  
  }
  private static String debugHostKey(HostKey hk, JSch jsch) {
    return
      "Host: "+hk.getHost()+" "+
      "Type: "+hk.getType()+" "+
      "Fingerprint: "+hk.getFingerPrint(jsch);
  }

  
  private static void usage() {
    System.err.println("Usage: -u user -h host -k knownhostsfile -p pass -r privatekeyfile");
    System.exit(1);
  }
  public static SSH cmdLine(String[] args) throws Exception {
    String user=null, host=null, knownHosts=null, pass=null, privateKeys=null;
    for (int i=0; i<args.length; i++){
      String arg=args[i];
      if (arg.startsWith("-help"))
        usage();
      if (arg.startsWith("-u"))
        user=args[++i];
      else
      if (arg.startsWith("-h"))
        host=args[++i];
      else
      if (arg.startsWith("-k"))
        knownHosts=args[++i];
      else
      if (arg.startsWith("-p"))
        pass=args[++i];
      else
      if (arg.startsWith("-r"))
        privateKeys=args[++i];
      else {
        System.err.println("Unexpected: "+arg);
        System.exit(1);
        return null;
      }
    }
    if (user==null || host==null){
      usage();
      return null;
    }
    else return new SSH(user, host)
      .withKnown(knownHosts)
      .withPassword(pass)
      .withPrivateKeys(privateKeys);
  }
  
}