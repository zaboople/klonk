package org.tmotte.common.swang;
import javax.swing.KeyStroke;
import java.awt.event.InputEvent;
import java.awt.event.KeyEvent;
import java.util.regex.Pattern;
import java.lang.reflect.Field;
import java.util.Map;
import java.util.HashMap;

/**
 * This is just for mapping shortcut keys to and from a text representation.
 */
public class KeyParser {
  Map<Object, String> keyMap=new HashMap<>();
  public KeyParser() {
    for (Field field: KeyEvent.class.getFields())
      try {
        Object o=field.get(field);
        String name=field.getName();
        if (name.startsWith("VK_") && o instanceof Integer)
          keyMap.put(o, name);
      } catch (Exception e) {
        throw new RuntimeException("Error processing field "+field.getName()+" "+field);
      }
  }
  public String toString(KeyStroke ks) {
    int mods=ks.getModifiers(), kc=ks.getKeyCode();
    String keyName=keyMap.get(kc);
    if (keyName==null)
      throw new RuntimeException("Can't parse key code "+kc+" from "+ks);
    String modName="";
    if (mods==0)
      modName="";
    if ((mods & KeyEvent.CTRL_DOWN_MASK) != 0)
      modName+=" ctrl";
    if ((mods & KeyEvent.META_DOWN_MASK) != 0)
      modName+=" command";
    if ((mods & KeyEvent.ALT_DOWN_MASK) != 0)
      modName+=" alt/option";
    if ((mods & KeyEvent.SHIFT_DOWN_MASK) != 0)
      modName+=" shift";
    return (modName+" "+keyName).trim();
  }


  private static Pattern dividerRegex=Pattern.compile("(-| )");

  public static KeyStroke key(String parse) throws Exception {
    try {
      int mods=0;
      int hotkey=0;
      for (String next : dividerRegex.split(parse)) {
        next=next.trim();
        if (!next.equals("")) {
          int mod=getMod(next);
          if (mod!=0)
            mods|=mod;
          else
          if (hotkey!=0)
            throw new RuntimeException("More than two keyboard characters specified (multiple modifiers are OK), cannot accept: "+next);
          else
            hotkey=getKey(next);
        }
      }
      return KeyMapper.key(hotkey, mods);
    } catch (Exception e) {
      throw new RuntimeException(
        "Failed to parse \""+parse+"\", original error was: "+e,
        e
      );
    }
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
    if (modName.equals("option") || modName.equals("alt") || modName.equals("opt") || modName.equals("alt/option"))
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
    KeyParser parser=new KeyParser();
    for (String arg: args) {
      KeyStroke parsed=key(arg);
      String reversed=parser.toString(parsed);
      KeyStroke parsedAgain=key(reversed);
      System.out.println("From: \""+arg+"\"\n    to: "+parsed+"\n  to: \""+reversed+"\"\n    to: "+parsedAgain);
    }
  }
}