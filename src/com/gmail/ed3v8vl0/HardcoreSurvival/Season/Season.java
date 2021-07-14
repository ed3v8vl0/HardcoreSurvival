package com.gmail.ed3v8vl0.HardcoreSurvival.Season;

import java.lang.reflect.Type;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Collection;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Money;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;
import com.google.gson.*;

public class Season {
	private final BorderManager borderManager;
	private FileConfiguration config;
	private final SeasonRunnable seasonRunnable;
	
	private Status status = Status.SEASON_END;

	private int season = 1;
	protected long time = -1; //Border Time
	protected long startTime = -1; //Border START
	
	public long pvpTime;
	public long blockTime;
	
	public Season(HardcoreSurvival mainClass) {
		this.config = mainClass.getConfig();
		this.borderManager = new BorderManager(mainClass, this);
		this.seasonRunnable = new SeasonRunnable(this, this.config);
		
		mainClass.getServer().getScheduler().scheduleSyncRepeatingTask(mainClass, this.seasonRunnable, 0L, 1L);
		
		this.configInit();
	}
	
	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.seasonRunnable.configInit();
		this.pvpTime = config.getInt("worldborder.location.size.pvpTime");
		this.blockTime = config.getInt("worldborder.location.size.blockTime");
	}
	
	/**
	 * 시즌을 시작합니다.
	 * @param time 시즌 진행 시간 (밀리세컨드가 아닌 초로 값을 받음)
	 */
	public boolean startSeason(long time) {
		if (time > 0) {
			this.status = Status.SEASON_READY;
			this.startTime = System.currentTimeMillis() + this.pvpTime;
			this.time = time;
			this.borderManager.Reset();
			LogManager.getInstance().getSeasonLog(this.season).clear();
			return true;
		} else {
			return false;
		}
	}
	
	/**
	 * 시즌을 카운팅하고 데이터를 초기화하며 시즌을 종료합니다.
	 * @param players 최종 우승 플레이어들
	 */
	public void seasonEnd(Collection<Player> playerList) {
		for (Player player : playerList) {
			PlayerData playeData = this.borderManager.getPlayerData(player);
			
			if (playeData != null) {
				Bukkit.broadcastMessage(this.config.getString("IngameMessage.FirstWinner").replace("<PLAYER>", player.getDisplayName()));
				playeData.addMoney(Money.First);
				break;
			}
		}
		
		this.borderManager.Reset();
		this.season++;
		this.status = Status.SEASON_END;
	}
	
	/**
	 * 현재 시즌을 변경하지 않고 데이터를 초기화합니다.
	 */
	public void Reset() {
		this.status = Status.SEASON_END;
		this.borderManager.Reset();
		this.time = 0;
		this.startTime = 0;
		LogManager.getInstance().getSeasonLog(this.season).clear();
	}
	
	/**
	 * 시즌 종료 <CONFIG> 전인지 반환합니다.
	 * @param lastTime
	 * @return
	 */
	public boolean blockTime() {
		if (this.getStatus() == Status.BORDER_START) {
			return this.getTimeUntilTarget() <= this.blockTime ? true : false;
		}
		
		return false;
	}
	
	/**
	 * 설정된 시간을 넘겼는지 반환합니다.
	 * @return
	 */
	public boolean afterPVP() {
		return this.startTime > 0 ? false : true;
	}
	
	/**
	 * 자기장 소멸 시간을 반환합니다.
	 * @return SystemZoneID
	 */
	public LocalDateTime getEndTime() {
		if (this.startTime > 0)
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.startTime + (this.time * 1000)), ZoneId.systemDefault());
		else
			return LocalDateTime.ofInstant(Instant.ofEpochMilli(this.getTimeUntilTarget() + System.currentTimeMillis()), ZoneId.systemDefault());
	}
	
	public long getStartTime() {
		return this.startTime - System.currentTimeMillis();
	}
	
	/**
	 * 현재 시즌이 진행중인지 반환합니다.
	 * @return
	 */
	public boolean isSeasonStart() {
		return this.startTime > 0 || this.status != Status.SEASON_END;
	}
	
	/**
	 * 자기장이 줄어들기까지의 시간을 반환합니다.
	 * @return
	 */
    public long getTimeUntilTarget()
    {
        return ((CraftWorld) this.borderManager.getWorld()).getHandle().getWorldBorder().i();
    }

	/**
	 * 현재 시즌 상태를 설정합니다.
	 * @param status
	 */
	protected void setStatus(Status status) {
		this.status = status;
	}
	
	/**
	 * 현재 시즌 상태를 반환합니다.
	 * @return
	 */
	public Status getStatus() {
		return this.status;
	}
	
	/**
	 * 현재 시즌을 반환합니다.
	 * @return
	 */
	public int getSeason() {
		return season;
	}
	
	/**
	 * BorderManager를 반환합니다.
	 * @return
	 */
	public BorderManager getBorderManager() {
		return this.borderManager;
	}
	
	public static enum Status {
		SEASON_READY, BORDER_START, BORDER_END, SEASON_END
	}
	
	public static class Seriazlier implements JsonSerializer<Season> {
		public static void deserialize(Season season, JsonObject jsonObject)
				throws JsonParseException {
			season.status = Status.valueOf(jsonObject.get("Status").getAsString());
			season.season = jsonObject.get("Season").getAsInt();
			
			if (jsonObject.get("Time").getAsLong() > 0) {
				season.time = jsonObject.get("Time").getAsLong();
			} else {
				season.time = -1;
			} if (jsonObject.get("StartTime").getAsLong() > 0) {
				season.startTime = jsonObject.get("StartTime").getAsLong() + System.currentTimeMillis();
			} else {
				season.startTime = -1;
			}
		}

		@Override
		public JsonElement serialize(Season season, Type type, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();
			
			jsonObject.addProperty("Status", season.status.name());
			jsonObject.addProperty("Season", season.season);
			jsonObject.addProperty("Time", season.time);
			
			
			if (season.time > 0)
				jsonObject.addProperty("Time", season.time);
			else
				jsonObject.addProperty("Time", -1);
			
			if (season.startTime > 0)
				jsonObject.addProperty("StartTime", season.startTime - System.currentTimeMillis());
			else
				jsonObject.addProperty("StartTime", -1);
			
			return jsonObject;
		}
	}
}
