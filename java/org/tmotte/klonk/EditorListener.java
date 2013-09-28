package org.tmotte.klonk;
import java.io.File;
public interface EditorListener {
  public void doCaretMoved(Editor ed, int caretPos);
  public void doCapsLock(boolean locked);
  public void closeEditor();
  public void fileDropped(File file);
  public void doEditorChanged(Editor ed);
}
