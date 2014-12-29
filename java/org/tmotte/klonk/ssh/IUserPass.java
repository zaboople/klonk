package org.tmotte.klonk.ssh;
public interface IUserPass {
  public boolean get(String user, String host, boolean authFail, boolean needsPassword);
  public String getUser();
  public String getPass();
}