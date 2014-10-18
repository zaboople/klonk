package org.tmotte.klonk.ssh;
import java.util.HashMap;
import java.util.Map;
import org.tmotte.common.text.StringChunker;
import java.util.regex.Pattern;
import java.util.Set;

public class ConnectionParse {

  private final Pattern sshPattern=Pattern.compile("ssh:/*", Pattern.CASE_INSENSITIVE);
  private final StringChunker chunker=new StringChunker();
  
  protected SSHFile parse(SSHConnections connMgr, String uri) throws ConnectionParseException {
    String user=null, host=null, dirFile=null;
    chunker.reset(uri);
    
    //Walk past the ssh:(////), we don't need it:
    if (!chunker.find(sshPattern))
      throw new RuntimeException(uri+" does not contain "+sshPattern);
          
    //Optionally get user@...:  FIXME TEST WITH MULTIPLE USERS
    if (!chunker.find("@")) {
      Set<String> users=connMgr.getUsers();
      if (users.size()!=1)
        throw new ConnectionParseException(uri+" does not designate \"user@\" in the connection string");
      user=users.iterator().next();
    }
    else 
      user=chunker.getUpTo();
      
    //Get host:
    if (!chunker.find(":"))
      throw new ConnectionParseException(uri+" does not seem to have a hostname ");
    host=chunker.getUpTo();

    SSH ssh=connMgr.getOrCreate(user, host);
    
    //Now get file:
    chunker.reset(chunker.getRest());    
    return parse(ssh, null, chunker);
  }
  
  private SSHFile parse(SSH ssh, SSHFile parent, StringChunker left) {
    if (!left.find("/")){
      String remains=left.getRest();
      if (remains==null) 
        //FIXME find out if the file is a dir or file
        return parent;
      else {
        remains=remains.trim();
        if (remains.equals(""))
          //FIXME find out if the file is a dir or file
          return parent;
        if (parent!=null)
          parent.isDir=true;
        return new SSHFile(ssh, parent, remains, false);//FIXME not false, find out if it's a dir!
      }
    }
    else {
      if (parent!=null)
        parent.isDir=true;
      String name=left.getUpTo();
      return parse(
        ssh, 
        new SSHFile(ssh, parent, name, false), //FIXME is it or not a dir?      
        left
      );
    }
  }
 
}