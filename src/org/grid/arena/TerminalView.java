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

public class TerminalView implements ArenaView {

	@Override
	public void update(Arena view) {
		

		System.out.println();
		
		for (int j = 0; j < view.getHeight(); j++) {
		
			for (int i = 0; i < view.getWidth(); i++) {
				
				int body = view.getBodyTile(i, j);

				if (body >= Arena.TILE_WALL_0 && body <= Arena.TILE_WALL_9) {
					System.out.print('#');
					continue;
				}
				
				switch (body) {
				case Arena.TILE_AGENT: 
					System.out.print('a');
					continue;
				case Arena.TILE_AGENT_FLAG: 
					System.out.print((char)134);
					continue;
				case Arena.TILE_HEADQUARTERS: 
					System.out.print('h');
					continue;
				case Arena.TILE_FLAG:
					System.out.print('f');
					continue;
				}
				
				System.out.print(' ');
			}
	
			System.out.println();
		}
		
		System.out.println();
	}

}
