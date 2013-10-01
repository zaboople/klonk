package org.tmotte.klonk.config.msg;
public interface StatusUpdate {
  public void show(String s);
  public void showBad(String s);  
  public void showNoStatus();
  public void showCapsLock(boolean b);  
  public void showRowColumn(int row, int column);
  public void showChangeThis(boolean b);
  public void showChangeAny(boolean b);
  public void showTitle(String title);
}