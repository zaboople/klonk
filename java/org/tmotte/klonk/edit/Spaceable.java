package org.tmotte.klonk.edit; 
public class Spaceable {

  final static char LINEFEED=((char)10),
                    TAB=((char)9),
                    SPACE=' ';
  final static String
    strRegular="ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789"  
   ,strWhite  =new String(new char[]{TAB,SPACE})
   ;
  final static String
    strRegularWhite=strRegular+strWhite;


  private static String possible;
  private static int len;
  private static void init(String p){
    possible=p;
    len=possible.length();
  }
  
  
  ////////////////// 
  //              //
  //  GET RIGHT:  //
  //              //
  //////////////////
  
                    
  public static int getRight(String possible) {
    init(possible);
    return getRight(0);
  }
  private static int getRight(int startWith) {
    
    //If we're at the next to last character, done, use it:
    if (startWith>=len-1) return len;
      
    //Get first char and flip thru:  
    char firstChar=possible.charAt(startWith++);
    if (strWhite.indexOf(firstChar)>-1)
      return rightJump(startWith, strWhite);
    else
    if (strRegular.indexOf(firstChar)>-1)
      return rightJump(startWith, strRegular);
    else 
      return rightJumpSpecialChars(startWith);
  }
  private static int rightJump(int startWith, String accept) {
    for (int i=startWith; i<len; i++)
      if (accept.indexOf(possible.charAt(i))==-1)
        return i;
    return len;
  }
  private static int rightJumpSpecialChars(int startWith) {
    for (int i=startWith; i<len; i++){
      char ch=possible.charAt(i);
      if (strRegularWhite.indexOf(ch)!=-1)
        return i;
    }
    return len;
  }
  
  /////////////////
  //             //
  //  GET LEFT:  //
  //             //
  /////////////////

  
  public static int getLeft(String possible) {
    init(possible);
    return getLeft(len-1);
  }
  private static int getLeft(int startWith) {
 
    //If we're at the next to last character, done, use it:
    if (startWith<=1) return 0;

    //Get first char and flip through:  
    char firstChar=possible.charAt(startWith--);
    if (strWhite.indexOf(firstChar)!=-1)
      return leftJump(startWith, strWhite);
    else
    if (strRegular.indexOf(firstChar)!=-1)
      return leftJump(startWith, strRegular);
    else 
      return leftJumpSpecialChars(startWith);
  }
  
  private static int leftJump(int startWith, String toSearch) {
    for (int i=startWith; i>-1; i--)
      if (toSearch.indexOf(possible.charAt(i))==-1)
        return i+1;
    return 0;
  }
  private static int leftJumpSpecialChars(int startWith) {
    for (int i=startWith; i>-1; i--){
      char ch=possible.charAt(i);
      if (strRegularWhite.indexOf(ch)!=-1)
        return i+1;
    }
    return 0;
  }

  

  //////////////////
  //              //
  // OTHER STUFF: //
  //              //
  //////////////////



  public static void main(String[] args) throws Exception {
    if (args.length==0 || args[1].toLowerCase().contains("-help")){
      System.out.println(
        "Usage: java org.tmotte.klonk.edit.Spaceable <-right|-left>  "
       +"\n  str will be searched for the next spot, from right or left, with backwards on or off. "
      );
      return;
    }
    
    //Left or right or what?
    boolean right=false;
    if (args[0].equals("-right")) right=true;
    else
    if (args[0].equals("-left"))  right=false;
    else{
      System.err.println("Don't know what to do with: "+args[0]);
      return;
    }  
  
    //What is the string
    String s=args[args.length-1];
      
    //Now do the dirty work:  
    if (right)  testRight(s);
    else        testLeft(s);
    System.out.flush();
  }
  
  public static void testRight(String s) {
    int r=getRight(s);
    System.out.println(""+r+">"+s.substring(0,r)+"<");
  }
  public static void testLeft(String s) {
    int l=getLeft(s);
    System.out.println(""+l+" "+s.length()+">"+s.substring(l,s.length())+"<");
  }
  
}