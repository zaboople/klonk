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

  public static JMenu doMenu(JMenuBar bar, String title, MenuListener listener, int mnemon) {
    JMenu f=doMenu(title, listener, mnemon);
    bar.add(f);
    return f;
  }
  public static JMenu doMenu(JMenuBar bar, String title, int mnemon) {
    return doMenu(bar, title, null, mnemon);
  }

  public static JMenu doMenu(String title, MenuListener listener, int mnemon) {
    JMenu f=new JMenu(title);
    if (mnemon>-1)
      f.setMnemonic(mnemon);
    if (listener!=null)
      f.addMenuListener(listener);
    return f;
  }
  public static JMenu doMenu(String title, int mnemon) {
    return doMenu(title, null, mnemon);
  }


  public static JMenuItem doMenuItem(String title, ActionListener listener) {
    return doMenuItem(title, listener, -1, null);
  }
  public static JMenuItem doMenuItem(String title, ActionListener listener, int mnemon) {
    return doMenuItem(title, listener, mnemon, null);
  }
  public static JMenuItem doMenuItem(String title, ActionListener listener, int mnemon, KeyStroke shortcut) {
    JMenuItem j=new JMenuItem(title);
    if (mnemon>-1)
      j.setMnemonic(mnemon);
    if (listener!=null)
      j.addActionListener(listener);
    if (shortcut!=null){
      //Both of the following *should* work alone, but each is flawed unless we use them together:
      //- Without the first the accelerator key won't show up in the menu;
      //- Without the second the response time will be much slower
      //  for heavily/repetitively used accelerators.
      j.setAccelerator(shortcut);
      KeyMapper.accel(j, listener, shortcut);
    }
    return j;
  }


  public static JCheckBoxMenuItem doMenuItemCheckbox(String title, ActionListener listener) {
    return doMenuItemCheckbox(title, listener, -1, true);
  }
  public static JCheckBoxMenuItem doMenuItemCheckbox(String title, ActionListener listener, int mnemon, boolean isOn) {
    JCheckBoxMenuItem j=new JCheckBoxMenuItem(title, true);
    if (mnemon>-1)
      j.setMnemonic(mnemon);
    if (listener!=null)
      j.addActionListener(listener);
    j.setState(isOn);
    return j;
  }



}