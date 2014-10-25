package org.tmotte.klonk.config.option;
import org.tmotte.common.swang.Fail;
import java.util.regex.Pattern;
import java.util.regex.Matcher;

public class TabAndIndentOptions {

  //JTextArea.setTabSize() is the function to call.
  public static int INDENT_TABS=1, INDENT_SPACES=2;
  public String buildIndentSpaces() {
    String s="";
    s="";
    for (int i=0; i<indentSpacesSize; i++)
      s+=" ";
    return s;
  }
  
  public boolean indentOnHardReturn;
  public boolean tabIndentsLine;
  public int indentionMode, 
             indentionModeDefault;
  public int indentSpacesSize, 
             tabSize;
             
  public String toString() {
    return 
      "indentOnHardReturn: "+indentOnHardReturn
     +"\ntabIndentsLine: "+tabIndentsLine
     +"\nindentionMode: "+indentionMode
     +"\nindentionModeDefault: "+indentionModeDefault
     +"\nindentSpacesSize: "+indentSpacesSize
     +"\ntabSize: "+tabSize;
  }

}
  