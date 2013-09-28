package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.windows.popup.Popups;


public class CtrlSearch {

  private Editors editors;
  private Popups popups;

  public CtrlSearch(Editors editors, Popups popups) {
    this.editors=editors;
    this.popups=popups;
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
    popups.goToLine(editors.getFirst().getTextArea());
  }
  
  
}