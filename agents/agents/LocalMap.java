package agents;

import java.awt.Color;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Hashtable;
import java.util.PriorityQueue;
import java.util.Vector;

import org.grid.arena.Arena;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.Position;
import org.grid.protocol.Message.Direction;


public class LocalMap {

	public static class Bounds {

		private int top = 0, bottom = 0, left = 0, right = 0;

		public Bounds(int top, int bottom, int left, int right) {
			this.top = top;
			this.bottom = bottom;
			this.left = left;
			this.right = right;
		}

		public Bounds(Bounds bounds) {

			this(bounds.top, bounds.bottom, bounds.left, bounds.right);

		}

		public int getTop() {
			return top;
		}

		public void setTop(int top) {
			this.top = top;
		}

		public int getBottom() {
			return bottom;
		}

		public void setBottom(int bottom) {
			this.bottom = bottom;
		}

		public int getLeft() {
			return left;
		}

		public void setLeft(int left) {
			this.left = left;
		}

		public int getRight() {
			return right;
		}

		public void setRight(int right) {
			this.right = right;
		}

		public void update(int x, int y) {

			top = Math.min(top, y);

			left = Math.min(left, x);

			bottom = Math.max(bottom, y);

			right = Math.max(right, x);

		}

		public boolean inside(Position p) {

			return p.getX() <= right && p.getX() >= left && p.getY() <= bottom
					&& p.getY() >= top;

		}

		public void offset(Position p) {

			right += p.getX();
			left += p.getX();

			top += p.getY();
			bottom += p.getY();

		}

		@Override
		public String toString() {
			return String.format("bounds (%d,%d,%d,%d)", top, bottom, left,
					right);
		}
	}

	public static class MapChunk {

		public static final int EMPTY = 0;

		public static final int WALL = 1;

		public static final int HEADQUARTERS = 2;

		public static final int FLAG = 3;

		public static final int OTHER_HEADQUARTERS = 4;

		public static final int OTHER_FLAG = 5;

		public static final int UNKNOWN = 6;

		private int data;

		private int timestep;

		private short x, y;

		public int get(int i) {

			int t = (data >> (i * 3)) & 7;

			return t;
		}

		public void set(int i, int v) {

			int t = (v & 7) << (i * 3);

			int m = ~(7 << (i * 3));

			data = (data & m) | t;

		}

		public void write(ObjectOutputStream out) throws IOException {

			out.writeShort(x);
			out.writeShort(y);

			out.writeInt(timestep);

			out.writeInt(data);
		}

		public void read(ObjectInputStream in) throws IOException {

			x = in.readShort();
			y = in.readShort();

			timestep = in.readInt();

			data = in.readInt();

		}
	}

	public class LocalMapArena implements Arena {

		private int scope;

		private int x, y;

		protected LocalMapArena(int scope, int x, int y) {
			super();
			this.scope = scope;
			this.x = x;
			this.y = y;
		}

		public void setOrigin(int x, int y) {
			this.x = x;
			this.y = y;
		}

		@Override
		public int getBaseTile(int x, int y) {

			Node n = get(x + this.x - scope, y + this.y - scope);

			if (n == null)
				return -1;

			Integer modifier = modifiers.get(n.point);
			int cost = n.getCost();
			if (modifier != null && cost != Integer.MAX_VALUE) {
				if (modifier == Integer.MAX_VALUE)
					cost = Integer.MAX_VALUE;
				else
					cost += modifier;
			}

			return cost / 300 + (n.foregin ? 10 : 0);
		}

		@Override
		public Color getBodyColor(int x, int y) {

			Node n = get(x + this.x - scope, y + this.y - scope);

			if (n == null)
				return null;

			if (n.body > 0 || n.body == Neighborhood.FLAG
					|| n.body == Neighborhood.HEADQUARTERS)
				return Color.BLUE;

			return Color.RED;
		}

		@Override
		public float getBodyOffsetX(int x, int y) {
			return 0;
		}

		@Override
		public float getBodyOffsetY(int x, int y) {
			return 0;
		}

		@Override
		public int getBodyTile(int x, int y) {
			Node n = get(x + this.x - scope, y + this.y - scope);

			if (n == null)
				return 0;

			return n.body;
		}

		@Override
		public int getHeight() {
			return scope * 2 + 1;
		}

		@Override
		public int getWidth() {
			return scope * 2 + 1;
		}

	}

	public static interface CostFormula {

		public int getCost(Node n);

	}

	public static final Filter BORDER_FILTER = new Filter() {

		@Override
		public boolean filter(Node n) {
			return n.body == 0 && n.isBorder();
		}

	};

