package com.gmail.ed3v8vl0.HardcoreSurvival.World;

import java.util.*;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.boss.BarColor;
import org.bukkit.boss.BarFlag;
import org.bukkit.boss.BarStyle;
import org.bukkit.boss.BossBar;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season.Status;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.Death;
import com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia.FoodMetaDataEx;
import com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia.ReflectionHelper;

import net.minecraft.server.v1_12_R1.EntityPlayer;

public class DamageRunnable implements Runnable {
	protected final BossBar bossBar;
	
	private final Season season;
	private final BorderManager borderManager;
	public String finishMessage;
	public String pvpMessage;
	public String endMessage;
	public double damage;
	public int delay;
	
	private HashMap<UUID, Integer> damageStamp = new HashMap<UUID, Integer>();
	private List<UUID> lastPlayer = new ArrayList<UUID>();
	
	public DamageRunnable(HardcoreSurvival mainClass, Season season, BorderManager borderManager) {
		this.bossBar = Bukkit.createBossBar("", BarColor.BLUE, BarStyle.SEGMENTED_10);
		
		this.season = season;
		this.borderManager = borderManager;
		this.configInit();
	}

	public void configInit() {
		FileConfiguration config = HardcoreSurvival.getInstance().getConfig();
		List<String> flags = config.getStringList("BossBar.BarFlag");
		
		this.bossBar.setColor(BarColor.valueOf(config.getString("BossBar.BarColor")));
		this.bossBar.setStyle(BarStyle.valueOf(config.getString("BossBar.BarStyle")));
		this.bossBar.removeFlag(BarFlag.CREATE_FOG);
		this.bossBar.removeFlag(BarFlag.DARKEN_SKY);
		this.bossBar.removeFlag(BarFlag.PLAY_BOSS_MUSIC);
		
		for (String flag : flags) {
			this.bossBar.addFlag(BarFlag.valueOf(flag));
		}

		this.finishMessage = config.getString("BossBar.message.finish");
		this.pvpMessage = config.getString("BossBar.message.pvp");
		this.endMessage = config.getString("BossBar.message.end");
		this.damage = config.getDouble("worldborder.damage.amount");
		this.delay = config.getInt("worldborder.damage.tick");
	}
	
	public void setWorld(World world) {
		this.damageStamp.clear();
		this.lastPlayer.clear();
	}
	
	@Override
	public void run() {
		this.bossBar.removeAll();
		
		if (!this.season.isSeasonStart())
			return;

		this.bossBarUpdate();
		List<Player> playerList = this.updatePlayers(); //onlinePlayers
		
		//SeasonEnd
		if (this.season.afterPVP()) {
			if (playerList.size() == 1) { //최종 승리자가 1명
				this.season.seasonEnd(playerList);
			} else { //최종 승리자가 2명 or 0명
				if (this.lastPlayer.size() > 0 && borderManager.joinList.size() == 0) {
					List<Player> winners = new ArrayList<Player>();
					
					for (UUID playerUID : this.lastPlayer) {
						winners.add(Bukkit.getPlayer(playerUID));
					}
					
					this.season.seasonEnd(winners);
				} else {
					if (this.lastPlayer.size() == 0 && playerList.size() == 0) {
						System.out.println("[ERROR] 모든 유저가 접속하지 않아 최종 우승자가 없는 상태로 게임이 끝났습니다.");
						this.season.seasonEnd(playerList); //Empty
					}
					
					this.lastPlayer.clear();
					
					for (Player player : playerList) {
						this.lastPlayer.add(player.getUniqueId());
					}
				}
			}
		}
		
		for (Player player : playerList) {
			EntityPlayer entityPlayer = ((CraftPlayer) player).getHandle();
			
			if (!(entityPlayer.getFoodData() instanceof FoodMetaDataEx)) { //FoodMetaDataEx Change
				ReflectionHelper.ReflectionFood(entityPlayer, new FoodMetaDataEx(entityPlayer));
			}
					
			if (!borderManager.isInside(player.getLocation()) || this.season.getStatus() == Status.BORDER_END) { //Damage Tick
				int tickCount = 0;

				if (this.damageStamp.containsKey(player.getUniqueId())) {
					tickCount = this.damageStamp.get(player.getUniqueId());

					if (tickCount > this.delay) {
						tickCount = 0;
						player.damage(this.damage);
						this.damageStamp.remove(player.getUniqueId());
					}
				}
				
				tickCount++;
				this.damageStamp.put(player.getUniqueId(), tickCount);
			} else {
				this.damageStamp.remove(player.getUniqueId());
			}
		}
	}
	
	public void bossBarUpdate() {
		if (this.season.getStatus() == Status.BORDER_START) {
			long day = this.season.getTimeUntilTarget() / 86400000;
			long hour = (this.season.getTimeUntilTarget() % 86400000) / 3600000;
			long minute = ((this.season.getTimeUntilTarget() % 86400000) % 3600000) / 60000;
			long second = ((this.season.getTimeUntilTarget() % 86400000) % 3600000) % 60000;
			
			this.bossBar.setTitle(this.finishMessage.replace("<DAY>", String.valueOf(day)).replace("<HOUR>", String.valueOf(hour)).replace("<MINUTE>", String.valueOf(minute)).replace("<SECOND>", String.valueOf(second))); //("자기장 소멸까지 남은 시간: " + day + "일 " + hour + "시 " + minute + "분 " + second + "초");
		} else if (this.season.getStatus() == Status.SEASON_READY) {
			long day = this.season.getStartTime() / 86400000;
			long hour = (this.season.getStartTime() % 86400000) / 3600000;
			long minute = ((this.season.getStartTime() % 86400000) % 3600000) / 60000;
			long second = ((this.season.getStartTime() % 86400000) % 3600000) % 60000;
			
			this.bossBar.setTitle(this.pvpMessage.replace("<DAY>", String.valueOf(day)).replace("<HOUR>", String.valueOf(hour)).replace("<MINUTE>", String.valueOf(minute)).replace("<SECOND>", String.valueOf(second)));
		} else if (this.season.getStatus() == Status.SEASON_END) {
			this.bossBar.setTitle(this.endMessage);
		}
	}
	
	public List<Player> updatePlayers() {
		List<Player> playerList = new ArrayList<Player>();
		
		for (Iterator<UUID> iterator = this.borderManager.joinList.keySet().iterator(); iterator.hasNext(); ) { //ConcurrentModificationException
			OfflinePlayer offlinePlayer = Bukkit.getOfflinePlayer(iterator.next());
			
			if (offlinePlayer.isOnline()) {
				Player player = offlinePlayer.getPlayer();
				
				if (!player.isDead()) {
					playerList.add(player);
					this.bossBar.addPlayer(player);
				}
			} else {
				if (this.season.blockTime()) {
					this.borderManager.playerLeave(offlinePlayer, null, Death.SURRENDER);
				}
			}
		}
		
		return playerList;
	}
}
