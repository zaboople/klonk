package org.tmotte.klonk.edit;
public class UndoSimilar {

  public static boolean matches(String ass, String bss) {
    if (ass==null || bss==null || ass.length()==0 || bss.length()==0)
      return false;
    char a=ass.charAt(0), b=bss.charAt(0);
    if (a==b) return true;
    final String[] possible={
      Selectable.strLetterCaps+Selectable.strLetterLow
      ,Selectable.strNum+"/-.+*"
      ,"<{[(\">}])"
      ,Selectable.strWhite
    };
    return matches(a, b, possible);
  }
  private static boolean matches(char a, char b, final String[] maybe) {
    for (String s: maybe) {
      int r=matches(a, b, s);
      //Stop if we find out one matches and the other doesn't;
      //or if we find out they both match.
      if (r==2)
        return false;
      else
      if (r==1)
        return true;
    }
    return false;
  }
  private static int matches(char a, char b, String maybe) {
    boolean ba=maybe.indexOf(a)!=-1,
            bb=maybe.indexOf(b)!=-1;
    if (!ba && !bb) return 0;
    else
    if (ba  &&  bb) return 1;
    else            return 2;
  }

  public static void main(String[] args) throws Exception {
    String[] maybe={"abcdef", "ABCDEF", "12345", ")(*&^"};
    char a1=(char)System.in.read(), a2=(char)System.in.read();
    System.out.println(matches(""+a1, ""+a2));
  }
}