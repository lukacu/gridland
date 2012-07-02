/* THIS FILE IS A MEMBER OF THE COFFEESHOP LIBRARY
 * 
 * License:
 * 
 * Coffeeshop is a conglomerate of handy general purpose Java classes.  
 * 
 * Copyright (C) 2006-2008 Luka Cehovin
 * This library is free software; you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as 
 *  published by the Free Software Foundation; either version 2.1 of 
 *  the License, or (at your option) any later version.
 *  
 *  This library is distributed in the hope that it will be useful, 
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of 
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the 
 *  GNU Lesser General Public License for more details. 
 *  
 *  http://www.opensource.org/licenses/lgpl-license.php
 *  
 *  You should have received a copy of the GNU Lesser General Public
 *  License along with this library; if not, write to the 
 *  Free Software Foundation, Inc., 59 Temple Place, Suite 330,
 *  Boston, MA 02111-1307 USA 
 * 
 * 
 */

package org.grid.server;

import java.awt.Component;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.Insets;
import java.awt.LayoutManager;

/**
 * This layout places the components to the container in a stack-like manner.
 * This means that the components are stacked in a specific direction in the same
 * sequence that they were added to the container. Each component progresses in 
 * location but retains its preferred size (in the progressing direction). If the
 * align is enabled the components are adjusted to have the same other dimension 
 * otherwise they retain their preferred size.
 * The orientation (the direction of progressing) can be either horizontal of vertical.
 * 
 * @author luka
 * @see LayoutManager
 * @since CoffeeShop 1.0
 *
 */
public class StackLayout implements LayoutManager {

	/**
	 * Orientation enumeration
	 * 
	 * @author luka
	 */
	public enum Orientation {
		HORIZONTAL, VERTICAL
	}

	private Orientation orientation;

	private boolean align = false;
	
	private int hPadding = 0, vPadding = 0;

	/**
	 * Create a layout with a specific orientation. Align option is
	 * set to false.
	 * 
	 * @param orientation orientation of the layout
	 */
	public StackLayout(Orientation orientation) {
		this.orientation = orientation;
	}

	/**
	 * Create a layout with a specific orientation and padding. Align option is
	 * set to false.
	 * 
	 * @param orientation orientation of the layout
	 * @param hPadding horizontal padding
	 * @param vPadding vertical padding
	 */
	public StackLayout(Orientation orientation, int hPadding, int vPadding) {
		this(orientation, hPadding, vPadding, false);
	}
	
	/**
	 * Create a layout with a specific orientation. Aligning can be
	 * enabled or disabled.
	 * 
	 * @param orientation orientation of the layout
	 * @param align align option
	 */
	public StackLayout(Orientation orientation, boolean align) {
		this(orientation, 0, 0, align);
	}

	/**
	 * Create a layout with a specific orientation. Aligning can be
	 * enabled or disabled.
	 * 
	 * @param orientation orientation of the layout
	 * @param hPadding horizontal padding
	 * @param vPadding vertical padding
	 * @param align align option
	 */
	public StackLayout(Orientation orientation, int hPadding, int vPadding, boolean align) {
		this.orientation = orientation;
		this.hPadding = hPadding;
		this.vPadding = vPadding;
		this.align = align;
	}
	
	/**
	 * @see LayoutManager#addLayoutComponent(String, Component)
	 */
	public void addLayoutComponent(String arg0, Component arg1) { }

	/**
	 * Places the components.
	 * 
	 * @see LayoutManager#layoutContainer(Container)
	 */
	public void layoutContainer(Container arg0) {

		Dimension base = arg0.getSize();

		Insets insets = arg0.getInsets();
		
		base.width -= insets.left + insets.right;
		base.height -= insets.top + insets.bottom;
		
		switch (orientation) {
		case HORIZONTAL: {

			int offset = hPadding + insets.left;

			base.height -= 2 * vPadding;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				Dimension d = c.getPreferredSize();
				
				c.setBounds(offset, vPadding + insets.top, d.width, align ? base.height : Math.min(
						d.height, base.height));
				
				offset += d.width + hPadding;
			}

			break;
		}
		case VERTICAL: {

			int offset = vPadding + insets.top;

			base.width -= 2 * hPadding;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				Dimension d = c.getPreferredSize();
				
				c.setBounds(hPadding + insets.left, offset, align ? base.width : Math.min(d.width,
						base.width), d.height);

				offset += d.height + vPadding;
			}

			break;
		}

		}

	}

	/**
	 * The minimum size of the container is calculated as the sum
	 * of all components minimum sizes (over the progressing dimension) and
	 * the maximum of the minimum sizes over the other dimension.
	 * 
	 * @see LayoutManager#minimumLayoutSize(Container)
	 */
	public Dimension minimumLayoutSize(Container arg0) {
		Dimension r = new Dimension();

		Insets insets = arg0.getInsets();
		
		switch (orientation) {
		case HORIZONTAL: {

			int visible = 0;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				visible++;
				
				Dimension d = c.getMinimumSize();

				r.width += d.width;

				r.height = Math.max(r.height, d.height);
			}

			r.width += (visible + 1) * hPadding; 
			
			r.height += 2 * vPadding;
			
			break;
		}
		case VERTICAL: {

			int visible = 0;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				visible++;
				
				Dimension d = c.getMinimumSize();

				r.height += d.height;

				r.width = Math.max(r.width, d.width);
			}

			r.height += (visible + 1) * vPadding; 
			
			r.width += 2 * hPadding;
			
			break;
		}

		}
		
		r.width += insets.left + insets.right;
		r.height += insets.bottom + insets.top;
		
		return r;
	}

	/**
	 * The preferred size of the container is calculated as the sum
	 * of all components preferred sizes (over the progressing dimension) and
	 * the maximum of the preferred sizes over the other dimension.
	 * 
	 * @see LayoutManager#preferredLayoutSize(Container)
	 */
	public Dimension preferredLayoutSize(Container arg0) {
		Dimension r = new Dimension();

		Insets insets = arg0.getInsets();
		
		switch (orientation) {
		case HORIZONTAL: {

			int visible = 0;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				visible++;
				
				Dimension d = c.getPreferredSize();

				r.width += d.width;

				r.height = Math.max(r.height, d.height);
			}

			r.width += (visible + 1) * hPadding; 
			
			r.height += 2 * vPadding;
			
			break;
		}
		case VERTICAL: {

			int visible = 0;
			
			for (int i = 0; i < arg0.getComponentCount(); i++) {

				Component c = arg0.getComponent(i);

				if (!c.isVisible())
					continue;
				
				visible++;
				
				Dimension d = c.getPreferredSize();

				r.height += d.height;

				r.width = Math.max(r.width, d.width);
			}

			r.height += (visible + 1) * vPadding; 
			
			r.width += 2 * hPadding;
			
			break;
		}

		}
		
		r.width += insets.left + insets.right;
		r.height += insets.bottom + insets.top;
		
		return r;
	}

	/**
	 * @see LayoutManager#removeLayoutComponent(Component)
	 */
	public void removeLayoutComponent(Component arg0) {	}

	/**
	 * Returns the align state for this layout
	 * 
	 * @return align state
	 */
	public boolean isAligned() {
		return align;
	}

	/**
	 * Set the align state (does not trigger layout revalidation)
	 * 
	 * @param align new align state
	 */
	public void setAlign(boolean align) {
		this.align = align;
	}

}
