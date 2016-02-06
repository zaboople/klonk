package org.tmotte.klonk.config;
import java.util.ArrayList;
import java.util.List;
import javax.swing.JFrame;
import org.tmotte.common.swang.CurrentOS;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.klonk.config.msg.Setter;

public class PopupInfo {
  public final JFrame parentFrame;
  public final CurrentOS currentOS;
  private final List<Setter<FontOptions>> fontListeners=new java.util.ArrayList<>(30);

  public PopupInfo(JFrame parentFrame, CurrentOS currentOS) {
    this.parentFrame=parentFrame;
    this.currentOS=currentOS;
  }
  public void addFontListener(Setter<FontOptions> listener) {
    fontListeners.add(listener);
  }
  public List<Setter<FontOptions>> getFontListeners() {
    return fontListeners;
  }

}