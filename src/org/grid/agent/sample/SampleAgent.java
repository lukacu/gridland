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
package org.grid.agent.sample;

import java.util.Arrays;
import java.util.HashMap;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.arena.TerminalView;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.Position;
import org.grid.protocol.Message.Direction;


// Run: java -cp bin fri.pipt.agent.Agent localhost fri.pipt.agent.sample.SampleAgent

@Membership(team="samples",passphrase="c66ddf4d73f77d52162cb3c2f9678074")
public class SampleAgent extends Agent {

	private static enum AgentState {
		EXPLORE, SEEK, RETURN
	}

	//private static double TEST = Math.random();
	
	private int x = 0;

	private int y = 0;

	private HashMap<String, Position> registry = new HashMap<String, Position>();

	private AgentState state = AgentState.EXPLORE;

	private Decision left, right, up, down, still;
	
	private Decision[] decisions;
	
	private int sn = 1;
	private long sx, sy;
	
	@Override
	public void initialize() {

		left = new Decision(0, Direction.LEFT);
		right = new Decision(0, Direction.RIGHT);
		up = new Decision(0, Direction.UP);
		down = new Decision(0, Direction.DOWN);
		still = new Decision(0, Direction.NONE);
		
		decisions = new Decision[] {
			left, right, up, down, still	
		};
		
	}

	@Override
	public void receive(int from, byte[] message) {

		String msg = new String(message);

		System.out.format("Message recived from %d: %s\n", from, msg);

	}

	@Override
	public void state(int stamp, Neighborhood neighborhood, Direction direction,
			boolean hasFlag) {

		
		synchronized (waitMutex) {
			this.neighborhood = neighborhood;
			this.direction = direction;

			if (state != AgentState.RETURN && hasFlag)
				state = AgentState.RETURN;

			this.hasFlag = hasFlag;
			
			waitMutex.notify();
		}
	}

	@Override
	public void terminate() {

	}

	private Direction moving = Direction.DOWN;

	protected static class Decision implements Comparable<Decision> {

		private float weight;

		private Direction direction;

		public float getWeight() {
			return weight;
		}

		public void setWeight(float weight) {
			this.weight = weight;
		}
		
		public void multiplyWeight(float f) {
			this.weight *= f;
		}
		
		public Direction getDirection() {
			return direction;
		}

		public void setDirection(Direction direction) {
			this.direction = direction;
		}

		public Decision(float weight, Direction direction) {
			super();
			this.weight = weight;
			this.direction = direction;
		}

		@Override
		public int compareTo(Decision o) {
			if (weight < o.weight)
				return -1;

			if (weight > o.weight)
				return 1;

			return 0;
		}

		public String toString() {
			return String.format("%s (%.2f)", direction.toString(), weight);
		}
		
	}

