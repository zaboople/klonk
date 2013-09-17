package org.tmotte.common.swang;
import java.awt.Color;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics2D;
import java.awt.Insets;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.font.FontRenderContext;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import javax.swing.JSpinner;
import javax.swing.JWindow;
import javax.swing.SpinnerNumberModel;
import org.tmotte.common.swang.Radios;

/** By default JSpinner tends to take up excessive space. This allows you to re-fit it to contain a given String.*/
public class SpinnerFix {

  public static void fix(JSpinner jsp, Window containerWindow, String toFit) {
    //Required to get the component to size itself and obtain a graphics object:
    containerWindow.pack();
    Font font=jsp.getFont();
    Graphics2D gr=(Graphics2D)jsp.getGraphics();
    Rectangle rect=font.getStringBounds(
      toFit.toCharArray(), 
      0, 
      toFit.length(), 
      gr.getFontRenderContext()
    ).getBounds();
    jsp.getEditor().setPreferredSize(
      new Dimension(rect.width, rect.height)
    );
  
  } 
}