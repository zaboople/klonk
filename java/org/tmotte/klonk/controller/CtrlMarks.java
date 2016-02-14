package org.tmotte.klonk.controller;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.Setter;
import org.tmotte.common.text.DelimitedString;
import javax.swing.SwingUtilities;
import java.util.LinkedList;

public class CtrlMarks {
  private Editors editors;
  private StatusUpdate status;
  private Setter<Boolean> markStateListener;

  public CtrlMarks(Editors editors, StatusUpdate status, Setter<Boolean> markStateListener) {
    this.editors=editors;
    this.status=status;
    this.markStateListener=markStateListener;
  }


  public void doMarkSet() {
    Editor e=editors.getFirst();
    int i=e.doSetMark();
    if (i!=-1){
      status.show("Mark set");
      markStatus.go(i, e.getMarkCount(), true);
    }
    else
      status.showBad("Mark already set at this position");
    if (i!=-1)
      markStateListener.set(true);
  }
  public void doMarkGoToPrevious() {
    Editor e=editors.getFirst();
    int i=e.doMarkGoToPrevious();
    if (i!=-1)
      markStatus.go(i, e.getMarkCount(), false);
    else
      status.showBad("Cursor is before first mark.");
  }
  public void doMarkGoToNext() {
    Editor e=editors.getFirst();
    int i=e.doMarkGoToNext();
    if (i!=-1)
      markStatus.go(i, e.getMarkCount(), false);
    else
      status.showBad("Cursor is after last mark.");
  }
  public void doMarkClearCurrent() {
    int i=editors.getFirst().doMarkClearCurrent();
    if (i==-1)
      status.showBad("Cursor is not on a set mark.");
    else
      status.show("Mark cleared; "+i+" marks left.");
    if (i==0)
      markStateListener.set(false);
  }
  public void doMarkClearAll() {
    editors.getFirst().doClearMarks();
    status.show("All marks cleared");
    markStateListener.set(false);
  }

  // Had to create this because updates weren't showing up. Dunno.
  private MarkStatus markStatus=new MarkStatus();
  private class MarkStatus implements Runnable {
    boolean set; int i, count;
    public void go(int i, int count, boolean set) {
      this.i=i;
      this.count=count;
      this.set=set;
      SwingUtilities.invokeLater(this);
    }
    public void run() {
      status.show("Mark "+i+" of "+count+(set ?" set." :"."));
    }
  }

}