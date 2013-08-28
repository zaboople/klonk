package org.tmotte.common.text;
import java.util.regex.*;

/**
 * Provides the ability to &quot;walk&quot; through a String
 * using <code>find()</code> methods, parsing found &quot;chunks&quot; one at a time. 
 * Interally, there is a text buffer and a pointer to a position in that buffer;
 * the pointer moves as <code>find()</code> executes.
 */
public class StringChunker{

  private String text;
  private int index=0;
  private int foundAt=-1;
  private String upTo, found;
  private Pattern regex;
  private Matcher matcher;
  

  //////////////////////////
  // CONSTRUCTORS AND     //
  // PSEUDO-CONSTRUCTORS: //
  //////////////////////////

  /** 
   * Creates a StringChunker ready to use. 
   * @param text The String to be parsed.
   */
  public StringChunker(String text){
    reset(text);
  }
  /**
   * Creates an uninitialized StringChunker. Invoke <code>reset(String)</code> to
   * initialize it.
   */
  public StringChunker(){
  }

  /**
   * Sets the internal index back to 0, as if find()
   * had never been invoked.
   */
  public StringChunker reset(){
    index=0;
    upTo=null;
    found=null;
    return this;
  }
  /**
   * Reinitializes this StringChunker with new text. This is slightly
   * better than creating a new instance for memory's sake. Preserves
   * any regex set via setRegex(), however.
   */
  public StringChunker reset(String text){
    reset();
    if (text==null)
      throw new IllegalStateException("Text provided was null");
    this.text=text;
    matcher=null;
    upTo=null;
    found=null;
    return this;
  }

  ///////////////////////
  // BASIC PROPERTIES: //
  ///////////////////////

  /** 
   * @return The entire internal buffer
   */
  public String getText(){
    return text;
  }

  ////////////////////////////
  // INDEX STATE RETRIEVAL: //
  ////////////////////////////
  
  /** 
   * @return The current index into the internal buffer.
   *   When getRest() is invoked, this will be the
   *   buffer length.
   */
  public int getIndex(){
    return index;
  }
  /** 
   * @return The index of where the last find() found
   *   something.
   */
  public int foundAt(){
    return foundAt;
  }
  /**
   * @return Whether the internal index has advanced
   *   to the end of the internal buffer.
   */
  public boolean finished(){
    return index>=text.length();
  }  

  /////////////////////////
  // INDEX MANIPULATION: //
  /////////////////////////

  /** Moves the internal buffer index forward the specified distance.*/
  public void move(int distance){
    index+=distance;
  }
  /** Sets the internal buffer index to a specific position.*/
  public void setIndex(int index){
    this.index=index;
  }

  ///////////////////
  // FIND METHODS: //
  ///////////////////
  
  /** 
   * Finds the given substring, starting at the current index, and sets
   * the internal index to the location directly following the text found.
   * Invoke getUpTo(), getIncluding(), getFound() etc. to retrieve
   * the text found and text relative to its location.
   * @return true if lookFor was located. 
   */    
  public boolean find(String lookFor) {
    upTo=null;
    found=null;
    if (!rangeCheck())
      return false;
    else {      
      foundAt=text.indexOf(lookFor, index);      
      if (foundAt<0) {
        upTo=null;
        found=null;
      } else {
        upTo=text.substring(index,foundAt);
        int endAt=foundAt+lookFor.length();
        index=endAt;
        found=text.substring(foundAt,endAt);
      }
      return foundAt > -1;
    }
  } 
  /**
   * Makes it easier to deal with end-of-String vs. text-found conditions in loops.
   * @return true if lookFor is found, or if  
   * not found but the internal index was still before the end of the
   * text. In the latter case, the next call to getUpTo() will return 
   * the remaining text, and further calls to findOrFinish() will return false.
   */
  public boolean findOrFinish(String lookFor) {
    if (finished())
      return false;
    else
    if (find(lookFor))
      return true;
    else{
      upTo=text.substring(index, text.length());
      index=text.length();
      found="";
      return true;
    }
  }
  
  
  /** 
   * Use this before calling find() with no arguments. 
   * @param regex This will be used for subsequent calls to find() and findOrFinish()
   *              until setRegex is called again. This is somewhat more efficient
   *              than find(Pattern) because it will also reuse the internal Matcher
   *              object instead of creating a new one each time.
   */
  public StringChunker setRegex(Pattern regex){
    this.regex=regex;
    this.matcher=null;
    return this;
  }
  
  public boolean find() {
    if (matcher==null) {
      if (regex==null) throw new RuntimeException("Must call setRegex() first");
      matcher=regex.matcher(text);
    }
    return find(matcher);
  }
  /**
   * Does the same as <code>find(String)</code>, but using a regular expression.
   */
  public boolean find(Pattern regex){
    upTo=null;
    found=null;
    foundAt=-1;
    return rangeCheck() && find(regex.matcher(text));      
  }
  private boolean find(Matcher matcher) {
    boolean worked=matcher.find(index);
    if (worked) {
      foundAt=matcher.start();
      int endAt=matcher.end();
      upTo=text.substring(index,foundAt);
      found=text.substring(foundAt,endAt);
      index=endAt;
    } else {
      upTo=null;
      found=null;
    }
    return worked;
  }
  
