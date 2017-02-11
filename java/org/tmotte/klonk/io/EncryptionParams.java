package org.tmotte.klonk.io;

public class EncryptionParams {
  public char[] pass={};
  public int bits;
  public @Override String toString() {
    return pass.length + " " + bits;
  }
  public void nullify() {
    for (int i=0; i<pass.length; i++)
      pass[i]=0;
    pass=new char[]{};
    bits=0;
  }
}

