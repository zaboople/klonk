package org.tmotte.klonk.edit;

/** Not threadsafe, as it doesn't need to be. */
class Selectable {

  final static char LINEFEED=((char)10),
                    TAB=((char)9),
                    SPACE=' ';
  final static String
    strLetterCaps="ABCDEFGHIJKLMNOPQRSTUVWXYZ"
   ,strLetterLow ="abcdefghijklmnopqrstuvwxyz"
   ,strNum       ="0123456789"
   //  Disabled because it annoys me. A better option would be to wait until we have hopped
   //  a good ways, come to a block of special chars, and then can say, hey, you know, we start hopping
   //  from a double-quote, let's finish on a double quote instead of finishing on the parentheses after it.
   //  You know?
   //  ,strStartSection="<{[(\""
   //  ,strEndSection  =">}])\""
   ,strWhite=new String(new char[]{TAB,SPACE})
   ;
  final static String strLetters=strLetterCaps+strLetterLow+strNum+strWhite+new String(new char[]{LINEFEED});

  //I know this is dumb, using static variables as instance variables,
  //but I'm not quite committed to the idea of an object here. Silly me.
  private static String possible;
  private static int len;
  private static boolean shift=false;
  private static void init(String p, boolean sh){
    possible=p;
    len=possible.length();
    shift=sh;
  }


  //////////////////
  //              //
  //  GET RIGHT:  //
  //              //
  //////////////////


  public static int getRight(String possible, boolean shift) {
    init(possible, shift);
    int i=getRight(0);
    return (i==1) ?getRight(1) :i;
  }
  private static int getRight(int startWith) {

    //If we're at the next to last character, done, use it:
    if (startWith>=len-1) return len;

    //Get first char:
    char firstChar=possible.charAt(startWith++);

    //Skip over linefeed:
    if (firstChar==LINEFEED) return getRight(startWith);

    //Now flip thru the possibilities:
    // int brack;
    // if (shift && startWith==1 && (brack=strStartSection.indexOf(firstChar))>-1)
    //   return rightFinishBrack(startWith, firstChar, strEndSection.charAt(brack), 1);
    // else
    if (strWhite.indexOf(firstChar)>-1)
      return rightJump(startWith, strWhite);
    else
    if (strLetterLow.indexOf(firstChar)>-1)
      return rightJump(startWith, strLetterLow);
    else
    if (strNum.indexOf(firstChar)>-1)
      return rightJump(startWith, strNum);
    else
    if (strLetterCaps.indexOf(firstChar)>-1)
      return rightFindLastUpper(startWith);
    else
      return rightJumpSpecialChars(startWith);

  }
  private static int rightFinishBrack(int startWith, char leftChar, char rightChar, int brackCount) {
    //Find end of bracket
    int nextRight=possible.indexOf(rightChar, startWith);

    //Not found? Then just signal to jump special chars.
    if (nextRight==-1)
      return rightJumpSpecialChars(1);

    //For the " & ' characters, right & left are the same:
    if (rightChar==leftChar)
      return nextRight+1;

    //See if we have another left:
    //If we found a right bracket first, either it's the last, or we need
    //to search forwards from there:
    int nextLeft=possible.indexOf(leftChar, startWith);
    if (nextLeft==-1 || nextRight<nextLeft)
      return --brackCount==0
        ?nextRight+1
        :rightFinishBrack(nextRight+1, leftChar, rightChar, brackCount);

    //OK so we found a left that we need to account for, keep searching with count increased:
    return rightFinishBrack(nextLeft+1, leftChar, rightChar, brackCount+1);
  }
  private static int rightJump(int startWith, String accept) {
    for (int i=startWith; i<len; i++)
      if (accept.indexOf(possible.charAt(i))==-1)
        return i;
    return len;
  }
  /** Opposite of right jump */
  private static int rightJumpSpecialChars(int startWith) {
    for (int i=startWith; i<len; i++){
      char ch=possible.charAt(i);
      if (strLetters.indexOf(ch)!=-1)
        return i;
    }
    return len;
  }

  private static int rightFindLastUpper(int startWith) {
    //Looks for last uppercase, but if the first char we find is
    //lowercase, keep going. Yes this is a special case of the general
    //case that we never jump only one char; we do it here because "-Beef"
    //would otherwise stop at the B.
    int i=rightJump(startWith, strLetterCaps);
    return i==startWith && strLetterLow.indexOf(possible.charAt(startWith))!=-1
      ?rightJump(i+1, strLetterLow)
      :i;
  }


  /////////////////
  //             //
  //  GET LEFT:  //
  //             //
  /////////////////