  /**
   * Does the same as <code>findOrFinish(String)</code>, but using a regular expression.
   */
  public boolean findOrFinish(java.util.regex.Pattern regex){
    return findOrFinishInternal(regex);
  }
  /** 
   * Must call setRegex() first; slightly more efficient than findOrFinish(Pattern) 
   * because it reuses the same regular expression for each invocation.
   */
  public boolean findOrFinish(){
    if (regex==null)
      throw new RuntimeException("Must call setRegex() first");
    return findOrFinishInternal(null);
  }

  private boolean findOrFinishInternal(java.util.regex.Pattern regex){
    if (finished())
      return false;
    else
    if (regex==null && find())
      return true;
    else
    if (regex!=null && find(regex))
      return true;
    else{
      upTo=text.substring(index, text.length());
      index=text.length();
      found="";
      return true;
    }
  }

  
  ////////////////////////
  // "CHUNK" RETRIEVAL: //
  ////////////////////////
  
  /** 
   * Obtains the text <i>before</i> the text found by <code>find()</code>.
   * @return The text between text found by the last successful find() and the successful 
   *   find() before that; or, if there has been only one successful find(), the text between 
   *   position 0 and the text found by that find(). 
   */
   public String getUpTo(){
    return upTo;
  }
  /** 
   * Obtains the text that was found by <code>find()</code>.
   * @return text located during last execution of find() 
   */
  public String getFound() {
    return found;
  }
  /** 
   * Combines the results of <code>getUpTo()</code. and <code>getFound()</code>.
   * @return getUpTo() + getFound() 
   */
  public String getIncluding(){
    return getUpTo()+getFound();
  }
  /** 
   * Provides all the text up to the current position of the internal pointer; i.e. everything
   * up to and <i>including</i> the last item found.
   */
  public String getEverythingSoFar(){
    StringBuilder sb=new StringBuilder();
    getEverythingSoFar(sb);
    return sb.toString();
  }
  private void getEverythingSoFar(StringBuilder sb){    
    sb.append(
      finished() ?text :text.substring(0, index)
    );    
  }

  ////////////////////////////
  // COMBINATION FIND/CHUNK //
  // STATE RETRIEVAL:       //
  ////////////////////////////

  /** 
   * Combines find() and getUpTo(). 
   * @return text located during the find() 
   */
  public String getUpTo(String lookFor) {
    find(lookFor);
    return getUpTo();
  }
  /** 
   * Combines find() and getIncluding(). 
   * @return text located during the find() 
   */
  public String getIncluding(String lookFor) {
    find(lookFor);
    return getIncluding();
  }
  /**  
   * Moves index to end of buffer and retrieves rest of text.
   * @return Remainder of buffer, or "" if nothing is left. 
   */
  public String getRest(){
    if (finished())
      return "";
    else {
      String result=text.substring(index);
      index=text.length();
      return result;
    }
  }
  /** 
   * If last find() failed, invokes getRest(), else getUpTo().
   */
  public String getUpToOrGetRest(){
    if (upTo==null)
      return getRest();
    else
      return upTo;
  }
  /** 
   * Combines find() with getUpToOrGetRest(). 
   */
  public String getUpToOrGetRest(String val){
    find(val);
    return getUpToOrGetRest();
  }


  ////////////////
  // INTERNALS: //
  ////////////////

  private boolean rangeCheck(){
    return text!=null && index<text.length();    
  } 


  ///////////////////
  // TEST HARNESS ///
  ///////////////////


  public static void main(String[] args)throws Exception{
    if (args.length==0 || args[0].equals("-help")){
      System.out.println("Call with two args: 1) A string to search 2) A regex to search with.");
      return;
    }
    Pattern regex=Pattern.compile(args[1]);
    
    
    {
      System.out.println("\n1. Testing using setRegex(): ");
      StringChunker sc=new StringChunker(args[0]);
      sc.setRegex(regex);
      //Test 1:
      System.out.println("\nTest 1.1:");
      while (sc.find()){
        rip("upto:", sc.getUpTo());
        rip("found:", sc.getFound());
      }
      System.out.println("Leftovers:");
      rip("rest:", sc.getRest());
  
      //Test 2:
      System.out.println("\nTest 1.2:");
      sc.reset();
      while (sc.findOrFinish()){
        rip("upto:", sc.getUpTo());
        rip("found:", sc.getFound());
      }
    }
    {
      System.out.println("\n2. Testing using find(regex) methods: ");
      StringChunker sc=new StringChunker(args[0]);
      //Test 3:
      System.out.println("\nTest 2.1:");
      while (sc.find(regex)){
        rip("upto:", sc.getUpTo());
        rip("found:", sc.getFound());
      }
      System.out.println("Leftovers:");
      rip("rest:", sc.getRest());
  
      //Test 2:
      System.out.println("\nTest 2.2:");
      sc.reset();
      while (sc.findOrFinish(regex)){
        rip("upto:", sc.getUpTo());
        rip("found:", sc.getFound());
      }
    }
  }  

  private static void rip(String s1, String s2){
    System.out.println(s1+">"+s2+"<");
  }

}