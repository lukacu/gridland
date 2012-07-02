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
import java.awt.Graphics;
import java.awt.GraphicsEnvironment;
import java.awt.Rectangle;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedList;

import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;

import org.grid.arena.Arena;
import org.grid.arena.SwingView;
import org.grid.server.ClientsPanel.SelectionObserver;
import org.grid.server.Dispatcher.Client;
import org.grid.server.Field.Body;
import org.grid.server.Field.BodyPosition;
import org.grid.server.Field.Cell;


public class Main {

	private static final int PORT = 5000;

	private static final String RELEASE = "0.9";

	private static Game game;

	private static long renderTime = 0;

	private static int renderCount = 0;

	private static long stepTime = 0;

	private static int stepCount = 0;

	private static Object mutex = new Object();

	private static History history = new History();

	private static boolean running = false;

	private static GameSwingView view = new GameSwingView();

	private static ClientsPanel clientsPanel = null;
	
	private static JLabel gameStepDisplay = new JLabel();
	
	private static PrintWriter log;
	
	private static final String[] ZOOM_LEVELS_TITLES = new String[] {"tiny", "small", "normal",
			"big", "huge" };

	private static final int[] ZOOM_LEVELS = new int[] {6, 12, 16, 20, 24 };
	
	private static final int MAX_TEAMS_VERBOSE = 4;
	
	private static Action playpause = new AbstractAction("Play") {

		private static final long serialVersionUID = 1L;

		@Override
		public void actionPerformed(ActionEvent arg0) {

			running = !running;

			setEnabled(false);
			
			//putValue(AbstractAction.NAME, running ? "Pause" : "Play");
		}
	};

	private static class GameSwingView extends SwingView implements
			GameListener, SelectionObserver, MouseListener {

		private static final long serialVersionUID = 1L;

		private static final int BUFFER_LIFE = 10;

		private LinkedList<Message> buffer = new LinkedList<Message>();

		private VisitMap visualization = null;

		public class Message {

			private int length, step;

			private Agent sender, receiver;

			public Message(Agent sender, Agent receiver, int length) {
				super();
				this.sender = sender;
				this.receiver = receiver;
				this.length = length;
				this.step = game.getStep();
			}

		}

		public GameSwingView() {
			super(12);
			addMouseListener(this);
		}

		@Override
		public void paint(Graphics g) {

			long start = System.currentTimeMillis();

			Arena view = getArena();

			paintBackground(g, visualization == null ? view : visualization);

			paintObjects(g, view);

			LinkedList<Message> active = new LinkedList<Message>();
			int current = game.getStep();

			synchronized (buffer) {

				for (Message m : buffer) {

					if (current - m.step < BUFFER_LIFE)
						active.add(m);
				}
			}

			Field field = game.getField();

			g.setColor(Color.YELLOW);

			for (Message m : active) {

				BodyPosition p1 = field.getPosition(m.sender);
				BodyPosition p2 = field.getPosition(m.receiver);

				if (p1 == null || p2 == null)
					continue;

				int x1 = (int) ((p1.getX() + p1.getOffsetX()) * cellSize)
						+ cellSize / 2;
				int y1 = (int) ((p1.getY() + p1.getOffsetY()) * cellSize)
						+ cellSize / 2;
				int x2 = (int) ((p2.getX() + p2.getOffsetX()) * cellSize)
						+ cellSize / 2;
				int y2 = (int) ((p2.getY() + p2.getOffsetY()) * cellSize)
						+ cellSize / 2;

				g.drawLine(x1, y1, x2, y2);

				float progress = (float) (current - m.step) / BUFFER_LIFE;

				int size = Math.min(8, Math.max(3, m.length / cellSize));

				g.fillRect((int) ((1 - progress) * x1 + progress * x2) - size
						/ 2, (int) ((1 - progress) * y1 + progress * y2) - size
						/ 2, size, size);

			}

			if (visualization != null) {
				
				BodyPosition p = field.getPosition(visualization.getAgent());
				
				if (p != null) {
					
					int translateX = (int) (p.getOffsetX() * cellSize);
					int translateY = (int) (p.getOffsetY() * cellSize);
					
					g.drawOval(p.getX() * cellSize + translateX, p.getY() * cellSize
							 + translateY, cellSize, cellSize);
					
					
				}
				
			}
			
			
			synchronized (buffer) {
				buffer = active;
			}

			long used = System.currentTimeMillis() - start;

			synchronized (mutex) {

				renderTime += used;
				renderCount++;

			}

		}

		@Override
		public void message(Team team, int from, int to, int length) {
			synchronized (buffer) {
				try {
					Agent sender = team.findById(from).getAgent();
					Agent receiver = team.findById(to).getAgent();
					if (sender == null || receiver == null)
						return;
					buffer.add(new Message(sender, receiver, length));
				} catch (NullPointerException e) {
				}
			}

		}

		@Override
		public void clientSelected(Client client) {

			synchronized (this) {
				if (client == null) {
					if (visualization != null)
						game.removeListener(visualization);
					visualization = null;
					setBasePallette(null);
					return;
				}

				Agent a = client.getAgent();

				if (a == null)
					return;

				visualization = new VisitMap(game.getField(), history, a, game
						.getNeighborhoodSize());
				setBasePallette((Palette) visualization);
				game.addListener(visualization);
			}

		}

		@Override
		public void position(Team team, int id, BodyPosition p) {

		}

		@Override
		public void step() {

		}

		@Override
		public void mouseClicked(MouseEvent e) {
			
			int x = e.getX() / cellSize;
			
			int y = e.getY() / cellSize;
			
			Field field = game.getField();
			
			Cell cell = field.getCell(x, y);
			
			if (cell != null) {
				Body b = cell.getBody();
				
				if (b instanceof Agent) {
					
					Client cl = ((Agent)b).getTeam().findById(((Agent)b).getId());
					
					if (cl != null && Main.clientsPanel != null) {
						Main.clientsPanel.select(cl);
					}
					
				}
				
			}
			
		}

		@Override
		public void mouseEntered(MouseEvent e) {}

		@Override
		public void mouseExited(MouseEvent e) {}

		@Override
		public void mousePressed(MouseEvent e) {}

		@Override
		public void mouseReleased(MouseEvent e) {}

	}

