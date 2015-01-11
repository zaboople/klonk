package org.tmotte.klonk.ssh;
import com.jcraft.jsch.HostKey;
import com.jcraft.jsch.HostKeyRepository;
import com.jcraft.jsch.JSch;
import com.jcraft.jsch.Session;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.LinkedList;
import java.util.List;
import org.tmotte.common.text.StringChunker;
import org.tmotte.klonk.config.msg.UserNotify;

//FIXME reconnect verify health
public class SSH {

  private JSch jsch=new JSch();
  private Session session;
  private String user, pass, host, knownHosts, privateKeys, defaultFilePerms, defaultDirPerms;
  private boolean connected=false;
  private boolean lastConnectAuthFail=false;
  private SFTP sftp;
  private SFTP sftpStreaming;
  private SSHExec exec;
  private String tildeFix;
  //private long lastConnectTest=0;
  
  //DI UI components:
  private IUserPass iUserPass;
  protected UserNotify userNotify;

  //For SSHExec & SFTP:
  private MeatCounter takeANumber=new MeatCounter(50);
  private WrapMap<SSHFileAttr> attrCache=new WrapMap<>(500, 10000);//FIXME do we need the file-level caching as well?
  
   
  ////////////////////
  // INSTANTIATION: //
  ////////////////////
   
  SSH(String user, String host, UserNotify userNotify) {
    this.user=user;
    this.host=host;
    this.userNotify=userNotify;
  }
  SSH(String host) {
    this.host=host;
  }
  SSH withPassword(String pass) {
    this.pass=pass;
    return this;
  }
  SSH withUser(String user) {
    this.user=user;
    return this;
  }
  SSH withKnown(String hosts) {
    this.knownHosts=hosts;
    return this;
  }
  SSH withPrivateKeys(String privateKeys) {
    this.privateKeys=privateKeys;
    return this;
  }
  SSH withIUserPass(IUserPass iUserPass) {
    this.iUserPass=iUserPass;
    return this;
  }
  SSH withDefaultPerms(String filePerms, String dirPerms) {
    this.defaultFilePerms=filePerms;
    this.defaultDirPerms=dirPerms;
    return this;
  }
  
  
  //////////
  // USE: //
  //////////

  String getHost() {
    return host;
  }
  String getUser() {
    return user;
  }
  public boolean verifyConnection() {
    getSession();
    return isConnected();
  }
  public void close() {
    try {
      if (sftpStreaming!=null)
        sftpStreaming.close();
      if (sftp!=null)
        sftp.close();
      if (session!=null) 
        session.disconnect();
    } catch (Exception e2) {}
    session=null;
    connected=false;
  }
  public String toString() {
    return "SSH: user: "+user+" host: "+host+" knownHosts: "+knownHosts+" privateKeys: "+privateKeys;
  }
  public String getTildeFix() {
    if (tildeFix==null) {
      SSHExecResult res=exec("cd ~; pwd");
      if (!res.success)
        throw new RuntimeException("Could not get home directory");
      tildeFix=res.output.trim();
    }
    return tildeFix;
  }

  ////////////////////////////////
  // PACKAGE-PRIVATE FUNCTIONS: //
  ////////////////////////////////

  
  SSHFileAttr getAttributes(String path) {
    SSHFileAttr sfa=attrCache.get(path);
    if (sfa!=null) 
      myLog("Cache hit "+path);
    else {
      sfa=getSFTP().getAttributes(path);
      attrCache.put(path, sfa);
    }
    return sfa;  
  }
  String[] list(String path) {
    return getSFTP().listFiles(path);
  }

  InputStream getInputStream(String file) {
    try {
      return getSFTPStreaming().getInputStream(file);
    } catch (Exception e) {
      close(); 
      throw new RuntimeException("Could not load: "+file, e);//FIXME handle file permission failure
    }
  }
  OutputStream getOutputStream(String file) {
    try {
      return getSFTPStreaming().getOutputStream(file);
    } catch (Exception e) {
      close();
      throw new RuntimeException("Could not write to: "+file+":", e);//FIXME handle file permission failure
    }
  }
  
  void makeFile(String path) {
    exec("touch "+path+"; chmod "+defaultFilePerms+" "+path, true);
  }
  boolean mkdir(String path) {
    return
      exec("mkdir -p "+path, true).success 
      &&
      exec("chmod "+defaultDirPerms+" "+path, true).success ;  
  }

