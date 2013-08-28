package org.tmotte.klonk.config;
import org.tmotte.common.swang.Fail;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class LineDelimiterOptions {


  public final static int CR=0, LF=1, CRLF=2;


  public final static String 
    LFs=new String(new char[]{10}), 
    CRs=new String(new char[]{13}); 
  public final static String CRLFs=CRs+LFs;
  public final static Pattern pattern=Pattern.compile("([\\r][\\n]|[\\r]|[\\n])");


  public int defaultOption=CRLF;
  public int thisFile=CRLF;
  
  public LineDelimiterOptions setDefault(String s, Fail failer) {
    defaultOption=translate(s, failer);
    return this;
  }
  public LineDelimiterOptions setThisFile(String s, Fail failer) {
    thisFile=translate(s, failer);
    return this;
  }
  
  public LineDelimiterOptions setDefault(int i, Fail failer) {
    defaultOption=check(i, failer);
    return this;
  }
  public LineDelimiterOptions setThisFile(int i, Fail failer) {
    thisFile=check(i, failer);
    return this;
  }

  public static int detect(String s) {
    Matcher m=pattern.matcher(s);
    if (m.find()){
      int i=translateActual(
        s.substring(m.start(), m.end())
      );
      return i;
    }
    return -1;
  }
  
  ////////////////
  // TRANSLATE: //
  ////////////////
  
  public static int translateActual(String s) {
    if (s==null)         return fail(null, "Null input");
    else
    if (s.equals(CRs))   return CR;
    else
    if (s.equals(CRLFs)) return CRLF;
    else
    if (s.equals(LFs))   return LF;
    else 				return fail(null, "Invalid input: "+s);
  }
  public static String translateActual(int i) {
    switch (i) {
      case CR:   return CRs;
      case CRLF: return CRLFs;
      case LF:   return LFs;
      default:   return failActual(null, i);
    }
  }
  
  
  public static int translate(String s) {
    return translate(s, null);
  }
  public static String translate(int i) {
    return translate(i, null);
  }
  public static int translate(String s, Fail failer) {
    if (s==null)            return fail(failer, "Null input");
    else
    if (s.equals("CR"))     return CR;
    else
    if (s.equals("CR-LF"))  return CRLF;
    else
    if (s.equals("LF"))     return LF;
    else                    return fail(failer, "Invalid input: "+s);
  }
  public static String translate(int i, Fail failer) {
    switch (i) {
      case CR:   return "CR";
      case CRLF: return "CR-LF";
      case LF:   return "LF";
      default:   return fail(failer, i);
    }
  }
  
  /////////////
  // ERRORS: //
  /////////////
  
  private static int check(int i, Fail failer) {
    if (i<CRLF || i>CR)
      return fail(failer, "Invalid numeric input: "+i);
    return i;
  }
  private static int fail(Fail failer, String error) {
    if (failer!=null)
      failer.fail(new RuntimeException(error));
    else
      throw new RuntimeException(error);
    return CRLF;
  }
  private static String fail(Fail failer, int i) {
    fail(failer, "Invalid delimiter option: "+i);
    return "CR-LF";
  }
  private static String failActual(Fail failer, int i) {
    fail(failer, "Invalid delimiter option: "+i);
    return CRLFs;
  }
  
  
  ///////////
  // TEST: //
  ///////////
  
  
  
  /** OK So this works. */
  public static void main(String[] args) throws Exception {
    java.io.InputStreamReader br=new java.io.InputStreamReader(new java.io.FileInputStream(args[0]));
    int charsRead;
    char[] readBuffer=new char[224096];
    while ((charsRead=br.read(readBuffer, 0, readBuffer.length))>0){
      String s=new String(readBuffer, 0, charsRead);
      Matcher m=pattern.matcher(s);
      int i=0;
      while (m.find(i))
        System.out.print(
          s.substring(i, m.start())+
          translate(
            translateActual(
              s.substring(m.start(), i=m.end())
            )
          )
        );

    }
  }
  
}