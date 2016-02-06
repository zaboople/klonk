package org.tmotte.common.swang;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Label;
import javax.swing.JComponent;
import javax.swing.JLabel;

/**
 * Allows me to increase the font for general-purpose controls
 * to a manageable size because Swing defaults to stupid tiny.
 * Just uses a JLabel to establish the system default font,
 * and derives a "more reasonable" font from that.
 */
public class MinimumFont {

  ///////////////////////////////
  // Initialization & Get/Set: //
  ///////////////////////////////

  Font font;
  Font fontBold;

  public MinimumFont(int size) {
    this(getFont(size));
  }
  private MinimumFont(Font font) {
    setFont(font);
  }

  public void setSize(int size) {
    setFont(getFont(size, font));
  }
  public int getSize() {
    return font.getSize();
  }

  ///////////////////////////////////////
  // Public font assignment functions: //
  ///////////////////////////////////////

  public void set(JComponent... jcs) {
    for (JComponent jc : jcs){
      set(jc);
    }
  }
  public void set(JComponent jc) {
    setFont(jc);
    expand(jc);
  }
  public void set(Component c) {
    setFont(c);
    if (c instanceof JComponent)
      expand((JComponent)c);
    else
    if (c instanceof Container)
      expand((Container)c);
  }

  ////////////////////////////////////////
  // Private font assignment functions: //
  ////////////////////////////////////////

  private void expand(JComponent jc) {
    if (jc.getComponentCount() > 0)
      for (Component c : jc.getComponents())
        set(c);
  }
  private void expand(Container jc) {
    if (jc.getComponentCount() > 0)
      for (Component c : jc.getComponents())
        set(c);
  }
  private void setFont(Component jc) {
    Font old=jc.getFont();
    if (font.isBold())
      System.out.println("MAN WTF MAN");
    if (old.getSize() != font.getSize()){
      if (old.isItalic())
        //Rare case, so we waste memory:
        jc.setFont(
          old.deriveFont(old.getStyle(), font.getSize())
        );
      else
      if (old.isBold())
        //Occasional case, yeah it's bold:
        jc.setFont(fontBold);
      else
        //Typical case:
        jc.setFont(font);
    }
  }

  //////////////////////////
  // Internal State Mgmt: //
  //////////////////////////

  private void setFont(Font font) {
    this.font=font;
    this.fontBold=new Font(
      font.getFontName(),
      font.getStyle() | Font.BOLD,
      font.getSize()
    );
  }

  /////////////////////////////////////
  // Utilities for making new fonts: //
  /////////////////////////////////////

  private static Font getFont(int minSize) {
    return getFont(minSize, new JLabel().getFont());
  }
  private static Font getFont(int minSize, Font f) {
    f=new Font(
      f.getFontName(),
      Font.PLAIN,
      minSize
    );
    return f;
  }
}