  SSHExecResult exec(String command) {
    return exec(command, false);
  }
  SSHExecResult exec(String command, boolean alertFail) {
    return getExec().exec(command, alertFail);
  }

  /** For locals only; SSHExec & SFTP */
  Session getSession() {
    if (!isConnected())
      connect();
    return isConnected() 
      ?session
      :null;
  }

  boolean isConnected(){
    System.out.println("SSH.isConnected() "+session);
    if (session!=null && session.isConnected())
      return true;
    session=null;
    return false;
  }


  //////////////////////////////
  // PRIVATE CONNECT METHODS: //
  ////////////////////////////// 

  private SFTP getSFTP() {
    if (this.sftp==null)
      this.sftp=new SFTP(this, new MeatCounter(30));  
    return sftp;
  }
  //private SFTP[] sftps;
  //private int sftpCounter=-1;
  //private SFTP getSFTP() {
  //  if (this.sftps==null){
  //    sftps=new SFTP[7];
  //    for (int i=0; i<sftps.length; i++)
  //      sftps[i]=new SFTP(this, new MeatCounter(30));  
  //  }
  //  synchronized (sftps) {
  //    sftpCounter++;
  //    if (sftpCounter>6)
  //      sftpCounter=0;
  //    System.out.println("Counter: "+sftpCounter);
  //    return sftps[sftpCounter];
  //  }
  //}

  private SFTP getSFTPStreaming() {
    if (this.sftpStreaming==null)
      this.sftpStreaming=new SFTP(this, new MeatCounter(30));  
    return sftpStreaming;
  }

  private SSHExec getExec() {
    if (this.exec==null)
      this.exec=new SSHExec(this, new MeatCounter(30), userNotify);  
    return exec;
  }


  
  private boolean connect() {
    try {
      return tryConnect1();
    } catch (java.io.IOException e) {
      //Includes java.net.NoRouteToHostException and some others:
      userNotify.alert(e);
      return false;
    } catch (com.jcraft.jsch.JSchException e) {
      userNotify.alert(e);
      return false;
    } catch (Exception e) {
      Throwable internal=e.getCause();
      if (internal!=null && (internal instanceof java.net.UnknownHostException)) 
        userNotify.alert(internal);
      else
        userNotify.alert(e);
      return false;
    }
  }
  private boolean tryConnect1() throws Exception {
  
    //Set known hosts:
    if (knownHosts!=null) {
      jsch.setKnownHosts(knownHosts);
      if (false)
        printHostKeys(jsch);
    }

    //OK close:
    close();
    session=makeNewSession();
    if (session==null)
      return false;
           
    //Try the password we have:
    if (pass!=null){
      session.setPassword(pass);
      return tryConnect2();
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
    while (iUserPass!=null && iUserPass.get(user, host, lastConnectAuthFail && pass!=null, true)){ 
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
      //lastConnectTest=System.currentTimeMillis();
      return connected=true;
    } catch (Exception e) {          
      myLog("Didn't.");
      close();
      if (e.getMessage().equals("Auth fail")) 
        return connected=!(lastConnectAuthFail=true);
      else
        throw e;
    }    
  }
  private Session makeNewSession() throws Exception {
    myLog("Creating session for "+user+" "+host);
    Session temp=jsch.getSession(user, host, 22);
    if (knownHosts==null)
      temp.setConfig("StrictHostKeyChecking", "no");    
    return temp;
  }
  private void myLog(String s) {
    userNotify.log("SSH: "+s);
  }
  
  /////////////////////////
  // INTERNAL INTERNALS: //
  /////////////////////////


  private static String[] noFiles={};
  private String[] sshexecList(String path) {
    SSHExecResult res=exec("ls -1 "+path, true); //FIXME quote the file
    //Fail, for whatever reason including nonexistence:
    if (!res.success) 
      return noFiles;
        
    //List files & dirs:
    StringChunker sc=new StringChunker(res.output);
    List<String> lFiles=new LinkedList<>();
    while (sc.find("\n"))
      lFiles.add(sc.getUpTo().trim());
    String last=sc.getRest().trim();
    if (!last.equals(""))
      lFiles.add(last);
      
    //This happens when it's not a directory, it's a file and you just ls'd it anyhow:
    if (lFiles.size()==1 && lFiles.get(0).equals(path))
      return noFiles;
      
    //Copy into results:
    String[] files=new String[lFiles.size()];
    for (int i=0; i<lFiles.size(); i++) 
      files[i]=lFiles.get(i);
    return files;
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