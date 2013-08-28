package org.tmotte.common.swang;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.List;
import java.util.ArrayList;

public class Various {

  public static void fix(ButtonGroup... bg) {
    for (ButtonGroup b: bg)
      fix(b);
  }
  public static void fix(ButtonGroup bg) {
    final List<AbstractButton> butts=new ArrayList<AbstractButton>(bg.getButtonCount());
    for (java.util.Enumeration<AbstractButton> en=bg.getElements(); en.hasMoreElements();)
      butts.add(en.nextElement());
    for (int i=0; i<butts.size(); i++){
      final AbstractButton 
        downGrab=i<butts.size()-1 ?butts.get(i+1) :butts.get(0),    
        upGrab  =i>0              ?butts.get(i-1) :butts.get(butts.size()-1);
      butts.get(i).addKeyListener(
        new KeyAdapter(){
          public void keyPressed(KeyEvent keyEvent) {
            final int code=keyEvent.getKeyCode();
            if (code==keyEvent.VK_DOWN && downGrab!=null)
              downGrab.grabFocus();
            else
            if (code==keyEvent.VK_UP   && upGrab!=null)
              upGrab.grabFocus();
          }
        }
      ); 
    }
  }
}