package org.tmotte.klonk.config.option;
public class EncryptionOptions {
  char[] pass={};
  int bits;
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

