package org.tmotte.klonk.edit;
public class Indenter {

  // Config:
  boolean tabIndents=false;
  int tabSize=1;
  int spaceIndentLen=1;

  // State:
  private int startLen;

  // Externally used state:
  public StringBuilder buffer;
  public boolean blank=true;
  public boolean anyChange=false;
  public int endPos;
  public int pastBlock;
  public int lenChange;


  public void repair(StringBuilder lineStr) {
    init(lineStr);
  }

  public void init(String lineStr) {
    init(new StringBuilder(lineStr));
  }

  public void init(StringBuilder buffer) {
    this.buffer=buffer;
    startLen=buffer.length();
    blank=false;
    anyChange=false;
    endPos=-1;
    pastBlock=0;
    lenChange=0;

    final int blockSize=tabIndents ?tabSize :spaceIndentLen;
    for (int i=0; i<buffer.length(); i++) {
      char c=buffer.charAt(i);
      System.out.println("CHAR ->"+c+"<-");
      if (c=='\t') {
        if (tabIndents) {
          if (pastBlock!=0) {
            anyChange=true;
            buffer.delete(i-pastBlock, i);
            i-=pastBlock;
          }
        } else {
          System.out.println("YANK TAB");
          anyChange=true;
          buffer.deleteCharAt(i);
          final int need=tabSize-pastBlock;
          for (int j=0; j<need; j++)
            buffer.insert(i, ' ');
          i+=need-1;
        }
        pastBlock=0;
      }
      else
      if (c==' ')
        pastBlock=(pastBlock==blockSize-1) ?0 :pastBlock+1;
      else {
        endPos=i;
        break;
      }
    }
    if (endPos==-1) {
      blank=true;
      endPos=buffer.length();
    }
  }

  public void indent(boolean remove, boolean fitToBlock) {
    if (fitToBlock && pastBlock > 0) {
      if (remove)
        trimPastBlock();
      else {
        final int blockSize=tabIndents ?tabSize :spaceIndentLen;
        if (tabIndents) {
          trimPastBlock();
          buffer.insert(endPos++, '\t');
        }
        else
          while (pastBlock++ < blockSize)
            buffer.insert(endPos++, ' ');
      }
      anyChange=true;
    }
    else
    if (remove) {
      if (buffer.length()>0) {
        if (tabIndents) {
          if (buffer.charAt(0)==' ')
            deleteFirstChars(tabSize);
          else
          if (buffer.charAt(0)=='\t')
            deleteFirstChar();
        }
        else
        if (buffer.charAt(0)=='\t')
          deleteFirstChar();
        else
        if (buffer.charAt(0)==' ')
          deleteFirstChars(spaceIndentLen);
      }
    }
    else
    if (tabIndents)
      insertFirstChar('\t');
    else
      for (int i=0; i<spaceIndentLen; i++)
        insertFirstChar(' ');
    lenChange=buffer.length()-startLen;
  }

  private void insertFirstChar(char c) {
    buffer.insert(0, c);
    endPos++;
    anyChange=true;
  }
  private void deleteFirstChar() {
    buffer.deleteCharAt(0);
    endPos--;
    anyChange=true;
  }
  private void deleteFirstChars(int len) {
    buffer.delete(0, len);
    endPos-=len;
    anyChange=true;
  }
  private void trimPastBlock() {
    buffer.delete(endPos-pastBlock, endPos);
    endPos-=pastBlock;
    anyChange=true;
  }
}
