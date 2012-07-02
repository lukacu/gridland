package org.grid.server;

import java.awt.Color;
import java.util.Arrays;

import org.grid.arena.Arena;
import org.grid.arena.SwingView.Palette;
import org.grid.server.Field.BodyPosition;
import org.grid.server.History.HistoryPosition;


public class VisitMap implements Arena, Palette, GameListener {

	private static Color heatPalette[];
	
	static {
		
		heatPalette = new Color[32];
		
		heatPalette[0] = Color.black;
		heatPalette[1] = Color.gray;
		
		for (int i = 2; i < heatPalette.length; i++) {
			heatPalette[i] = new Color(Math.max(100, (i * 255) / heatPalette.length),
					Math.max(0, (i * 255) / heatPalette.length), 50);	
		}

	}
	
	public static Color getHeatColor(int value) {

		return heatPalette[Math.min(heatPalette.length, value)];

	}
	
	private int[] cells;
	
	private int width, height;
	
	private Field field;
	
	private Agent agent;
	
	private BodyPosition lastPosition = null;
	
	private int neighborhoodSize;
	
	public VisitMap(Field field, History history, Agent agent, int neighborhoodSize) {
		this.field = field;
		this.width = field.getWidth();
		this.height = field.getHeight();
		
		this.agent = agent;
		this.cells = new int[width * height];
		Arrays.fill(cells, 0);
		
		this.neighborhoodSize = neighborhoodSize;
		
		setFromHistory(history, agent.getTeam(), agent.getId());
	}
	
	public int getHeight() {
		return height;
	}

	public int getWidth() {
		return width;
	}

	public void clear() {
		Arrays.fill(cells, 0);
	}
	
	private void setFromHistory(History history, Team team, int agent) {
		
		clear();
		
		Iterable<HistoryPosition> h = history.getAgentHistory(team, agent);
		
		if (h == null)
			return;
		
		lastPosition = null;
		
		for (HistoryPosition p : h) {
			if (lastPosition == null || !p.equals(lastPosition)) {

				cells[p.getY() * width + p.getX()]++;
				
				markNeighborhood(p.getX(), p.getY());
			}
			
			lastPosition = p;
		}
		
		
	}

	@Override
	public Color getColor(int tile) {
		
		if (tile > -1) {
			return heatPalette[Math.min(heatPalette.length-1, tile)];
		}
		
		return Color.gray;
	}

	@Override
	public int getBaseTile(int x, int y) {
		return cells[y * width + x];
	}

	@Override
	public Color getBodyColor(int x, int y) {
		return field.getBodyColor(x, y);
	}

	@Override
	public float getBodyOffsetX(int x, int y) {
		return field.getBodyOffsetX(x, y);
	}

	@Override
	public float getBodyOffsetY(int x, int y) {
		return field.getBodyOffsetY(x, y);
	}

	@Override
	public int getBodyTile(int x, int y) {
		return field.getBodyTile(x, y);
	}

	@Override
	public void message(Team team, int from, int to, int length) {

	}

	@Override
	public void position(Team team, int id, BodyPosition p) {
		
		if (agent.getId() != id)
			return;
		
		if (lastPosition == null || !p.equals(lastPosition)) {

			cells[p.getY() * width + p.getX()]++;
			markNeighborhood(p.getX(), p.getY());
		}
		lastPosition = p;
	}

	private void markNeighborhood(int x, int y) {
		
		int sX = Math.max(0, x - neighborhoodSize);
		int eX = Math.min(width-1, x + neighborhoodSize);
		int sY = Math.max(0, y - neighborhoodSize);
		int eY = Math.min(height-1, y + neighborhoodSize);		
		
		for (int j = sY; j <= eY; j++) {
			for (int i = sX; i <= eX; i++) {
				
				if (cells[j * width + i] == 0)
					cells[j * width + i] = 1;
				
			}
		}
	
	}
	
	@Override
	public void step() {

	}
	
	public Agent getAgent() {
		return agent;
	}
}