	@Override
	public void run() {

		while (isAlive()) {

			try {

				scanAndWait();

				analyzeNeighborhood(neighborhood);

				//System.out.printf("%f %s\n", TEST, this.getClass().getClassLoader());
				
				System.out.printf("Current position: %d, %d, state: %s \n", x, y, state.toString());
				
				if (direction == Direction.NONE) {

					if (moving != Direction.NONE) {
						switch (moving) {
						case DOWN:
							y += 1;
							break;
						case UP:
							y -= 1;
							break;
						case LEFT:
							x -= 1;
							break;
						case RIGHT:
							x += 1;
							break;
						}
						
						sn++;
						sx += x;
						sy += y;
						
					}
					
					Decision d = updateDecisions(neighborhood, state);
					
					TerminalView view = new TerminalView();
					view.update(neighborhood);
					
					System.out.printf("Best move: %s %.2f \n", d.getDirection().toString(), d.getWeight());
					
					if (d.getDirection() != Direction.NONE) 
						move(d.getDirection());

					moving = d.getDirection();
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(1000);
			} catch (InterruptedException e) {
			}

		}

	}

	private Object waitMutex = new Object();

	private Neighborhood neighborhood;

	private Direction direction;

	private boolean hasFlag = false;
	
	private void scanAndWait() throws InterruptedException {

		synchronized (waitMutex) {
			scan(0);
			waitMutex.wait();
		}

	}

	private void analyzeNeighborhood(Neighborhood n) {

		for (int i = -n.getSize(); i <= n.getSize(); i++) {

			for (int j = -n.getSize(); j <= n.getSize(); j++) {

				if (n.getCell(i, j) == Neighborhood.FLAG && !hasFlag) {

					System.out.println("Found flag !!!");
					
					registry.put("flag", new Position(x + i, y + j));

					state = AgentState.SEEK;

					continue;
				}

				if (n.getCell(i, j) == Neighborhood.HEADQUARTERS) {

					registry.put("hq", new Position(x + i, y + j));
					
					continue;
				}

				if (n.getCell(i, j) > 0) {

					if (! (i == 0 && j == 0) )
						send(n.getCell(i, j), "Hello " + n.getCell(i, j) + "!");
					
					continue;
				}
				
			}

		}

	}

	private Decision updateDecisions(Neighborhood n, AgentState state) {
		
		still.setWeight(0.01f);
		down.setWeight(canMove(n, 0, 1, state) ? 1 : 0);
		up.setWeight(canMove(n, 0, -1, state) ? 1 : 0);
		left.setWeight(canMove(n, -1, 0, state) ? 1 : 0);
		right.setWeight(canMove(n, 1, 0, state) ? 1 : 0);
		
		switch (state) {
		case EXPLORE:
			
			int cx = (int) (sx / sn);
			int cy = (int) (sy / sn);
			
			//System.out.printf("%d %d %d %d %d\n", sx, sy, sn, cx, cy);
			
			//System.out.printf("%.2f\n", Math.log(Math.max(2, cy - y)));
			
			down.multiplyWeight((float)Math.log(Math.max(2, cy - y)) * random(0.7f, 1));
			up.multiplyWeight((float)Math.log(Math.max(2, y - cy)) * random(0.7f, 1));
			left.multiplyWeight((float)Math.log(Math.max(2, x - cx)) * random(0.7f, 1));
			right.multiplyWeight((float)Math.log(Math.max(2, cx - x)) * random(0.7f, 1));
			
			break;
		case RETURN: {
			
			Position p = registry.get("hq");
			
			if (p == null)
				return updateDecisions(n, AgentState.EXPLORE);
			
			down.multiplyWeight(Math.max(0.2f, p.getY() - y));
			up.multiplyWeight(Math.max(0.2f, y - p.getY()));
			left.multiplyWeight(Math.max(0.2f, x - p.getX()));
			right.multiplyWeight(Math.max(0.2f, p.getX() - x));
			
			break;
		}
		case SEEK: {
			
			Position p = registry.get("flag");
			
			if (p == null)
				return updateDecisions(n, AgentState.EXPLORE);
			
			down.multiplyWeight(Math.max(0.2f, p.getY() - y));
			up.multiplyWeight(Math.max(0.2f, y - p.getY()));
			left.multiplyWeight(Math.max(0.2f, x - p.getX()));
			right.multiplyWeight(Math.max(0.2f, p.getX() - x));

			break;
		}
		}
		
		Arrays.sort(decisions);
		
		/*for (Decision d : decisions) 
			System.out.println(d);*/
		
		return decisions[decisions.length - 1];
		
	}
	
	private boolean canMove(Neighborhood n, int x, int y, AgentState state) {
		
		switch (state) {
		case RETURN:
			return n.getCell(x, y) == Neighborhood.EMPTY || n.getCell(x, y) == Neighborhood.HEADQUARTERS;
		case SEEK:
			return n.getCell(x, y) == Neighborhood.EMPTY || n.getCell(x, y) == Neighborhood.FLAG;
		default:
			return n.getCell(x, y) == Neighborhood.EMPTY;		
		}
		
	}
	
	private static float random(float min, float max) {
		
		return (float) (Math.random() * (max - min)) + min;
		
	}
	
}
