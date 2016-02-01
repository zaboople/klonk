package org.tmotte.klonk.config;
import javax.swing.JFrame;
import org.tmotte.common.swang.MinimumFont;
import org.tmotte.common.swang.CurrentOS;

public class PopupInfo {
  public final JFrame parentFrame;
  public final CurrentOS currentOS;
  public final MinimumFont mFont;

  public PopupInfo(JFrame parentFrame, CurrentOS currentOS, MinimumFont mFont) {
    this.parentFrame=parentFrame;
    this.currentOS=currentOS;
    this.mFont=mFont;
  }
}