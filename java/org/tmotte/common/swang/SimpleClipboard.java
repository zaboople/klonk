package org.tmotte.common.swang;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.Toolkit;

public class SimpleClipboard {
  public static void set(String name) {
    StringSelection stringSelection = new StringSelection(name);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, stringSelection);  
  }
}