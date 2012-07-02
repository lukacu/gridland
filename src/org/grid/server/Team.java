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
package org.grid.server;

import java.awt.Color;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.arena.Arena;
import org.grid.server.Dispatcher.Client;
import org.grid.server.Field.Body;
import org.grid.server.Field.BodyPosition;
import org.grid.server.Game.MessageContainter;


public class Team {

	private static final int MAX_ID = (1 << 30) - 1;
	
	public static class TeamBody extends Body {
		
		private Team team;
		
		public TeamBody(int tile, Team team) {
			super(tile);
			this.team = team;
			
			
		}
		
		public Team getTeam() {
			return team;
		}
	}
	
	public static class Flag extends TeamBody {

		private float weight;
		
		private Flag(int tile, Team team, float weight) {
			super(tile, team);
			this.weight = weight;
		}
		
		public float getWeight() {
			return weight;
		}
		
	}

	public static class Headquarters extends TeamBody {

		public Headquarters(int tile, Team team) {
			super(tile, team);
		}

		public void putFlag(Flag flag) {
			
			if (flag != null && flag.getTeam() == getTeam()) {
				
				if (getTeam().flags.contains(flag)) {
				
					getTeam().score++;
					getTeam().scoreChange(getTeam().score);
					getTeam().flags.remove(flag);
					
					Main.log("Team %s: %d flags collected", getTeam().getName(), getTeam().score);
					
				}
			}
			
		}
		
	}
	
	private HashSet<Client> used = new HashSet<Client>();
	
	private ConcurrentLinkedQueue<Client> pool = new ConcurrentLinkedQueue<Client>();
	
	private LinkedList<Agent> removed = new LinkedList<Agent>();
	
	private HashSet<Integer> allocatedIds = new HashSet<Integer>();
	
	private HashSet<Flag> flags = new HashSet<Flag>();
	
	private String name, passphrase;
	
	private Headquarters hq;
	
	private Color color;
	
	private int score = 0;
	
	public Team(String name, Color color) {
		
		this.name = name;
		this.color = color;
		this.passphrase = null;

		hq = new Headquarters(Arena.TILE_HEADQUARTERS, this);

	}
	
	public String getPassphrase() {
		return passphrase;
	}

	public void setPassphrase(String passphrase) {
		this.passphrase = passphrase;
	}

	public Headquarters getHeadquarters() {
		
		return hq;
		
	}
	
	public Color getColor() {
		
		return color;
		
	}
	
	public String getName() {
		
		return name;
		
	}
	
	public Flag newFlag(float weight) {
		
		Flag f = new Flag(Arena.TILE_FLAG, this, weight);
		
		flags.add(f);
		
		return f;
		
	}
	
	public Agent newAgent() {
		
		synchronized (pool) {
			
			Client client = pool.poll();
			
			if (client == null)
				return null;
			
			Agent agt = new Agent(this, getUniqueId());
			
			client.setAgent(agt);
			used.add(client);
			
			Main.log("New agent spawned for team: " + name + " (id: " + agt.getId() + ")");
			
			return agt;
		}
		

	}
	
	public void addClient(Client client) {

		if (client == null)
			return;
		
		synchronized (pool) {
			
			pool.add(client);
			
		}

		clientConnected(client);
	}
	
	public void removeClient(Client client) {
		
		Main.log("Remove client: " + client);
		
		if (client == null)
			return;
		
		synchronized (pool) {

			if (client.getAgent() != null) {
				client.getAgent().die();
				removed.add(client.getAgent());
			}
			
			// just in case ... remove from everywhere :)
			pool.remove(client);
			used.remove(client);
			
		}

		clientDisconnected(client);
		
	}
	
	public int size() {
		
		synchronized (pool) {
			
			return used.size();
			
		}
		
		
	}

	public void cleanup(Field field) {
		
		synchronized (pool) {
			Vector<Client> remove = new Vector<Client>();
			
			for (Client c : used) {
			
				if (c.getAgent() == null) {
					remove.add(c);
					continue;
				}
				
				if (!c.getAgent().isAlive()) {

					removed.add(c.getAgent());
					
					c.setAgent(null);
					remove.add(c);
				}
				
			}
			
			used.removeAll(remove);
			pool.addAll(remove);
			
			for (Agent a : removed) {
				
				BodyPosition pos = field.getPosition(a);
				
				field.removeBody(a);
				
				if (a.hasFlag() && pos != null) {
					for (Flag flag : a.getFlags())
						field.putBodyCloseTo(flag, new BodyPosition(pos.getX(), pos.getY()));
				}
			}

			removed.clear();
			
		}
	}
	
	
	public List<Agent> move(Field field) {
		
		Vector<Agent> moved = new Vector<Agent>();
		
		synchronized (pool) {

			for (Client c : used) {
			
				if (c.getAgent() != null) {
					if (c.getAgent().move(field))
						moved.add(c.getAgent());
				}
				
			}

		}
	
		return moved;
	}
	
	public void dispatch() {
		
		synchronized (pool) {

			for (Client c : used) {
			
				if (c.getAgent() != null) {
					MessageContainter msg = c.getAgent().pullMessage();
					if (msg != null) {
						
						Client cltto = findById(msg.getTo());
						
						if (cltto != null)
							cltto.send(c.getAgent().getId(), msg.getMessage());
						
					}
				}
			}
		}
	}
	
	public Client findById(int id) {
		
		synchronized (pool) {
		
			for (Client cl : used) {
				
				if (cl.getAgent() != null && cl.getAgent().getId() == id)
					return cl;
				
			}
			
			return null;
		}
		
	}
	
	public int getActiveFlagsCount() {
		return flags.size();
	}
	
	public String toString() {
		return name;
	}
	
	private int getUniqueId() {
		
		while (true) {
		
		int id = Math.max(1, Math.min(MAX_ID, (int) (Math.random() * MAX_ID)));
		
			if (!allocatedIds.contains(id)) {
				allocatedIds.add(id);
				return id;
			}
		
		}

	}
	
	private Vector<TeamListener> listeners = new Vector<TeamListener>();

	public void addListener(TeamListener listener) {
		synchronized (listeners) {
			listeners.add(listener);
		}
		
	}
	
	public void removeListener(TeamListener listener) {
		synchronized (listeners) {
			listeners.remove(listener);
		}
	}
	
	private void clientConnected(Client client) {
		
		synchronized (listeners) {
			for (TeamListener l : listeners) {
				try {
					l.clientConnect(this, client);
				} catch (Exception e) {e.printStackTrace();}
				
			}
		}
	}
	
	private void clientDisconnected(Client client) {
		
		synchronized (listeners) {
			for (TeamListener l : listeners) {
				try {
					l.clientDisconnect(this, client);
				} catch (Exception e) {e.printStackTrace();}
				
			}
		}
	}
	
	private void scoreChange(int score) {
		
		synchronized (listeners) {
			for (TeamListener l : listeners) {
				try {
					l.scoreChange(this, score);
				} catch (Exception e) {e.printStackTrace();}
				
			}
		}
	}
}
