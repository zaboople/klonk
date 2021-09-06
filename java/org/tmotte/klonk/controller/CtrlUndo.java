package org.tmotte.klonk.controller;
import java.util.List;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.KPersist;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.windows.popup.YesNoCancel;


public class CtrlUndo {
  private Editors editors;
  private StatusUpdate status;
  private KPersist persist;
  private boolean fastUndos=false;
  private YesNoCancel yesNo;
  private List<Setter<Boolean>> fastUndoListeners;

  public CtrlUndo(Editors editors, List<Setter<Boolean>> fastUndoListeners, StatusUpdate status, YesNoCancel yesNo, KPersist persist) {
    this.editors=editors;
    this.yesNo=yesNo;
    this.status=status;
    this.persist=persist;
    this.fastUndoListeners=fastUndoListeners;
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
  public void undoToHistorySwitch() {
    editors.getFirst().undoToHistorySwitch();
    status.show("Undone to last history rewrite");
  }
  public void redoToHistorySwitch() {
    editors.getFirst().redoToHistorySwitch();
    status.show("Undone to last history rewrite");
  }
  public void doUndoFast() {
    persist.setFastUndos(fastUndos=!fastUndos);
    persist.save();
    for (Editor e: editors.forEach())
      e.setFastUndos(fastUndos);
    fastUndoListeners.stream().forEach(f -> f.set(fastUndos));
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