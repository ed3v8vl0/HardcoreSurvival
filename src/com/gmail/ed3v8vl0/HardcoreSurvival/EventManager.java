package com.gmail.ed3v8vl0.HardcoreSurvival;

import java.util.HashMap;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.World.Environment;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.entity.Arrow;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Player;
import org.bukkit.entity.Projectile;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.entity.*;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.bukkit.event.entity.EntityDamageEvent.DamageCause;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType.SlotType;
import org.bukkit.event.player.*;
import org.bukkit.event.world.ChunkUnloadEvent;
import org.bukkit.inventory.ItemStack;

import com.gmail.ed3v8vl0.HardcoreSurvival.Command.InventoryHelper;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.SurvivalCalc;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Ability;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.LastDamage;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Money;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.Death;
import com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia.InventoryManager;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;

public class EventManager implements Listener {
	private final Season season;
	private final BorderManager borderManager;
	
	private HashMap<UUID, Long> joinStamp = new HashMap<UUID, Long>();
	public long leaveTime;
	public long lastTime;

	public EventManager(HardcoreSurvival mainClass) {
		this.season = mainClass.getSeason();
		this.borderManager = season.getBorderManager();
		this.configInit();
	}

	public void configInit() {
		FileConfiguration config = HardcoreSurvival.getInstance().getConfig();
		
		this.leaveTime = config.getLong("worldborder.leave");
		this.lastTime = config.getLong("worldborder.last");
	}
	
