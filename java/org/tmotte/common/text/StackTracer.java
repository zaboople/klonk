package org.tmotte.common.text;
import java.io.PrintWriter;
import java.io.OutputStreamWriter;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
/** Converts a java stack trace to a String as well as to Strings written to an Appendable.*/
public class StackTracer {
  public static String getStackTrace(Throwable t) {
    ByteArrayOutputStream b=new ByteArrayOutputStream();
    PrintWriter pw=new PrintWriter(b);
    t.printStackTrace(pw);
    pw.flush();
    return b.toString();
  }
  public static String recurseStackTrace(Throwable t) {
    StringBuilder sb=new StringBuilder();
    recurseStackTrace(t, sb);
    return sb.toString();
  }
  public static void getStackTrace(Throwable t, Appendable s) {
    try {
      s.append(getStackTrace(t));
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  public static void recurseStackTrace(Throwable t, Appendable s) {
    try {
      getStackTrace(t, s);
      Throwable t2=t.getCause();
      if (t2!=null)
        recurseStackTrace(t2, s);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }

}