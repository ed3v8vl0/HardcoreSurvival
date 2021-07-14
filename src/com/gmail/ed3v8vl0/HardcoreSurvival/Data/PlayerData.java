package com.gmail.ed3v8vl0.HardcoreSurvival.Data;

import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.UUID;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.SurvivalCalc.MoneyReward.Reward;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.Death;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.LogData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.LogManager;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;
import com.google.gson.*;

import net.milkbowl.vault.economy.Economy;

public class PlayerData {
	private final UUID playerUID;
	private final UUID prevWorldUID;
	private final Ability ability;
	
	private long lastTime;
	private long limitTime;
	
	public HashMap<UUID, Integer> attackCount = new HashMap<UUID, Integer>();
	public LastDamage lastDamage;
	public long sprintStamp;
	
	private double money;
	private int killCount;
	
	public PlayerData(Player player, Ability ability) {
		this.playerUID = player.getUniqueId();
		this.prevWorldUID = player.getWorld().getUID();
		this.ability = ability;
	}
	
	public void onPlayerJoin(BorderManager borderManager, long lastTime) {
		if (this.lastTime <= 0) {
			this.lastTime = Math.round(lastTime * SurvivalCalc.lastPercent);
			this.limitTime = System.currentTimeMillis() + SurvivalCalc.lastLimit;
		} else {
			if (this.limitTime <= System.currentTimeMillis()) {
				borderManager.playerLeave(this.getOfflinePlayer(), null, Death.SURRENDER);
			}
		}
	}
	
	public void onPlayerQuit(BorderManager borderManager, long onlineTime) {
		if (lastTime > 0) {
			this.lastTime -= onlineTime;
		}
	}
	
	public void addMoney(Money money) { //따로 킬 카운트 세서 추가 돈이랑 시즌 연속 1등시 보상 그것도 설계해야됨
		FileConfiguration config = HardcoreSurvival.getInstance().getConfig();
		
		if (money == Money.Kill) {
			this.killCount++;
			this.money += SurvivalCalc.MoneyReward.Kill;
			this.sendMessage(config.getString("IngameMessage.Kill").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.Kill)));
			
			if (this.killCount > 0) {
				this.money += SurvivalCalc.MoneyReward.KillingSpree;
				this.sendMessage(config.getString("IngameMessage.KillingSpree").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.KillingSpree)));
				
				for (Reward reward : SurvivalCalc.MoneyReward.KillRewards) {
					if (reward.getKey() == this.killCount) {
						this.money += reward.getValue();
						this.sendMessage(config.getString("IngameMessage.KillRewards").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.KillRewards)));
					}
				}
			}
		} else if (money == Money.FirstBlood) {
			this.money += SurvivalCalc.MoneyReward.FirstBlood;
			this.killCount++;

			this.sendMessage(config.getString("IngameMessage.FirstBlood").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.FirstBlood)));
		} else if (money == Money.First) { //Write
			Log log = LogManager.getInstance().getLastIndex();
			
			if (log != null) {
				LogData logData = log.getRanking(1);

				if (logData != null && logData.getUUID().equals(this.playerUID)) {
					this.money += SurvivalCalc.MoneyReward.ConsecutiveWin;
					this.sendMessage(config.getString("IngameMessage.ConsecutiveWin").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.ConsecutiveWin)));
				}
			}
			
			this.money += SurvivalCalc.MoneyReward.Frist;
			this.sendMessage(config.getString("IngameMessage.First").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.Frist)));
		} else if (money == Money.Second) { //Write
			this.money += SurvivalCalc.MoneyReward.Second;
			this.sendMessage(config.getString("IngameMessage.Second").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.Second)));
		} else if (money == Money.Third) { //Write
			this.money += SurvivalCalc.MoneyReward.Third;
			this.sendMessage(config.getString("IngameMessage.Third").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.Third)));
		} else if (money == Money.Supply) { //Write
			this.money += SurvivalCalc.MoneyReward.Supply;
			this.sendMessage(config.getString("IngameMessage.Supply").replace("<MONEY>", String.valueOf(SurvivalCalc.MoneyReward.Supply)));
		}

		this.sendMessage(config.getString("IngameMessage.Result").replace("<MONEY>", String.valueOf(this.money)));
	}
	
	private void sendMessage(String message) {
		OfflinePlayer offlinePlayer = this.getOfflinePlayer();
		if (offlinePlayer.isOnline()) {
			offlinePlayer.getPlayer().sendMessage(message);
		}
	}
	
	public int getKills() {
		return this.killCount;
	}
	
	public double giveMoney() {
		Economy economy = HardcoreSurvival.getInstance().getEconomy();
		double money = this.money;
		
		if (economy != null) {
			economy.depositPlayer(this.getOfflinePlayer(), this.money);
		} else {
			System.out.println("Vault 플러그인이 설치되지 않아 플레이어에게 돈을 지급하지 못하였습니다.");
		}
		
		return money;
	}
	
	public UUID getPlayerUID() {
		return this.playerUID;
	}
	
	public UUID getPrevWorldUID() {
		return this.prevWorldUID;
	}
	
	public Ability getAbility() {
		return this.ability;
	}
	
	public OfflinePlayer getOfflinePlayer() {
		return Bukkit.getOfflinePlayer(this.playerUID);
	}
	
	public World getPrevWorld() {
		return Bukkit.getWorld(this.prevWorldUID);
	}
	
	@Override
	public int hashCode() {
		return playerUID.hashCode();
	}
	
	public static enum Ability {
		Attack, Resistance, Evasion, Lucky
	}
	
	public static enum Money {
		Kill, FirstBlood, Supply, First, Second, Third
	}
	
	public static class LastDamage implements Entry<UUID, Long> {
	    private UUID key;
	    private Long value;

	    public LastDamage(UUID key, Long value) {
	        this.key = key;
	        this.value = value;
	    }
	    
		@Override
		public UUID getKey() {
			// TODO Auto-generated method stub
			return key;
		}

		@Override
		public Long getValue() {
			// TODO Auto-generated method stub
			return value;
		}

		public UUID setKey(UUID key) {
			UUID old = this.key;
	        this.key = key;
			return old;
		}
		
		@Override
		public Long setValue(Long value) {
			Long old = this.value;
	        this.value = value;
			return old;
		}
	}
	
	public static class Serializer implements JsonSerializer<PlayerData>, JsonDeserializer<PlayerData> {
		private final static Gson gson = new GsonBuilder().create();
		
		@Override
		public PlayerData deserialize(JsonElement jsonElement, Type type, JsonDeserializationContext context) throws JsonParseException {
			PlayerData playerData = gson.fromJson(jsonElement, PlayerData.class);
			playerData.limitTime += System.currentTimeMillis();
			
			return playerData;
		}

		@Override
		public JsonElement serialize(PlayerData playerData, Type type, JsonSerializationContext context) {
			playerData.limitTime -= System.currentTimeMillis();
			
			return gson.toJsonTree(playerData);
		}
	}
}