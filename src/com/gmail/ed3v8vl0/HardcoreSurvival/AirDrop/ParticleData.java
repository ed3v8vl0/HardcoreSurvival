package com.gmail.ed3v8vl0.HardcoreSurvival.AirDrop;

import javax.annotation.Nonnull;

import org.bukkit.Particle;
import org.bukkit.configuration.file.FileConfiguration;

public class ParticleData {
	@Nonnull
	public Particle particle;
	
	public double xPos;
	public double yPos;
	public double zPos;
	
	public double offsetX;
	public double offsetY;
	public double offsetZ;
	
	public int amount;
	
	public ParticleData(FileConfiguration config) {
		this.particle = Particle.valueOf(config.getString("particle"));
		this.xPos = config.getDouble("xPos");
		this.yPos = config.getDouble("yPos");
		this.zPos = config.getDouble("zPos");
		this.offsetX = config.getDouble("offsetX");
		this.offsetY = config.getDouble("offsetY");
		this.offsetZ = config.getDouble("offsetZ");
		this.amount = config.getInt("amount");
	}
}
