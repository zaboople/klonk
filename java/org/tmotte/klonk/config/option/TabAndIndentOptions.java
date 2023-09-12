package org.tmotte.klonk.config.option;
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
  public boolean inferTabIndents;
  public int tabSize;
  public int indentSpacesSize;
  public boolean indentSpacesSizeMatchTabs;
  public int indentionMode;
  public int indentionModeDefault;

  public String toString() {
    return
      "indentOnHardReturn: "+indentOnHardReturn
     +"\ntabIndentsLine: "+tabIndentsLine
     +"\ninferTabIndents: "+inferTabIndents
     +"\nindentionMode: "+indentionMode
     +"\nindentionModeDefault: "+indentionModeDefault
     +"\nindentSpacesSize: "+indentSpacesSize
     +"\nindentSpacesSizeMatchTabs: "+indentSpacesSizeMatchTabs
     +"\ntabSize: "+tabSize;
  }

}
