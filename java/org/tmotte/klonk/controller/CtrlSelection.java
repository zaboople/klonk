package org.tmotte.klonk.controller;
import java.util.LinkedList;
import org.tmotte.common.swang.SimpleClipboard;
import org.tmotte.common.text.DelimitedString;
import org.tmotte.klonk.Editor;
import org.tmotte.klonk.config.msg.Editors;
import org.tmotte.klonk.config.msg.StatusUpdate;
import org.tmotte.klonk.config.msg.Setter;

public class CtrlSelection {
  private Editors editors;
  private StatusUpdate status;
  private Setter<String> alerter;
  
  public CtrlSelection(Editors editors, StatusUpdate status, Setter<String> alerter) {
    this.editors=editors;
    this.status=status;
    this.alerter=alerter;
  }

  public void doWeirdUpperCase() {
    editors.getFirst().doUpperCase();
  }
  public void doWeirdLowerCase() {
    editors.getFirst().doLowerCase();
  }
  public void doWeirdSortLines() {
    if (!editors.getFirst().doSortLines())
      status.show("Only one line was selected for sort");
  }
  public void doWeirdSelectionSize() {
    alerter.set("Selected text size: "+editors.getFirst().getSelection().length());
  }
  public void doWeirdAsciiValues() {
    String text=editors.getFirst().getSelection();
    DelimitedString result=new DelimitedString(" ");
    int len=text.length();
    for (int i=0; i<len; i++)
      result.add((int)text.charAt(i));
    String r=result.toString();
    SimpleClipboard.set(r);
    if (r.length()>1000)
      r=r.substring(1000)+"...";
    alerter.set("ASCII/UTF-8 values copied to Clipboard as: "+r);
  }
  
}