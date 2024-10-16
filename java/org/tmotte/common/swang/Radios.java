package org.tmotte.common.swang;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

/**
 * Convenience functions for doing typical things you would expect to be able to do without
 * needing to write convenience functions...
 */
public class Radios {

  public static ButtonGroup create(JRadioButton... js) {
    ButtonGroup bg=new ButtonGroup();
    for (JRadioButton j: js)
      bg.add(j);
    return bg;
  }

  public static void doUpDownArrows(ButtonGroup... bg) {
    for (ButtonGroup b: bg)
      doUpDownArrows(b);
  }
  public static ButtonGroup doUpDownArrows(ButtonGroup bg) {
    return doArrows(bg, true);
  }
  public static void doLeftRightArrows(ButtonGroup... bg) {
    for (ButtonGroup b: bg)
      doLeftRightArrows(b);
  }
  public static ButtonGroup doLeftRightArrows(ButtonGroup bg) {
    return doArrows(bg, false);
  }


  private static ButtonGroup doArrows(ButtonGroup bg, final boolean upDown) {
    int count=bg.getButtonCount();
    if (count==0)
      return bg;
    final AbstractButton[] butts=new AbstractButton[count];
    {
      int i=0;
      for (java.util.Enumeration<AbstractButton> en=bg.getElements(); en.hasMoreElements();)
        butts[i++]=en.nextElement();
    }
    for (int i=0; i<butts.length; i++){
      final AbstractButton
        nextButt=i<butts.length-1 ?butts[i+1] :butts[0],
        prevButt=i>0              ?butts[i-1] :butts[butts.length-1];
      butts[i].addKeyListener(
        new KeyAdapter(){
          public void keyPressed(KeyEvent keyEvent) {
            final int code=keyEvent.getKeyCode();
            if (code==KeyEvent.VK_DOWN  && upDown  && nextButt!=null)
              nextButt.grabFocus();
            else
            if (code==KeyEvent.VK_UP    && upDown  && prevButt!=null)
              prevButt.grabFocus();
            else
            if (code==KeyEvent.VK_RIGHT && !upDown && nextButt!=null)
              nextButt.grabFocus();
            else
            if (code==KeyEvent.VK_LEFT  && !upDown && prevButt!=null)
              prevButt.grabFocus();
          }
        }
      );
    }
    return bg;
  }
}