package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.windows.popup.YesNoCancel;
import java.util.LinkedList;

public class CtrlUndo {
  private Editors editors;
  private StatusUpdate status;
  private KPersist persist;
  private boolean fastUndos=false;
  private YesNoCancel yesNo;
  
  public CtrlUndo(Editors editors, YesNoCancel yesNo, StatusUpdate status, KPersist persist) {
    this.editors=editors;
    this.yesNo=yesNo;
    this.status=status;
    this.persist=persist;
    fastUndos=persist.getFastUndos();
  }
  
  public void doUndo(){
    editors.getFirst().undo();
  }
  public void doRedo(){
    editors.getFirst().redo();
  }
  public void doUndoToBeginning() {
    editors.getFirst().undoToBeginning();
    status.show("Undone to beginning");
  }
  public void doRedoToEnd() {
    editors.getFirst().redoToEnd();
    status.show("Redone to end");
  }
  public void doUndoFast() {
    persist.setFastUndos(fastUndos=!fastUndos);
    persist.save();
    for (Editor e: editors.forEach())
      e.setFastUndos(fastUndos);
  }
  public void doClearUndos() {
    if (yesNo.show("Clear undos?").isYes()){
      editors.getFirst().clearUndos();
      status.show("Undo stack cleared");
    }
    else
      status.showBad("Action cancelled");
  }
  public void doClearRedos() {
    if (yesNo.show("Clear redos?").isYes()){
      editors.getFirst().clearRedos();
      status.show("Redo stack cleared");
    }
    else
      status.showBad("Action cancelled");
  }
  public void doClearUndosAndRedos() {
    if (yesNo.show("Clear undos and redos?").isYes()){
      editors.getFirst().clearUndos();
      editors.getFirst().clearRedos();
      status.show("Undos & redos cleared");
    }
    else
      status.showBad("Action cancelled");
  }
  

  
}