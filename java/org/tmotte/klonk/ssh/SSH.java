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
  private String lastConnectError;
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
    if (session!=null) {
      if (session.isConnected())
        return true;
      session=null;
    }
    return false;
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


  protected Session getSession() throws Exception {
    if (!isConnected())
      connect();
    return session;
  }
  private boolean connect() throws Exception {
    if (knownHosts!=null) {
      jsch.setKnownHosts(knownHosts);
      if (debug)
        printHostKeys(jsch);
    }

    if (session!=null) 
      try {
        session.disconnect();
      } catch (Exception e) {
        //FIXME log it
      }
    session=jsch.getSession(user, host, 22);  
    
    if (knownHosts==null)
      session.setConfig("StrictHostKeyChecking", "no");
        
    //Try the password we have:
    if (pass!=null){
      session.setPassword(pass);
      if (tryConnect())
        return true;
    }

    //Try private keys:
    if (privateKeys!=null) {
      session.setConfig("PreferredAuthentications", "publickey");
      jsch.addIdentity(privateKeys);
      if (tryConnect())
        return true;
    }

    //Try calling a user interface to get it. If the user gives
    //us a bad password
    while (iUserPass!=null && iUserPass.get(user, host, lastConnectError)){ 
      String newUser=iUserPass.getUser();
      if (!newUser.equalsIgnoreCase(user)) { //FIXME ssh is not case sensitive right?
        user=newUser;
        session.disconnect();
        session=jsch.getSession(user, host, 22);
      }
      pass=iUserPass.getPass();
      session.setPassword(pass);
      if (tryConnect())
        return true;
    }
    if (session!=null){
      session.disconnect();
      session=null;
    }
    return false;
  }
  private boolean tryConnect() throws Exception {
    lastConnectError=null;
    session.setTimeout(10000);
    try {
      session.connect(); 
      return connected=true;
    } catch (Exception e) {    
      lastConnectError=e.getMessage().equals("Auth fail")
        ?"Bad password"
        :e.getMessage();
      return connected=false;
    }    
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