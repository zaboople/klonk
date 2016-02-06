package org.tmotte.klonk.config.option;
import java.awt.Color;
import java.awt.Font;
import org.tmotte.common.swang.MinimumFont;
import org.tmotte.common.text.DelimitedString;

public class FontOptions {
  private Font font;
  private int fontSize=12;
  private int fontStyle=Font.PLAIN;
  private String fontName=new Font(Font.MONOSPACED, 0, 12).getFontName();
  private Color fontColor=Color.BLACK;
  private Color caretColor=Color.BLACK;
  private Color backgroundColor=Color.WHITE;
  private MinimumFont minFont=new MinimumFont(fontSize);

  // GETS: //
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
      font=new Font(fontName, fontStyle, fontSize);
    return font;
  }
  public String getFontName() {
    return fontName;
  }
  public int getFontSize() {
    return fontSize;
  }
  public int getFontStyle() {
    return fontStyle;
  }
  public MinimumFont getControlsFont() {
    return minFont;
  }

  // SETS: //
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
  public FontOptions setFontStyle(int style) {
    this.fontStyle=style;
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
  public FontOptions setControlsFont(int size) {
    this.minFont.setSize(size);
    return this;
  }
  public String toString() {
    DelimitedString ds=new DelimitedString(", ");
    ds.addEach(
      "Caret:"+getCaretColor(), "BG:"+getBackgroundColor(), "Color:"+getColor(),
      "Font: "+getFont(), "Name: "+getFontName(), "Size: "+getFontSize(),
      "Minimum control size: "+getControlsFont().getSize()
    );

    return ds.toString();
  }


}