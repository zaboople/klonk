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
  public static void main(String[] args) throws Exception {
    for (String s: args)
      System.out.println("FUCK "+s);
    /*
    FileGrab grab=new FileGrab();
    SSHConnections conns=SSHCommandLine.cmdLine(args, grab);
    //System.out.println(grab.file);
    File file=conns.getFile(grab.file);
    //System.out.println(file);
    */
  }
  private static class FileGrab implements ArgHandler {
    String file;
    public int handle(String[] args, int i) {
      if (args[i].equals("-f"))
        file=args[++i];
      else
        i=-1;
      return i;      
    }
  }
  
  
  private static void usage() {
    System.err.println("Usage: -u user -h host -k knownhostsfile -p pass -r privatekeyfile");
    System.exit(1);
  }
  public static SSHConnections cmdLine(String[] args, ArgHandler argHandler) throws Exception {
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
          return null;
        }
      }
    }
    return 
      new SSHConnections(new Setter<String>(){
        public void set(String err) {
          System.err.println(err);
        }
      })
      .withLogin(new Login(user, pass))
      .withKnown(knownHosts)
      .withPrivateKeys(privateKeys);
  }
  
  private static class Login implements IUserPass {
    String user, pass;
    public Login(String user, String pass) {
      this.user=user;
      this.pass=pass;
    }
    final BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    public boolean get(String u, String host, boolean authFail) {
      try {
        user=u;
        System.out.println("Host: "+host);
        
        System.out.print("User: ");
        if (user!=null && !user.trim().equals("")) 
          System.out.print("<"+user+"> press en");
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
    public String getUser(){
      return user;
    }
    public String getPass(){
      return pass;
    }  
  }

}