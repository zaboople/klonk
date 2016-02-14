package org.tmotte.klonk.edit;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

class MyCaret extends DefaultCaret {

  int betterWidth=1;
  int halfWidth=0;

  public MyCaret(){
    //Commenting this out, but it works if you want to use it:
    //setBlinkRate(500);
  }
  public MyCaret setMyWidth(int w) {
    betterWidth=w;
    halfWidth=w/2;
    return this;
  }
  protected synchronized void damage(Rectangle r) {
    if (r==null)
      return;

    // Give values to x,y,width,height (inherited from java.awt.Rectangle)
    x=r.x-halfWidth;
    y=r.y;
    height=r.height;

    // A value for width was probably set by paint(), which we leave alone.
    // But the first call to damage() precedes the first call to paint(), so
    // in this case we must be prepared to set a valid width, or else
    // paint() will receive a bogus clip area and caret will not get drawn properly.
    //if (width <=0)
    //  width=getComponent().getWidth();

    repaint(); // calls getComponent().repaint(x, y, width, height)
  }

  public void paint(Graphics g) {
    JTextComponent comp=getComponent();
    if (comp==null)
      return;

    Rectangle r=null;
    try {
      r=comp.modelToView(getDot());
      if (r==null)
        return;
    } catch (BadLocationException e) {
      return;
    }

    if (x!=r.x || y!=r.y) {
      // paint() has been called directly, without a previous call to
      // damage(), so do some cleanup. (This happens, for example, when
      // the text component is resized.)
      repaint(); // erase previous location of caret
      x=r.x-halfWidth; // Update dimensions (width gets set later in this method)
      y=r.y;
      height=r.height;
    }

    g.setColor(comp.getCaretColor());
    g.setXORMode(comp.getBackground()); // do this to draw in XOR mode

    width=betterWidth;
    if (isVisible())
      g.fillRect(r.x-halfWidth, r.y, width, r.height);
  }

}