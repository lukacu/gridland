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
package org.grid.arena;

import java.awt.Color;

public interface Arena {
	
	public static final int TILE_WALL_0 = 2024 + 0;
	public static final int TILE_WALL_1 = 2024 + 1;
	public static final int TILE_WALL_2 = 2024 + 2;
	public static final int TILE_WALL_3 = 2024 + 3;
	public static final int TILE_WALL_4 = 2024 + 4;
	public static final int TILE_WALL_5 = 2024 + 5;
	public static final int TILE_WALL_6 = 2024 + 6;
	public static final int TILE_WALL_7 = 2024 + 7;
	public static final int TILE_WALL_8 = 2024 + 8;
	public static final int TILE_WALL_9 = 2024 + 9;	
	
	public static final int TILE_HEADQUARTERS = 3024;
	public static final int TILE_FLAG = 4024;
	public static final int TILE_AGENT = 5024;
	public static final int TILE_AGENT_FLAG = 5024 + 1;
	
	public int getWidth();
	
	public int getHeight();
	
	public int getBaseTile(int x, int y);
	
	public int getBodyTile(int x, int y);
	
	public float getBodyOffsetX(int x, int y);
	
	public float getBodyOffsetY(int x, int y);
	
	public Color getBodyColor(int x, int y);
	
}
