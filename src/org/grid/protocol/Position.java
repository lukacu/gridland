/**
 * 
 */
package org.grid.protocol;

import java.io.Serializable;

public class Position implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private int x;
	
	private int y;

	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	

	public Position(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	public Position(Position p) {
		super();
		this.x = p.x;
		this.y = p.y;
	}
	
	public Position(Position p, int factor) {
		super();
		this.x = p.x * factor;
		this.y = p.y * factor;
	}
	
	public String toString() {
		return String.format("Position: %d, %d", getX(), getY());
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof Position) 
			return (((Position) obj).x == x && ((Position) obj).y == y);
		else return false;
	}

    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 31*hash + x;
        hash = 31*hash + y;
        return hash;
    }

    public static int distance(Position p1, Position p2) {
		return Math.max(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY()
				- p2.getY()));
    }
    
    public void offset(Position p) {
    	
    	x += p.x;
    	y += p.y;
    	
    }
}
