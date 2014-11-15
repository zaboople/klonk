package org.tmotte.klonk.ssh;
public interface IUserPass {
  public boolean get(String user, String host, String lastError);
  public String getUser();
  public String getPass();
}