	public static interface Filter {

		public boolean filter(Node n);

	}

	private static final CostFormula defaultFormula = new CostFormula() {

		@Override
		public int getCost(Node n) {
			switch (n.body) {
			case Neighborhood.EMPTY:
				return n.timestep;
			case Neighborhood.FLAG:
				return n.timestep;
			case Neighborhood.OTHER_FLAG:
			case Neighborhood.OTHER_HEADQUARTERS:
			case Neighborhood.OTHER:
			case Neighborhood.HEADQUARTERS:
			case Neighborhood.WALL:
			default:
				return Integer.MAX_VALUE;
			}
		}
	};

	private static Direction getDirection(Position from, Position to) {
		if (from.getY() == to.getY()) {
			if (from.getX() - to.getX() == -1)
				return Direction.RIGHT;
			if (from.getX() - to.getX() == 1)
				return Direction.LEFT;
			return Direction.NONE;
		}

		if (from.getY() - to.getY() == -1)
			return Direction.DOWN;
		if (from.getY() - to.getY() == 1)
			return Direction.UP;
		return Direction.NONE;
	}

	public static class Paths {

		private int P[];
		private Node N[];
		private Node from;

		public List<Direction> shortestPathTo(Node to) {

			Vector<Direction> path = new Vector<Direction>();

			int loc = to.id;
			Node current = to;
			// backtrack from the target by P(revious), adding to the result
			// list
			while (P[loc] != from.id) {
				if (P[loc] == -1) {
					// looks like there's no path from source to target
					return null;
				}
				path.add(0, getDirection(N[P[loc]].point, current.point));
				current = N[P[loc]];
				loc = P[loc];
			}
			path.add(0, getDirection(from.point, current.point));

			return path;

		}

		public List<Direction> shortestPathTo(Collection<Node> candidates) {

			int shortestWeight = Integer.MAX_VALUE;
			Vector<Direction> shortestPath = null;

			// System.out.println(Arrays.toString(P));

			for (Node to : candidates) {

				Vector<Direction> path = new Vector<Direction>();
				int weight = 0;

				int loc = to.id;
				Node current = to;
				// backtrack from the target by P(revious), adding to the result
				// list
				while (P[loc] != from.id) {
					if (P[loc] == -1) {
						// looks like there's no path from source to target
						weight = Integer.MAX_VALUE;
						break;
					}
					path.add(0, getDirection(N[P[loc]].point, current.point));
					weight += N[P[loc]].getCost();
					current = N[P[loc]];
					loc = P[loc];
				}
				path.add(0, getDirection(from.point, current.point));
				// if (path.lastElement() == Direction.NONE)
				// System.out.println(N[P[to.id]].point + " " + to.point);
				if (weight < shortestWeight && weight != Integer.MAX_VALUE) {
					shortestWeight = weight;
					shortestPath = path;
				}

			}

			return shortestPath;

		}

	}

	public class Node implements Comparable<Node> {

		private Position point;

		private int cost;

		private int timestep, body;

		private Node left, right, up, down;

		private int id;

		private boolean foregin;
		
		private Node(int x, int y, int timestamp, int body, int id) {
			point = new Position(x, y);
			update(timestamp, body);
			this.id = id;
		}

		public boolean isNeighbour(Node n) {

			return (n == left || n == right || n == up || n == down);

		}

		public boolean isBorder() {

			return (left == null || right == null || up == null || down == null);

		}

		public int getCost() {
			return cost;
		}

		public int getBody() {
			return body;
		}

		public Position getPosition() {
			return point;
		}

		private void update(int timestep, int body) {
			this.timestep = timestep;
			this.body = body;

			recalculate();
		}

		private void recalculate() {

			this.cost = costFormula.getCost(this);
		}

		@Override
		public int compareTo(Node o) {
			return -((Integer) timestep).compareTo(o.timestep);
		}

		@Override
		public String toString() {
			return String.format("Node: %d, timestep %d, body: %d", id,
					timestep, body);
		}

