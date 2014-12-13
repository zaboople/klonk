package org.tmotte.klonk.ssh;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.common.text.ArgHandler;

public class SSHCommandLine {
  private ArgHandler argHandler;
  
  public SSH ssh;
  public SSHConnections connections;
  
  private void usage() {
    System.err.println(
      "Usage: -u user -h host -k knownhostsfile -p pass -r privatekeyfile "+
      (argHandler==null ?"" :argHandler.document())
    );
    System.exit(1);
  }
  public SSHCommandLine(String[] args) throws Exception {  
    this(args, null);
  }
  public SSHCommandLine(String[] args, ArgHandler argHandler) throws Exception {
    this.argHandler=argHandler;
    int max=0;
    String user=null, host=null, knownHosts=null, pass=null, privateKeys=null;
    for (int i=0; i<args.length && max<100; i++){
      max++;
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
        boolean complain=true;
        if (argHandler!=null) {
          i=argHandler.handle(args, i);
          complain=i==-1;
        }
        if (complain) {
          System.err.println("Unexpected: "+arg);
          System.exit(1);
          return;
        }
      }
    }
    this.connections= 
      new SSHConnections(
        new Setter<String>(){
          public void set(String s) {System.out.println(s);}
        }
        ,
        new Setter<String>(){
          public void set(String err) {System.err.println(err);}
        }
      )
      .withLogin(new Login(user, pass))
      .withKnown(knownHosts)
      .withPrivateKeys(privateKeys);
    if (user != null && host != null && (pass != null || privateKeys !=null))
      ssh=connections.getOrCreate(user, host);
  }//process()
  
  
  private static class Login implements IUserPass {
    String user, pass;
    public Login(String user, String pass) {
      this.user=user;
      this.pass=pass;
    }
    final BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    public boolean get(String u, String host, boolean authFail) {
      user=u;
      if (user!=null && pass !=null) 
        return true;
      try {
        System.out.println("Host: "+host);
        
        System.out.print("User: ");
        if (user!=null && !user.trim().equals("")) 
          System.out.print("<"+user+"> press enter to keep or: ");
        u=br.readLine().trim();
        if (!u.trim().equals(""))
          this.user=u;
        
        System.out.print("Pass: ");
        String p=br.readLine().trim();
        if (p!=null && !p.trim().equals(""))
          this.pass=p;
        
        return user!=null && !user.trim().equals("") && 
               pass!=null && !pass.trim().equals("");
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    }
    public String getUser(){return user;}
    public String getPass(){return pass;}  
  }

}