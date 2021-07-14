package com.gmail.ed3v8vl0.HardcoreSurvival.World;

import java.lang.reflect.Type;
import java.util.*;

import javax.annotation.Nullable;

import org.bukkit.*;
import org.bukkit.attribute.Attribute;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.SurvivalCalc;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Ability;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Money;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.Death;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.LogData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.LogManager;
import com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia.InventoryManager;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;

import net.minecraft.server.v1_12_R1.PacketPlayInClientCommand;
import net.minecraft.server.v1_12_R1.PacketPlayInClientCommand.EnumClientCommand;

public class BorderManager {
	public final DamageRunnable damageRunnable;
	private final HardcoreSurvival mainClass;
	private FileConfiguration config;
	private final Season season;
	
	private World world;
	private WorldBorder worldBorder;

	// Center Position
	public boolean allowSpawn;
	private double xPos;
	private double zPos;

	protected HashMap<UUID, PlayerData> joinList = new HashMap<UUID, PlayerData>();
	protected List<UUID> banList = new ArrayList<UUID>();
	
	private boolean firstKill = false;
	
	public BorderManager(HardcoreSurvival mainClass, Season season) {
		this.mainClass = mainClass;
		this.config = this.mainClass.getConfig();
		this.season = season;
		this.world = Bukkit.getWorld(this.config.getString("worldborder.world"));
		
		if (this.world == null)
			this.world = Bukkit.createWorld(new WorldCreator(this.config.getString("worldborder.world")));
		
		this.worldBorder = this.world.getWorldBorder();

		this.damageRunnable = new DamageRunnable(mainClass, season, this);
		mainClass.getServer().getScheduler().scheduleSyncRepeatingTask(mainClass, damageRunnable, 0L, 1L);
		
		this.configInit();
		this.borderSetting();
	}

	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.damageRunnable.configInit();
		this.allowSpawn = this.config.getBoolean("worldborder.worldSpawn");
		this.xPos = this.config.getDouble("worldborder.location.center.xPos") + 0.5D;
		this.zPos = this.config.getDouble("worldborder.location.center.zPos") + 0.5D;
	}
	
	/**
	 * 해당 플레이어를 참가시킵니다.
	 * 
	 * @param player
	 * @return
	 */
	public void playerJoin(PlayerData playerData) { //따로 OfflinePlayer 검사는 생략 (Command 입력했다는 것 자체가 오프라인이 아님을 입증)
		if (this.season.isSeasonStart()) {
			if (!this.containsPlayer(playerData.getOfflinePlayer().getPlayer())) {
				if (!this.isBan(playerData.getOfflinePlayer().getPlayer())) {
					if (!this.season.afterPVP()) {
						if (this.playerCount() < 100) {
							Player player = playerData.getOfflinePlayer().getPlayer();
							Ability ability = playerData.getAbility();

							this.setSize();
							this.joinList.put(playerData.getPlayerUID(), playerData);
							player.teleport(this.getLocation(1));

							if (ability == Ability.Attack) {
							} else if (ability == Ability.Resistance) {
								player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20 + SurvivalCalc.Resistance.AddHealth);
								player.setHealth(20 + SurvivalCalc.Resistance.AddHealth);
							} else if (ability == Ability.Evasion) {
								player.setWalkSpeed((float) (0.2 + (0.2 * SurvivalCalc.Evasion.AddSpeed)));
							} else if (ability == Ability.Lucky) {
							}
						}
					}
				}
			}
		}
	}

	/**
	 * 해당 플레이어를 퇴장시킵니다.
	 * 
	 * @param player
	 * @return
	 */
	public void playerLeave(OfflinePlayer offlinePlayer, Entity entity, Death death) {
		PlayerData playerData = this.joinList.get(offlinePlayer.getUniqueId());
		String toString = entity != null ? entity.toString() : "";

		if (this.season.afterPVP()) {
			if (this.joinList.size() == 3) {
				Bukkit.broadcastMessage(this.config.getString("IngameMessage.ThirdWinner").replace("<PLAYER>", offlinePlayer.getName()));
				playerData.addMoney(Money.Third);
			} else if (this.joinList.size() == 2) {
				playerData.addMoney(Money.Second);
				Bukkit.broadcastMessage(this.config.getString("IngameMessage.SecondWinner").replace("<PLAYER>", offlinePlayer.getName()));
			}

			if (!this.firstKill && entity != null && entity instanceof Player) {
				this.firstKill = true;
			}
		}
		
		if (offlinePlayer.isOnline()) {
			Player player = offlinePlayer.getPlayer();
			
			player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
			player.setWalkSpeed(0.2F);
		} else {
			InventoryManager.nbtRestore(offlinePlayer);
		}
		
		LogManager.getInstance().getSeasonLog(this.season.getSeason()).add(new LogData(playerData.getPlayerUID(), toString, death, playerData.getKills(), this.joinList.size(), playerData.giveMoney()));
		this.joinList.remove(offlinePlayer.getUniqueId());
		this.banList.add(offlinePlayer.getUniqueId());
	}

	/**
	 * 해당 플레이어를 퇴장시킵니다.
	 * 
	 * @param player
	 * @return
	 */
	public void playerForceLeave(PlayerData playerData, @Nullable Entity entity, Death death) {
		OfflinePlayer offlinePlayer = playerData.getOfflinePlayer();
		String toString = entity != null ? entity.toString() : "";
		
		if (this.season.afterPVP()) {
			if (this.joinList.size() == 3) {
				Bukkit.broadcastMessage("[Hardcore] 3등 플레이어: " + offlinePlayer.getName());
				playerData.addMoney(Money.Third);
			} else if (this.joinList.size() == 2) {
				playerData.addMoney(Money.Second);
				Bukkit.broadcastMessage("[Hardcore]  2등 플레이어: " + offlinePlayer.getName());
			}

			if (!this.firstKill && entity != null && entity instanceof Player) {
				this.firstKill = true;
			}
		}
		
		if (offlinePlayer.isOnline()) {
			Player player = offlinePlayer.getPlayer();
			World world = playerData.getPrevWorld();
			
			player.teleport(world.getSpawnLocation());
			player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0);
			player.setWalkSpeed(0.2F);
		} else {
			InventoryManager.nbtRestore(offlinePlayer);
		}

		LogManager.getInstance().getSeasonLog(this.season.getSeason()).add(new LogData(playerData.getPlayerUID(), toString, death, playerData.getKills(), this.joinList.size(), playerData.giveMoney()));
		this.joinList.remove(offlinePlayer.getUniqueId());
		this.banList.add(offlinePlayer.getUniqueId());
	}
	
	public boolean firstKill() {
		return !this.firstKill;
	}

	/**
	 * WorldBorder를 초기 상태로 리셋시킵니다.
	 */
	public void Reset() {
		for (Player player : this.world.getPlayers()) {
			if (player.isDead()) {
				((CraftPlayer) player).getHandle().playerConnection.a(new PacketPlayInClientCommand(EnumClientCommand.PERFORM_RESPAWN));
			}
		}
		
		for (Object object : ((HashMap<?, ?>) this.joinList.clone()).values()) {
			if (object instanceof PlayerData) {
				PlayerData playerData = (PlayerData) object;
				OfflinePlayer offlinePlayer = playerData.getOfflinePlayer();
				
				if (offlinePlayer.isOnline()) {
					Player player = offlinePlayer.getPlayer();
					
					if (player.isDead()) {
						((CraftPlayer) player).getHandle().playerConnection.a(new PacketPlayInClientCommand(EnumClientCommand.PERFORM_RESPAWN));
					}
				}

				this.playerForceLeave(playerData, null, Death.FORCE);
			}
		}
		
		this.joinList.clear();
		this.banList.clear();
		this.world.getWorldBorder().reset();
		this.borderSetting();
	}

	public void unban(Player player) {
		this.banList.remove(player.getUniqueId());
	}
	
	/**
	 * 월드를 새롭게 지정하고 모든 설정을 초기화합니다. 기존 월드는 기본 WorldBorder 설정으로 설정합니다.
	 * 
	 * @param world
	 */
	public void setWorld(World world) {
		this.Reset();
		this.world = world;
		this.worldBorder = this.world.getWorldBorder();
		this.damageRunnable.setWorld(world);
	}

	/**
	 * WorldBorder를 Config에 설정된 초기 세팅값으로 설정합니다. Size는 최소 크기로 설정됩니다.
	 */
	private void borderSetting() {
		this.worldBorder.setCenter(this.xPos, this.zPos);
		this.worldBorder.setDamageAmount(0);
		this.worldBorder.setDamageBuffer(0);
		this.worldBorder.setWarningDistance(this.config.getInt("worldborder.warning.distance"));
		this.worldBorder.setWarningTime(this.config.getInt("worldborder.warning.time"));
		this.worldBorder.setSize(this.config.getDouble("worldborder.location.size.10_Player"));
	}
	
	public void bossBarRemove() {
		this.damageRunnable.bossBar.removeAll();
	}
	
	public PlayerData getPlayerData(Player player) {
		return this.joinList.get(player.getUniqueId());
	}
	
	public PlayerData getPlayerData(UUID playerUUID) {
		return this.joinList.get(playerUUID);
	}
	
	/**
	 * 해당 위치가 WorldBorder 안에 있는지 확인합니다.
	 * 
	 * @param location
	 * @return
	 */
	public boolean isInside(Location location) {
		return this.worldBorder.isInside(location);
	}

	/**
	 * 해당 플레이어가 참여했는지 반환합니다.
	 * 
	 * @param player
	 * @return
	 */
	public boolean containsPlayer(Player player) {
		return this.joinList.containsKey(player.getUniqueId());
	}

	/**
	 * 해당 플레이어가 밴 목록에 포함되는지 반환합니다.
	 * 
	 * @param player
	 * @return
	 */
	public boolean isBan(Player player) {
		return this.banList.contains(player.getUniqueId());
	}

	/**
	 * 밴 리스트를 반환합니다.
	 * 
	 * @return
	 */
	public Iterator<UUID> getBanList() {
		return this.banList.iterator();
	}

	/**
	 * 참여 플레이어 수를 반환합니다.
	 * 
	 * @return
	 */
	public int playerCount() {
		return this.joinList.size() + this.banList.size();
	}

	/**
	 * WorldBorder의 작동을 시작합니다.
	 * 
	 * @param time
	 */
	public void startBorder(long time) {
		double maximum = this.playerCount() > 50 ? this.config.getDouble("worldborder.location.size.100_Player") : (this.playerCount() > 10 ? this.config.getDouble("worldborder.location.size.50_Player") : this.config.getDouble("worldborder.location.size.10_Player"));
		double mininum = this.config.getDouble("worldborder.location.size.minimum");

		this.worldBorder.setSize(maximum);

		if (time > 0)
			this.worldBorder.setSize((maximum * -1) + mininum, time);
	}
	
	/**
	 * WorldBorder의 크기를 플레이어의 인원수에 맞게 설정합니다.
	 */
	public void setSize() {
		double maximum = this.playerCount() > 50 ? this.config.getDouble("worldborder.location.size.100_Player") : (this.playerCount() > 10 ? this.config.getDouble("worldborder.location.size.50_Player") : this.config.getDouble("worldborder.location.size.10_Player"));
		
		if (maximum == this.worldBorder.getSize()) {
			this.worldBorder.setSize(maximum);
		}
	}

	public void chunkLoad() {
		double distance = this.worldBorder.getSize() / 2;
		
		for (int minX = ((int) Math.floor((distance * -1) + this.xPos)) >> 4; minX < ((int) (Math.floor(distance + this.xPos)) >> 4); minX++) {
			for (int minZ = ((int) Math.floor((distance * -1) + this.zPos)) >> 4; minZ < ((int) (Math.floor(distance + this.zPos)) >> 4); minZ++) {
				Chunk chunk = this.world.getChunkAt(minX, minZ);
				
				if (!chunk.isLoaded()) {
					chunk.load();
				}
			}
		}
	}
	
	public boolean chunkUnload;
	
	/**
	 * WorldBorder의 크기를 설정합니다.
	 * 
	 * @param size
	 */
	public void setSize(double size) {
		this.worldBorder.setSize(size);
	}
	
	private int count = 0;
	
	/**
	 * 무조건 동기 함수 사용
	 * 랜덤 텔레포트를 위한 Location을 반환합니다. 물과 용암의 경우에는 새로운 위치를 다시 반환합니다.
	 * 
	 * @return
	 */
	public Location getLocation(double margin) {
		if (this.count >= 100) //무한루프 방지
			return new Location(this.world, 0, 0, 0);
		
		double distance = (this.worldBorder.getSize() / 2) - margin;
		Location location = new Location(this.world, (int) ((Math.random() * (distance * 2)) - distance), 0, (int) ((Math.random() * (distance * 2)) - distance));

		location.add(this.xPos, 0, this.zPos);
		location.setY(this.world.getHighestBlockYAt(location));

		if (this.world.getBlockAt(location.clone().add(0, -1, 0)).isLiquid()) {
			return this.getLocation(++count);
		}

		this.count = 0;
		return location;
	}
	
	/**
	 * BorderManager에 설정된 월드를 반환합니다.
	 * 
	 * @return
	 */
	public World getWorld() {
		return this.world;
	}

	public static class Seriazlier implements JsonSerializer<BorderManager> {
		private static final Gson gson = new GsonBuilder().registerTypeAdapter(PlayerData.class, new PlayerData.Serializer()).create();

		public static void deserialize(BorderManager borderManager, JsonObject jsonObject) throws JsonParseException {
			Type type = new TypeToken<Map<UUID, PlayerData>>() {
			}.getType();
			Type type_ = new TypeToken<List<UUID>>() {
			}.getType();

			borderManager.joinList = gson.fromJson(jsonObject.get("joinList"), type);
			borderManager.banList = gson.fromJson(jsonObject.get("banList"), type_);

			if (jsonObject.get("BorderSize").getAsDouble() > 0) {
				((CraftWorld) borderManager.getWorld()).getHandle().getWorldBorder().transitionSizeBetween(jsonObject.get("BorderSize").getAsDouble(), jsonObject.get("TargetSize").getAsDouble(), jsonObject.get("UntillTime").getAsLong());
			} else {
				borderManager.worldBorder.setSize(borderManager.config.getDouble("worldborder.location.size.10_Player"));
			}
		}

		@Override
		public JsonElement serialize(BorderManager borderManager, Type type, JsonSerializationContext context) {
			JsonObject jsonObject = new JsonObject();

			jsonObject.add("joinList", gson.toJsonTree(borderManager.joinList, borderManager.joinList.getClass()));
			jsonObject.add("banList", gson.toJsonTree(borderManager.banList, borderManager.banList.getClass()));

			if (borderManager.season.getTimeUntilTarget() > 0) {
				jsonObject.addProperty("BorderSize", borderManager.worldBorder.getSize());
				jsonObject.addProperty("TargetSize", ((CraftWorld) borderManager.getWorld()).getHandle().getWorldBorder().j()); // getTargetSize()
				jsonObject.addProperty("UntillTime", borderManager.season.getTimeUntilTarget());
			} else {
				jsonObject.addProperty("BorderSize", -1);
				jsonObject.addProperty("TargetSize", -1);
				jsonObject.addProperty("UntillTime", -1);
			}

			return jsonObject;
		}
	}
}