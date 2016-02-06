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
 *
 * Note that this only accounts for bold and normal Fonts.
 * So a bold-italic font would get replaced with bold, no italics.
 * This is sloppy, but less expensive memory-wise.
 */
public class MinimumFont {
  Font font;
  Font fontBold;

  public MinimumFont(int size) {
    this(getFont(size));
  }
  private MinimumFont(Font font) {
    this.font=font;
    this.fontBold=new Font(
      font.getFontName(),
      font.getStyle() | Font.BOLD,
      font.getSize()
    );
  }

  public void setSize(int size) {
    this.font=getFont(size, font);
  }
  public int getSize() {
    return font.getSize();
  }


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
    if (old.getSize() < font.getSize()){
      if (old.isItalic())
        jc.setFont(
          old.deriveFont(old.getStyle(), font.getSize())
        );
      else
      if (old.isBold())
        jc.setFont(fontBold);
      else
        jc.setFont(font);
    }
  }

  private static Font getFont(int minSize) {
    return getFont(minSize, new JLabel().getFont());
  }
  private static Font getFont(int minSize, Font f) {
    f=new Font(
      f.getFontName(),
      f.getStyle(),
      f.getSize() < minSize ?minSize :f.getSize()
    );
    return f;
  }
}
