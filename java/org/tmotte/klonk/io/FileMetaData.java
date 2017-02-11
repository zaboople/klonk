package org.tmotte.klonk.io;

public class FileMetaData {
  public static String UTF8="UTF-8",
                       UTF16BE="UTF-16BE",
                       UTF16LE="UTF-16LE";
  public String delimiter;
  public boolean hasTabs=false;
  public String encoding=UTF8;
  public boolean encodingNeedsBOM=false;
  public int readOffset=0;
  public EncryptionParams encryption=null;
}