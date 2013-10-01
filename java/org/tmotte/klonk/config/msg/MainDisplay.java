package org.tmotte.klonk.config.msg;
import java.awt.Rectangle;
import java.awt.Component;

public interface MainDisplay {
  public boolean isMaximized();
  public Rectangle getBounds();
  public void setEditor(Component c);
}
