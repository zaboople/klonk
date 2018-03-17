package org.tmotte.klonk.edit;
public class IndenterTest extends Indenter {
  public static void main(String[] args) {
    IndenterTest test=new IndenterTest();
    test.testRepair();
  }
  private void testRepair() {
    tabIndents=false;
    tabSize=4;
    spaceIndentLen=2;
    testRepair("__T", "__T", 6);
    testRepair("_T", "__", 2);

    tabIndents=false;
    tabSize=4;
    spaceIndentLen=4;
    testRepair("__T", "____", 4);
    testRepair("____T", "____T", 8);
    testRepair("T___", "T___", 7);

    tabIndents=true;
    tabSize=4;
    spaceIndentLen=2;
    testRepair("__T", "T", 4);
    testRepair("_T", "T", 4);
    testRepair("_T_T", "TT", 8);
    testRepair("____T", "____T", 8);
    testRepair("T___", "T___", 7);
  }

  private void testRepair(String has, String expects) {
    testRepair(has, expects, -1);
  }

  private void testRepair(String has, String expects, int viewLenExpect) {
    System.out.append("--------------\n")
      .append("Had:  ").append(has).append("\n")
      .append("Want: ").append(expects).append("\n");
    has=toIndents(has);
    expects=toIndents(expects);
    String repaired=repair(has);
    System.out.append("Got:->").append(repaired).append("<-").append("\n");
    if (viewLenExpect!= -1 && viewLen!=viewLenExpect)
      throw new RuntimeException("View len mismatch; expect "+viewLenExpect+" got "+viewLen);
    if (anyChange==repaired.equals(has))
      throw new RuntimeException("Change mismatch: "+anyChange);
    if (!repaired.equals(expects))
      throw new RuntimeException("Mismatch");
  }

  private static String toIndents(String s) {
    StringBuilder sb=new StringBuilder();
    int len=s.length();
    for (int i=0; i<len; i++) {
      char c=s.charAt(i);
      if (c=='t' || c=='T')
        sb.append('\t');
      else
      if (c=='_' || c=='-')
        sb.append(' ');
      else
        sb.append(c);
    }
    return sb.toString();
  }
}