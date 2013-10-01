package org.tmotte.klonk.config.option;
import org.tmotte.common.swang.Fail;
import java.util.regex.Pattern;
import java.util.regex.Matcher;
import java.awt.Font;
import java.awt.Color;

public class FontOptions {
  private Font font;
  private int fontSize=13;
  private String fontName=new Font(Font.MONOSPACED, 0, 12).getFontName();
  private Color fontColor=Color.BLACK;
  private Color caretColor=Color.BLACK;
  private Color backgroundColor=Color.WHITE;

  public Color getCaretColor() {
    return caretColor;
  }  
  public Color getBackgroundColor() {
    return backgroundColor;
  }  
  public Color getColor() {
    return fontColor;
  }
  public Font getFont() {
    if (font==null)
      font=new Font(fontName, Font.PLAIN, fontSize);
    return font;
  }
  public String getFontName() {
    return fontName;
  }
  public int getFontSize() {
    return fontSize;
  }
  
  public FontOptions setFontName(String name) {
    this.fontName=name;
    this.font=null;
    return this;
  }
  public FontOptions setFontSize(int size) {
    this.fontSize=size;
    this.font=null;
    return this;
  }
  public FontOptions setColor(Color color) {
    this.fontColor=color;
    return this;
  }
  public FontOptions setBackgroundColor(Color color) {
    this.backgroundColor=color;
    return this;
  }
  public FontOptions setCaretColor(Color color) {
    this.caretColor=color;
    return this;
  }
  
}