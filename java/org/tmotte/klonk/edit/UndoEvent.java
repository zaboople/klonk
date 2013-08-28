package org.tmotte.klonk.edit;

public class UndoEvent {
  public boolean isNoMoreUndos=false, 
                 isNoMoreRedos=false,
                 isUndoSaveStable=false;
  UndoEvent setNoMoreUndos() {
    isNoMoreUndos=true;
    return this;
  }
  UndoEvent setNoMoreRedos() {
    isNoMoreRedos=true;
    return this;
  }
  UndoEvent setUndoSaveStable() {
    isUndoSaveStable=true;
    return this;
  }
}