package org.tmotte.klonk.ssh;
public interface IUserPass {
  public boolean get(String user, String host);
  public String getUser();
  public String getPass();
}