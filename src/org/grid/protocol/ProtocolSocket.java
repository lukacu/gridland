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
package org.grid.protocol;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.util.concurrent.ConcurrentLinkedQueue;

public class ProtocolSocket {

	public static class AppendableObjectOutputStream extends ObjectOutputStream {

		  public AppendableObjectOutputStream(OutputStream out) throws IOException {
		    super(out);
		    
		    super.writeStreamHeader();
		  }

		  @Override
		  protected void writeStreamHeader() throws IOException {
		    // do not write a header
		  }

		}

	
	private ObjectInputStream in;
	
	private ObjectOutputStream out;
	
	private Thread inputThread;
	
	private Thread outputThread;
	
	private boolean running = true;
	
	private boolean debug = Boolean.getBoolean("fri.pipt.protocol.debug");
	
	private ConcurrentLinkedQueue<Message> inQueue = new ConcurrentLinkedQueue<Message>();
	
	private ConcurrentLinkedQueue<Message> outQueue = new ConcurrentLinkedQueue<Message>();
	
	private Socket socket;
	
	public ProtocolSocket(Socket sck) throws IOException {

		this.socket = sck;

		inputThread = new Thread(new Runnable() {

			@Override
			public void run() {
				
				try {
					in = new ObjectInputStream(socket.getInputStream());
				} catch (IOException e1) {
					return;
				}
				
				while (running) {

					try {

						Object obj = in.readObject();

						if (obj == null || !(obj instanceof Message))
							continue;
						
						Message message = (Message) obj;
						
						if (debug)
							System.err.println("*** PROTOCOL INCOMING <<< " + message.getClass().getSimpleName() + " <<<");
						
						handleMessage(message);
						
					} catch (ClassNotFoundException e) {
						if (debug)
							e.printStackTrace();
					} catch (IOException e) {
						if (debug)
							e.printStackTrace();
						close();
					}

				}
			}
			
		});
		inputThread.start();
		
		outputThread = new Thread(new Runnable() {

			@Override
			public void run() {
				

				try {
					out = new ObjectOutputStream(socket.getOutputStream());
				} catch (IOException e1) {
					return;
				}

				
				while (running) {

					try {
						synchronized (outQueue) {
							while (outQueue.isEmpty()) {
								try {
									outQueue.wait();
								} catch (InterruptedException e) {}
							}
						}

						Message message = outQueue.poll();
						
						if (debug)
							System.err.println("*** PROTOCOL OUTGOING >>> " + message.getClass().getSimpleName() + " >>>");
						
						out.writeObject(message);

						out.flush();
						
					} catch (IOException e) {
						if (debug)
							e.printStackTrace();
						close();
					} 

				}
			}
			
		});
		outputThread.start();
	}
	
	public Message receiveMessage() {
		
		synchronized (inQueue) {
		
			if (inQueue.isEmpty())
				return null;
		
			return inQueue.poll();
			
		}
		
	}
	
	public Message waitMessage() {
		
		synchronized (inQueue) {
			while (true) {
			
				if (inQueue.isEmpty())
					try {
						inQueue.wait();
					} catch (InterruptedException e) {}
				else break;
			}
			return inQueue.poll();
		}
		
	}
	
	public void sendMessage(Message msg) {
		
		if (msg == null)
			return;
			
		synchronized (outQueue) {
		
			outQueue.add(msg);
		
			outQueue.notifyAll();
			
		}
		
	}
	
	public void close() {
		
		outQueue.clear();
		
		running = false;
		
		onTerminate();
		
		try {
			in.close();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
		
		try {
			out.close();
		} catch (IOException e) {
		} catch (NullPointerException e) {
		}
	}
	
	protected void handleMessage(Message message) {
		
		synchronized (inQueue) {
			
			inQueue.add(message);
			inQueue.notifyAll();
			
		}
		
	}
	
	protected void onTerminate() {
		
	}
	
	public InetAddress getRemoteAddress() {
		return socket.getInetAddress();
	}
	
	public int getRemotePort() {
		return socket.getPort();
	}
	
}