  public static int getLeft(String possible, boolean shift) {
    init(possible, shift);
    int i=getLeft(len-1);
    return (i==len-1) ?getLeft(len-2) :i;
  }
  private static int getLeft(int startWith) {

    //If we're at the next to last character, done, use it:
    if (startWith<=1) return 0;

    //Get first char:
    char firstChar=possible.charAt(startWith--);

    //Skip over linefeed:
    if (firstChar==LINEFEED) return getLeft(startWith);

    //Start a bracket, end a bracket:
    // int brack;
    // if (shift && startWith==len-2 && (brack=strEndSection.indexOf(firstChar))>-1)
    //   return leftFinishBrack(startWith, strStartSection.charAt(brack), firstChar, 1);
    // else
    if (strLetterLow.indexOf(firstChar)!=-1)
      return leftFindLastLower(startWith);
    else
    if (strWhite.indexOf(firstChar)!=-1)
      return leftJump(startWith, strWhite);
    else
    if (strLetterCaps.indexOf(firstChar)!=-1)
      return leftJump(startWith, strLetterCaps);
    else
    if (strNum.indexOf(firstChar)!=-1)
      return leftJump(startWith, strNum);
    else
      return leftJumpSpecialChars(startWith);
  }

  private static int leftFinishBrack(int startWith, char leftChar, char rightChar, int brackCount) {
    //Find left end of bracket, else just jump to end of special chars:
    int nextLeft=indexOfBack(startWith, leftChar);
    if (nextLeft==-1)
      return leftJumpSpecialChars(len-2);

    //For the " & ' characters, right & left are the same:
    if (rightChar==leftChar)
      return nextLeft;

    //See if we have another right:
    //If next left precedes next right, then we may be done, or we keep searching from there.
    //else we up the right count and proceed searching from there, even if there is no left;
    //if there is no left, the prior statement will blow out.
    int nextRight=indexOfBack(startWith, rightChar);
    if (nextLeft>nextRight)
      return --brackCount==0
        ?nextLeft
        :leftFinishBrack(nextLeft-1, leftChar, rightChar, brackCount);
    else
      return leftFinishBrack(nextRight-1, leftChar, rightChar, brackCount+1);
  }
  private static int leftFindLastLower(int startWith) {
    //Allows us to jump past end of last lower
    //*through* preceding uppercase characters (camel-case).
    for (int i=startWith; i>-1; i--){
      char ch=possible.charAt(i);
      if (strLetterLow.indexOf(possible.charAt(i))==-1)
        return strLetterCaps.indexOf(ch)!=-1
          ?leftJump(i-1, strLetterCaps)
          :i+1;
    }
    return 0;
  }
  private static int leftJump(int startWith, String toSearch) {
    for (int i=startWith; i>-1; i--)
      if (toSearch.indexOf(possible.charAt(i))==-1)
        return i+1;
    return 0;
  }
  /** Note that this is the opposite of the other jumpers **/
  private static int leftJumpSpecialChars(int startWith) {
    for (int i=startWith; i>-1; i--){
      char ch=possible.charAt(i);
      if (strLetters.indexOf(ch)!=-1)
        return i+1;
    }
    return 0;
  }

  private static int indexOfBack(int startWith, char lookFor) {
    for (int i=startWith; i>-1; i--)
      if (possible.charAt(i)==lookFor)
        return i;
    return -1;
  }



  //////////////////
  //              //
  // OTHER STUFF: //
  //              //
  //////////////////



  public static void main(String[] args) throws Exception {
    if (args.length==0 || args[1].toLowerCase().contains("-help")){
      System.out.println(
        "Usage: java org.tmotte.klonk.edit.Selectable <-right|-left> [-shift] "
       +"\n  str will be searched for the next spot, from right or left, with shift on or off. "
      );
      return;
    }

    //Left or right or what?
    boolean right=false;
    if (args[0].equals("-right"))
      right=true;
    else
    if (args[0].equals("-left"))
      right=false;
    else{
      System.err.println("Don't know what to do with: "+args[0]);
      return;
    }

    //Shift:
    boolean sh=args.length==1
      ?false
      :args[1].equals("-shift");

    //What is the string
    String s=args[args.length-1];

    //Now do the dirty work:
    if (right)  testRight(s, shift);
    else        testLeft(s, shift);
    System.out.flush();
  }

  public static void testRight(String s, boolean shift) {
    int r=getRight(s, shift);
    System.out.println(""+r+">"+s.substring(0,r)+"<");
  }
  public static void testLeft(String s, boolean shift) {
    int l=getLeft(s, shift);
    System.out.println(""+l+" "+s.length()+">"+s.substring(l,s.length())+"<");
  }

}