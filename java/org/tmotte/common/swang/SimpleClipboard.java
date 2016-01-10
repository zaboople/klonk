package org.tmotte.common.swang;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.Toolkit;

public class SimpleClipboard {
  public static void set(String name) {
    StringSelection stringSelection = new StringSelection(name);
    Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
    clipboard.setContents(stringSelection, stringSelection);
  }
  public static String get() {
    try {
      Clipboard clipboard = Toolkit.getDefaultToolkit().getSystemClipboard();
      Transferable trs=clipboard.getContents(null);
      Object o=null;
      if (trs.isDataFlavorSupported(DataFlavor.stringFlavor))
        o=trs.getTransferData(DataFlavor.stringFlavor);
      if (o==null)
        o="";
      return o.toString();
    } catch (Exception e) {
      throw new RuntimeException(e);
    }
  }
}