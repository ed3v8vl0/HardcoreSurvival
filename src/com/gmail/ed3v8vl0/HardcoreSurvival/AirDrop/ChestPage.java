package com.gmail.ed3v8vl0.HardcoreSurvival.AirDrop;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.block.Chest;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.inventory.InventoryOpenEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.PlayerData.Money;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;

public class ChestPage implements Listener, CommandExecutor, Runnable {
	private HashMap<Integer, HashMap<Integer, ItemStack>> pages = new HashMap<Integer, HashMap<Integer, ItemStack>>();
	private FileConfiguration config;
	private final Season season;
	private final SupplyManager supplyManager;
	public String prefix;
	public String inventoryName;
	public String settingInventoryName;
	
	private long supplyTime;
	private int amount;
	private boolean continuous;
	
	private long timeStamp;
	private long count;
	
	public ChestPage(HardcoreSurvival mainClass) {
		this.config = mainClass.getConfig();
		this.season = mainClass.getSeason();
		this.supplyManager = new SupplyManager(mainClass, this);
		Bukkit.getScheduler().runTaskTimer(mainClass, this, 0L, 1L);
		this.deserializer();
		this.configInit();
	}
	
	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.prefix = this.config.getString("airdrop.message.prefix");
		this.inventoryName = this.config.getString("airdrop.inventoryName");
		this.settingInventoryName = this.config.getString("airdrop.settingInventoryName");
	}
	
	public HashMap<Integer, ItemStack> getPages() {
		if (this.pages.size() > 0) {
			return new ArrayList<HashMap<Integer, ItemStack>>(this.pages.values()).get((int) (Math.random() * this.pages.size()));
		} else {
			return null;
		}
	}
	
	public SupplyManager getSupplyManager() {
		return this.supplyManager;
	}
	
	
	@Override
	public void run() {
		if (this.season.afterPVP()) {
			if (this.amount > 0) {
				long currentTime = System.currentTimeMillis();

				if (currentTime - this.timeStamp >= 0) {
					BorderManager borderManager = this.season.getBorderManager();
					
					if (this.continuous) {
						this.supplyManager.supply(borderManager.getLocation(10));
						
						if (++count >= this.amount) {
							this.timeStamp = currentTime + this.supplyTime;
							this.count = 0;
						} else {
							this.timeStamp = currentTime + ((this.supplyManager.fadeIn + this.supplyManager.stay + this.supplyManager.fadeOut) * 50);
						}
					} else {
						this.supplyManager.supply(borderManager.getLocation(10));
						this.timeStamp = currentTime + ((this.supplyManager.fadeIn + this.supplyManager.stay + this.supplyManager.fadeOut) * 50);
						
						if (--amount == 0) {
							this.supplyTime = 0;
						}
					}
				}
			}
		}
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.config.getString("airdrop.message.player_not"));
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			for (String s : this.config.getStringList("airdrop.message.info"))
				player.sendMessage(this.prefix + s);
		} else {
			if (args[0].equals("페이지")) {
				if (args.length > 1) {
					try {
						player.openInventory(Bukkit.createInventory(null, InventoryType.CHEST, this.settingInventoryName + " #" + Integer.parseInt(args[1])));
						player.sendMessage(this.prefix + this.config.getString("airdrop.message.page.sucess"));
					} catch (NumberFormatException e)  {
						player.sendMessage(this.prefix + this.config.getString("airdrop.message.page.format"));
					}
				} else {
					player.sendMessage(this.prefix + this.config.getString("airdrop.message.page.error"));
				}
			} else if (args[0].equals("자동투하")) {
				if (args.length > 3) {
					try {
						this.supplyTime = Integer.parseInt(args[1]) * 1000;
						this.amount = Integer.parseInt(args[2]);
						this.continuous = Boolean.parseBoolean(args[3]);
						this.timeStamp = System.currentTimeMillis() + this.supplyTime;
						player.sendMessage(this.prefix + this.config.getString("airdrop.message.auto.sucess"));
					} catch (NumberFormatException e) {
						player.sendMessage(this.prefix + this.config.getString("airdrop.message.auto.format"));
					}
				} else {
					player.sendMessage(this.prefix + this.config.getString("airdrop.message.auto.error"));
				}
			} else if (args[0].equals("강제투하")) {
				this.supplyManager.supply(player.getLocation());
				player.sendMessage(this.prefix + this.config.getString("airdrop.message.force.message"));
			} else if (args[0].equals("랜덤투하")) {
				BorderManager borderManager = this.season.getBorderManager();

				this.supplyManager.supply(borderManager.getLocation(10));
				player.sendMessage(this.prefix + this.config.getString("airdrop.message.force.message"));
			}
		}
		return false;
	}
	
	@EventHandler
	public void onInventoryOpenEvent(InventoryOpenEvent event) {
		Inventory inventory = event.getView().getTopInventory();
		
		if (inventory.getName().equals(this.inventoryName + "§r")) {
			Chest chest = ((Chest) event.getPlayer().getWorld().getBlockAt(event.getInventory().getLocation()).getState());
			PlayerData playerData = this.season.getBorderManager().getPlayerData(event.getPlayer().getUniqueId());
			
			if (playerData != null) {
				playerData.addMoney(Money.Supply);
				System.out.println(playerData.getOfflinePlayer().getName() + " Supply Money");
			} else {
				System.out.println(event.getPlayer().getName() + " No Supply Money");
			}
			
			chest.setCustomName(this.inventoryName);
			chest.update();
			Bukkit.broadcastMessage(this.config.getString("airdrop.brodcastMessage").replace("<PLAYER>", ((Player) event.getPlayer()).getDisplayName()));
		} else if (inventory.getName().indexOf(this.settingInventoryName) > -1) {
			int page = Integer.parseInt(inventory.getName().split("#")[1]); //NumberFormatException
			
			if (this.pages.containsKey(page)) {
				for (Entry<Integer, ItemStack> entry : this.pages.get(page).entrySet()) {
					inventory.setItem(entry.getKey(), entry.getValue());
				}
			}
		}
	}
	
	@EventHandler
	public void onInventoryCloseEvent(InventoryCloseEvent event) {
		Inventory inventory = event.getView().getTopInventory();
		
		if (inventory.getName().indexOf(this.settingInventoryName) > -1) {
			HashMap<Integer, ItemStack> items = new HashMap<Integer, ItemStack>();
			ItemStack[] itemStacks = inventory.getStorageContents();
			int page = Integer.parseInt(inventory.getName().split("#")[1]); //NumberFormatException
			
			for (int i = 0; i < itemStacks.length; i++) {
				if (itemStacks[i] != null) {
					items.put(i, itemStacks[i]);
				}
			}
			this.pages.put(page, items);
		}
	}
	
	public void serializer() {
		FileConfiguration config = new YamlConfiguration();
		File file = new File("./plugins/Hardcore/airdrop");
		
		if (!file.exists())
			file.mkdirs();
		
		for (Entry<Integer, HashMap<Integer, ItemStack>> entry : this.pages.entrySet()) {
			for (Entry<Integer, ItemStack> entry2 : entry.getValue().entrySet()) {
				config.set("page_" + entry.getKey() + ".itemStack_" + entry2.getKey(), entry2.getValue());
			}
		}
		
		try {
			config.save(new File(file, "Items.yml"));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deserializer() {
		File file = new File("./plugins/Hardcore/airdrop");
		
		if (file.exists()) {
			HashMap<Integer, HashMap<Integer, ItemStack>> pages = new HashMap<Integer, HashMap<Integer, ItemStack>>();

			if (file.exists()) {
				FileConfiguration config = YamlConfiguration.loadConfiguration(new File(file, "Items.yml"));

				for (String page : config.getKeys(false)) {
					HashMap<Integer, ItemStack> chest = new HashMap<Integer, ItemStack>();

					ConfigurationSection selection = config.getConfigurationSection(page);
					for (String slot : selection.getKeys(false)) {
						chest.put(Integer.parseInt(slot.split("_")[1]), selection.getItemStack(slot));
					}

					pages.put(Integer.parseInt(page.split("_")[1]), chest);
				}
			}

			this.pages = pages;
		}
	}
}
