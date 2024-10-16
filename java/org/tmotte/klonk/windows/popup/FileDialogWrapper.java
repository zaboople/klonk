package org.tmotte.klonk.windows.popup;
import java.awt.Dimension;
import java.awt.FileDialog;
import java.awt.Image;
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
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.klonk.config.PopupInfo;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.ssh.SSHFile;

/**
 * The FileDialog/JFileChooser classes already do most of the work, so this just does a minimal
 * bit of upkeep and whatnot.
 */
public class FileDialogWrapper {

  // DI:
  private PopupInfo pInfo;

  // State:
  private boolean initialized=false;

  // Controls:
  //JFileChooser sucks but not as bad when used in native mode on windows,
  //and it works great with my SSH tricks:
  private JFileChooser fileChooser;
  private FileView fileView;
  private FileSystemView fileSystemView;

  public FileDialogWrapper(PopupInfo pInfo, FileSystemView fsv, FileView fv){
    this.pInfo=pInfo;
    this.fileSystemView=fsv;
    this.fileView=fv;
  }
  public FileDialogWrapper(PopupInfo pInfo){
    this(pInfo, null, null);
  }

  public final static class DialogOpts {
    boolean forSave;
    File startFile;
    File startDir;
    boolean dirsOnly;
    String approveButtonText;
    public DialogOpts setForSave(boolean t){forSave=t;return this;}
    public DialogOpts setStartFile(File f){startFile=f;return this;}
    public DialogOpts setStartDir(File d){startDir=d;return this;}
    public DialogOpts setDirsOnly(){dirsOnly=true;return this;}
    public DialogOpts setApproveButton(String s){approveButtonText=s;return this;}
  }


  public File show(boolean forSave) {
    return show(
      new DialogOpts().setForSave(forSave)
    );
  }
  public File show(boolean forSave, File startFile) {
    return show(
      new DialogOpts().setForSave(forSave)
        .setStartFile(startFile)
    );
  }
  public File showForDir(boolean forSave, File startDir) {
    return show(
      new DialogOpts().setForSave(forSave)
        .setStartDir(startDir)
    );
  }
  public File show(boolean forSave, File startFile, File startDir) {
    return show(
      new DialogOpts().setForSave(forSave)
        .setStartFile(startFile).setStartDir(startDir)
    );
  }

  public File show(DialogOpts opts) {
    init();
    if (pInfo.currentOS.isOSX && SSHFile.cast(opts.startFile)==null && SSHFile.cast(opts.startDir)==null)
      try {
        // Broken on MS Windows XP. If you do "save as" and the old file
        // name is longer than the new one, the last characters from the old
        // file get appended to the new one - very likely a null terminated string
        // problem. I tried everything and it was unfixable.
        //
        // On OSX? Well, then we can't reuse file dialogs or errors start dumping out.
        // So we make a new one every time, which doesn't seem to use that much
        // memory.
        FileDialog fd=new java.awt.FileDialog(pInfo.parentFrame);
        if (opts.startFile!=null) {
          //On OSX, passing the full filename screws up everything unlike
          //Windows. So we have to set the file name & dir name individually.
          fd.setFile(opts.startFile.getName());
          if (opts.startDir==null && opts.startFile.getParentFile()!=null)
            fd.setDirectory(opts.startFile.getParentFile().getCanonicalPath());
        }
        if (opts.startDir!=null)
          fd.setDirectory(opts.startDir.getCanonicalPath());
        fd.setTitle(opts.forSave ?"Save" :"Open");
        fd.setMode(opts.forSave ?FileDialog.SAVE :FileDialog.LOAD);
        fd.setVisible(true);

        if (opts.dirsOnly) {
          final String d = fd.getDirectory();
          return d==null
            ?null
            :new File(fd.getDirectory());
        }
        File[] f=fd.getFiles();
        if (f==null || f.length==0)
          return null;
        return f[0];
      } catch (Exception e) {
        throw new RuntimeException(e);
      }
    else {
      fileChooser.setFileSelectionMode(
        opts.dirsOnly
          ?JFileChooser.DIRECTORIES_ONLY
          :JFileChooser.FILES_ONLY
      );
      if (opts.startFile!=null){
        if (opts.startFile.isDirectory()){
          opts.startDir=opts.startFile;
        }
        else {
          fileChooser.setSelectedFile(opts.startFile);
          try {
            opts.startDir=opts.startFile.getCanonicalFile().getParentFile();
          } catch (Exception e) {
            throw new RuntimeException(e);
          }
        }
      }
      if (opts.startDir!=null)
        fileChooser.setCurrentDirectory(opts.startDir);
      int returnVal;
      if (opts.approveButtonText!=null)
        returnVal=fileChooser.showDialog(pInfo.parentFrame, opts.approveButtonText);
      else
      if (opts.forSave)
        returnVal=fileChooser.showSaveDialog(pInfo.parentFrame);
      else
        returnVal=fileChooser.showOpenDialog(pInfo.parentFrame);

      if (returnVal==JFileChooser.APPROVE_OPTION)
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
  }

  public static void main(final String[] args) {
    if (args.length<2)
      System.err.println("Need a directory & file");
    else
      javax.swing.SwingUtilities.invokeLater(new Runnable() {
        public void run() {
          FileDialogWrapper fdw=new FileDialogWrapper(new PopupTestContext().getPopupInfo());
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