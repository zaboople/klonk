package org.tmotte.klonk.windows.popup;
public class YesNoCancelAnswer {
  public static int YES=1, NO=2, CANCEL=3;
  private int answer;
  public YesNoCancelAnswer(int answer) {this.answer=answer;}
  public boolean isYes(){return answer==YES;}
  public boolean isNo() {return answer==NO;}
  public boolean isCancel() {return answer==CANCEL;}
  public String toString() {
    if (isYes())
      return "Yes";
    else
    if (isNo())
      return "No";
    else
    if (isCancel())
      return "Cancel";
    else
      throw new RuntimeException("Unexpected result "+answer);
  }
}