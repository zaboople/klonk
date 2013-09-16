package org.tmotte.klonk.windows.popup;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;

class Positioner {
  static void set(Window parent, Window popup) {
    set(parent, popup, false);
  }
  static void set(Window parent, Window popup, boolean unless) {
    if (!unless) {
      Rectangle pt2=parent.getBounds();
      popup.setLocation(pt2.x+pt2.width-popup.getWidth(), pt2.y+20);
    }
    Dimension dim=Toolkit.getDefaultToolkit().getScreenSize();    
    Point p=popup.getLocation();
    boolean badX=p.x<0 || p.x>dim.width,
            badY=p.y<0 || p.y>dim.height;
    if (badX||badY){
      if (badX) p.x=0;
      if (badY) p.y=0;
      popup.setLocation(p);
    }
  }
}