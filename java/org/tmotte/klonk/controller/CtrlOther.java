package org.tmotte.klonk.controller;
import org.tmotte.common.swang.SimpleClipboard;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.windows.popup.Shell;
import org.tmotte.klonk.windows.popup.Popups;
import java.util.LinkedList;


public class CtrlOther {
  private Popups popups;
  private Shell shell;
  
  public CtrlOther(Shell shell, Popups popups) {
    this.shell=shell;
    this.popups=popups;
  }

  public void doShell() {
    shell.show();
  }
  public void doHelpShortcuts() {
    popups.showHelp();
  }
  public void doHelpAbout() {
    popups.showHelpAbout();
  }
}