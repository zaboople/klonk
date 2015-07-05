package org.tmotte.klonk.config.msg;

public class UserServer {
  public final String user;
  public final String server;
  public UserServer(String user, String server) {
    this.user=user;
    this.server=server;
  }
  public @Override boolean equals(Object o) {
    if (o instanceof UserServer) {
      UserServer us=(UserServer)o;
      return this.user.equals(us.user) && this.server.equals(us.server);
    }
    else
      return false;
  }
  public @Override int hashCode(){
    return user.hashCode() + server.hashCode();
  }
}