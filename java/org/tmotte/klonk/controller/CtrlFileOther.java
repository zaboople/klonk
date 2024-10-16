package org.tmotte.klonk.controller;
import org.tmotte.klonk.io.Printing;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.common.swang.SimpleClipboard;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import java.util.LinkedList;


public class CtrlFileOther {
  private Editors editors;
  private StatusUpdate statusBar;
  private CtrlFavorites ctrlFavorites;
  private CurrentOS currentOS;

  public CtrlFileOther(
      Editors editors, StatusUpdate statusBar, CtrlFavorites ctrlFavorites, CurrentOS currentOS
    ) {
    this.editors=editors;
    this.statusBar=statusBar;
    this.ctrlFavorites=ctrlFavorites;
    this.currentOS=currentOS;
  }

  public void doDocumentDirectoryExplore() {
    try{
      String cmd=null;
      if (currentOS.isOSX)
        cmd="open";
      else
      if (currentOS.isMSWindows)
        cmd="explorer";
      else
        throw new RuntimeException("Unknown operating system");
      String filename=editors.getFirst().getFile().getParentFile().getCanonicalPath();
      Runtime.getRuntime().exec(new String[]{cmd, filename});
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

  public void doPrint() {
    if (Printing.print(editors.getFirst().getTextArea()))
      statusBar.show("Print job scheduled");
    else
      statusBar.showBad("Action cancelled");
  }

  public void doClipboardDoc() {
    toClipboard(
      ControllerUtils.getFullPath(editors.getFirst().getFile())
    );
  }
  public void doClipboardDocDir() {
    toClipboard(
      ControllerUtils.getFullPath(editors.getFirst().getFile().getParentFile())
    );
  }

  public void doAddCurrentToFaveDirs(){
    String s=ControllerUtils.getFullPath(editors.getFirst().getFile().getParentFile());
    ctrlFavorites.getDirs().add(s);
    ctrlFavorites.set();
    statusBar.show("\""+s+"\" added to favorite directories.");
  }

  public void doAddCurrentToFaveFiles(){
    String s=ControllerUtils.getFullPath(editors.getFirst().getFile());
    ctrlFavorites.getFiles().add(s);
    ctrlFavorites.set();
    statusBar.show("\""+s+"\" added to favorite files.");
  }



  private void toClipboard(String name) {
    SimpleClipboard.set(name);
    statusBar.show("Copied to clipboard: "+name);
  }

  public static void main(String[] args) throws Exception {
    java.io.File file=new java.io.File(args[0]);
    System.out.println(file.getCanonicalPath());
    System.out.println(file.getCanonicalFile().getParentFile());
  }

}