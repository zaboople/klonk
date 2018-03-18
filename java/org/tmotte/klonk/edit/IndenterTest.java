package org.tmotte.klonk.edit;
public class IndenterTest extends Indenter {

  public static void main(String[] args) {
    IndenterTest test=new IndenterTest();
    test.testIndent();
    test.testRepair();
  }

  private void testIndent() {
    tabIndents=false;
    tabSize=4;
    spaceIndentLen=2;
    testIndent("__T", "__----__", "____", false);
    testIndent("____T", "____----__", "____--", false);
    testIndent("_T", "____--", "__", false);
    testIndent("", "__", "", false);
    testIndent("_", "__", "", true);

    tabSize=2;
    spaceIndentLen=2;
    testIndent("__T_*", "____--_*", "___*", false);//REALLY WRONG asterisk is gone

  }

  private void testIndent(String has, String expectIndent, String expectRemove, boolean fitToBlock) {
    testIndent(has, expectIndent, false, fitToBlock);
    testIndent(has, expectRemove, true, fitToBlock);
  }

  private void testIndent(String has, String expects, boolean remove, boolean fitToBlock) {
    System.out.append("--------------\n")
      .append("Had:  ").append(has).append("\n")
      .append("Want: ").append(expects).append("\n");

    has=toIndents(has);
    expects=toIndents(expects);
    init(has);
    indent(remove, fitToBlock);
    String indented=buffer.toString();

    System.out.append("Got:->").append(indented).append("<-").append("\n");
    if (anyChange==indented.equals(has))
      throw new RuntimeException("Change mismatch: "+anyChange);
    if (!indented.equals(expects))
      throw new RuntimeException("Mismatch");
  }


  private void testRepair() {
    tabIndents=false;
    tabSize=2;
    spaceIndentLen=2;
    testRepair("_TT_", "_____");
    testRepair("T__T", "______");
    testRepair("_T", "__");

    tabIndents=false;
    tabSize=4;
    spaceIndentLen=2;
    testRepair("_TT_", "____----_");
    testRepair("T___", "____---");

    if (true) return;

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
    init(has);
    String repaired=buffer.toString();
    System.out.append("Got:->").append(repaired).append("<-").append("\n");
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