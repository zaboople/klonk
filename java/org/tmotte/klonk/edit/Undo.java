package org.tmotte.klonk.edit;
import java.util.*;


final class Undo {

  private ArrayDeque<UndoStep> undos=new ArrayDeque<UndoStep>(),
                               redos=new ArrayDeque<UndoStep>();
  private UndoStep savedState;

  public Undo() {
    markSave();
  }

  public void doAdd(int start, int len, String text, boolean doubleUp) {
    makeRedosUndos();
    undos.add(new UndoStep(UndoStep.ADD,    start, len, text, doubleUp, doubleUp));
  }
  public void doRemove(int start, int len, String text, boolean doubleUp) {
    makeRedosUndos();
    undos.add(new UndoStep(UndoStep.REMOVE, start, len, text, doubleUp, doubleUp));
  }
  public void markSave() {
    if (savedState!=null)
      //This will only be null when we invoke from the constructor.
      //This object is marked as "limbo" to indicate it means
      //nothing and is awaiting cleanup, which happens in getLast():
      savedState.uType=savedState.LIMBO;
    savedState=UndoStep.createSaveState();
    undos.add(savedState);
  }

  public boolean isSavedState() {
    //Save state can be on either stack, doesn't matter.
    //If it's on either, we're at the saved state.
    UndoStep us1=undos.size()==0 ?null :undos.getLast(),
             us2=redos.size()==0 ?null :redos.getLast();
    return (us1!=null && us1.uType==us1.MARK_SAVE)
           ||
           (us2!=null && us2.uType==us2.MARK_SAVE)
            ;
  }
  public void debug(String yeah) {
    StringBuilder sb=new StringBuilder(yeah);
    for (UndoStep us: undos)
      us.debug("", sb);
    sb.append(" ---- ");
    for (UndoStep us: redos)
      us.debug("", sb);
    System.out.println(sb.toString());
    System.out.flush();
  }

  ///////////
  // UNDO: //
  ///////////

  public UndoStep doUndo() {
    return removeLast(undos, redos);
  }
  public UndoStep getUndo() {
    return getLast(undos, redos);
  }
  public boolean hasUndos() {
    return getLast(undos, redos)!=null;
  }
  public void clearUndos() {
    undos.clear();
  }


  ///////////
  // REDO: //
  ///////////

  public UndoStep doRedo() {
    return removeLast(redos, undos);
  }
  public UndoStep getRedo() {
    return getLast(redos, undos);
  }
  public boolean hasRedos() {
    return getLast(redos, undos)!=null;
  }
  public void clearRedos() {
    redos.clear();
  }

  ////////////////
  // INTERNALS: //
  ////////////////

  private static UndoStep removeLast(ArrayDeque<UndoStep> mainList, ArrayDeque<UndoStep> otherList) {
    if (mainList.size()==0)
      return null;
    UndoStep st=mainList.removeLast();
    if (st.uType!=st.LIMBO)
      otherList.add(st);
    return st.isAddOrRemove() ?st :removeLast(mainList, otherList);
  }

  private UndoStep getLast(ArrayDeque<UndoStep> mainList, ArrayDeque<UndoStep> otherList) {

    //Normal undos:
    if (mainList.size()==0)
      return null;
    UndoStep us=mainList.getLast();
    if (us.isAddOrRemove())
      return us;

    //SAVE_STATE is pushed; skip LIMBO (by default).
    //Then recurse:
    mainList.removeLast();
    if (us.isSaveState())
      otherList.add(us);
    return getLast(mainList, otherList);
  }

  private void makeRedosUndos() {
    //This is seriously major thinking backwards.
    //1. Push the original edits onto the redo stack, first to last
    //2. Push their undos onto the redo stack, last to first:
    for (Iterator<UndoStep> iter=redos.descendingIterator(); iter.hasNext();){
      UndoStep us=iter.next();
      undos.add(us);
    }
    UndoStep last=null, first=redos.size()==0 ?null :redos.getFirst();
    if (first!=null)
      first.doubleUpDone=true;
    for (Iterator<UndoStep> iter=redos.iterator(); iter.hasNext();){
      last=iter.next();
      int uType;
      if (last.isAddOrRemove()){
        last=new UndoStep(
           last.uType==last.REMOVE ?last.ADD :last.REMOVE
          ,last.start, last.len, last.text, true, false //last.doubleUp
        );
        undos.add(last);//Save state already added once
      }
    }
    if (last!=null)
      last.doubleUpDone=true;
    redos.clear();
  }

}