package com.gmail.ed3v8vl0.HardcoreSurvival.Season;

import java.lang.reflect.Type;
import java.util.Iterator;
import java.util.TreeSet;
import java.util.UUID;

import javax.annotation.Nonnull;

import com.google.gson.*;

public class Log {
	private final int seasonId;
	private final TreeSet<LogData> logData = new TreeSet<LogData>();
	
	public Log(int seasonId) {
		this.seasonId = seasonId;
	}
	
	public void add(LogData log) {
		this.logData.add(log);
	}
	
	public void clear() {
		this.logData.clear();
	}
	
	public LogData getRanking(int ranking) {
		Iterator<LogData> iterator = this.logData.iterator();
		LogData logData = null;
		
		while (iterator.hasNext()) {
			LogData target = iterator.next();
			
			if (target.getRanking() == ranking) {
				logData = target;
			}
		}
		
		return logData;
	}
	
	public int getSeasonId() {
		return this.seasonId;
	}
	
	@SuppressWarnings("unchecked")
	@Nonnull
	public TreeSet<LogData> getData() {
		return (TreeSet<LogData>) this.logData.clone();
	}

	@Nonnull
	public int getId() {
		return this.seasonId;
	}

	public static class LogData implements Comparable<LogData> {
		private final UUID playerUUID;
		private final String entityToString;
		private final Death deathReason;
		private final int killCount;
		private final double money;
		private final int ranking;
		private final long timeLog;

		public LogData(UUID playerUUID, String entityToString, Death deathReason, int killCount, int ranking, double money) {
			this.playerUUID = playerUUID;
			this.entityToString = entityToString;
			this.deathReason = deathReason;
			this.killCount = killCount;
			this.money = money;
			this.ranking = ranking;
			this.timeLog = System.currentTimeMillis();
		}

		@Nonnull
		public UUID getUUID() {
			return this.playerUUID;
		}

		@Nonnull
		public String getEntityToString() {
			return this.entityToString;
		}
		
		@Nonnull
		public Death getDeath() {
			return this.deathReason;
		}

		@Nonnull
		public int getKills() {
			return this.killCount;
		}

		@Nonnull
		public double getMoney() {
			return this.money;
		}

		@Nonnull
		public int getRanking() {
			return this.ranking;
		}
		
		@Nonnull
		public long getTime() {
			return this.timeLog;
		}

		@Override
		public int hashCode() {
			return this.playerUUID.hashCode();
		}

		@Override
		public int compareTo(LogData o) {
			return this.timeLog > o.timeLog ? -1 : this.timeLog == o.timeLog ? 0 : 1;
		}
	}

	public enum Death {
		CONTACT, ENTITY_ATTACK, ENTITY_SWEEP_ATTACK, PROJECTILE, SUFFOCATION, FALL, FIRE, FIRE_TICK, MELTING, LAVA, DROWNING, BLOCK_EXPLOSION, ENTITY_EXPLOSION, VOID, LIGHTNING, SUICIDE, STARVATION, POISON, MAGIC, WITHER, FALLING_BLOCK, THORNS, DRAGON_BREATH, CUSTOM, FLY_INTO_WALL, HOT_FLOOR, CRAMMING, SURRENDER, FORCE
	}
	
	static class Serializer implements JsonSerializer<Log>, JsonDeserializer<Log> {
		private static final Gson gson = new GsonBuilder().create();
		
		@Override
		public Log deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
			JsonObject jsonObject = (JsonObject) jsonElement;
			Log log = new Log(jsonObject.get("seasonId").getAsInt());
			JsonArray jsonArray = jsonObject.get("logDatas").getAsJsonArray();
			
			for (JsonElement element : jsonArray) {
				log.add(gson.fromJson(element, LogData.class));
			}
			
			return log;
		}

		@Override
		public JsonElement serialize(Log log, Type type, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();
			JsonArray jsonArray = new JsonArray();
			Iterator<LogData> iterator = log.logData.iterator();
			
			while (iterator.hasNext()) {
				LogData logData = iterator.next();
				
				jsonArray.add(gson.toJsonTree(logData));
			}
			
			jsonObject.addProperty("seasonId", log.seasonId);
			jsonObject.add("logDatas", jsonArray);
			
			return jsonObject;
		}
		
	}
}
