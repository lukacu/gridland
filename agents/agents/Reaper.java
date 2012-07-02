package agents;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import javax.swing.JFrame;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.arena.SwingView;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.Position;
import org.grid.protocol.Message.Direction;

import agents.LocalMap.Bounds;
import agents.LocalMap.CostFormula;
import agents.LocalMap.Filter;
import agents.LocalMap.LocalMapArena;
import agents.LocalMap.MapChunk;
import agents.LocalMap.Node;
import agents.LocalMap.Paths;

//java -cp bin fri.pipt.agent.Agent localhost agents.Reaper

@Membership(team = "reapers", passphrase = "360ebf43b02eca90c249302ea8a48af4")
public class Reaper extends Agent {

	private static enum Mode {
		EXPLORE, SURVEIL, SEEK, RETURN, ATTACK, FOLLOW, CLEAR
	}

	private static Filter flagFilter = new Filter() {

		@Override
		public boolean filter(Node n) {
			return n.getBody() == Neighborhood.FLAG;
		}

	};

	private static class Message {

		int from;

		byte[] message;

		protected Message(int from, byte[] message) {
			super();
			this.from = from;
			this.message = message;
		}

	}

	private static class MemberData {

		private int id;

		private int info;

		private boolean map = false;
		
		private int timediff;

		private Position position;

		int notified;

		boolean hasFlag;

		private Bounds bounds;

		private Position origin, center;

		public int getId() {
			return id;
		}

		public Position getPosition() {
			return position;
		}

		public void setPosition(Position position) {
			this.position = position;
		}

		protected MemberData(int id) {
			super();
			this.id = id;
		}

		@Override
		public String toString() {

			return String.format("ID: %d, Flag: %b", id, hasFlag);

		}
	}

	private static class UnknownAreaFilter implements Filter {

		private Bounds known;

		private Position center;

		private int radius;

		public UnknownAreaFilter(Bounds known, Position center) {
			this.known = known;

			this.center = center;

			radius = Math.min(known.getRight() - known.getLeft(), known
					.getBottom()
					- known.getTop());
		}

		@Override
		public boolean filter(Node n) {

			if (n.getPosition().getX() % 3 != 0
					|| n.getPosition().getY() % 3 != 0)
				return false;

			if (!known.inside(n.getPosition()))
				return true;

			if (Position.distance(center, n.getPosition()) > radius)
				return true;

			return false;
		}

	}

	private static class DistanceFilter implements Filter {

		private Position p;

		private int mindistance, maxdistance;

		public DistanceFilter(Position p, int mindistance, int maxdistance) {
			this.p = p;
			this.mindistance = mindistance;
			this.maxdistance = maxdistance;
		}

		@Override
		public boolean filter(Node n) {
			int distance = Position.distance(n.getPosition(), p);
			return distance <= maxdistance && distance >= mindistance;
		}

	}

	private static final CostFormula returnFormula = new CostFormula() {

		@Override
		public int getCost(Node n) {
			switch (n.getBody()) {
			case Neighborhood.HEADQUARTERS:
			case Neighborhood.EMPTY:
				return 1;
			case Neighborhood.FLAG:
				return 60;
			case Neighborhood.OTHER_FLAG:
			case Neighborhood.OTHER_HEADQUARTERS:
			case Neighborhood.OTHER:
			case Neighborhood.WALL:
			default:
				return Integer.MAX_VALUE;
			}
		}
	};

	private LocalMap map = new LocalMap();

	private Position position = new Position(0, 0);

	private Mode mode = Mode.EXPLORE;

	private int patience = 0;

	private int parent = -1;

	private int timestep = 1;

	private boolean hasFlag = false;

	private static class State {

		Neighborhood neighborhood;

		Direction direction;

		boolean hasFlag;

		public State(int stamp, Neighborhood neighborhood, Direction direction,
				boolean hasFlag) {
			super();
			this.neighborhood = neighborhood;
			this.direction = direction;
			this.hasFlag = hasFlag;
		}

	}

	private ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<Message>();

	private ConcurrentLinkedQueue<State> buffer = new ConcurrentLinkedQueue<State>();

	private ConcurrentLinkedQueue<Direction> plan = new ConcurrentLinkedQueue<Direction>();

	private JFrame window;

	private LocalMapArena arena;

	private SwingView view;

