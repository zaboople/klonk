package org.tmotte.klonk.windows;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Dimension;
import java.awt.Image;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.GraphicsConfiguration;
import java.awt.GraphicsEnvironment;
import java.awt.GraphicsDevice;

public class Positioner {
  public static void set(Window parent, Window popup) {
    set(parent, popup, false);
  }
  
  public static void set(Window parent, Window popup, boolean unless) {

    //Align to top right of window:
    if (!unless) {
      Rectangle pt2=parent.getBounds();
      popup.setLocation(pt2.x+Math.max(0, pt2.width-popup.getWidth()), pt2.y+20);
    }

    //Just make sure it fits:
    Rectangle screen=getScreenBounds();
    Rectangle p=popup.getBounds();
    fix(p);
    popup.setBounds(p);
  }
  
  public static void fix(Rectangle windowPos) {
    Rectangle w=windowPos;
    Rectangle screen=getScreenBounds();
        
    boolean badX=w.x<screen.x || w.x>screen.width,
            badY=w.y<screen.y || w.y>screen.height;
    if (badX) w.x=screen.x;
    if (badY) w.y=screen.y;

    boolean tooWide=w.x+w.width -screen.width  > screen.x,
            tooTall=w.y+w.height-screen.height > screen.y;
    if (tooWide) w.width =screen.width -w.x;
    if (tooTall) w.height=screen.height-w.y;
  }
  
  private static Rectangle getScreenBounds() {
    Rectangle bds = new Rectangle();
    for (GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices()) 
      for (GraphicsConfiguration gc: gd.getConfigurations()) 
        bds=bds.union(gc.getBounds());
    return bds;
  }
}