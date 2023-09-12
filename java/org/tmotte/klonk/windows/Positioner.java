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
    Rectangle p=popup.getBounds();
    fix(p);
    popup.setBounds(p);
  }

  public static Rectangle fix(Rectangle windowPos) {
    Rectangle w=windowPos;
    Rectangle screen=getScreenBounds();

    // Keep in mind, on a dual display, screen.x will be negative; 0 will be
    // left edge of right-hand display.
    int rightEdge=screen.x+screen.width,
      bottomEdge=screen.y+screen.height;

    boolean badX=w.x<screen.x || w.x>rightEdge,
            badY=w.y<screen.y || w.y>bottomEdge;
    if (badX) w.x=screen.x+120;
    if (badY) w.y=screen.y;

    boolean tooWide=w.x+w.width  > rightEdge,
            tooTall=w.y+w.height > bottomEdge;
    if (tooWide) w.width =rightEdge  - w.x;
    if (tooTall) w.height=bottomEdge - w.y;
    return w;
  }

  private static Rectangle getScreenBounds() {
    Rectangle bds = new Rectangle();
    for (GraphicsDevice gd: GraphicsEnvironment.getLocalGraphicsEnvironment().getScreenDevices())
      for (GraphicsConfiguration gc: gd.getConfigurations())
        bds=bds.union(gc.getBounds());
    return bds;
  }
}