package com.jdbernard.pit.swing

import java.awt.Component
import java.awt.Graphics
import javax.swing.Icon

/**
 * This class provides a composite icon. A composite icon
 * draws its parts one on the other, all aligned to the
 * left top corner. The size of the compsite icon is the
 * max size of its parts.
 *
 */
public class CompositeIcon implements Icon {
  List<Icon> icons

  /**
   * Construct a composite icon.
   *
   * @param i The parts.
   */
  public CompositeIcon(List<Icon> i) { icons = i; }

  /**
   * Draw the icon at the specified location.  Icon implementations
   * may use the Component argument to get properties useful for
   * painting, e.g. the foreground or background color.
   *
   * @param c The component to take attributes from.
   * @param g The graphics port to draw into.
   * @param x The x drawing coordinate.
   * @param y The y drawing coordinate.
   */
  public void paintIcon(Component c, Graphics g, int x, int y) {
    icons.each { it.paintIcon(c, g, x, y) }
  }

  /**
   * Returns the icon's width.
   *
   * @return an int specifying the fixed width of the icon.
   */
  public int getIconWidth() {
    int width = 0;
    icons.each { width = Math.max(width, it.iconWidth) }
    return width;
  }

  /**
   * Returns the icon's height.
   *
   * @return an int specifying the fixed height of the icon.
   */
  public int getIconHeight() {
    int height = 0;
    icons.each { height = Math.max(height, it.iconHeight) }
    return height;
  }

}
