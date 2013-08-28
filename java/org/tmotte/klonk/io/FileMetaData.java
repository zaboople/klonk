package org.tmotte.klonk.io;

public class FileMetaData {
  public static String UTF8="UTF-8",
                       UTF16BE="UTF-16BE",
                       UTF16LE="UTF-16LE";
  public int delimiter;
  public boolean hasTabs;
  public String encoding;
  public boolean encodingNeedsBOM;
  public int readOffset;
}