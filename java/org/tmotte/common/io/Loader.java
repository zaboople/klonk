package org.tmotte.common.io;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.BufferedReader;

/**
 * A convenience class for loading data from InputStreams.
 */
public class Loader {
  public static String loadUTF8String(java.lang.Class c, String fileName) {
    try {
      java.io.InputStream is=c.getResourceAsStream(fileName);    
      return loadUTF8String(is);
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
  /** 
   * Invokes loadString() with approxlen parameter of 512.
   */
  public static String loadUTF8String(java.io.InputStream ios) throws Exception{
    return loadString(ios, "utf-8", 512);
  }

  /** 
   * Loads a String from an input stream.
   * @param approxlen The size of the byte buffer to use when loading. 
   */
  public static String loadString(java.io.InputStream ios, String encoding, int approxlen) throws Exception{
    try {
      InputStreamReader br=new InputStreamReader(ios, encoding);
      StringBuffer buffer=new StringBuffer();
      char[] readBuffer=new char[Math.min(4096, approxlen)];
      try {
       int charsRead;
       while ((charsRead=br.read(readBuffer, 0, readBuffer.length))>0)
         buffer.append(readBuffer, 0, charsRead);
      } finally {
       ios.close();
      }    
      return buffer.toString();
    } finally {
      ios.close();
    }
  }

  /** 
   * Creates a byte array of the specifed size and loads it from the given InputStream. The stream
   * will be closed at completion, even if there are more bytes to read.
   */
  public static byte[] loadBytes(java.io.InputStream ios, int size) throws Exception{
    try {
      byte[] bytes=new byte[size];
      int off=0;
      while (off<size){
        int x=ios.read(bytes, off, size-off);
        if (x<0)
          break;
        off+=x;
      }
      return bytes;
    } finally {
      ios.close();
    }
  }
  /** 
   * A shortcut to loadBytes(InputStream, int) that uses URL.openStream() to get the InputStream.
   */
  public static byte[] loadBytes(java.net.URL url, int size) throws Exception{
    return loadBytes(url.openStream(), size);
  }
  /** 
   * A shortcut to loadBytes(InputStream, int) that uses URLConnection.getInputStream() to get the stream, 
   * and URLConnection.getContentLength() to get the size of the byte array. 
   * @throws RuntimeException if URLConnection.getContentLength() returns 0, which happens with some
   * poorly supported URL types, depending on platform.
   */
  public static byte[] loadBytes(java.net.URL url) throws Exception{
    java.net.URLConnection conn=url.openConnection();
    int size=conn.getContentLength();
    if (size==0)
      throw new RuntimeException("URLConnection.getContentLength() returned 0");
    return loadBytes(conn.getInputStream(), size);
  }
  public static void main(String[] args) throws Exception {
    loadBytes(System.in, 10);
  }
}