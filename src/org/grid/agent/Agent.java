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
package org.grid.agent;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;
import java.util.Vector;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.sample.SampleAgent;
import org.grid.protocol.Message;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.ProtocolSocket;
import org.grid.protocol.Message.Direction;
import org.grid.protocol.Message.ReceiveMessage;
import org.grid.protocol.Message.StateMessage;


/**
 * The base class for all agents. This class also includes main method that is
 * used to launch the client and handles all low level protocol communication
 * and the lifecycle of the agent.
 * 
 * To run the sample agent type: java -cp bin/ fri.pipt.agent.Agent localhost
 * fri.pipt.agent.sample.SampleAgent
 * 
 * @author lukacu
 * @see SampleAgent
 */
@Membership(team="default",passphrase="")
public abstract class Agent {

	private static Class<Agent> agentClassStatic = null;

	private static Vector<ClientProtocolSocket> clients = new Vector<ClientProtocolSocket>();

	private static String teamOverride = null;
	
	private static String passphraseOverride = null;
	
	public static class ProxyClassLoader extends ClassLoader {
		
		private Set<String> protectedClassPrefixes = new HashSet<String>();
		
		public ProxyClassLoader() {
			super(ProxyClassLoader.class.getClassLoader());
			
			protectedClassPrefixes.add("sun.");
			protectedClassPrefixes.add("java.");
			protectedClassPrefixes.add("javax.");
			protectedClassPrefixes.add("fri.pipt.agent.Agent");
			protectedClassPrefixes.add("fri.pipt.protocol");
			protectedClassPrefixes.add("fri.pipt.arena");
		}

		public Class<?> loadClass(String className)
				throws ClassNotFoundException {
			return findClass(className);
		}

		public Class<?> findClass(String className) {
			Class<?> result = null;
			result = (Class<?>) classes.get(className);
			if (result != null) {
				return result;
			}

			try {
				Class<?> cls = this.findSystemClass(className);
				
				for (String prefix : protectedClassPrefixes) {
					if (className.startsWith(prefix)) {
						classes.put(className, cls);
						return cls;	
					}
				}
					
				
				String classResource = className.substring(className.lastIndexOf('.')+1) + ".class";

				InputStream in = cls.getResourceAsStream(classResource);
				ByteArrayOutputStream ba = new ByteArrayOutputStream();

				byte b[] = new byte[1024];

				while (true) {
					int len = in.read(b);
					if (len == -1)
						break;
					ba.write(b, 0, len);
				}

				Class<?> cl = defineClass(className, ba.toByteArray(), 0, ba
						.size());

				classes.put(className, cl);

				return cl;

			} catch (IOException e) {

			} catch (NullPointerException e) {
			
			} catch (ClassNotFoundException e) {

			}
			/*
			 * try { return findSystemClass(className); } catch (Exception e) {
			 * }
			 */
			return null;
		}

		private Hashtable<String, Class<?>> classes = new Hashtable<String, Class<?>>();
	}

	public static enum Status {
		UNKNOWN, REGISTERED, INITIALIZED
	}

