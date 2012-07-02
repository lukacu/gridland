package org.grid.server;

import java.awt.Color;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Hashtable;
import java.util.StringTokenizer;

public class TeamDatabase {

	public static class TeamData {
		
		private String id, name, passphrase;
		
		private Color color;

		protected TeamData(String id, String name, String passphrase,
				Color color) {
			super();
			this.id = id;
			this.name = name;
			this.passphrase = passphrase;
			this.color = color;
		}

		public String getId() {
			return id;
		}

		public String getName() {
			return name;
		}

		public String getPassphrase() {
			return passphrase;
		}

		public Color getColor() {
			return color;
		}

	}
	
	private Hashtable<String, TeamData> data = new Hashtable<String, TeamData>();
	
	public TeamDatabase(File file) throws IOException {
		
		BufferedReader reader = new BufferedReader(new FileReader(file));
		
		while (true) {
			
			String line = reader.readLine();
			
			if (line == null) break;
			
			StringTokenizer tokens = new StringTokenizer(line, ";");
			
			String id = tokens.nextToken();
			String name = tokens.nextToken();
			String passphrase = tokens.nextToken();
			Color color = Color.decode(tokens.nextToken());
			
			TeamData d = new TeamData(id, name, passphrase, color);
			
			data.put(d.getId(), d);
			
		}
		
	}
	
	public Team createTeam(String id) {
		
		TeamData d = data.get(id);
		
		if (d == null) return null;
		
		Team team = new Team(d.getId(), d.getColor());
		
		team.setPassphrase(d.getPassphrase());
		
		return team;
		
	}
	
	public void print(PrintStream out) {
		
		for (String id : data.keySet()) {
			
			TeamData t = data.get(id);
			
			out.format("----- \nID: %s\nName: %s\nPassphrase: %s\n-----\n", t.getId(), t.getName(), t.getPassphrase());
			
		}
		
	}
	
}
