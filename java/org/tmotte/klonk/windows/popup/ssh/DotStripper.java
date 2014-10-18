package org.tmotte.klonk.windows.popup.ssh;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
class DotStripper {
  static Pattern ripDots=Pattern.compile("(^\\./\n|^\\.\\./\n)", Pattern.MULTILINE|Pattern.DOTALL);
  
  public static String process(String input) {
    Matcher matcher=ripDots.matcher(input);
    StringBuilder result=new StringBuilder();
    int offset=0;
    while (matcher.find()) {
      result.append(
        input.substring(offset, matcher.start())
      );
      offset=matcher.end();
    }  
    result.append(input.substring(offset, input.length()));
    return result.toString();
  }
  
  //////////////
  // TESTING: //
  //////////////
  
  private static void test(String... args) {
    StringBuilder sb=new StringBuilder();
    for (String s: args) {
      sb.append(s);
      sb.append("\n");
    }
    System.out.println("\n\nStarting with:\n>"+sb.toString()+"< becomes:");
    System.out.println(">"+process(sb.toString())+"<");
  }
  public static void main(String[] args) {
    test("../", "./", "dir1/", "file1", "file2");
    test("../", "./");
    test("dir1/", "file1", "../", "dir2/", "./");
    test("dir1/", "file1", "dir2/");
  }
  
}
