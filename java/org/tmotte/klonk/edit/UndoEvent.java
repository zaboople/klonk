package org.tmotte.klonk.edit;

public class UndoEvent {
  public boolean isNoMoreUndosError=false,
                 isNoMoreRedosError=false,
                 isUndoSaveStable=false;
  UndoEvent setNoMoreUndosError() {
    isNoMoreUndosError=true;
    return this;
  }
  UndoEvent setNoMoreRedosError() {
    isNoMoreRedosError=true;
    return this;
  }
  UndoEvent setUndoSaveStable() {
    isUndoSaveStable=true;
    return this;
  }
  public String toString(){
    return "No more undos: "+isNoMoreUndosError+
      " No more redos: "+isNoMoreRedosError
      +" Stable: "+isUndoSaveStable;
  }
}