	private static class ClientProtocolSocket extends ProtocolSocket implements
			Runnable {

		private ConcurrentLinkedQueue<Message> inbox = new ConcurrentLinkedQueue<Message>();

		private Status status = Status.UNKNOWN;

		private Agent agent = null;

		private boolean terminated = false;

		private String name;

		public ClientProtocolSocket(Socket sck, String name) throws IOException {
			super(sck);

			String team = "default";
			String passphrase = "";
			
			if (teamOverride == null) {
				
				Membership m = agentClassStatic.getAnnotation(Membership.class);
	
				if (m != null) {
					team = m.team();
					passphrase = m.passphrase();
				}
			} else {
				team = teamOverride;
				passphrase = passphraseOverride;
			}

			sendMessage(new Message.RegisterMessage(team, passphrase));

			this.name = name;

		}

		public String getName() {
			return name;
		}

		@SuppressWarnings("unchecked")
		@Override
		protected void handleMessage(Message message) {

			switch (status) {
			case UNKNOWN:
				if (message instanceof Message.AcknowledgeMessage)
					status = Status.REGISTERED;
				break;

			case REGISTERED:
				if (message instanceof Message.InitializeMessage) {

					try {

						ProxyClassLoader loader = new ProxyClassLoader();

						Class<Agent> agentClass = (Class<Agent>) loader
								.loadClass(agentClassStatic.getCanonicalName());

						Agent agent = agentClass.newInstance();

						agent.id = ((Message.InitializeMessage) message)
								.getId();

						agent.client = this;

						agent.maxMessageSize = ((Message.InitializeMessage) message)
								.getMaxMessageSize();

						agent.gameSpeed = ((Message.InitializeMessage) message)
								.getGameSpeed();

						try {
							agent.initialize();
						} catch (Exception e) {
							e.printStackTrace();
						}

						sendMessage(new Message.AcknowledgeMessage());

						status = Status.INITIALIZED;
						
						this.agent = agent;
						
					} catch (Throwable e) {
						e.printStackTrace();
					}

				}

				break;

			case INITIALIZED:
				if (message instanceof Message.StateMessage)
					super.handleMessage(message);

				if ((message instanceof Message.ReceiveMessage)
						|| (message instanceof Message.StateMessage)) {

					synchronized (inbox) {

						inbox.add(message);
						inbox.notifyAll();

					}

				}

				if (message instanceof Message.TerminateMessage) {

					try {
						agent.terminate();
					} catch (Throwable e) {
						e.printStackTrace();
					}

					agent = null;
					status = Status.REGISTERED;

				}

				break;

			}

		}

		public boolean isAlive() {
			return status == Status.INITIALIZED;
		}

		@Override
		protected void onTerminate() {
			switch (status) {
			case UNKNOWN:
				System.out
						.println("ERROR: Unable to connect. Did you set the membership information correctly?");
				break;
			case REGISTERED:
				System.out.println("ERROR: Disconnected by server.");
				break;
			case INITIALIZED:
				System.out.println("ERROR: Disconnected by server.");
				break;
			}
			super.onTerminate();
			terminated = true;
		}

		@Override
		public void run() {
			Thread messages = new Thread(new Runnable() {

				@Override
				public void run() {

					while (true) {

						synchronized (inbox) {
							while (inbox.isEmpty()) {
								try {
									inbox.wait();
								} catch (InterruptedException e) {
								}
							}
						}

						Message msg = inbox.poll();

						if (agent != null && isAlive()) {
							try {

								if (msg instanceof ReceiveMessage) {
									agent.receive(((ReceiveMessage) msg)
											.getFrom(), ((ReceiveMessage) msg)
											.getMessage());
								} else if (msg instanceof StateMessage) {
									agent
											.state(((StateMessage) msg)
													.getStamp(),
													((StateMessage) msg)
															.getNeighborhood(),
													((StateMessage) msg)
															.getDirection(),
													((StateMessage) msg)
															.hasFlag());
								}

							} catch (Exception e) {
								e.printStackTrace();
							}
						}

					}
				}
			});
			messages.start();

			try {

				while (true) {

					if (agent != null)
						agent.run();

					try {
						Thread.sleep(100);
					} catch (InterruptedException e) {
					}

				}

			} catch (Exception e) {
				e.printStackTrace();
			}
		}

	}

	@SuppressWarnings("unchecked")
	public static void main(String[] args) throws NumberFormatException,
			UnknownHostException, IOException, ClassNotFoundException {

		agentClassStatic = (Class<Agent>) Class.forName(args[1]);

		int count = 1;

		if (args.length > 2)
			count = Integer.parseInt(args[2]);

		if (args.length > 3)
			teamOverride = args[3];

		if (args.length > 4)
			passphraseOverride = args[4];
		
		for (int i = 0; i < count; i++) {
			Socket socket = new Socket(args[0], 5000);
			socket.setTcpNoDelay(true);
			
			ClientProtocolSocket client = new ClientProtocolSocket(socket,
					"Client " + i);

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {}
			
			Thread thread = new Thread(client);
			thread.setName(client.getName());
			thread.start();

			clients.add(client);

		}

		while (true) {

			boolean alive = false;

			for (ClientProtocolSocket c : clients) {

				alive |= !c.terminated;

			}

			if (!alive)
				break;

			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
			}

		}

