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
  Font font;

  public MinimumFont(int size) {
    this(getFont(size));
  }
  public MinimumFont(Font font) {
    this.font=font;
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
    if (jc.getFont().getSize() < font.getSize())
      jc.setFont(font);
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
