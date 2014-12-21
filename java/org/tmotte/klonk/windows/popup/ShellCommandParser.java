package org.tmotte.klonk.windows.popup;
import java.util.List;
import java.io.File;
import java.util.LinkedList;
import org.tmotte.common.text.StringChunker;
import java.util.regex.Pattern;

class ShellCommandParser {
  static Pattern delimiterPattern=Pattern.compile("(\"|'|\\p{Blank})");
  static String currFileMarker="[$1]";
  public static List<String> parse(String cmd) {
    return parse(cmd, null);
  }
  public static List<String> parse(String cmd, String currFileName) {
    List<String> results=new LinkedList<>();
    System.out.println("FUCKING "+cmd+" "+currFileName);
    if (referencesCurrFile(cmd) & currFileName!=null)
      cmd=cmd.replace(currFileMarker, currFileName);
    System.out.println("FUCKING "+cmd);

    //Maybe it's just one big command that references a real file, spaces or no spaces -
    //like C:\program files\booger hooger\foo.exe:
    if (new File(cmd).exists()){
      results.add(cmd);
      return results;
    }

    //See if we can parse out an actual command that has spaces in it, and parameters after that -
    //because stupid people put programs in C:\Program Files:
    StringChunker sc=new StringChunker(cmd);
    boolean foundProgram=false;
    String execute="";
    while (sc.find(" ")) {
      execute+=sc.getUpTo();
      if (new File(execute).exists()){
        results.add(execute);
        return getProgramArguments(results, sc);
      }
      execute+=sc.getFound();
    }

    //If we never found a blank, this was definitely a straight command with no arguments
    //even though it doesn't point to an actual file, like say "ls" or "ps"
    if (execute.equals("")){
      results.add(cmd);
      return results;
    }
    
    //OK then let's just assume the first blank ends the program, even the program doesn't seem to exist, 
    //like "ps -aux". Then everything else is an argument, isn't it:
    sc.reset(cmd);
    results.add(sc.getUpTo(" "));
    return getProgramArguments(results, sc);
  }
    
  private static boolean referencesCurrFile(String cmd) {
    return cmd.indexOf(currFileMarker)!=-1;
  }
    
  private static List<String> getProgramArguments(List<String> results, StringChunker sc){

    while (sc.find(delimiterPattern)){
      String found=sc.getFound();
      if (found.equals("\"")){
        if (!sc.find("\""))
          throw new RuntimeException("You appear to be missing a trailing \" character");
        results.add(sc.getUpTo());
      }
      else
      if (found.equals("'")){
        if (!sc.find("'"))
          throw new RuntimeException("You appear to be missing a trailing ' character");
        results.add(sc.getUpTo());
      }
      else {
        String r=sc.getUpTo().trim();
        if (!r.equals(""))
          results.add(r);
      }
    }
    if (!sc.finished())
      results.add(sc.getRest());
    return results;
  }
  
  ///////////
  // TEST: //
  ///////////
  
  public static void main(String[] args) {
    String 
      command=args[0],
      currFileName=args.length>1
        ?args[1] :null;
    List<String> result=parse(args[0], currFileName);
    for (String s: result)
      System.out.println("-->"+s);
  }
}