		public MapChunk getChunk(Position offset, int timediff) {

			MapChunk chunk = new MapChunk();

			chunk.x = (short) (point.getX() + offset.getX());
			chunk.y = (short) (point.getY() + offset.getY());

			chunk.timestep = timestep + timediff;

			int size = 1;

			int c = 0;

			for (int i = chunk.x - size; i <= chunk.x + size; i++) {
				for (int j = chunk.y - size; j <= chunk.y + size; j++) {

					Node n = get(i, j);

					if (n == null) {
						chunk.set(c, MapChunk.UNKNOWN);
						c++;
						continue;
					}

					switch (n.getBody()) {
					case Neighborhood.EMPTY: {
						chunk.set(c, MapChunk.EMPTY);
						break;
					}
					case Neighborhood.WALL: {
						chunk.set(c, MapChunk.WALL);
						break;
					}
					case Neighborhood.FLAG: {
						chunk.set(c, MapChunk.FLAG);
						break;
					}
					case Neighborhood.HEADQUARTERS: {
						chunk.set(c, MapChunk.HEADQUARTERS);
						break;
					}
					case Neighborhood.OTHER_FLAG: {
						chunk.set(c, MapChunk.OTHER_FLAG);
						break;
					}
					case Neighborhood.OTHER_HEADQUARTERS: {
						chunk.set(c, MapChunk.OTHER_HEADQUARTERS);
						break;
					}

					}

					c++;
				}

			}

			return chunk;

		}

	}

	private Hashtable<Position, Node> nodes = new Hashtable<Position, Node>();

	private Hashtable<Position, Integer> modifiers = new Hashtable<Position, Integer>();

	private CostFormula costFormula = defaultFormula;

	private Bounds bounds = new Bounds(0, 0, 0, 0);

	private int sumX = 0;

	private int sumY = 0;

	public synchronized boolean put(int x, int y, int timestep, int body, boolean foregin) {

		Position p = new Position(x, y);

		Node n = nodes.get(p);

		bounds.update(x, y);

		if (n == null) {

			n = new Node(x, y, timestep, body, nodes.size());

			n.foregin = foregin;
			
			Node nb = nodes.get(new Position(x - 1, y));

			if (nb != null) {
				n.left = nb;
				nb.right = n;
			}

			nb = nodes.get(new Position(x + 1, y));

			if (nb != null) {
				n.right = nb;
				nb.left = n;
			}

			nb = nodes.get(new Position(x, y - 1));

			if (nb != null) {
				n.up = nb;
				nb.down = n;
			}

			nb = nodes.get(new Position(x, y + 1));

			if (nb != null) {
				n.down = nb;
				nb.up = n;
			}

			nodes.put(p, n);

			sumX += x;
			sumY += y;

			return body != 0;
		} else {

			if (timestep > n.timestep) {

				boolean update = n.body != body;

				n.update(timestep, body);

				n.foregin = foregin;
				
				return update;
			}
			return false;

		}

	}

	public Node get(int x, int y) {

		Position p = new Position(x, y);

		return nodes.get(p);

	}

	public void addModifier(int x, int y, int weight) {
		Position key = new Position(x, y);

		if (modifiers.contains(key)) {
			if (modifiers.get(key) == Integer.MAX_VALUE) {
				weight = Integer.MAX_VALUE;
			} else {
				weight += modifiers.get(key);
			}
		}
		modifiers.put(key, weight);
	}

	public void addModifier(int x, int y) {
		addModifier(x, y, Integer.MAX_VALUE);
	}

	public void clearModifiers() {
		modifiers.clear();
	}

	public boolean update(Neighborhood neighborhood, Position anchor,
			int timestep) {

		Node n = nodes.get(anchor);

		if (n == null) {
			put(anchor.getX(), anchor.getY(), timestep, Neighborhood.EMPTY, false);
			return update(neighborhood, anchor, timestep);
		}

		int x = anchor.getX();
		int y = anchor.getY();

		boolean changed = false;

		for (int i = -neighborhood.getSize(); i <= neighborhood.getSize(); i++) {

			for (int j = -neighborhood.getSize(); j <= neighborhood.getSize(); j++) {

				if ((i == 0 && j == 0)) {
					changed |= put(x + i, y + j, timestep, 0, false);
					continue;
				}

				int body = neighborhood.getCell(i, j);

				if (body == Neighborhood.OTHER || body > 0)
					body = 0;

				changed |= put(x + i, y + j, timestep, body, false);

			}

		}

		return changed;
	}

