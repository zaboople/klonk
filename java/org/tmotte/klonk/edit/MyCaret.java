package org.tmotte.klonk.edit;
import java.awt.BorderLayout;
import java.awt.Graphics;
import java.awt.Rectangle;
import java.awt.geom.Rectangle2D;
import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.text.BadLocationException;
import javax.swing.text.DefaultCaret;
import javax.swing.text.JTextComponent;

class MyCaret extends DefaultCaret {
  private static final long serialVersionUID = 1L;

  int betterWidth=1;
  int halfWidth=0;

  public MyCaret(){
    //Commenting this out, but it works if you want to use it:
    //setBlinkRate(500);
  }
  public MyCaret setMyWidth(int w) {
    betterWidth=w;
    //This caused problems because apparently it recursed repaints() adnauseum and jacked up cpu
    //halfWidth=betterWidth > 4 ?0 :w/2;
    return this;
  }
  protected @Override synchronized void damage(Rectangle r) {
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

  public @Override void paint(Graphics g) {
    JTextComponent comp=getComponent();
    if (comp==null)
      return;

    Rectangle r=null;
    try {
      Rectangle2D r2=comp.modelToView2D(getDot());
      if (r2==null)
        return;
      r=new Rectangle(
        (int)Math.round(r2.getX()),
        (int)Math.round(r2.getY()),
        (int)Math.round(r2.getWidth()),
        (int)Math.round(r2.getHeight())
      );
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