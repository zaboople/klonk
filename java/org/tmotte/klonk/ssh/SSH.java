package org.tmotte.klonk.ssh;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.ByteArrayOutputStream;
import org.tmotte.klonk.config.msg.Setter;

//FIXME reconnect verify health
public class SSH {

  private JSch jsch=new JSch();
  private Session session;
  private boolean debug;
  private String user, pass, host, knownHosts, privateKeys;
  private boolean connected=false;
  private boolean lastConnectAuthFail=false;
  private SFTP sftp;
  private SSHExec exec;
  
  //DI UI components:
  private IUserPass iUserPass;
  private Setter<String> alertHandler;
   
  ////////////////////
  // INSTANTIATION: //
  ////////////////////
   
  public SSH(String user, String host, Setter<String> alertHandler) {
    this.user=user;
    this.host=host;
    this.alertHandler=alertHandler;
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
  public SSHExecResult exec(String command) throws WrappedSSHException {
    return getExec().exec(command);
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
  public boolean verifyConnection() {
    getSession();
    return isConnected();
  }
  public void close() throws Exception {
    if (sftp!=null)
      sftp.close();
    if (session!=null) 
      try {session.disconnect();} catch (Exception e2) {}
    session=null;
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

  /** For locals only; SSHExec & SFTP */
  protected Session getSession() {
    myLog("getSession");
    if (!isConnected())
      connect();
    return isConnected() 
      ?session
      :null;
  }
  
  //////////////////////////////
  // PRIVATE CONNECT METHODS: //
  ////////////////////////////// 
  
  private boolean connect() {
    try {
      return tryConnect1();
    } catch (java.io.IOException e) {
      //Includes java.net.NoRouteToHostException and some others:
      alertHandler.set(e.getMessage());
      return false;
    } catch (com.jcraft.jsch.JSchException e) {
      alertHandler.set(e.getMessage());
      return false;
    } catch (Exception e) {
      Throwable internal=e.getCause();
      if (internal!=null){
        if ((internal instanceof java.net.UnknownHostException)) {
          alertHandler.set(internal.toString());
          return false;
        }
      }
      e.printStackTrace();//FIXME print to standard handler
      String s=e.getMessage();
      if (s==null)
        s=e.toString();
      alertHandler.set(s);
      return false;
    }
  }
  private boolean tryConnect1() throws Exception {
  
    //Set known hosts:
    if (knownHosts!=null) {
      jsch.setKnownHosts(knownHosts);
      if (debug)
        printHostKeys(jsch);
    }

    //OK close:
    close();
    session=makeNewSession();
           
    //Try the password we have:
    if (pass!=null){
      session.setPassword(pass);
      if (tryConnect2())
        return true;
    }

    //Try private keys:
    if (privateKeys!=null) {
      session.setConfig("PreferredAuthentications", "publickey");
      jsch.addIdentity(privateKeys);
      if (tryConnect2())
        return true;
    }

    //Try calling a user interface to get it. If the user gives
    //us a bad password
    while (iUserPass!=null && iUserPass.get(user, host, lastConnectAuthFail)){ 
      String newUser=iUserPass.getUser();
      if (!newUser.equals(user)) { 
        user=newUser;
        close();
        session=makeNewSession();
      }
      if (session==null)
        session=makeNewSession();
      pass=iUserPass.getPass();
      session.setPassword(pass);
      if (tryConnect2())
        return true;
    }
    close();
    return false;
  }
  private boolean tryConnect2() throws Exception {
    connected=false;
    lastConnectAuthFail=false;
    session.setTimeout(10000);
    try {
      myLog("Connecting...");
      session.connect(); 
      return connected=true;
    } catch (Exception e) {          
      myLog("Didn't.");
      close();
      if (e.getMessage().equals("Auth fail")) {
        return connected=!(lastConnectAuthFail=true);
      }
      else
        throw e;
    }    
  }
  private Session makeNewSession() throws Exception {
    Session temp=jsch.getSession(user, host, 22);
    if (knownHosts==null)
      temp.setConfig("StrictHostKeyChecking", "no");    
    return temp;
  }
  private static void myLog(String s) {
    if (true)
      System.out.println("SSH.myLog(): "+s);
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

  
}