	@EventHandler
	public void onEntityDamageByEntityEvent(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();
			PlayerData playerData = this.borderManager.getPlayerData((Player) event.getEntity());
			Entity damager = event.getEntity() instanceof Projectile ? (Entity) ((Projectile) event.getEntity()).getShooter() : event.getDamager();
			
			if (this.borderManager.containsPlayer(player)) {
				if (this.season.afterPVP()) {
					playerData.lastDamage = new LastDamage(damager.getUniqueId(), System.currentTimeMillis());
				} else {
					if (event.getDamager() instanceof Player) {
						event.setCancelled(true);
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerDeathEvent(PlayerDeathEvent event) {
		Player player = event.getEntity();
		PlayerData playerData = this.borderManager.getPlayerData(player);

		if (this.borderManager.containsPlayer(player)) {
			LastDamage lastDamage = playerData.lastDamage;
			Death death = Death.valueOf(player.getLastDamageCause().getCause().name());

			if (lastDamage != null) {
				Entity entity = Bukkit.getEntity(lastDamage.getKey());
				
				if (entity instanceof Player && lastDamage.getValue() - System.currentTimeMillis() <= 5000) { // 5초 이내에 사망시
					if (this.borderManager.firstKill()) {
						this.borderManager.getPlayerData(lastDamage.getKey()).addMoney(Money.FirstBlood);
					} else {
						this.borderManager.getPlayerData(lastDamage.getKey()).addMoney(Money.Kill);
					}
					
					this.borderManager.playerLeave(player, entity, death);
				}
			} else {
				this.borderManager.playerLeave(player, null, death);
			}
		}
	}

	@EventHandler
	public void onPlayerQuitEvent(PlayerQuitEvent event) {
		Player player = event.getPlayer();
		PlayerData playerData = this.borderManager.getPlayerData(player);

		if (this.borderManager.containsPlayer(player)) {
			if (this.season.afterPVP()) {
				if (playerData.lastDamage != null) {
					long time = System.currentTimeMillis() - playerData.lastDamage.getValue();

					if (time < this.leaveTime) {
						PlayerData targetData = this.borderManager.getPlayerData(playerData.lastDamage.getKey()); //.addMoney(Money.Kill);
						
						if (targetData != null) {
							targetData.addMoney(Money.Kill);
						}
						
						this.borderManager.playerForceLeave(playerData, null, Death.SURRENDER);
					}
				} else {
					if (this.joinStamp.containsKey(player.getUniqueId())) {
						long onlineTime = System.currentTimeMillis() - this.joinStamp.get(player.getUniqueId());

						playerData.onPlayerQuit(this.borderManager, onlineTime);
						this.joinStamp.remove(player.getUniqueId());
					}
				}
			}
		}
	}

	@EventHandler
	public void onPlayerJoinEvent(PlayerJoinEvent event) {
		Player player = event.getPlayer();

		if (this.borderManager.containsPlayer(player)) {
			PlayerData playerData = borderManager.getPlayerData(player);
			long lastTime = System.currentTimeMillis() - player.getLastPlayed();

			if (!this.joinStamp.containsKey(player.getUniqueId())) {
				this.joinStamp.put(player.getUniqueId(), System.currentTimeMillis());
			}

			if (lastTime >= this.lastTime || this.season.blockTime()) {
				this.season.getBorderManager().playerLeave(player, null, Death.SURRENDER);
			} else {
				if (true) {
					playerData.onPlayerJoin(this.borderManager, lastTime);
				}
			}
		}
	}

	@EventHandler
	public void onPlayerLoginEvent(PlayerLoginEvent event) {
		for (Player target : Bukkit.getOnlinePlayers()) {
			if (event.getAddress().equals(target.getAddress().getAddress())) {
				// event.disallow(Result.KICK_OTHER, "Duplicate IP Kick");
			}
		}
	}

	@EventHandler
	public void onPlayerTeleportEvent(PlayerTeleportEvent event) {
		Player player = event.getPlayer();
		World world = event.getFrom().getWorld();
		World target = event.getTo().getWorld();
		
		if (!world.equals(target)) { // world != target
			if (this.borderManager.getWorld().equals(world)) { // Hardcore -> World
				if (target.getEnvironment() == Environment.THE_END || target.getEnvironment() == Environment.NETHER) {
					event.setCancelled(true);
				} else if (target.getEnvironment() == Environment.NORMAL) {
					InventoryManager.load((CraftPlayer) player, target.getName());
				}
			} else {
				if (this.borderManager.getWorld().equals(target)) {
					InventoryManager.save((CraftPlayer) player, world.getName());
					InventoryManager.load((CraftPlayer) player, target.getName());
				}
			}
		}
	}

	@EventHandler
	public void onPlayerCommandPreprocessEvent(PlayerCommandPreprocessEvent event) {
		Player player = event.getPlayer();

		if (this.borderManager.containsPlayer(player))
			if (!event.getPlayer().isOp())
				event.setCancelled(true);
	}

	@EventHandler
	public void onCreatureSpawnEvent(CreatureSpawnEvent event) {
		World world = event.getEntity().getWorld();

		if (this.borderManager.getWorld().equals(world)) {
			if (!this.borderManager.allowSpawn) {
				if (event.getSpawnReason() != SpawnReason.CUSTOM) {
					event.setCancelled(true);
				}
			}
		}
	}

	@EventHandler
	public void onInventoryClickEvent(InventoryClickEvent event) {
		if (InventoryHelper.compareInventory(event.getView().getTopInventory())) {
			event.setCancelled(true);

			if (event.getSlot() >= 0 && event.getSlot() <= 8 && event.getSlotType() == SlotType.CONTAINER) {
				Player player = (Player) event.getWhoClicked();
				int slot = event.getSlot();

				if (InventoryHelper.attackClick(slot)) {
					this.borderManager.playerJoin(new PlayerData(player, Ability.Attack));
				} else if (InventoryHelper.resistanceClick(slot)) {
					this.borderManager.playerJoin(new PlayerData(player, Ability.Resistance));
				} else if (InventoryHelper.evasionClick(slot)) {
					this.borderManager.playerJoin(new PlayerData(player, Ability.Evasion));
				} else if (InventoryHelper.luckyClick(slot)) {
					this.borderManager.playerJoin(new PlayerData(player, Ability.Lucky));
				}
			}
		}
	}

	@EventHandler
	public void onPlayerMoveEvent(PlayerMoveEvent event) {
		Player player = event.getPlayer();

		if (this.borderManager.containsPlayer(player)) {
			PlayerData playerData = this.borderManager.getPlayerData(player);

			if (player.isSprinting()) {

				if (playerData.sprintStamp == 0) {
					playerData.sprintStamp = System.currentTimeMillis();
				}
			} else {
				if (playerData.sprintStamp > 0) {
					playerData.sprintStamp = 0;
				}
			}
		}
	}

	@EventHandler
	public void onPlayerExpChangeEvent(PlayerExpChangeEvent event) {
		Player player = (Player) event.getPlayer();

		if (this.borderManager.containsPlayer(player)) {
			event.setAmount((int) Math.round(event.getAmount() * SurvivalCalc.xpReceive));
		}
	}

	@EventHandler
	public void onEntityDamageByEntityEvent2(EntityDamageByEntityEvent event) {
		if (event.getEntity() instanceof Player) {
			Player player = (Player) event.getEntity();

			if (this.borderManager.containsPlayer(player)) {
				player.sendMessage("연산 적용 전 데미지: " + event.getDamage());
				// Surival Calc
				event.setDamage(event.getDamage() * SurvivalCalc.damageReceive);

				// Ability Calc
				PlayerData playerData = this.borderManager.getPlayerData(player);
				Ability ability = playerData.getAbility();

				if (ability == Ability.Resistance) { // 플레이어및 몹에게서 받는 피해량 10%감소하고 (최대체력 5증가 - PlayerJoin측 연산)
					if (event.getCause() == DamageCause.PROJECTILE) {
						event.setDamage(event.getDamage() - (event.getDamage() * SurvivalCalc.Resistance.ProjectileDamageReduction));
					} else {
						event.setDamage(event.getDamage() - (event.getDamage() * SurvivalCalc.Resistance.DamageReduction));
					}
				} else if (ability == Ability.Evasion) { // 이동속도가 15%증가하고 달리고다닐시 0.5초안에 적에게서 피격당할 회피율이 12% 증가
					if (playerData.sprintStamp != 0) {
						long time = System.currentTimeMillis() - playerData.sprintStamp;

						if (time <= SurvivalCalc.Evasion.SprintEvasionTime) { // 0.5s
							if (Math.random() <= SurvivalCalc.Evasion.SprintEvasionPercent) {
								event.setCancelled(true);
								// event.setDamage(0);
							}
						}
					}
				} else if (ability == Ability.Lucky) { // 타격시 8%확률로 100%의 추가피해를 입히고 익히지 않은 음식을먹으면 50%확률로 허기가생기지않음
					ItemStack[] armors = player.getInventory().getArmorContents();
					int diamond = 0;
					int gold = 0;
					int iron = 0;
					int leather = 0;

					for (ItemStack itemStack : armors) {
						if (itemStack == null)
							continue;

						if (itemStack.getType() == Material.DIAMOND_HELMET || itemStack.getType() == Material.DIAMOND_CHESTPLATE || itemStack.getType() == Material.DIAMOND_LEGGINGS || itemStack.getType() == Material.DIAMOND_BOOTS) {
							diamond++;
						} else if (itemStack.getType() == Material.GOLD_HELMET || itemStack.getType() == Material.GOLD_CHESTPLATE || itemStack.getType() == Material.GOLD_LEGGINGS || itemStack.getType() == Material.GOLD_BOOTS) {
							gold++;
						} else if (itemStack.getType() == Material.IRON_HELMET || itemStack.getType() == Material.IRON_CHESTPLATE || itemStack.getType() == Material.IRON_LEGGINGS || itemStack.getType() == Material.IRON_BOOTS) {
							iron++;
						} else if (itemStack.getType() == Material.LEATHER_HELMET || itemStack.getType() == Material.LEATHER_CHESTPLATE || itemStack.getType() == Material.LEATHER_LEGGINGS || itemStack.getType() == Material.LEATHER_BOOTS) {
							leather++;
						}
					}

					double armorDamageReduction = (Math.max(Math.max(Math.max(diamond, gold), iron), leather) - 1) * SurvivalCalc.Lucky.ArmorDamageReduction;

					if (armorDamageReduction > 0) {
						event.setDamage(event.getDamage() - (event.getDamage() * armorDamageReduction));
					}
				}
				player.sendMessage("연산 적용 후 데미지: " + event.getDamage());
			}

		}

		if (event.getDamager() instanceof Player || (event.getDamager() instanceof Arrow && ((Arrow) event.getDamager()).getShooter() instanceof Player)) {
			Player damager = event.getDamager() instanceof Player ? (Player) event.getDamager() : (Player) ((Arrow) event.getDamager()).getShooter();
			Entity entity = event.getEntity();

			if (this.borderManager.containsPlayer(damager)) {
				damager.sendMessage("연산 적용 전 데미지: " + event.getDamage());
				// Survial Calc
				event.setDamage(event.getDamage() * SurvivalCalc.damageAttack);

				// Ability Calc
				PlayerData playerData = this.borderManager.getPlayerData(damager);
				Ability ability = playerData.getAbility();

				if (ability == Ability.Attack) { // 적에게 주는 피해량 24%증가 (단 타격대상이 몹일경우 12%)
					if (event.getCause() == DamageCause.PROJECTILE) {
						if (event.getEntity() instanceof Player) {
							event.setDamage(event.getDamage() + (event.getDamage() * (SurvivalCalc.Attack.AddPlayerDamage + SurvivalCalc.Attack.AddProjectileDamage)));
						} else {
							event.setDamage(event.getDamage() + (event.getDamage() * (SurvivalCalc.Attack.AddCreatureDamage + SurvivalCalc.Attack.AddProjectileDamage)));
						}
					} else {
						if (event.getEntity() instanceof Player) {
							event.setDamage(event.getDamage() + (event.getDamage() * SurvivalCalc.Attack.AddPlayerDamage));
						} else {
							event.setDamage(event.getDamage() + (event.getDamage() * SurvivalCalc.Attack.AddCreatureDamage));
						}
					}
				} else if (ability == Ability.Lucky) {
					if (playerData.attackCount.containsKey(entity.getUniqueId())) {
						int attackCount = playerData.attackCount.get(entity.getUniqueId()) + 1;

						if (attackCount >= SurvivalCalc.Lucky.IFAttackCount) {
							if (Math.random() <= SurvivalCalc.Lucky.AddDamagePercent) {
								event.setDamage(event.getDamage() + (event.getDamage() * SurvivalCalc.Lucky.AddDamageValue));
							}

							playerData.attackCount.remove(entity.getUniqueId());
						} else {
							playerData.attackCount.put(entity.getUniqueId(), attackCount);
						}
					} else {
						playerData.attackCount.put(entity.getUniqueId(), 1);
					}
				}
				
				damager.sendMessage("연산 적용 후 데미지: " + event.getDamage());
			}
		}
	}
	
	@EventHandler
	public void onChunkUnloadEvent(ChunkUnloadEvent event) {
		if (this.borderManager.getWorld().equals(event.getWorld())) {
			if (this.borderManager.chunkUnload)
				event.setCancelled(true);
		}
	}
}
