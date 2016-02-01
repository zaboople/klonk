package org.tmotte.common.swang;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Label;
import javax.swing.JComponent;
import javax.swing.JLabel;

public class MinimumFont {
  Font font;

  public MinimumFont(int size) {
    this(getFont(size));
  }
  public MinimumFont(Font font) {
    this.font=font;
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
    JLabel jlabel=new JLabel("nothing");
    Font f=jlabel.getFont();
    f=new Font(
      f.getFontName(),
      f.getStyle(),
      f.getSize() < minSize ?minSize :f.getSize()
    );
    return f;
  }
}
