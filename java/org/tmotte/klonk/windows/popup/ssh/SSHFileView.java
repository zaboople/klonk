package org.tmotte.klonk.windows.popup.ssh;
import org.tmotte.klonk.ssh.SSHFile;
import javax.swing.filechooser.FileView;
import java.io.File;

public class SSHFileView extends FileView{

  public SSHFileView() {
    super();
  }
  /** 
   * For some reason this gets called even though we already have a FileSystemView class that 
   * does the same thing. 
   */
  public Boolean isTraversable(File fdir){
    //System.out.println("SSHFileView.isTraversable: "+f);
    if (SSHFile.cast(fdir)==null)
      return super.isTraversable(fdir);
    return fdir.isDirectory();
  }


}