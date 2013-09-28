package org.tmotte.klonk.config.msg;
import java.util.Iterator;
import org.tmotte.klonk.Editor;
public interface Editors {
  public Editor getFirst();
  public Iterable<Editor> forEach();  
  public int size();
}