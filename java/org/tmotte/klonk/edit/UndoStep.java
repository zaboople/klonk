package org.tmotte.klonk.edit;

public final class UndoStep {
  public final static int ADD=1, REMOVE=2, MARK_SAVE=3, LIMBO=4;

  public int start, len, uType;
  public String text;
  public boolean doubleUp, doubleUpDone;
  UndoStep(int uType, int start, int len, String text, boolean doubleUp) {
    this(uType, start, len, text, doubleUp, false);
  }
  UndoStep(int uType, int start, int len, String text, boolean doubleUp, boolean doubleUpDone) {
    this.uType=uType;
    this.start=start;
    this.len=len;
    this.text=text;
    this.doubleUp=doubleUp;
    this.doubleUpDone=doubleUpDone;
    //if (doubleUp) System.out.println(this.toString());
  }
  public static UndoStep createSaveState() {
    return new UndoStep(MARK_SAVE, -1, -1, null, false, false);
  }


  public boolean isSaveState() {
    return uType==MARK_SAVE;
  }
  public boolean isAddOrRemove() {
    return uType<=2;
  }

  public void debug(String yeah) {
    Appendable ap=new StringBuilder();
    debug(yeah, ap);
    System.out.println(ap);
    System.out.flush();
  }
  public void debug(String yeah, Appendable sb) {
    try {
      sb.append(yeah);
      sb.append(this.toString());
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public String toString() {
    return uType+" "+start+" "+len+" "+text+" "+doubleUp+"; ";
  }
}
