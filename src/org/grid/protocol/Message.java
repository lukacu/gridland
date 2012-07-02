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

import java.io.Serializable;

public abstract class Message implements Serializable {

	public static enum Direction {NONE, UP, DOWN, LEFT, RIGHT}
	
	private static final long serialVersionUID = 1L;

	public String toString() {
		return getClass().getSimpleName();
	}
	
	public static class RegisterMessage extends Message {

		private static final long serialVersionUID = 1L;

		public RegisterMessage(String team, String passphrase) {
			this.team = team;
			this.passphrase = passphrase;
		}

		private String team, passphrase;

		public String getTeam() {
			return team;
		}

		public void setTeam(String team) {
			this.team = team;
		}

		public String getPassphrase() {
			return passphrase;
		}

		public void setPassphrase(String passphrase) {
			this.passphrase = passphrase;
		}
		
	}
	
	public static class AcknowledgeMessage extends Message {

		private static final long serialVersionUID = 1L;
		
	}
	
	
	public static class InitializeMessage extends Message {

		private static final long serialVersionUID = 1L;
		
		private int id;

		private int maxMessageSize;
		
		private int gameSpeed;
		
		public int getId() {
			return id;
		}

		public void setId(int id) {
			this.id = id;
		}

		public int getMaxMessageSize() {
			return maxMessageSize;
		}

		public void setMaxMessageSize(int maxMessageSize) {
			this.maxMessageSize = maxMessageSize;
		}

		public int getGameSpeed() {
			return gameSpeed;
		}

		public void setGameSpeed(int gameSpeed) {
			this.gameSpeed = gameSpeed;
		}

		public InitializeMessage(int id, int maxMessageSize, int gameSpeed) {
			super();
			this.id = id;
			this.maxMessageSize = maxMessageSize;
			this.gameSpeed = gameSpeed;
		}

	}
	
	public static class TerminateMessage extends Message {

		private static final long serialVersionUID = 1L;
		
	}

	public static class ScanMessage extends Message {

		private static final long serialVersionUID = 1L;
		
		public ScanMessage(int stamp) {
			super();
			this.stamp = stamp;
		}
		
		private int stamp;
		
		public int getStamp() {
			return stamp;
		}

		public void setStamp(int stamp) {
			this.stamp = stamp;
		}
		
	}
	
	public static class StateMessage extends Message {

		private int stamp;
		
		public int getStamp() {
			return stamp;
		}

		public void setStamp(int stamp) {
			this.stamp = stamp;
		}

		private Direction direction;
		
		private Neighborhood neighborhood;
		
		private boolean hasFlag;
		
		private static final long serialVersionUID = 1L;
		
		public Direction getDirection() {
			return direction;
		}

		public void setDirection(Direction direction) {
			this.direction = direction;			
		}

		public StateMessage(Direction direction, Neighborhood neighborhood, boolean hasFlag) {
			super();
			this.direction = direction;
			this.neighborhood = neighborhood;
			this.hasFlag = hasFlag;
		}

		public Neighborhood getNeighborhood() {
			return neighborhood;
		}

		public boolean hasFlag() {
			return hasFlag;
		}

		public void setFlag(boolean hasFlag) {
			this.hasFlag = hasFlag;
		}

	}
	
	public static class MoveMessage extends Message {

		private Direction direction;
		
		private static final long serialVersionUID = 1L;

		public Direction getDirection() {
			return direction;
		}

		public void setDirection(Direction direction) {
			this.direction = direction;
		}

		public MoveMessage(Direction direction) {
			super();
			this.direction = direction;
		}

	}
	
	public static class SendMessage extends Message {

		private static final long serialVersionUID = 1L;
	
		private int to;
		
		private byte[] message;

		public SendMessage(int to, byte[] message) {
			super();
			this.to = to;
			this.message = message;
		}

		public int getTo() {
			return to;
		}

		public void setTo(int to) {
			this.to = to;
		}

		public byte[] getMessage() {
			return message;
		}

		public void setMessage(byte[] message) {
			this.message = message;
		}
		
	}
	
	public static class ReceiveMessage extends Message {

		private static final long serialVersionUID = 1L;
		
		private int from;
		
		private byte[] message;

		public ReceiveMessage(int from, byte[] message) {
			super();
			this.from = from;
			this.message = message;
		}

		public int getFrom() {
			return from;
		}

		public void setFrom(int from) {
			this.from = from;
		}

		public byte[] getMessage() {
			return message;
		}

		public void setMessage(byte[] message) {
			this.message = message;
		}
		
		
	}
}