	@Override
	public void initialize() {

		arena = map.getArena(6);

		if (System.getProperty("reaper") != null) {
			view = new SwingView(24);
	
			view.setBasePallette(new SwingView.HeatPalette(32));
	
			window = new JFrame("Agent " + getId());
	
			window.setContentPane(view);
	
			window.setSize(view.getPreferredSize(arena));
	
			window.setVisible(true);
		
		}
	}

	@Override
	public void receive(int from, byte[] message) {

		inbox.add(new Message(from, message));

	}

	@Override
	public void run() {

		int sleeptime = Math.max(5, (1000 / getSpeed()) / 2);
		int sleepcount = 0;

		scan(0);

		while (isAlive()) {

			State state = buffer.poll();
			if (state != null) {

				hasFlag = state.hasFlag;

				if (state.direction == Direction.NONE) {

					sleepcount = 0;

					Set<Position> moveable = analyzeNeighborhood(state.neighborhood);

					boolean replanMap = map.update(state.neighborhood,
							position, timestep);

					registerMoveable(moveable, state.neighborhood);

					boolean replanAgents = blockMoveable(moveable,
							state.neighborhood);

					Set<Node> enemies = filterEnemies(moveable,
							state.neighborhood);

					while (!inbox.isEmpty()) {
						Message m = inbox.poll();

						replanMap &= 
							parse(m.from, m.message,
								state.neighborhood);
					}

					if (view != null)
						view.update(arena);

					if (replanMap || replanAgents) {
						plan.clear();
					}

					if (plan.isEmpty()) {

						if (state.hasFlag) {
							changeMode(Mode.RETURN);
							map.recalculateCost(returnFormula);
						}

						List<Direction> directions = null;

						Paths paths = map.findShortestPaths(position);

						while (directions == null) {

							switch (mode) {
							case EXPLORE: {

								if (stohastic(0.9)) {
									List<Node> candidates = map
											.filter(flagFilter);

									directions = paths
											.shortestPathTo(candidates);

									if (directions != null) {
										changeMode(Mode.SEEK);
										break;
									}
								}

								if (!enemies.isEmpty() && stohastic(0.2)) {

									changeMode(Mode.ATTACK);
									patience = 10;
									break;

								}

								List<Node> candidates = map
										.filter(LocalMap.BORDER_FILTER);

								Collections.shuffle(candidates);
								
								directions = paths.shortestPathTo(candidates);

								if (directions == null) {
									if (!replanAgents) {
										changeMode(Mode.SURVEIL);
										continue;
									}
								}
								break;
							}
							case SURVEIL: {

								if (stohastic(0.9)) {
									List<Node> candidates = map
											.filter(flagFilter);

									directions = paths
											.shortestPathTo(candidates);

									if (directions != null) {
										changeMode(Mode.SEEK);
										break;
									}
								}

								if (!enemies.isEmpty() && stohastic(0.05)) {

									changeMode(Mode.ATTACK);
									patience = 30;
									break;

								}

								List<Node> candidates = map.getOldestNodes(10);

								directions = paths.shortestPathTo(candidates);

								break;
							}
							case SEEK: {

								List<Node> candidates = map.filter(flagFilter);

								directions = paths.shortestPathTo(candidates);

								if (directions == null) {
									changeMode(Mode.EXPLORE);
									continue;
								}

								break;
							}
							case RETURN: {

								Position p = origin;

								Node n = map.get(p.getX(), p.getY());

								directions = paths.shortestPathTo(n);

								break;
							}
							case FOLLOW: {

								if (parent > 0) {

									MemberData parendData = registry
											.get(parent);

									if (!checkParent(state.neighborhood)) {
										changeMode(Mode.EXPLORE);
										parent = -1;
										continue;
									}

									if (!enemies.isEmpty()) {

										changeMode(Mode.ATTACK);
										patience = 30;
										continue;

									}

									List<Node> candidates = map
											.filter(new DistanceFilter(
													parendData.getPosition(),
													2, 2));

									directions = paths
											.shortestPathTo(candidates);

									if (directions == null) {
										changeMode(Mode.EXPLORE);
										parent = -1;
										continue;
									}
								}

								break;
							}
							case ATTACK: {

								patience--;

								if (patience < 0) {
									patience = 0;
									changeMode(Mode.EXPLORE);
									continue;
								}

								directions = paths
										.shortestPathTo(filterEnemies(moveable,
												state.neighborhood));

								if (directions == null) {
									changeMode(Mode.EXPLORE);
									continue;
								}

								break;
							}
							case CLEAR: {

								List<Node> candidates = map
										.filter(new DistanceFilter(
												position,
												state.neighborhood.getSize() - 1,
												state.neighborhood.getSize() + 1));

								directions = paths.shortestPathTo(candidates);

								changeMode(Mode.EXPLORE);

								break;
							}
							default:
								break;
							}

							// cannot move anywhere ...
							if (directions == null) {
								// if (directions == null) {
								// otherwise just hold still for a timestep
								directions = new Vector<Direction>();
								for (int i = 0; i < 5; i++)
									directions.add(Direction.NONE);
								// }
							}
						}

						plan.addAll(directions);

					}

					if (!plan.isEmpty()) {

						Direction d = plan.poll();
						// debug("Next move: %s", d);

						timestep++;

						if (d != Direction.NONE) {
							move(d);
						}

						if (d == Direction.LEFT)
							position.setX(position.getX() - 1);
						if (d == Direction.RIGHT)
							position.setX(position.getX() + 1);
						if (d == Direction.UP)
							position.setY(position.getY() - 1);
						if (d == Direction.DOWN)
							position.setY(position.getY() + 1);

						arena.setOrigin(position.getX(), position.getY());

						if (detectLock()
								&& (mode == Mode.EXPLORE || mode == Mode.SURVEIL)) {

							changeMode(Mode.CLEAR);

						}

						scan(0);

					}

				} else
					scan(0);

			}

			sleepcount++;
			if (sleepcount > 2)
				sleeptime++;

			if (timestep % 20 == 0) {
				sleeptime = 1000 / getSpeed();
			}

			try {
				Thread.sleep(sleeptime);
			} catch (InterruptedException e) {
			}

		}

	}

