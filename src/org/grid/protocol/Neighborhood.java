/*
 *  AgentField - a simple capture-the-flag simulation for distributed intelligence
 *  Copyright (C) 2011 Luka Cehovin <http://vicos.fri.uni-lj.si/lukacu>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with this program. If not, see <http://www.gnu.org/licenses/>. 
 */
package org.grid.protocol;

import java.awt.Color;
import java.io.Serializable;

import org.grid.arena.Arena;


public class Neighborhood implements Serializable, Arena {
	
	public static final int EMPTY = 0;
	
	public static final int WALL = -1;
	
	public static final int HEADQUARTERS = -2;
	
	public static final int FLAG = -3;
	
	public static final int OTHER_HEADQUARTERS = -4;
	
	public static final int OTHER_FLAG = -5;
	
	public static final int OTHER = -6;
	
	private static final long serialVersionUID = 1L;

	private int size;
	
	private int[] grid;
	
	public Neighborhood(int size) {
	
		this.size = size;
		
		this.grid = new int[(size * 2 + 1) * (size * 2 + 1)];
		
	}

	public int getSize() {
		return size;
	}
	
	public int getCell(int x, int y) {
	
		if (x > size || x < -size || y > size || y < -size)
			return WALL;
		
		x += size;
		y += size;
		

		return grid[x + y * (2 * size + 1)]; 
		
	}
	
	public void setCell(int x, int y, int c) {

		if (x > size || x < -size || y > size || y < -size)
			return;
		
		x += size;
		y += size;
		
		grid[x + y * (2 * size + 1)] = c;
		
	}

	@Override
	public int getBaseTile(int x, int y) {
		return 0;
	}

	private static transient final Color TEAM_COLOR = Color.BLUE.darker().darker(); 
	
	private static transient final Color OTHER_COLOR = Color.RED.brighter();
	
	@Override
	public Color getBodyColor(int x, int y) {
		
		int b = getCell(x - size, y - size);
		
		if (b == HEADQUARTERS || b == FLAG || b > 0)
			return TEAM_COLOR;
		
		return OTHER_COLOR;
	}

	@Override
	public int getBodyTile(int x, int y) {
	
		int b = getCell(x - size, y - size);
		
		if (b == WALL)
			return Arena.TILE_WALL_0;
		
		if (b == HEADQUARTERS || b == OTHER_HEADQUARTERS)
			return Arena.TILE_HEADQUARTERS;

		if (b == FLAG || b == OTHER_FLAG)
			return Arena.TILE_FLAG;
		
		if (b > 0 || b == OTHER)
			return Arena.TILE_AGENT;
		
		return 0;
	}

	@Override
	public int getHeight() {
		return size*2 + 1;
	}

	@Override
	public int getWidth() {
		return size*2 + 1;
	}

	@Override
	public float getBodyOffsetX(int x, int y) {
		return 0;
	}

	@Override
	public float getBodyOffsetY(int x, int y) {
		return 0;
	}
}
