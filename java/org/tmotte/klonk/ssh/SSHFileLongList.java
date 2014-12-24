package org.tmotte.klonk.ssh;
import java.io.File;
import java.io.FilenameFilter;
import java.io.FileFilter;
import java.nio.file.Path;
import org.tmotte.common.text.StringChunker;
import java.util.List;
import java.util.LinkedList;
import java.util.regex.Pattern;

class SSHFileLongList  {
  
  private static Pattern blankPattern=Pattern.compile(" +");
  public final String perms;
  public final String size;
  public final String month;
  public final String day;
  public final String yearTime;
  public final boolean isDir;

  SSHFileLongList(String value){
    isDir=value.startsWith("d");
    StringChunker sc=new StringChunker(value);
    perms=sc.getUpTo(blankPattern);
    sc.find(blankPattern);//Weird #
    sc.find(blankPattern);//user
    sc.find(blankPattern);//group
    size=sc.getUpTo(blankPattern);
    month=sc.getUpTo(blankPattern);
    day=sc.getUpTo(blankPattern);
    yearTime=sc.getUpTo(blankPattern);
  }
  public String toString() {
    return perms+" "+size;
  }

  /////////////
  //  TEST:  //
  /////////////
  
  public static void main(String[] args) throws Exception {
    System.out.println(new SSHFileLongList(args[0]));
  }
}