	public boolean update(MapChunk chunk) {

		int size = 1;

		int c = 0;

		boolean changed = false;

		chunk.timestep = Math.max(1, chunk.timestep);
		
		for (int i = chunk.x - size; i <= chunk.x + size; i++) {
			for (int j = chunk.y - size; j <= chunk.y + size; j++) {

				switch (chunk.get(c)) {
				case MapChunk.EMPTY: {
					changed |= put(i, j, chunk.timestep, Neighborhood.EMPTY, true);
					break;
				}
				case MapChunk.WALL: {
					changed |= put(i, j, chunk.timestep, Neighborhood.WALL, true);
					break;
				}
				case MapChunk.FLAG: {
					changed |= put(i, j, chunk.timestep, Neighborhood.FLAG, true);
					break;
				}
				case MapChunk.HEADQUARTERS: {
					changed |= put(i, j, chunk.timestep,
							Neighborhood.HEADQUARTERS, true);
					break;
				}
				case MapChunk.OTHER_FLAG: {
					changed |= put(i, j, chunk.timestep,
							Neighborhood.OTHER_FLAG, true);
					break;
				}
				case MapChunk.OTHER_HEADQUARTERS: {
					changed |= put(i, j, chunk.timestep,
							Neighborhood.OTHER_HEADQUARTERS, true);
					break;
				}

				}

				c++;
			}

		}

		return changed;

	}

	public synchronized Paths findShortestPaths(Position from) {
		int[] D = new int[nodes.size()];
		int[] P = new int[nodes.size()];
		Node[] N = new Node[nodes.size()];
		ArrayList<Node> C = new ArrayList<Node>();

		Node fromNode = nodes.get(from);

		if (fromNode == null)
			return null;

		// initialize:
		// (C)andidate set,
		// (D)yjkstra special path length, and
		// (P)revious Node along shortest path

		for (Node n : nodes.values()) {
			Integer modifier = modifiers.get(n.point);
			int cost = n.getCost();
			if (modifier != null && cost != Integer.MAX_VALUE) {
				if (modifier == Integer.MAX_VALUE)
					cost = Integer.MAX_VALUE;
				else
					cost += modifier;
			}

			if (cost == Integer.MAX_VALUE) {
				D[n.id] = Integer.MAX_VALUE;
				continue;
			}

			C.add(n);
			N[n.id] = n;
			if (n.id == fromNode.id)
				D[n.id] = 0;
			else if (fromNode.isNeighbour(n))
				D[n.id] = n.getCost();
			else
				D[n.id] = Integer.MAX_VALUE;
		}

		for (int i = 0; i < nodes.size(); i++) {
			P[i] = (D[i] != Integer.MAX_VALUE) ? fromNode.id : -1;
		}

		// crawl the graph
		int candidates = C.size();
		for (int i = 0; i < candidates; i++) {
			// find the lightest Edge among the candidates
			int l = Integer.MAX_VALUE;
			Node n = C.get(0);
			for (Node j : C) {
				if (D[j.id] < l) {
					n = j;
					l = D[j.id];
				}
			}
			C.remove(n);

			// see if any Edges from this Node yield a shorter path than from
			// source->that Node
			for (int j = 0; j < nodes.size(); j++) {
				if (D[n.id] != Integer.MAX_VALUE && n.isNeighbour(N[j])
						&& D[n.id] + n.getCost() < D[j]) {
					// found one, update the path
					D[j] = D[n.id] + n.getCost();
					P[j] = n.id;
				}
			}
		}

		Paths result = new Paths();

		result.N = N;
		result.P = P;
		result.from = fromNode;

		return result;
	}

	public List<Node> filter(Filter filter) {

		Vector<Node> selected = new Vector<Node>();

		for (Node n : nodes.values()) {
			if (filter.filter(n))
				selected.add(n);
		}

		return selected;
	}

	public List<Node> getOldestNodes(int count) {

		PriorityQueue<Node> queue = new PriorityQueue<Node>();

		for (Node n : nodes.values()) {
			if (n.body == 0) {
				queue.offer(n);
				if (queue.size() > count)
					queue.poll();
			}
		}

		return new Vector<Node>(queue);
	}

	public void recalculateCost(CostFormula formula) {

		if (formula == null)
			formula = defaultFormula;

		costFormula = formula;

		for (Node n : nodes.values()) {
			n.recalculate();
		}

	}

	public LocalMapArena getArena(int scope) {
		return new LocalMapArena(scope, 0, 0);
	}

	public Bounds getBounds() {
		return new Bounds(bounds);
	}

	public Position getCenter() {
		return new Position(sumX / size(), sumY / size());
	}

	public int size() {
		return nodes.size();
	}

	public boolean verify() {
		
		boolean valid = true;
		
		for (Node n : nodes.values()) {
			
			Node up = get(n.getPosition().getX(), n.getPosition().getY()-1);
			Node down = get(n.getPosition().getX(), n.getPosition().getY()+1);
			Node left = get(n.getPosition().getX()-1, n.getPosition().getY());
			Node right = get(n.getPosition().getX()+1, n.getPosition().getY());
			
			valid &= n.up == up && n.down == down && n.left == left && n.right == right;
		}
		
		return valid;
	}
	
}
