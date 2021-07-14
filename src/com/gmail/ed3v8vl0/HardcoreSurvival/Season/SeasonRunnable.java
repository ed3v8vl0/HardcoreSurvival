package com.gmail.ed3v8vl0.HardcoreSurvival.Season;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season.Status;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;

public class SeasonRunnable implements Runnable {
	private final Season season;
	private FileConfiguration config;
	
	private int lastTime = 1;
	private int borderTime = 1;
	private boolean isBorder = false;
	private int minPlayer;
	private long minTime;

	public SeasonRunnable(Season season, FileConfiguration config) {
		this.season = season;
		this.config = config;
	}

	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.minPlayer = this.config.getInt("worldborder.location.size.minPlayer");
		this.minTime = this.config.getLong("worldborder.location.size.minTime");
	}
	
	@Override
	public void run() {
		long pvpTime = this.season.startTime - System.currentTimeMillis();
		BorderManager borderManager = this.season.getBorderManager();

		if (pvpTime <= 0) {
			if (this.season.time > 0) {
				if (borderManager.playerCount() >= this.minPlayer) {
					this.season.getBorderManager().startBorder(this.season.time);
					this.season.time = 0;
					this.season.startTime = 0;
					this.season.setStatus(Status.BORDER_START);
					Bukkit.broadcastMessage(this.config.getString("IngameMessage.PVPStart"));
				} else {
					Bukkit.broadcastMessage(this.config.getString("IngameMessage.MinimumPlayer").replace("<TIME>", String.valueOf(Math.round(this.minTime / 100) / 10)));
					this.season.startTime = System.currentTimeMillis() + this.minTime; // 플레이어 인원수가 최소 2명 이상 되지 않을경우 10초 추가
					this.lastTime = 1;
				}
			}
		} else {
			int temp = (int) Math.ceil(pvpTime / 1000.0);

			if (this.lastTime != temp) {
				if (temp > 0 && temp <= 5) {
					if (borderManager.playerCount() >= this.minPlayer) {
						Bukkit.broadcastMessage(this.config.getString("IngameMessage.PVPCount").replace("<TIME>", String.valueOf(temp)));
						this.lastTime = temp;
					} else {
						Bukkit.broadcastMessage(this.config.getString("IngameMessage.MinimumPlayer").replace("<TIME>", String.valueOf(Math.round(this.minTime / 100) / 10)));
						this.season.startTime = System.currentTimeMillis() + this.minTime; // 플레이어 인원수가 최소 2명 이상 되지 않을경우 10초 추가
						this.lastTime = 1;
					}
				}
			}
		}

		long borderTime = this.season.getTimeUntilTarget();

		if (borderTime <= 0) {
			if (this.isBorder) {
				this.season.getBorderManager().setSize(6.0E7D);
				this.season.setStatus(Status.BORDER_END);
				this.isBorder = false;
				Bukkit.broadcastMessage(this.config.getString("IngameMessage.BorderEnd"));
			}
		} else {
			int temp = (int) Math.ceil(borderTime / 1000.0);

			if (this.borderTime != temp) {
				if (temp > 0 && temp <= 5) {
					Bukkit.broadcastMessage(this.config.getString("IngameMessage.BorderCount").replace("<TIME>", String.valueOf(temp)));
					this.borderTime = temp;
					this.isBorder = true;
				}
			}
		}
	}
}
