package org.tmotte.common.swang;
public class CurrentOS {
  public final boolean isOSX;
  public final boolean isMSWindows;
  public CurrentOS(){
    String os=System.getProperty("os.name").toLowerCase();
    isMSWindows=os!=null && os.indexOf("windows")>-1;
    isOSX=os.indexOf("mac os")>-1;
  }
  public CurrentOS(boolean isOSX, boolean isMSWindows) {
    this.isOSX=isOSX;
    this.isMSWindows=isMSWindows;
  }
}