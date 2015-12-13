package org.tmotte.klonk.windows.popup;
import java.awt.Image;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.io.File;
import java.util.List;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.filechooser.FileSystemView;
import javax.swing.filechooser.FileView;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.CurrentOS;
import org.tmotte.klonk.ssh.SSHFile;

/**
 * The FileDialog/JFileChooser classes already do most of the work, so this just does a minimal
 * bit of upkeep and whatnot. It used to be in the Popups class, but at the risk of earning the title
 * of Wrapper/Facade/Factory Abuser, I moved it down here so Popups could be more focused on its primary task.
 */
public class FileDialogWrapper {

  private JFrame mainFrame;
  private CurrentOS currentOS;
  private boolean initialized=false;

  //FileDialog is best for OSX at least when not doing SSH:
  private FileDialog fileDialog;

  //JFileChooser sucks but not as bad when used in native mode on windows,
  //and it works great with my SSH tricks:
  private JFileChooser fileChooser;
  private FileView fileView;
  private FileSystemView fileSystemView;



  public FileDialogWrapper(JFrame mainFrame, CurrentOS currentOS, FileSystemView fsv, FileView fv){
    this.mainFrame=mainFrame;
    this.currentOS=currentOS;
    this.fileSystemView=fsv;
    this.fileView=fv;
  }
  public FileDialogWrapper(JFrame mainFrame, CurrentOS currentOS){
    this(mainFrame, currentOS, null, null);
  }


  public File show(boolean forSave) {
    return show(forSave, null);
  }
  public File show(boolean forSave, File startFile) {
    return show(forSave, startFile, null);
  }
  public File showForDir(boolean forSave, File startDir) {
    return show(forSave, null, startDir);
  }

  public File show(boolean forSave, File startFile, File startDir) {
    init();
    if (currentOS.isOSX && SSHFile.cast(startFile)==null || SSHFile.cast(startDir)==null)
      try {
        //Broken on MS Windows XP. If you do "save as" and the old file
        //name is longer than the new one, the last characters from the old
        //file get appended to the new one - very likely a null terminated string
        //problem. I tried everything and it was unfixable.
        FileDialog fd=fileDialog;
        if (startFile!=null)
          fd.setFile(startFile.getCanonicalPath().trim());
        if (startDir!=null)
          fd.setDirectory(startDir.getCanonicalPath());
        fd.setTitle(forSave ?"Save" :"Open");
        fd.setMode(forSave ?fd.SAVE :fd.LOAD);
        fd.setVisible(true);

        File[] f=fd.getFiles();
        if (f==null || f.length==0)
          return null;
        return f[0];
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    else {
      if (startFile!=null){
        if (startFile.isDirectory()){
          startDir=startFile;
        }
        else {
          fileChooser.setSelectedFile(startFile);
          try {
            startDir=startFile.getCanonicalFile().getParentFile();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
      if (startDir!=null)
        fileChooser.setCurrentDirectory(startDir);
      int returnVal=forSave
        ?fileChooser.showSaveDialog(mainFrame)
        :fileChooser.showOpenDialog(mainFrame);
      if (returnVal==fileChooser.APPROVE_OPTION)
        return fileChooser.getSelectedFile();
      else
        return null;
    }
  }


  private void init() {
    if (!initialized){
      create();
      initialized=true;
    }
  }
  private void create() {
    fileChooser=new JFileChooser();
    if (fileSystemView!=null)
      fileChooser.setFileSystemView(fileSystemView);
    if (fileView!=null)
      fileChooser.setFileView(fileView);
    fileDialog=new FileDialog(mainFrame);
  }

  public static void main(final String[] args) {
    if (args.length<2)
      System.err.println("Need a directory & file");
    else
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          FileDialogWrapper fdw=new FileDialogWrapper(
            PopupTestContext.makeMainFrame(), new org.tmotte.klonk.config.CurrentOS()
          );
          File d=new File(args[0]),
              f=new File(args[1]);
          System.out.println("For save: >"+fdw.show(true, null, null)+"<");
          System.out.println("For save to file: "+f+" -> "+fdw.show(true, f, null)+"<");
          System.out.println("For save to dir: "+d+" -> "+fdw.show(true, null, d)+"<");

          System.out.println("For open: >"+fdw.show(false, null, null)+"<");
          System.out.println("For open to file: "+f+" -> "+fdw.show(false, f, null)+"<");
          System.out.println("For open to dir: "+d+" ->"+fdw.show(false, null, d)+"<");
        }
      });
  }

}