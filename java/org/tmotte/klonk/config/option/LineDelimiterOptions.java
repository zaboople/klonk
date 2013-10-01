package org.tmotte.klonk.config.option;
import java.util.regex.Pattern;
import java.util.regex.Matcher;


public class LineDelimiterOptions {


  public final static String 
    LFs=new String(new char[]{10}), 
    CRs=new String(new char[]{13}); 
  public final static String CRLFs=CRs+LFs;
  public final static Pattern pattern=Pattern.compile("([\\r][\\n]|[\\r]|[\\n])");


  public String defaultOption=CRLFs,
                thisFile=CRLFs;
  
  public LineDelimiterOptions setDefault(String s) {
    defaultOption=translateFromReadable(s);
    return this;
  }
  public LineDelimiterOptions setThisFile(String s) {
    thisFile=translateFromReadable(s);
    return this;
  }
  

  public static String detect(String s) {
    Matcher m=pattern.matcher(s);
    if (m.find())
      return s.substring(m.start(), m.end());
    return null;
  }
  
  ////////////////
  // TRANSLATE: //
  ////////////////
  
  public static String translateFromReadable(String s) {
    if (s==null)            fail("Null input");
    else
    if (s.equals("CR"))     return CRs;
    else
    if (s.equals("CR-LF"))  return CRLFs;
    else
    if (s.equals("LF"))     return LFs;
    else                    fail("Invalid input: "+s);
    return null;
  }
  public static String translateToReadable(String s) {
    if (s==null)
      fail("Received null");
    else
    if (s.equals(CRs))  return "CR";
    else
    if (s.equals(CRLFs))return "CR-LF";
    else
    if (s.equals(LFs))  return "LF";
    else
      fail("Invalid translation from actual: "+s);
    return null;
  }
  
  /////////////
  // ERRORS: //
  /////////////
  
  private static void fail(String error) {
    throw new RuntimeException(error);
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
          translateFromReadable(
            translateToReadable(
              s.substring(m.start(), i=m.end())
            )
          )
        );

    }
  }
  
}