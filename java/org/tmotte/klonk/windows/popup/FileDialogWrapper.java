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
import org.tmotte.common.swang.Fail;
import org.tmotte.klonk.config.FontOptions;
import org.tmotte.klonk.config.KHome;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.Kontext;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.StatusNotifier;

class FileDialogWrapper {

  private JFrame mainFrame;
  private FileDialog fileDialog;
  private JFileChooser fileChooser;
  
  FileDialogWrapper(JFrame mainFrame){
    this.mainFrame=mainFrame;
  }

  File show(boolean forSave, File startFile, File startDir) {
    if (true) {
      //File chooser sucks but not as bad as it used to when in native mode:
      if (fileChooser==null)
        fileChooser=new JFileChooser();
      if (startFile!=null){
        if (startFile.isDirectory()){
          startDir=startFile;
        }
        else {
          fileChooser.setSelectedFile(startFile);
          startDir=startFile.getParentFile();
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
    else
      try {
        //Unused; Broken on MS Windows XP. If you do "save as" and the old file
        //name is longer than the new one, the last characters from the old
        //file get appended to the new one. I tried everything and it was
        //unfixable.
        if (fileDialog==null)
          fileDialog=new FileDialog(mainFrame);
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
  }


}