package org.tmotte.klonk.config;
import java.io.File;

public class KHome {
  public String dirName;
  public File dir;
  public boolean ready=false;

  public KHome(String directory) {
    dirName=directory;
    dir=new File(directory);
    if (!dir.exists())
      try {
        dir.mkdir();
        if (!dir.exists())
          throw new RuntimeException("Could not make directory "+directory);
      } catch (Exception e) {
        dirName=null;
        dir=null;
        throw new RuntimeException("Could not make directory: "+directory, e);
      }
    try {
      dirName=dir.getCanonicalPath();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    ready=true;
  }


  public KHome mkdir(String newDir) {
    return new KHome(nameIt(dirName, newDir));
  }
  public File nameFile(String file) {
    return nameFile(dirName, file);
  }
  public String toString() {
    return dirName;
  }
  
  
  public String getUserHome() {
    return dirName;
  }
  public static File nameFile(String dir, String file) {
    return new File(nameIt(dir, file));
  }
  public static String nameIt(String dir, String file) {
    if (!dir.endsWith(File.separator))
      dir+=File.separator;
    return dir+file;
  }
}