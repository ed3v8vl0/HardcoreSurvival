package com.gmail.ed3v8vl0.HardcoreSurvival.Command;

import java.time.LocalDateTime;
import java.time.temporal.ChronoField;
import java.util.Iterator;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Log.Death;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;

public class HardcoreCommandUser implements CommandExecutor {
	private FileConfiguration config;
	private final Season season;
	public String prefix;

	public HardcoreCommandUser(HardcoreSurvival mainClass) {
		this.season = mainClass.getSeason();
		this.config = mainClass.getConfig();
		this.configInit();
	}
	
	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.prefix = config.getString("message.user.prefix");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.config.getString("message.player_not"));
			return true;
		}

		Player player = (Player) sender;
		BorderManager borderManager = this.season.getBorderManager();

		if (args.length == 0) {
			for (String s : this.config.getStringList("message.user.info"))
				player.sendMessage(this.prefix + s);
		} else {
			if (args[0].equals("입장")) {
				if (this.season.isSeasonStart()) {
					if (!borderManager.containsPlayer(player)) {
						if (!borderManager.isBan(player)) {
							if (!this.season.afterPVP()) {
								if (borderManager.playerCount() < 100) {
									InventoryHelper.openGui(player);
									player.sendMessage(this.prefix + this.config.getString("message.user.join.sucess"));
								} else {
									player.sendMessage(this.prefix + this.config.getString("message.user.join.max"));
								}
							} else {
								player.sendMessage(this.prefix + this.config.getString("message.user.join.pvp"));
							}
						} else {
							player.sendMessage(this.prefix + this.config.getString("message.user.join.ban"));
						}
					} else {
						player.sendMessage(this.prefix + this.config.getString("message.user.join.already"));
					}
				} else {
					player.sendMessage(this.prefix + this.config.getString("message.user.join.ready"));
				}
			} else if (args[0].equals("시즌확인")) { //Time Error
				LocalDateTime dateTime = this.season.getEndTime();
				
				player.sendMessage(this.prefix + this.config.getString("message.user.check.currentSeason").replace("<SEASON>", String.valueOf(this.season.getSeason()) + ChatColor.RESET));
				
				if (dateTime.getLong(ChronoField.MILLI_OF_SECOND) > 0)
					player.sendMessage(this.prefix + this.config.getString("message.user.check.remainingTime").replace("<DATE>", dateTime.toString() + ChatColor.RESET));
			} else if (args[0].equals("밴목록")) {
				Iterator<UUID> iterator = borderManager.getBanList();

				while (iterator.hasNext()) { //EDIT
					player.sendMessage(Bukkit.getPlayer(iterator.next()).getDisplayName());
				}
			} else if (args[0].equals("기권")) {
				if (borderManager.containsPlayer(player)) {
					borderManager.playerForceLeave(borderManager.getPlayerData(player), null, Death.SURRENDER);
					player.sendMessage(this.prefix + this.config.getString("message.user.surrender.message"));
				} else {
					player.sendMessage(this.prefix + this.config.getString("message.user.surrender.error"));
				}
			}
		}

		return true;
	}
}
