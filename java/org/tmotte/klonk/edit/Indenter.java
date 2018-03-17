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

  public void init(String lineStr) {
    anyChange=false;
    viewLen=0;

    final int blockSize=tabIndents ?tabSize :spaceIndentLen;
    buffer=new StringBuilder(lineStr);
    int currBlock=0;
    for (int i=0; i<buffer.length(); i++) {
      char c=buffer.charAt(i);
      if (c=='\t') {
        if (currBlock==0)
          viewLen+=tabSize;
        else
        if (tabIndents) {
          anyChange=true;
          buffer.delete(i-currBlock, i);
          i-=currBlock;
          viewLen=tabSize + viewLen - currBlock;
          currBlock=0;
        }
        else {
          anyChange=true;
          buffer.deleteCharAt(i);
          for (int j=0; j<blockSize-currBlock; j++)
            buffer.insert(i, ' ');
          viewLen++;
          currBlock++;
        }
      }
      else
      if (c==' ') {
        viewLen++;
        currBlock=(currBlock==blockSize-1) ?0 :currBlock+1;
      }
      else
        break;
    }
  }

  public String repair(String lineStr) {
    init(lineStr);
    return buffer.toString();
  }

  public void indent(String lineStr, boolean remove, boolean singleLine) {


  }
}
