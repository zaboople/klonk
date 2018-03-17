package org.tmotte.klonk.edit;
public class Indenter {

  // Config:
  boolean tabIndents=false;
  int tabSize=1;
  int spaceIndentLen=1;

  // State:
  boolean anyChange=false;
  int viewLen=0;
  StringBuilder buffer;
  int endPos;
  int pastBlock;

  public void init(String lineStr) {
    anyChange=false;
    viewLen=0;
    endPos=-1;
    pastBlock=0;

    final int blockSize=tabIndents ?tabSize :spaceIndentLen;
    buffer=new StringBuilder(lineStr);
    for (int i=0; i<buffer.length(); i++) {
      char c=buffer.charAt(i);
      if (c=='\t') {
        if (pastBlock==0)
          viewLen+=tabSize;
        else
        if (tabIndents) {
          anyChange=true;
          buffer.delete(i-pastBlock, i);
          i-=pastBlock;
          viewLen=tabSize + viewLen - pastBlock;
          pastBlock=0;
        }
        else {
          anyChange=true;
          buffer.deleteCharAt(i);
          for (int j=0; j<blockSize-pastBlock; j++)
            buffer.insert(i, ' ');
          viewLen++;
          pastBlock++;
        }
      }
      else
      if (c==' ') {
        viewLen++;
        pastBlock=(pastBlock==blockSize-1) ?0 :pastBlock+1;
      }
      else {
        endPos=i;
        break;
      }
    }
    if (endPos==-1)
      endPos=buffer.length();
  }

  public String repair(String lineStr) {
    init(lineStr);
    return buffer.toString();
  }

  public String indent(String lineStr, boolean remove, boolean fitToBlock) {
    init(lineStr);
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
          while (pastBlock-- < blockSize)
            buffer.insert(endPos++, ' ');
      }
    }
    else
    if (remove) {
      if (tabIndents) {
        if (buffer.charAt(0)==' ')
          deleteFirstChars(tabSize);
        else
          deleteFirstChar();
      }
      else
      if (buffer.charAt(0)=='\t')
        deleteFirstChar();
      else
        deleteFirstChars(spaceIndentLen);
    }
    else
    if (tabIndents)
      insertFirstChar('\t');
    else
      for (int i=0; i<spaceIndentLen; i++)
        insertFirstChar(' ');
    return buffer.toString();
  }

  private void insertFirstChar(char c) {
    buffer.insert(0, c);
    endPos++;
  }
  private void deleteFirstChar() {
    buffer.deleteCharAt(0);
    endPos--;
  }
  private void deleteFirstChars(int len) {
    buffer.delete(0, len);
    endPos-=len;
  }
  private void trimPastBlock() {
    buffer.delete(endPos-pastBlock, endPos);
    endPos-=pastBlock;
  }
}
