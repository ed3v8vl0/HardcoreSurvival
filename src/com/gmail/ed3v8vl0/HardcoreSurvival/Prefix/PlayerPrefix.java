package com.gmail.ed3v8vl0.HardcoreSurvival.Prefix;

import java.util.ArrayList;
import java.util.Collection;

public class PlayerPrefix {
	private ArrayList<Prefix> prefixList = new ArrayList<Prefix>();
	private Prefix prefix;
	
	public PlayerPrefix(String ingamePrefix, String lobbyPrefix) {
		this.prefix = new Prefix(ingamePrefix, lobbyPrefix);
		this.prefixList.add(this.prefix.clone());
	}

	public Prefix getPrefix() {
		return this.prefix;
	}
	
	public boolean setPrefix(int id) {
		Prefix prefix;
		
		try {
			prefix = this.prefixList.get(id);
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
		
		this.prefix.ingame = prefix.ingame;
		this.prefix.lobby = prefix.lobby;
		
		return true;
	}
	
	public boolean addPrefix(Prefix prefix) {
		for (Prefix p : this.prefixList) {
			if (p.equals(prefix)) {
				return false;
			}
		}
		
		this.prefixList.add(prefix);
		return true;
		
	}
	
	public boolean removePrefix(int id) {
		try {
			this.prefixList.remove(id);
			return true;
		} catch (IndexOutOfBoundsException e) {
			return false;
		}
	}
	
	@SuppressWarnings("unchecked")
	public Collection<Prefix> getPrefixs() {
		return (Collection<Prefix>) this.prefixList.clone();
	}
	
	public static class Prefix implements Cloneable {
		private String ingame = "";
		private String lobby = "";
		
		public Prefix(String ingame, String lobby) {
			this.ingame = ingame;
			this.lobby = lobby;
		}
		
		public Prefix(String prefix, int value) {
			if (value == 1) {
				this.lobby = prefix;
			} else if (value == 2) {
				this.ingame = prefix;
				this.lobby = prefix;
			} else {
				this.ingame = prefix;
			}
		}
		
		public String getIngame() {
			return this.ingame;
		}
		
		public String getLobby() {
			return this.lobby;
		}

		@Override
		public boolean equals(Object object) {
			if (object instanceof Prefix) {
				Prefix prefix = (Prefix) object;
				
				if ((!prefix.getIngame().isEmpty() && prefix.getIngame().equals(this.ingame)) || (!prefix.getLobby().isEmpty() && prefix.getLobby().equals(this.lobby))) {
					return true;
				}
			}

			return false;
		}
		
		@Override
		public Prefix clone() {
	        try {
	            return (Prefix) super.clone();
	        } catch (CloneNotSupportedException e) {
	            throw new Error(e);
	        }
		}
	}
}
