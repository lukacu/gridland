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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridLayout;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.AbstractAction;
import javax.swing.BorderFactory;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.Scrollable;

import org.grid.server.Dispatcher.Client;
import org.grid.server.StackLayout.Orientation;


public class ClientsPanel extends JPanel {

	private static final long serialVersionUID = 1L;

	private final Color selectedBackground, normalBackground;
	
	public static interface SelectionObserver {
		
		public void clientSelected(Client client);
		
	}
	
	private interface Selectable {
		
		public void select();
		
		public void deselect();
		
	}
	
	private static class ScrollabeListPanel extends JPanel implements Scrollable {

		private static final long serialVersionUID = 1L;

		public ScrollabeListPanel() {
			super(new StackLayout(Orientation.VERTICAL));
		}
		
		@Override
		public Dimension getPreferredScrollableViewportSize() {
			return new Dimension(1000, 500);
		}

		@Override
		public int getScrollableBlockIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			return 1;
		}

		@Override
		public boolean getScrollableTracksViewportHeight() {
			return false;
		}

		@Override
		public boolean getScrollableTracksViewportWidth() {
			return true;
		}

		@Override
		public int getScrollableUnitIncrement(Rectangle visibleRect,
				int orientation, int direction) {
			return 1;
		}
		
	}
	
	private class TeamPanel extends JPanel implements TeamListener {

		private static final long serialVersionUID = 1L;
		
		private Hashtable<Client, ClientPanel> clients = new Hashtable<Client, ClientPanel>();

		private JPanel clientPanel = new ScrollabeListPanel();
		
		private JLabel score = new JLabel("0");
		
		private JLabel title;
		
		private JPanel header = new JPanel();
		
		private Team team;
		
		public TeamPanel(Team team) {
			
			this.team = team;
			
			Color color = team.getColor();
			
			double gray = 0.2989 * color.getRed() + 0.5870 * color.getGreen() + 0.1140 * color.getBlue(); 
			
			header.setBackground(gray > 128 ? Color.DARK_GRAY : Color.WHITE);
			
			setLayout(new BorderLayout());

			header.setBorder(BorderFactory.createEmptyBorder(2, 5, 2, 5));
			
			header.setLayout(new BorderLayout(4, 4));
			
			title = new JLabel(team.getName());
			title.setForeground(color);
			title.setFont(getFont().deriveFont(Font.BOLD, 14));
			title.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
			
			score.setOpaque(false);
			score.setForeground(color);
			score.setFont(getFont().deriveFont(Font.BOLD, 14));
			score.setBorder(BorderFactory.createEmptyBorder(5, 0, 5, 0));
			score.setHorizontalAlignment(JLabel.RIGHT);
			
			header.add(title, BorderLayout.CENTER);
			header.add(score, BorderLayout.EAST);
			
			add(header, BorderLayout.NORTH);
			add(new JScrollPane(clientPanel), BorderLayout.CENTER);
			
			
			team.addListener(this);

		}
		
		@Override
		public void clientConnect(Team team, Client client) {
			
			if (this.team != team)
				return;
			
			ClientPanel panel = new ClientPanel(client);
			
			clients.put(client, panel);
			clientPanel.add(panel);
			clientPanel.revalidate();
			
		}

		@Override
		public void clientDisconnect(Team team, Client client) {
			
			if (this.team != team)
				return;
			
			ClientPanel panel = clients.remove(client);
			
			if (panel == selected) {
				ClientsPanel.this.select((Client) null);
			}
			
			if (panel != null) {
				clientPanel.remove(panel);
			}
			
			//revalidate();
			clientPanel.repaint();
			clientPanel.revalidate();
		}

		@Override
		public void scoreChange(Team team, int score) {
			this.score.setText(score + "");
		}


	}
	
	private class ClientPanel extends JPanel implements ClientListener, Selectable {

		private static final long serialVersionUID = 1L;
		
		Client client;
		
		JLabel clientInfo = new JLabel();
		
		JLabel agentInfo = new JLabel();
		
		JPanel buttons;
		
		JButton disconnect = new JButton(new AbstractAction("Disconnect") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				ClientPanel.this.client.close();
				
			}
		});
		
		JButton kill = new JButton(new AbstractAction("Kill") {
			
			private static final long serialVersionUID = 1L;

			@Override
			public void actionPerformed(ActionEvent arg0) {
				
				Agent agent = ClientPanel.this.client.getAgent();
				
				if (agent != null) {
					agent.die();
				}
				
			}
		});
		
		TrafficMonitor traffic = new TrafficMonitor(20);
		
		private ClientPanel(Client cl) {
			super();
			this.client = cl;
			client.addListener(this);
			
			setBorder(BorderFactory.createEmptyBorder(5, 5, 5, 5));
			
			setLayout(new BorderLayout(3, 3));
			Box box = new Box(BoxLayout.Y_AXIS);
			add(box, BorderLayout.CENTER);
			
			clientInfo.setPreferredSize(new Dimension(500, 30));
			
			clientInfo.setFont(getFont().deriveFont(Font.BOLD, 12.0f));
			agentInfo.setFont(getFont().deriveFont(Font.PLAIN, 9.0f));
			
			box.add(clientInfo);
			box.add(agentInfo);
			
			clientInfo.setText(client.toString());
			
			agentInfo.setText("n/a");
			
			Box side = new Box(BoxLayout.X_AXIS);
			add(side, BorderLayout.EAST);
			
			side.add(traffic);
			
			buttons = new JPanel(new StackLayout(Orientation.HORIZONTAL));
			buttons.setOpaque(false);
			buttons.add(Box.createHorizontalStrut(5));
			buttons.add(disconnect);
			buttons.add(Box.createHorizontalStrut(5));
			buttons.add(kill);
			
			addMouseListener(new MouseAdapter() {
				
				@Override
				public void mouseClicked(MouseEvent e) {
					
					ClientsPanel.this.select(ClientPanel.this.client);
				}
				
			});
		}

		@Override
		public void agent(Client client, Agent agent) {

			if (this.client != client) {
				System.out.println("Not correct client " + client + " "  + this.client);
				return;
			}
			
			agentInfo.setText(agent == null ? "n/a" : "Id: " + agent.getId());
			
			if (selected == this) {
				ClientsPanel.this.select(this.client);
				ClientsPanel.this.select(this.client);
			}
			
		}

		@Override
		public void transfer(Client client, int messages) {
			if (this.client != client) {
				System.out.println("Not correct client " + client + " "  + this.client);
				return;
			}
			traffic.push((float)messages / game.getSpeed());
		}

		@Override
		public void deselect() {
			setBackground(normalBackground);
			clientInfo.setBackground(normalBackground);
			agentInfo.setBackground(normalBackground);
			
			remove(buttons);
			
			revalidate();
		}

		@Override
		public void select() {
			setBackground(selectedBackground);
			clientInfo.setBackground(selectedBackground);
			agentInfo.setBackground(selectedBackground);
			
			add(buttons, BorderLayout.SOUTH);
			
			revalidate();
		}
		
	}
	
	private static class TrafficMonitor extends JPanel {

		private static final long serialVersionUID = 1L;

		private ConcurrentLinkedQueue<Float> buffer = new ConcurrentLinkedQueue<Float>();
		
		private int max = 5, length;
		
		public TrafficMonitor(int length) {
			super();
			this.length = length;
			setBackground(Color.BLACK);
		}
		
		public void push(float messages) {
			buffer.add(messages);
			if (buffer.size() > length) {
				buffer.poll();
			}
			
			repaint();
		}
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			
			int barWidth = getWidth() / length;
			
			Iterator<Float> traffic = buffer.iterator();
			
			for (int i = 0; i < length; i++) {
				
				if (!traffic.hasNext())
					break;
				
				float output = ((float)traffic.next() / (float) max);
				
				if (output < 0.7f) {
					g.setColor(Color.GREEN);
				} else if (output < 0.9f) {
					g.setColor(Color.YELLOW);
				} else {
					g.setColor(Color.RED);
				}
			
				int barHeight = (int) (getHeight() * output); 
				
				g.fillRect(barWidth*i, getHeight() - barHeight, barWidth, barHeight);
				
			}

		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(3 * length, max);
		}
	}
	
	private Hashtable<Team, TeamPanel> teams = new Hashtable<Team, TeamPanel>();
	
	private SelectionObserver observer;
	
	private Game game;
	
	public ClientsPanel(Game game, SelectionObserver observer) {
		super(true);

		this.game = game;
		
		this.observer = observer;
		
		setLayout(new GridLayout(game.getTeams().size(), 1));
		
		selectedBackground = getBackground().brighter().brighter().brighter();
		normalBackground = getBackground();
		
		for (Team t : game.getTeams()) {
			
			TeamPanel tp = new TeamPanel(t);
			
			add(tp);
	
			teams.put(t, tp);
			
		}
		
		setMaximumSize(new Dimension(400, Integer.MAX_VALUE));
		setMinimumSize(new Dimension(200, 200));
	}
	
	@Override
	public Dimension getPreferredSize() {
		return new Dimension(240, 200);
	}
	
	private Selectable selected = null;
	
	public void select(Client client) {
		if (client == null) {
			if (selected != null) {
				selected.deselect();
				selected = null;
			}
			if (observer != null)
				observer.clientSelected(null);
			return;
		}
		
		TeamPanel tp = teams.get(client.getTeam());
		
		if (tp == null)
			return;
	
		ClientPanel cp = tp.clients.get(client);
		
		if (cp == null) {
			return;
		}
		
		if (selected != null && selected == cp) {
			selected.deselect();	
			selected = null;
			
			if (observer != null)
				observer.clientSelected(null);
		} else {
			if (selected != null) {
				selected.deselect();
			}

			selected = cp;
			cp.select();
			
			if (observer != null)
				observer.clientSelected(cp.client);
			
		}

	}
}
