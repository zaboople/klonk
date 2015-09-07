package org.tmotte.klonk.windows.popup;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.KeyStroke;
import javax.swing.SwingConstants;
import javax.swing.text.Caret;
import javax.swing.text.Document;
import javax.swing.text.Position;
import javax.swing.text.Segment;

class Finder {
  //Results:
  public String replaceResult;
  public String lastError;

  private int location=-1, locationEnd=-1;

  //Inputs held as instance variables for convenience:
  private Document doc;
  private int offset;
  private boolean replaceOn;
  private String replaceWith;
  //Pattern & matcher preserved for multiple invocations:
  private Pattern pattern;
  private Matcher matcher;

  public int getEnd(){return locationEnd;}
  public int getStart() {return location;}
  
  public void reset() {
    pattern=null;
    matcher=null;
  }
  public Finder setDocument(Document doc, int offset) {
    this.doc=doc;
    this.offset=offset;
    return this;
  }
  public Finder setReplace(boolean replaceOn, String replaceWith) {
    this.replaceOn=replaceOn;
    this.replaceWith=replaceWith;
    return this;
  }
  
  public boolean find(
      String searchFor, 
      boolean forwards, 
      boolean caseSensitive, 
      boolean regex,
      boolean multiline
    ) {
    replaceResult=null;
    location=-1;
    locationEnd=-1;

    String searchIn;
    try {
      searchIn=forwards
        ?doc.getText(offset, doc.getLength()-offset)
        :doc.getText(0, offset);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
    if (regex)
      findRegex(  searchFor, searchIn, forwards, caseSensitive, multiline);
    else
      findRegular(searchFor, searchIn, forwards, caseSensitive);
    return location!=-1;
  }
  private void findRegular(String searchFor, String searchIn, boolean forwards, boolean caseSensitive) {
    if (caseSensitive) 
      location=forwards 
        ?searchIn.indexOf(searchFor)
        :searchIn.lastIndexOf(searchFor);
    else {
      //We search both low & up case because apparently
      //there are character sets where this is the only thing
      //that works. Turkish or something. Whatever.
      searchFor=searchFor.toLowerCase();
      String searchInLow=searchIn.toLowerCase();
      location=forwards ?searchInLow.indexOf(searchFor)
                        :searchInLow.lastIndexOf(searchFor);
      searchFor=searchFor.toUpperCase();
      String searchInHi=searchIn.toUpperCase();
      int loc =forwards ?searchInHi.indexOf(searchFor)
                        :searchInHi.lastIndexOf(searchFor);
      if (location==-1 || (loc!=-1 && loc<location))
        location=loc;
    }
    locationEnd=location==-1 ?-1 :location+searchFor.length();
    replaceResult=replaceWith;
  }
  private void findRegex(
      String searchFor, String searchIn, boolean forwards, boolean caseSensitive, boolean multiline
    ) {
    if (pattern==null){
      int flags=multiline 
        ?(Pattern.MULTILINE|Pattern.DOTALL)
        :0;
      if (!caseSensitive)
        flags|=Pattern.CASE_INSENSITIVE;
      try {
        pattern=Pattern.compile(searchFor, flags);
      } catch (PatternSyntaxException e) {
        lastError="Regex syntax is wrong: "+e.getMessage();
        return;            
      }
      matcher=pattern.matcher(searchIn);
    }
    else
      matcher.reset(searchIn);
    if (matcher.find()){
    
      //This is kooky but we have to go all the way
      //from start to end to get last match with regex.
      do {
        location=matcher.start();
        locationEnd=matcher.end();
      } while (!forwards && matcher.find());
      
      if (replaceOn) {
        
        //Follow thru on the kooky part above:
        if (!forwards)
          matcher.find(location);
          
        //Getting the replacement is dumb. We have no choice but to let it give
        //us all the crap we don't need so we can grab the good part at the end.
        StringBuffer sb=new StringBuffer();
        try {
          matcher.appendReplacement(sb, replaceWith);
        } catch (IllegalArgumentException e) {
          lastError="Replacement syntax is wrong (typically caused by $ characters):\n"
                    +e.getMessage();
          return;
        } catch (IndexOutOfBoundsException e) {
          lastError="Replacement syntax is wrong (typically caused by $ characters):\n"
                    +e.getMessage();
          return;
        }
        replaceResult=sb.substring(location);
      }
    }
  }
  
}//end inner class


