package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.edit.MyTextArea;
import org.tmotte.klonk.windows.popup.GoToLine;
import org.tmotte.klonk.windows.popup.Popups;

public class CtrlSearch {

  private Editors editors;
  private Popups popups;
  private GoToLine gtl;
  private StatusUpdate statusBar;

  public CtrlSearch(Editors editors, StatusUpdate statusBar, Popups popups, GoToLine gtl) {
    this.statusBar=statusBar;
    this.editors=editors;
    this.popups=popups;
    this.gtl=gtl;
  }
  
  public void doSearchFind(){
    popups.doFind(editors.getFirst().getTextArea());
  }
  public void doSearchReplace(){
    popups.doReplace(editors.getFirst().getTextArea());
  }
  public void doSearchRepeat(){
    popups.repeatFindReplace(editors.getFirst().getTextArea(), true);
  }
  public void doSearchRepeatBackwards(){
    popups.repeatFindReplace(editors.getFirst().getTextArea(), false);
  }
  public void doSearchGoToLine() {
    MyTextArea target=editors.getFirst().getTextArea();
    int i=gtl.show();
    if (i==-1)
      statusBar.showBad("Go to line cancelled.");
    else
    if (!target.goToLine(i-1))
      statusBar.showBad("Line number "+i+" is out of range"); 
  }
  
  
}