	@Override
	public void state(int stamp, Neighborhood neighborhood,
			Direction direction, boolean hasFlag) {

		buffer.add(new State(stamp, neighborhood, direction, hasFlag));

	}

	@Override
	public void terminate() {

		if (view != null)
			window.setVisible(false);

	}

	protected void debug(String format, Object... objects) {
		System.out.println("[" + getId() + "]: "
				+ String.format(format, objects));
	}

	private Set<Position> moveable = new HashSet<Position>();

	private HashMap<Integer, MemberData> registry = new HashMap<Integer, MemberData>();

	private Position origin = null;

	private Vector<Position> history = new Vector<Position>();

	private Set<Position> analyzeNeighborhood(Neighborhood n) {
		int x = position.getX();
		int y = position.getY();

		HashSet<Position> moveable = new HashSet<Position>();

		for (int i = -n.getSize(); i <= n.getSize(); i++) {

			for (int j = -n.getSize(); j <= n.getSize(); j++) {

				if ((i == 0 && j == 0))
					continue;

				if (n.getCell(i, j) == Neighborhood.HEADQUARTERS) {

					if (origin == null)
						origin = new Position(x + i, y + j);

					continue;
				}

				if (n.getCell(i, j) > 0
						|| n.getCell(i, j) == Neighborhood.OTHER) {

					moveable.add(new Position(x + i, y + j));

				}

			}

		}
		return moveable;
	}

	private void registerMoveable(Set<Position> moveable, Neighborhood n) {

		int x = position.getX();
		int y = position.getY();

		HashSet<Position> noticed = new HashSet<Position>(moveable);
		HashSet<Position> lost = new HashSet<Position>(this.moveable);

		noticed.removeAll(this.moveable);
		lost.removeAll(moveable);

		this.moveable.removeAll(lost);

		for (Position p : moveable) {

			int i = p.getX();
			int j = p.getY();

			if (n.getCell(i - x, j - y) < 1)
				continue;

			int id = n.getCell(i - x, j - y);

			synchronized (registry) {

				if (registry.containsKey(id)) {

					registry.get(id).setPosition(p);

				} else {

					MemberData member = new MemberData(id);
					member.setPosition(p);
					member.notified = -30;
					registry.put(id, member);
				}

				MemberData data = registry.get(id);

				if (Math.abs(timestep - data.notified) > 20) {
					sendInfo(id);
					data.notified = timestep;
					data.map = false;
				}

				if (Math.abs(timestep - data.info) < 5 && !data.hasFlag && !data.map) {
					sendMap(id);
					data.map = true;
				}
			}
		}

		for (Position p : noticed) {
			int i = p.getX();
			int j = p.getY();

			if (n.getCell(i - x, j - y) > 0) {

			}

		}

	}

