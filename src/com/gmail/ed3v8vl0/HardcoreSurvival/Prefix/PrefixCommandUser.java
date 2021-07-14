package com.gmail.ed3v8vl0.HardcoreSurvival.Prefix;

import java.util.List;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Prefix.PlayerPrefix.Prefix;

public class PrefixCommandUser implements CommandExecutor {
	private final PrefixManager prefixManager;
	private FileConfiguration config;
	public String prefix;
	
	public PrefixCommandUser(HardcoreSurvival mainClass) {
		this.prefixManager = mainClass.getPrefixManager();
		this.config = mainClass.getConfig();
		this.configInit();
	}

	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.prefix = this.config.getString("prefixMessage.user.prefix");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.config.getString("message.player_not"));
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			for (String s : this.config.getStringList("prefixMessage.user.info"))
				player.sendMessage(this.prefix + s);
		} else {
			PlayerPrefix prefix = prefixManager.getPrefix(player);
			
			if (args[0].equals("설정")) {
				if (args.length > 1) {
					try {
						int id = Integer.parseInt(args[1]) - 1;

						if (prefix.setPrefix(id)) {
							player.sendMessage(this.config.getString("prefixMessage.user.set.sucess"));
						} else {
							player.sendMessage(this.config.getString("prefixMessage.user.set.empty"));
						}
					} catch (NumberFormatException e) {
						player.sendMessage(this.config.getString("prefixMessage.user.set.format"));
					}
				} else {
					player.sendMessage(this.config.getString("prefixMessage.user.set.error"));
				}
			} else if (args[0].equals("목록")) {
				List<Prefix> prefixes = (List<Prefix>) prefix.getPrefixs();
				
				for (int i = 1; i <= prefixes.size(); i++) {
					Prefix p = prefixes.get(i - 1);
					String s = p.getIngame().isEmpty() ? p.getLobby() : p.getIngame();
					String s2 = !p.getIngame().isEmpty() && !p.getLobby().isEmpty() ? "모든 월드" : p.getIngame().isEmpty() ? "로비" : "인게임";
					
					player.sendMessage(this.config.getString("prefixMessage.user.list.message").replace("<ID>", String.valueOf(i) + ChatColor.RESET).replace("<PREFIX>", s + ChatColor.RESET).replace("<WORLD>", s2 + ChatColor.RESET));
				}
			}
		}

		return true;
	}
}
