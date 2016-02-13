package org.tmotte.common.swang;
import java.awt.Component;
import java.awt.Container;
import java.awt.Font;
import java.awt.Label;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JMenu;
import java.util.Set;

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
    for (JComponent jc : jcs)
      set(jc);
  }
  public void set(JComponent jc) {
    set(jc, null);
  }
  public void set(Component jc) {
    set(jc, null);
  }
  public void set(Component c, Set<Component> ignore) {
    if (c==null)
      //Nulls happen during normal traversal, deal with it:
      return;
    if (ignore!=null && ignore.contains(c))
      return;
    setFont(c);
    if (c instanceof JMenu)
      //This is necessary because at least on OSX the JComponent/Container
      //functions frequently come up empty:
      expand((JMenu)c, ignore);
    else
    if (c instanceof JComponent)
      expand((JComponent)c, ignore);
    else
    if (c instanceof Container)
      expand((Container)c, ignore);
  }

  ////////////////////////////////////////
  // Private font assignment functions: //
  ////////////////////////////////////////

  private void expand(JComponent jc, Set<Component> ignore) {
    if (jc.getComponentCount() > 0)
      for (Component c : jc.getComponents())
        set(c, ignore);
  }
  private void expand(Container jc, Set<Component> ignore) {
    if (jc.getComponentCount() > 0)
      for (Component c : jc.getComponents())
        set(c, ignore);
  }
  private void expand(javax.swing.JMenu j, Set<Component> ignore) {
    int count=j.getItemCount();
    for (int i=0; i<count; i++)
      set(j.getItem(i), ignore);
  }
  private void setFont(Component jc) {
    //Warning, JFrames have no font
    Font old=jc.getFont();
    if (old != null && old.getSize() != font.getSize()){
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

  // Debugging: //
  private boolean debugOn=false;
  private void debug(Component jc) {
    if (debugOn){
      System.out.print("DEBUG ");
      if (jc instanceof javax.swing.JMenu)
        System.out.println("JMenu "+((javax.swing.JMenu)jc).getText()+" "+((javax.swing.JMenu)jc).getItemCount());
      else
      if (jc instanceof javax.swing.JMenuItem)
        System.out.println("JMenuItem "+((javax.swing.JMenuItem)jc).getText()+" "+((javax.swing.JMenuItem)jc).getComponents().length);
      else
      if (jc instanceof javax.swing.JPopupMenu)
        System.out.println("Popup "+((javax.swing.JPopupMenu)jc).getComponentCount());
      else
        System.out.println("? "+jc);
      System.out.flush();
    }
  }

}