	private Set<Node> filterEnemies(Set<Position> moveable, Neighborhood n) {

		int x = position.getX();
		int y = position.getY();

		HashSet<Node> enemies = new HashSet<Node>();

		for (Position p : moveable) {

			int i = p.getX() - x;
			int j = p.getY() - y;

			if (n.getCell(i, j) == Neighborhood.OTHER)
				enemies.add(map.get(p.getX(), p.getY()));

		}

		return enemies;
	}

	private Set<Node> filterTeam(Set<Position> moveable, Neighborhood n) {

		int x = position.getX();
		int y = position.getY();

		HashSet<Node> enemies = new HashSet<Node>();

		for (Position p : moveable) {

			int i = p.getX() - x;
			int j = p.getY() - y;

			if (n.getCell(i, j) > 0)
				enemies.add(map.get(p.getX(), p.getY()));

		}

		return enemies;
	}

	private boolean blockMoveable(Set<Position> moveable, Neighborhood n) {

		boolean replan = false;

		int x = position.getX();
		int y = position.getY();

		map.clearModifiers();

		for (Position p : moveable) {

			int i = p.getX() - x;
			int j = p.getY() - y;


			if (n.getCell(i, j) > 0 || n.getCell(i, j) == Neighborhood.OTHER) {

				int size = 3;

				if (n.getCell(i, j) == Neighborhood.OTHER && hasFlag)
					size = 5;

				int factor = (Integer.MAX_VALUE - 1) / size;
				
				for (int ii = -size; ii <= size; ii++) {

					for (int jj = -size; jj <= size; jj++) {

						int cost = Math.max(0, size
								- (Math.abs(ii) + Math.abs(jj)));

						if (cost > 0)
							map
									.addModifier(x + i + ii, y + j + jj,
											cost * factor);
					}

				}

				replan = true;
			}

			if (n.getCell(i, j) > 0) {

				if (n.getCell(i, j) > getId() && n.getCell(i, j) != parent) {
					map.addModifier(x + i, y + j);
				} else {
					map.addModifier(x + i, y + j - 1);
					map.addModifier(x + i - 1, y + j);
					map.addModifier(x + i, y + j);
					map.addModifier(x + i + 1, y + j);
					map.addModifier(x + i, y + j + 1);
				}

			} else if (n.getCell(i, j) == Neighborhood.OTHER) {

				if (mode != Mode.ATTACK) {

					// map.addModifier(x+i-1, y+j-1); map.addModifier(x+i,
					// y+j-1); map.addModifier(x+i+1, y+j-1);
					// map.addModifier(x+i-1, y+j);
					map.addModifier(x + i, y + j);
					// map.addModifier(x+i+1, y+j);
					// map.addModifier(x+i-1, y+j+1); map.addModifier(x+i,
					// y+j+1); map.addModifier(x+i+1, y+j+1);
				}

			}

		}

		return replan;

	}

	private boolean checkParent(Neighborhood neighborhood) {

		int x = position.getX();
		int y = position.getY();

		MemberData member = registry.get(parent);

		if (member == null)
			return false;

		Position p = member.getPosition();

		return neighborhood.getCell(p.getX() - x, p.getY() - y) == member
				.getId();

	}

	private boolean stohastic(double probability) {
		return Math.random() < probability;
	}

	private boolean parse(int from, byte[] message, Neighborhood neighborhood) {

		try {
			ObjectInputStream in = new ObjectInputStream(
					new ByteArrayInputStream(message));

			int type = in.readByte();

			switch (type) {
			case 1: { // info message

				boolean hasFlag = in.readByte() == 1;

				Position origin = new Position(0, 0);
				origin.setX(in.readInt());
				origin.setY(in.readInt());

				Bounds bounds = new Bounds(0, 0, 0, 0);
				Position center = new Position(0, 0);
				bounds.setTop(in.readInt());
				bounds.setBottom(in.readInt());
				bounds.setLeft(in.readInt());
				bounds.setRight(in.readInt());

				center.setX(in.readInt());
				center.setY(in.readInt());

				int timediff = in.readInt() - timestep;

				synchronized (registry) {
					if (registry.containsKey(from)) {
						MemberData data = registry.get(from);
						data.hasFlag = hasFlag;
						data.bounds = bounds;
						data.origin = origin;
						data.center = center;
						data.info = timestep;
						data.timediff = timediff;
						debug("New info: %s", data);
					}

				}

				if (hasFlag) {

					if ((mode == Mode.EXPLORE || mode == Mode.SURVEIL)) {

						if (filterTeam(moveable, neighborhood).size() < 3) {

							parent = from;
							changeMode(Mode.FOLLOW);

							return true;
						}
					}
				}

				break;
			}
			case 2: { // map message

				MapChunk chunk = new MapChunk();

				boolean replan = false;

				int chunks = 0;
				
				while (true) {

					try {

						chunk.read(in);

						replan |= map.update(chunk);

						chunks++;
						
					} catch (IOException e) {
						break;
					}

				}

				debug("Got %d map chunks from %d, new data %b", chunks, from, replan);
				
				if (!map.verify()) 
					debug("Map no longer valid!");
				
				return replan;
			}

			}

		} catch (Exception e) {
			debug("Error parsing message from %d: %s", from, e);
		}

		return false;
	}

