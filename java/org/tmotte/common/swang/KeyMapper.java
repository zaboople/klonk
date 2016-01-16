package org.tmotte.common.swang;
import java.awt.Toolkit;
import java.awt.event.KeyEvent;
import javax.swing.AbstractAction;
import javax.swing.AbstractButton;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.KeyStroke;

public class KeyMapper {

  static Toolkit toolkit=Toolkit.getDefaultToolkit();

  //////////////////////////////////////////////////////////
  // ADDING HOTKEYS TO COMPONENTS; NOTE HOW THE COMPONENT //
  // ITSELF IS RETURNED FOR CONVENIENCE:                  //
  //////////////////////////////////////////////////////////

  public static AbstractButton accel(AbstractButton jc, Action action, KeyStroke hotKey) {
    String text=jc.getText();
    if (text.length()>20)
      text=text.substring(0,20);
    text+="-"+hotKey;
    accel((JComponent)jc, text, action, hotKey);
    return jc;
  }
  public static AbstractButton accel(AbstractButton jc, Action action, int hotKey) {
    return accel(jc, action, key(hotKey));
  }
  public static AbstractButton accel(AbstractButton jc, Action action, int hotKey, int modifiers) {
    return accel(jc, action, key(hotKey, modifiers));
  }
  public static AbstractButton accel(AbstractButton jc, Action action, int hotKey, int... modifiers) {
    return accel(jc, action, key(hotKey, modifiers));
  }

  public static JComponent accel(JComponent jc, Action action, KeyStroke hotKey) {
    return accel(jc, String.valueOf(jc.hashCode()), action, hotKey);
  }
  public static JComponent accel(JComponent jc, String stupidName, Action action, KeyStroke hotKey) {
    action.putValue(Action.ACCELERATOR_KEY, hotKey);
    jc.getActionMap()
      .put(stupidName, action);
    jc.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW)
      .put(hotKey, stupidName);
    return jc;
  }
  public static JComponent accel(JComponent jc, String stupidName, Action action, int hotKey) {
    return accel(jc, stupidName, action, key(hotKey));
  }
  public static JComponent accel(JComponent jc, String stupidName, Action action, int hotKey, int modifiers) {
    return accel(jc, stupidName, action, key(hotKey, modifiers));
  }
  public static JComponent accel(JComponent jc, String stupidName, Action action, int hotKey, int... modifiers) {
    return accel(jc, stupidName, action, key(hotKey, modifiers));
  }

  /////////////////////////////////////
  // DETECTING MODIFIER KEY PRESSES: //
  /////////////////////////////////////

  public static boolean ctrlPressed(KeyEvent k) {
    return ctrlPressed(k.getModifiersEx());
  }
  public static boolean ctrlPressed(int mods) {
    return (mods & KeyEvent.CTRL_DOWN_MASK)==KeyEvent.CTRL_DOWN_MASK;
  }
  public static boolean shiftPressed(KeyEvent k) {
    return shiftPressed(k.getModifiersEx());
  }
  public static boolean shiftPressed(int mods) {
    return (mods & KeyEvent.SHIFT_DOWN_MASK)==KeyEvent.SHIFT_DOWN_MASK;
  }
  public static boolean altPressed(KeyEvent k) {
    return altPressed(k.getModifiersEx());
  }
  public static boolean altPressed(int mods) {
    return (mods & KeyEvent.ALT_DOWN_MASK)==KeyEvent.ALT_DOWN_MASK;
  }

  /** This is the macintosh "command" key */
  public static boolean metaPressed(int mods, CurrentOS currentOS) {
    return currentOS.isOSX && (mods & KeyEvent.META_DOWN_MASK)==KeyEvent.META_DOWN_MASK;
  }
  public static boolean metaPressed(KeyEvent k, CurrentOS currentOS) {
    return currentOS.isOSX && metaPressed(k.getModifiersEx(), currentOS);
  }
  /** This is the macintosh "option" key */
  public static boolean optionPressed(int mods, CurrentOS currentOS) {
    return currentOS.isOSX && (mods & KeyEvent.ALT_DOWN_MASK)==KeyEvent.ALT_DOWN_MASK;
  }
  public static boolean optionPressed(KeyEvent k, CurrentOS currentOS) {
    return currentOS.isOSX && optionPressed(k.getModifiersEx(), currentOS);
  }

  /**
   * Tells us if the default modifier for the OS (Ctrl or Command or what)
   * was pressed.
   */
  public static boolean modifierPressed(int mods, CurrentOS currentOS) {
    int s=currentOS.isOSX
      ?KeyEvent.META_DOWN_MASK
      :KeyEvent.CTRL_DOWN_MASK;
    return (mods & s) == s;
  }
  public static boolean modifierPressed(KeyEvent k, CurrentOS currentOS) {
    return modifierPressed(k.getModifiersEx(), currentOS);
  }

  ///////////////////////////////
  // CREATE KeyStroke OBJECTS: //
  ///////////////////////////////

  public static int shortcutByOS(){
    return toolkit.getMenuShortcutKeyMask();
  }
  public static KeyStroke keyByOS(int hotKey) {
    return KeyStroke.getKeyStroke(hotKey, shortcutByOS());
  }
  public static KeyStroke key(int hotKey) {
    return KeyStroke.getKeyStroke(hotKey, 0);
  }
  /**
   * @param modifiers Note that this is not KeyEvent.VK_etc, but InputEvent.etc_MASK
   */
  public static KeyStroke key(int hotKey, int modifiers) {
    return KeyStroke.getKeyStroke(hotKey, modifiers);
  }
  public static KeyStroke key(int hotKey, int... modifiers) {
    int mod=0;
    for (int m: modifiers)
      mod|=m;
    return key(hotKey, mod);
  }

  public static void easyCancel(JButton btn, Action action){
    KeyMapper.accel(btn, action, KeyMapper.key(KeyEvent.VK_ESCAPE));
    KeyMapper.accel(btn, action, KeyMapper.keyByOS(KeyEvent.VK_W));
    KeyMapper.accel(btn, action, KeyMapper.key(KeyEvent.VK_F4, KeyEvent.ALT_DOWN_MASK));
  }
}