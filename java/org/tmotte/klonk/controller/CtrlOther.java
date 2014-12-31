package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.windows.popup.Shell;
import org.tmotte.klonk.windows.popup.Help;
import org.tmotte.klonk.windows.popup.About;
import java.util.LinkedList;


public class CtrlOther {
  private Shell shell;
  private Help help;
  private About about;
  
  public CtrlOther(Shell shell, Help help, About about) {
    this.shell=shell;
    this.help=help;
    this.about=about;
  }

  public void doShell() {
    shell.show();
  }
  public void doHelpShortcuts() {
    help.show();
  }
  public void doHelpAbout() {
    about.show();
  }
}