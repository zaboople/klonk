package org.tmotte.common.swang;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.lang.reflect.Field;

/**
 * This is just for mapping shortcut keys to and from a text representation.
 */
public class KeyParser {
  private static Pattern dividerRegex=Pattern.compile("(-| )");

  public static KeyStroke key(String parse) throws Exception {
    int mods=0;
    int hotkey=0;
    for (String next : dividerRegex.split(parse)) {
      next=next.trim();
      if (!next.equals("")) {
        int mod=getMod(next);
        if (mod!=0)
          mods|=mod;
        else
          hotkey=getKey(next);
      }
    }
    return KeyMapper.key(hotkey, mods);
  }
  private static int getMod(String s) {
    String modName=s.toLowerCase();
    int downMask=modName.indexOf("_down_mask");
    if (downMask > 0)
      modName=modName.substring(0, downMask);
    if (modName.equals("ctrl") || modName.equals("control"))
      return KeyEvent.CTRL_DOWN_MASK;
    else
    if (modName.equals("command") || modName.equals("cmd") || modName.equals("meta"))
      return KeyEvent.META_DOWN_MASK;
    else
    if (modName.equals("option") || modName.equals("alt") || modName.equals("opt"))
      //Not a bug - on mac, option is alt. Don't ask me.
      return KeyEvent.ALT_DOWN_MASK;
    else
    if (modName.equals("shift"))
      return KeyEvent.SHIFT_DOWN_MASK;
    else
      return 0;
  }
  private static int getKey(String original) {
    String keyName=original.toUpperCase();
    if (!keyName.startsWith("VK_"))
      keyName="VK_"+keyName;
    try {
      Field field=KeyEvent.class.getField(keyName);
      return field.getInt(field);
    } catch (Exception e) {
      throw new RuntimeException(
        "Could not find key that maps to \""+keyName
       +"\" derived from \""+original+"\", "
       +"you may want to consult with "
       +"the java documentation for the java.awt.event.KeyEvent class; original error: "
       +e.getMessage(),
       e
      );
    }
  }
  public static void main(String[] args) throws Exception {
    for (String arg: args)
      System.out.println("Key: "+key(arg));
  }
}