package com.gmail.ed3v8vl0.HardcoreSurvival.Prefix;

import java.io.*;
import java.lang.reflect.Type;
import java.util.*;

import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.player.AsyncPlayerChatEvent;
import org.bukkit.event.player.PlayerInteractEvent;
import org.bukkit.inventory.EquipmentSlot;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Prefix.PlayerPrefix.Prefix;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;
import com.google.gson.*;
import com.google.gson.reflect.TypeToken;
import com.google.gson.stream.JsonWriter;

public class PrefixManager implements Listener {
	//Serializer
	private HashMap<UUID, PlayerPrefix> playerPrefix = new HashMap<UUID, PlayerPrefix>();
	
	private final BorderManager borderManager;
	private String baseIngame;
	private String baseLobby;
	private int distance;
	private int itemCode;
	private String displayName;
	private List<String> lore;
	
	private String suceess;
	private String failed;
	
	
	public PrefixManager(HardcoreSurvival mainClass) {
		this.borderManager = mainClass.getSeason().getBorderManager();
		this.deserializer();
		this.configInit();
	}
	
	public void configInit() {
		FileConfiguration config = HardcoreSurvival.getInstance().getConfig();
		
		this.baseIngame = config.getString("prefix.chat.Ingame");
		this.baseLobby = config.getString("prefix.chat.Lobby");
		this.distance = config.getInt("prefix.chat.distance");
		this.itemCode = config.getInt("prefix.itemCode");
		this.displayName = config.getString("prefix.displayName");
		this.lore = config.getStringList("prefix.lore");
		this.suceess = config.getString("prefix.sucess");
		this.failed = config.getString("prefix.failed");
	}
	
	public PlayerPrefix getPrefix(Player player) {
		PlayerPrefix prefix = this.playerPrefix.get(player.getUniqueId());
		
		if (prefix == null) {
			prefix = new PlayerPrefix(this.baseIngame, this.baseLobby);
			this.playerPrefix.put(player.getUniqueId(), prefix);
		}
		
		return prefix;
	}
	
	public ItemStack createBook(String prefix, int value) {
		@SuppressWarnings("deprecation")
		ItemStack itemStack = new ItemStack(itemCode);
		ItemMeta itemMeta = itemStack.getItemMeta();
		List<String> lore = new ArrayList<String>();
		String s2 = value == 0 ? "인게임" : value == 1 ? "로비" : "모든 월드";
		
		
		for (String s : this.lore) {
			lore.add(s.replace("<PREFIX>", prefix).replace("<WORLD>", s2 + ChatColor.RESET));
		}
		
		itemMeta.setDisplayName(this.displayName.replace("<PREFIX>", prefix + ChatColor.RESET));
		itemMeta.setLore(lore);
		itemStack.setItemMeta(itemMeta);
		
		return itemStack;
	}
	
	@SuppressWarnings("deprecation")
	@EventHandler
	public void onPlayerInteractEvent(PlayerInteractEvent event) {
		if ((event.getAction() == Action.RIGHT_CLICK_AIR || event.getAction() == Action.RIGHT_CLICK_BLOCK) && event.getHand() == EquipmentSlot.HAND) {
			Player player = event.getPlayer();
			ItemStack itemStack = player.getInventory().getItemInMainHand();
			
			if (itemStack != null && itemStack.getTypeId() == this.itemCode && itemStack.hasItemMeta()) {
			ItemMeta itemMeta = itemStack.getItemMeta();
			
				if (itemMeta.hasDisplayName() && itemMeta.hasLore()) {
					List<String> lore = itemMeta.getLore();
					String prefix = lore.get(0).split(":")[1].trim();
					String temp =  lore.get(1).split(":")[1].trim();
					int value = -1;
					
					if (temp.indexOf("인게임") > -1) {
						value = 0;
					} else if (temp.indexOf("로비") > -1) {
						value = 1;
					} else if (temp.indexOf("모든 월드") > -1) {
						value = 2;
					}
					
					if (this.getPrefix(player).addPrefix(new Prefix(prefix, value))) {
						itemStack.setAmount(0);
						player.sendMessage(this.suceess);
					} else {
						player.sendMessage(this.failed);
					}
				}
			}
		}
	}
	
	@EventHandler(ignoreCancelled = true, priority = EventPriority.HIGHEST)
	public void onAsyncPlayerChatEvent(AsyncPlayerChatEvent event) {
		Player player = event.getPlayer();
		Prefix prefix = this.getPrefix(player).getPrefix();
		
		if (this.borderManager.containsPlayer(player)) {
			String message = String.format(event.getFormat(), prefix.getIngame() + player.getDisplayName(), event.getMessage());
			
			event.setCancelled(true);
			
			World world = player.getWorld();

			for (Player target : world.getEntitiesByClass(Player.class)) {
				if (target.equals(player))
					continue;

				Location loc = target.getLocation().subtract(player.getLocation());

				if ((Math.abs(loc.getX()) <= this.distance && Math.abs(loc.getY()) <= this.distance && Math.abs(loc.getZ()) <= this.distance) || target.isOp()) {
					target.sendMessage(message);
				}
			}

			player.sendMessage(message);
			player.getServer().getConsoleSender().sendMessage(message);
		} else {
			event.setFormat(prefix.getLobby() + event.getFormat());
			
			for (Player target : new ArrayList<Player>(event.getRecipients())) {
				if (this.borderManager.containsPlayer(target) && !target.isOp()) {
					event.getRecipients().remove(target);
				}
			}
		}
	}
	
	private static Gson gson = new GsonBuilder().create();
	
	public void serializer() {
		try {
			File file = new File("./plugins/Hardcore/Data");
			
			if (!file.exists()) {
				file.mkdirs();
			}
			
			JsonWriter jsonWriter = new JsonWriter(new FileWriter(new File(file, "PlayerPrefix.json")));
			
			jsonWriter.setIndent(" ");
			gson.toJson(this.playerPrefix, this.playerPrefix.getClass(), jsonWriter);
			jsonWriter.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void deserializer() {
		File file = new File("./plugins/Hardcore/Data");
		
		if (file.exists()) {
			JsonParser jsonParser = new JsonParser();
			Type type = new TypeToken<HashMap<UUID, PlayerPrefix>>() {
			}.getType();

			if (file.exists()) {
				try {
					FileReader fileReader = new FileReader(new File(file, "PlayerPrefix.json"));

					this.playerPrefix = gson.fromJson(jsonParser.parse(fileReader), type);
					fileReader.close();
				} catch (JsonSyntaxException | JsonIOException | IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		}
	}
}
