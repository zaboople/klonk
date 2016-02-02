package org.tmotte.klonk.config;
import javax.swing.JFrame;
import org.tmotte.klonk.config.option.FontOptions;
import org.tmotte.common.swang.CurrentOS;

public class PopupInfo {
  public final JFrame parentFrame;
  public final CurrentOS currentOS;
  public final FontOptions fontOptions;

  public PopupInfo(JFrame parentFrame, CurrentOS currentOS, FontOptions fo) {
    this.parentFrame=parentFrame;
    this.currentOS=currentOS;
    this.fontOptions=fo;
  }
}