		System.exit(0);

	}

	private int id;

	private int maxMessageSize;

	private int gameSpeed;

	private ClientProtocolSocket client;

	/**
	 * Called when the agent is no longer needed.
	 */
	public abstract void terminate();

	/**
	 * Called when the agent is initialized.
	 */
	public abstract void initialize();

	/**
	 * Send a message to another agent in the same team. Note that if distance
	 * criteria apply in the game the agent may not receive the message if it is
	 * too far.
	 * 
	 * @param to
	 *            the id of the agent in the same team that should receive this
	 *            message
	 * @param message
	 *            the message as a byte array
	 */
	public final void send(int to, byte[] message) {

		if (!isAlive())
			return;

		client.sendMessage(new Message.SendMessage(to, message));

	}

	/**
	 * Send a message to another agent in the same team. Note that if distance
	 * criteria apply in the game the agent may not receive the message if it is
	 * too far.
	 * 
	 * @param to
	 *            the id of the agent in the same team that should receive this
	 *            message
	 * @param message
	 *            the message as a string
	 */
	public final void send(int to, String message) {

		if (!isAlive())
			return;

		client.sendMessage(new Message.SendMessage(to, message.getBytes()));

	}

	/**
	 * Sends a move command to the server. Note that depending on the current
	 * state of the agent, the command may be acknowledged or ignored. You
	 * should check the state of the agent to see the actual
	 * 
	 * @param direction
	 *            the desired direction
	 */
	public final void move(Direction direction) {

		if (!isAlive())
			return;

		client.sendMessage(new Message.MoveMessage(direction));

	}

	/**
	 * Sends a scan request to the server. The server will respond with the
	 * local state of the environment that will be returned to the agent using
	 * the {@link #state(int, Neighborhood, Direction, boolean)} callback.
	 * 
	 * @param stamp
	 *            the stamp of the request
	 */
	public final void scan(int stamp) {

		if (!isAlive())
			return;

		client.sendMessage(new Message.ScanMessage(stamp));

	}

	/**
	 * Called when a new message arrives. Should execute quickly.
	 * 
	 * @param from
	 *            the id of the sender agent
	 * @param message
	 *            the message as a byte array
	 */
	public abstract void receive(int from, byte[] message);

	/**
	 * Called as a result of a {@link #scan(int)} instruction
	 * 
	 * @param stamp
	 *            the stamp of the request
	 * @param neighborhood
	 *            the neighborhood information
	 * @param direction
	 *            the direction of the movement
	 * @param hasFlag
	 *            does this agent carry the flag of the team
	 */
	public abstract void state(int stamp, Neighborhood neighborhood,
			Direction direction, boolean hasFlag);

	/**
	 * The main method of the agent. Should loop while the agent is alive.
	 */
	public abstract void run();

	/**
	 * Checks if the agent is alive.
	 * 
	 * @return true if the agent is alive, false otherwise
	 */
	public final boolean isAlive() {
		return client.agent == this;
	}

	/**
	 * Returns the id of the local agent
	 * 
	 * @return the id of the agent
	 */
	public final int getId() {
		return id;
	}

	/**
	 * Returns the execution speed of the server. Useful for setting delays.
	 * 
	 * @return the speed of the server. The server iterates time-steps with
	 *         (approximately) <tt>1000 / speed</tt> milliseconds delay.
	 */
	public final int getSpeed() {
		return gameSpeed;
	}

	/**
	 * Returns the maximum allowed message size. Sending a message that is
	 * longer will result in server dropping the message.
	 * 
	 * @return the maximum message size in bytes
	 */
	public final int getMaxMessageSize() {
		return maxMessageSize;
	}

}
