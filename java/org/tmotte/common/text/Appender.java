package org.tmotte.common.text;
/**
 * This is a companion class to Appendable. By implementing this interface, a class makes it known
 * that it can print directly to an Appendable. This is useful when a class is designed to be
 * "printable", but implementing toString() would force a great deal of String concatenation. For example:
 <pre>
   public class MyClass implements Appender {
     String a, b, c, d, e, f;

     //Less efficient:
     public void toString() {
       StringBuilder sb=new StringBuilder();
       sb.append(a);
       sb.append(b);
       ...
       sb.append(f);
       return sb.toString();//Creates a large String, wasting memory.
     }
     
     //More efficient:
     public void appendTo(Appendable app) {
       app.append(a);
       app.append(b);
       ...
       app.append(f);//No additional Strings created.
     }
   }
 </pre>
 */
public interface Appender {
  /** 
   * When invoked, the Appender should print itself to the Appendable.
   */
  public void appendTo(Appendable appendable) throws java.io.IOException;
}