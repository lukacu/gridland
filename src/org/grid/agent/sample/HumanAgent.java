package org.grid.agent.sample;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.KeyEventDispatcher;
import java.awt.KeyboardFocusManager;
import java.awt.Polygon;
import java.awt.event.KeyEvent;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.arena.SwingView;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.Message.Direction;


@Membership(team="humans",passphrase="")
public class HumanAgent extends Agent {

	private JFrame window;

	private SwingView view = new SwingView(CELL_SIZE);

	private SideView side = new SideView();
	
	protected static final int CELL_SIZE = 42;
	
	private Polygon flag = SwingView.getFlagGlyph(CELL_SIZE);

	private class SideView extends JPanel {
		
		private static final long serialVersionUID = 1L;

		private Direction direction = Direction.NONE;
		
		private boolean hasFlag = false;
		
		@Override
		public void paint(Graphics g) {
			super.paint(g);
			
			if (hasFlag) {
				g.fillPolygon(flag);
			}
			
			g.drawString(direction.toString(), 10, 70);
					
		}
		
		@Override
		public Dimension getPreferredSize() {
			return new Dimension(50, 100);
		}
		
		public void update(Direction direction, boolean hasFlag) {
			this.direction = direction;
			this.hasFlag = hasFlag;
			
			repaint();
		}
		
	}
	
	private KeyEventDispatcher keys = new KeyEventDispatcher() {
	    @Override
	    public boolean dispatchKeyEvent(KeyEvent e) {

			if (!isAlive())
				return false;
			
			switch (e.getKeyCode()) {
			case KeyEvent.VK_UP: 
				move(Direction.UP);
				break;
			case KeyEvent.VK_LEFT: 
				move(Direction.LEFT);
				break;
			case KeyEvent.VK_RIGHT: 
				move(Direction.RIGHT);
				break;
			case KeyEvent.VK_DOWN: 
				move(Direction.DOWN);
				break;
			}
	        return false;
	    }
	};

	
	@Override
	public void initialize() {

		window = new JFrame("Remote Control [id: " + getId() + "]");

		window.getContentPane().setLayout(new BorderLayout());
		
		window.getContentPane().add(view, BorderLayout.CENTER);
		
		window.getContentPane().add(side, BorderLayout.EAST);

		window.getContentPane().setFocusable(false);

		window.setResizable(false);
		
		window.pack();
		
		window.setVisible(true);
		
		KeyboardFocusManager.getCurrentKeyboardFocusManager().addKeyEventDispatcher(keys);

	}

	@Override
	public void receive(int from, byte[] message) {

	}

	private Object waitMutex = new Object();

	private void scanAndWait() throws InterruptedException {

		synchronized (waitMutex) {
			scan(0);
			waitMutex.wait();
		}

	}
	
	@Override
	public void run() {

		while (isAlive()) {

			try {
				scanAndWait();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			try {
				Thread.sleep(500);
			} catch (InterruptedException e) {
			}

		}
	}

	@Override
	public void state(int stamp, Neighborhood neighborhood,
			Direction direction, boolean hasFlag) {
		
		side.update(direction, hasFlag);
		view.update(neighborhood);
		
		window.pack();
		
		synchronized (waitMutex) {
			waitMutex.notifyAll();
		}


	}

	@Override
	public void terminate() {
	
		window.setVisible(false);
	
		KeyboardFocusManager.getCurrentKeyboardFocusManager().removeKeyEventDispatcher(keys);
		
	}

}
