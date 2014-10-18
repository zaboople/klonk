package org.tmotte.klonk.config.option;
import org.tmotte.common.text.DelimitedString;
import java.awt.Font;
import java.awt.Color;

public class FontOptions {
  private Font font;
  private int fontSize=13;
  private String fontName=new Font(Font.MONOSPACED, 0, 12).getFontName();
  private Color fontColor=Color.BLACK;
  private Color caretColor=Color.BLACK;
  private Color backgroundColor=Color.WHITE;

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
      font=new Font(fontName, Font.PLAIN, fontSize);
    return font;
  }
  public String getFontName() {
    return fontName;
  }
  public int getFontSize() {
    return fontSize;
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
  public String toString() {
    DelimitedString ds=new DelimitedString(", ");
    ds.addEach("Caret:"+getCaretColor(), "BG:"+getBackgroundColor(), "Color:"+getColor(), 
               "Font: "+getFont(), "Name: "+getFontName(), "Size: "+getFontSize());
    return ds.toString();
  }
  
  
}