package org.tmotte.common.swang;
import javax.swing.BorderFactory;
import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;

/** 
 * User gridwidth and gridheight to make a component span extra columns/rows
 */
public class GridBug extends GridBagConstraints {

  public GridBagLayout gbl=new GridBagLayout();
  public Container container;

  public GridBug(Container c) {
    this.container=c;
    container.setLayout(gbl);
    gridx=1;
    gridy=1;
  }
  public GridBug add(Component... cs) {
    for (Component c: cs)
      add(c);
    return this;
  }
  public GridBug add(Component c) {
    gbl.setConstraints(c, this);
    container.add(c);
    return this;
  }

  public GridBug setY(int gridy){
    this.gridy=gridy;
    return this;
  }
  public GridBug addY(Component... cs) {
    for (Component c: cs)
      addY(c);
    return this;
  }
  public GridBug addY(Component c) {
    if (container.getComponentCount()!=0)
      gridy++;
    return add(c);
  }

  public GridBug setX(int gridx){
    this.gridx=gridx;
    return this;
  }
  public GridBug addX(Component... cs) {
    for (Component c: cs)
      addX(c);
    return this;
  }
  public GridBug addX(Component c) {
    if (container.getComponentCount()!=0)
      gridx++;
    return add(c);
  }
  
  public GridBug weightXY(double x, double y) {
    weightx=x; weighty=y; return this;
  }
  public GridBug weightXY(double xy) {
    return weightXY(xy, xy);
  }
  public GridBug gridXY(int x, int y) {
    gridx=x; gridy=y; return this;
  }
  public GridBug gridXY(int xy) {
    return gridXY(xy, xy);
  }
}