	private void sendInfo(int to) {

		try {
			ByteArrayOutputStream buffer = new ByteArrayOutputStream(
					getMaxMessageSize());
			ObjectOutputStream out = new ObjectOutputStream(buffer);

			out.writeByte(1);

			out.writeByte(hasFlag ? 1 : 0);

			Bounds bounds = map.getBounds();

			Position center = map.getCenter();

			out.writeInt(origin.getX());
			out.writeInt(origin.getY());
			out.writeInt(bounds.getTop());
			out.writeInt(bounds.getBottom());
			out.writeInt(bounds.getLeft());
			out.writeInt(bounds.getRight());

			out.writeInt(center.getX());
			out.writeInt(center.getY());

			out.writeInt(timestep);

			out.flush();

			send(to, buffer.toByteArray());

		} catch (IOException e) {
			debug("Error sending message to %d: %s", to, e);
		}

	}

	private void sendMap(int to) {

		Collection<Node> nodes = null;
		Position position = null;
		Position offset = null;
		int timediff = 0;

		synchronized (registry) {

			MemberData data = registry.get(to);

			if (data == null || data.bounds == null)
				return;

			Bounds bounds = new Bounds(data.bounds);
			Position center = new Position(data.origin, -1);
			offset = new Position(data.origin, -1);

			position = new Position(data.position);

			bounds.offset(new Position(data.origin, -1));
			bounds.offset(origin);

			offset.offset(origin);
			center.offset(offset);

			nodes = map.filter(new UnknownAreaFilter(bounds, data.center));

			timediff = data.timediff;

		}

		if (nodes == null || nodes.isEmpty())
			return;

		debug("Sending map to %d: %d chunks", to, nodes.size());
		
		try {

			ByteArrayOutputStream buffer = new ByteArrayOutputStream(
					getMaxMessageSize());
			ObjectOutputStream out = new ObjectOutputStream(buffer);

			out.writeByte(2);

			Vector<Node> list = new Vector<Node>(nodes);

			final Position center = position;

			Collections.sort(list, new Comparator<Node>() {

				@Override
				public int compare(Node o1, Node o2) {

					int dist1 = Position.distance(center, o1.getPosition());
					int dist2 = Position.distance(center, o2.getPosition());

					if (dist1 > dist2)
						return 1;
					if (dist1 < dist2)
						return -1;

					return 0;
				}
			});

			for (Node n : list) {

				if (buffer.size() + 20 >= getMaxMessageSize())
					break;

				MapChunk chunk = n.getChunk(offset, timediff);

				chunk.write(out);
				
				out.flush();
			}

			send(to, buffer.toByteArray());

		} catch (IOException e) {
			debug("Error sending message to %d: %s", to, e);
		}
	}

	private boolean detectLock() {

		history.add(new Position(position));

		if (history.size() < 20)
			return false;

		float meanX = 0;
		float meanY = 0;

		for (Position p : history) {
			meanX += p.getX();
			meanY += p.getY();
		}

		meanX /= history.size();
		meanY /= history.size();

		float varianceX = 0;
		float varianceY = 0;

		for (Position p : history) {
			varianceX += Math.pow(p.getX() - meanX, 2);
			varianceY += Math.pow(p.getY() - meanY, 2);
		}

		varianceX /= history.size();
		varianceY /= history.size();

		history.clear();

		float variability = (float) Math.sqrt(varianceX * varianceX + varianceY
				* varianceY);

		return variability < 2;
	}

	private void changeMode(Mode mode) {

		if (this.mode != mode) {
			debug("Switching from %s to %s", this.mode, mode);
		}

		this.mode = mode;
	}

}