	public static void main(String[] args) throws IOException {
		
		info("Starting game server (release %s)", RELEASE);

		if (args.length < 1) {
			info("Please provide game description file location as an argument.");
			System.exit(1);
		}

		info("Java2D OpenGL acceleration "
				+ (("true".equalsIgnoreCase(System
						.getProperty("sun.java2d.opengl"))) ? "enabled"
						: "not enabled"));

		game = Game.loadFromFile(new File(args[0]));

		try {
			log = new PrintWriter(new File(logDate.format(new Date()) + "_" + game.getTitle() + ".log"));
			
		} catch (Exception e) {}
		
		
		Dispatcher dispatcher = new Dispatcher(PORT, game);

		final int gameSpeed = game.getSpeed();

		game.addListener(view);

		game.addListener(history);
		
		(new Thread(new Runnable() {

			@Override
			public void run() {
				int sleep = 1000 / gameSpeed;
				long start, used;
				while (true) {

					start = System.currentTimeMillis();

					if (running)
						game.step();

					view.update(game.getField());

					used = System.currentTimeMillis() - start;

					stepTime += used;
					stepCount++;

					if (game.getStep() % 100 == 0 && running) {
						long renderFPS, stepFPS;

						synchronized (mutex) {
							renderFPS = (renderCount * 1000)
									/ Math.max(1, renderTime);
							renderCount = 0;
							renderTime = 0;
						}

						stepFPS = (stepCount * 1000) / Math.max(1, stepTime);
						stepCount = 0;
						stepTime = 0;

						info(
										"Game step: %d (step: %d fps, render: %d fps)",
										game.getStep(), stepFPS, renderFPS);
					}

					if (game.getStep() % 10 == 0) {
						gameStepDisplay.setText(String.format("Step: %d", game.getStep()));
					}
					
					try {
						if (used < sleep)
							Thread.sleep(sleep - used);
						else {
							info("Warning: low frame rate");
						}
					} catch (InterruptedException e) {
						e.printStackTrace();
					}

				}

			}
		})).start();

		JFrame window = new JFrame("AgentField - " + game.getTitle());

		window.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		final JScrollPane pane = new JScrollPane(view);

		JPanel left = new JPanel(new BorderLayout());

		left.add(pane, BorderLayout.CENTER);

		JPanel status = new JPanel(new BorderLayout());

		status.add(new JButton(playpause), BorderLayout.WEST);

		final JComboBox zoom = new JComboBox(ZOOM_LEVELS_TITLES);

		zoom.addActionListener(new ActionListener() {

			@Override
			public void actionPerformed(ActionEvent e) {
				int ind = zoom.getSelectedIndex();
				
				if (ind > -1) {
					view.setCellSize(ZOOM_LEVELS[ind]);
					pane.repaint();
				}
			}
		});

		zoom.setSelectedIndex(1);
		zoom.setEditable(false);
		
		status.add(zoom, BorderLayout.EAST);
		
		gameStepDisplay.setHorizontalAlignment(JLabel.CENTER);
		status.add(gameStepDisplay, BorderLayout.CENTER);
		
		left.add(status, BorderLayout.NORTH);

		if (game.getTeams().size() > MAX_TEAMS_VERBOSE) {
			
			log("Warning: too many teams, reducing the GUI");
			
			window.getContentPane().add(left);
			
		} else {
			
			clientsPanel = new ClientsPanel(game, view);	
			window.getContentPane().add(
					new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, left,
							clientsPanel));
			
		}
		
		GraphicsEnvironment ge = GraphicsEnvironment
				.getLocalGraphicsEnvironment();

		Rectangle r = ge.getDefaultScreenDevice().getDefaultConfiguration()
				.getBounds();

		window.pack();

		Dimension ws = window.getSize();
		
		if (r.width - ws.width < 100) {
			ws.width = r.width - 100;
		}
		
		if (r.height - ws.height < 100) {
			ws.height = r.height - 100;
		}
		
		window.setSize(ws);
		
		window.setVisible(true);

		(new Thread(dispatcher)).start();
		
		log("Server ready.");
		
	}
	
	private static DateFormat logDate = new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss");
	
	private static DateFormat date = new SimpleDateFormat("[hh:mm:ss] ");
	
	public static void log(String format, Object ... objects) {
		
		try {
		
			String msg = String.format(format, objects);
			
			System.out.println(date.format(new Date()) + msg);
		
			if (log != null) {
				log.println(date.format(new Date()) + msg);
				log.flush();
			}
			
		} catch (Exception e) { e.printStackTrace(); }
	}
	
	public static void info(String format, Object ... objects) {
		
		//System.out.println(date.format(new Date()) + String.format(format, objects));
		
	}
	
}
