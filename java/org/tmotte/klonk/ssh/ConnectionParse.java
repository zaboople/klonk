package org.tmotte.klonk.ssh;
import java.util.HashMap;
import java.util.Map;
import org.tmotte.common.text.StringChunker;
import java.util.regex.Pattern;
import java.util.Set;

/** 
 * This is really just an extension of SSHConnections, should maybe be combined
 */
class ConnectionParse {

  private final Pattern sshPattern=Pattern.compile("ssh:/*", Pattern.CASE_INSENSITIVE);
  private final StringChunker chunker=new StringChunker();

  /** FIXME no COnnectionParseExceptions */
  protected SSHFile parse(SSHConnections connMgr, String uri) throws ConnectionParseException {
    String user=null, host=null, dirFile=null;
    chunker.reset(uri);
    
    //Walk past the ssh:(////), we don't need it:
    if (!chunker.find(sshPattern))
      throw new RuntimeException(uri+" does not contain "+sshPattern);
          
    //Optionally get user@...:  FIXME TEST WITH MULTIPLE USERS
    if (chunker.find("@")) 
      user=chunker.getUpTo();          
      
    //Get host:
    if (!chunker.find(":"))
      host=chunker.getRest();
    else
      host=chunker.getUpTo();
    
    //Go back and try again on user if we need to:
    if (user==null)
      user=connMgr.inferUserForHost(host);
    if (user==null)
      return null;
  
    //Now make the SSh object & get file name:
    SSH ssh=connMgr.getOrCreate(user, host);
    chunker.reset(chunker.getRest());    
    SSHFile result=parse(ssh, null, chunker);
    if (result==null) 
      //No name given, default to "~"
      result=new SSHFile(ssh, null, "~", true);
    return result;
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