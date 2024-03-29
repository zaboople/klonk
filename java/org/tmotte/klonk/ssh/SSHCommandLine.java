package org.tmotte.klonk.ssh;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.nio.file.Path;
import java.util.List;
import java.util.LinkedList;
import org.tmotte.klonk.config.option.SSHOptions;
import org.tmotte.klonk.config.msg.UserNotify;

public class SSHCommandLine {

  public interface ArgHandler {
    /** Return a new index, or -1 to indicate nothing found */
    public int handle(String[] args, int currIndex);
    public String document();
  }
  public SSH ssh;
  public SSHConnections connections;
  public SSHFile sshFile;

  private ArgHandler argHandler;

  public SSHCommandLine(String[] args) throws Exception {
    this(args, null);
  }
  public SSHCommandLine(String[] args, ArgHandler argHandler) throws Exception {
    this.argHandler=argHandler;
    int max=0;
    String user=null, host=null, knownHosts=null, pass=null, privateKeys=null, fileName=null;
    for (int i=0; i<args.length && max<100; i++){
      max++;
      String arg=args[i];
      if (arg.startsWith("-help"))
        usage(null);
      else
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
      else
      if (arg.startsWith("-f"))
        fileName=args[++i];
      else {
        boolean complain=true;
        if (argHandler!=null) {
          i=argHandler.handle(args, i);
          complain=i==-1;
        }
        if (complain) {
          usage("Unexpected: "+arg);
          return;
        }
      }
    }
    SSHOptions opts=new SSHOptions();
    opts.setKnownHostsFilename(knownHosts);
    opts.setPrivateKeysFilename(privateKeys);
    this.connections=
      new SSHConnections(new UserNotify(System.out))
        .withOptions(opts)
        .withLogin(new Login(user, pass));
    if (user != null && host != null && (pass != null || privateKeys !=null))
      ssh=connections.getOrCreate(user, host);
    if (fileName != null)
      sshFile=connections.getSSHFile(fileName);
  }//process()


  private void usage(String error) {
    if (error!=null)
      System.err.println("\nERROR: "+error);
    System.err.println(
      "Usage: <[-f sshpath] [-h host] [-u user]> [-k knownhostsfile] [-p pass] [-r privatekeyfile] "+
      (argHandler==null ?"" :argHandler.document())
    );
    System.exit(1);
  }

  ////////////////////////
  // INTERACTIVE LOGIN: //
  ////////////////////////

  private static class Login implements IUserPass {
    String user, pass;
    public Login(String user, String pass) {
      this.user=user;
      this.pass=pass;
    }
    final BufferedReader br=new BufferedReader(new InputStreamReader(System.in));
    public boolean get(String u, String host, boolean authFail, boolean needsPassword) {
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