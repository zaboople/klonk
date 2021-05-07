package org.tmotte.common.swang;
import javax.swing.Action;
import javax.swing.AbstractAction;
import java.awt.event.ActionListener;
import javax.swing.event.MenuListener;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.KeyStroke;
import javax.swing.JPopupMenu;

public class MenuUtils {

  public static JMenu add(JMenu menu, JMenuItem... items) {
    for (JMenuItem i: items)
      if (i!=null)
        menu.add(i);
    return menu;
  }

  public static JPopupMenu add(JPopupMenu menu, JMenuItem... items) {
    for (JMenuItem i: items)
      if (i!=null)
        menu.add(i);
    return menu;
  }

  public static JMenu doMenu(JMenuBar bar, String title, MenuListener listener, int hotkey) {
    JMenu f=doMenu(title, listener, hotkey);
    bar.add(f);
    return f;
  }
  public static JMenu doMenu(JMenuBar bar, String title, int hotkey) {
    return doMenu(bar, title, null, hotkey);
  }

  public static JMenu doMenu(String title, MenuListener listener, int hotkey) {
    JMenu f=new JMenu(title);
    if (hotkey>-1)
      f.setMnemonic(hotkey);
    if (listener!=null)
      f.addMenuListener(listener);
    return f;
  }
  public static JMenu doMenu(String title, int hotkey) {
    return doMenu(title, null, hotkey);
  }


  public static JMenuItem doMenuItem(String title, ActionListener listener) {
    return doMenuItem(title, listener, -1, null);
  }
  public static JMenuItem doMenuItem(String title, ActionListener listener, int hotkey) {
    return doMenuItem(title, listener, hotkey, null);
  }
  public static JMenuItem doMenuItem(String title, ActionListener listener, int hotkey, KeyStroke ks) {
    JMenuItem j=new JMenuItem(title);
    if (hotkey>-1)
      j.setMnemonic(hotkey);
    if (listener!=null)
      j.addActionListener(listener);
    if (ks!=null){
      //Both of the following *should* work alone, but each is flawed unless we use them together:
      //- Without the first the accelerator key won't show up in the menu;
      //- Without the second the response time will be much slower
      //  for heavily/repetitively used accelerators.
      j.setAccelerator(ks);
      KeyMapper.accel(j, listener, ks);
    }
    return j;
  }


  public static JCheckBoxMenuItem doMenuItemCheckbox(String title, ActionListener listener) {
    return doMenuItemCheckbox(title, listener, -1, true);
  }
  public static JCheckBoxMenuItem doMenuItemCheckbox(String title, ActionListener listener, int hotkey, boolean isOn) {
    JCheckBoxMenuItem j=new JCheckBoxMenuItem(title, true);
    if (hotkey>-1)
      j.setMnemonic(hotkey);
    if (listener!=null)
      j.addActionListener(listener);
    j.setState(isOn);
    return j;
  }



}