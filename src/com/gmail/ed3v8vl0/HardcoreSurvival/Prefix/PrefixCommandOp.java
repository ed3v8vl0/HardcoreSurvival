package com.gmail.ed3v8vl0.HardcoreSurvival.Prefix;

import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;

public class PrefixCommandOp implements CommandExecutor {
	private final PrefixManager prefixManager;
	private FileConfiguration config;
	public String prefix;
	
	public PrefixCommandOp(HardcoreSurvival mainClass) {
		this.prefixManager = mainClass.getPrefixManager();
		this.config = mainClass.getConfig();
		this.configInit();
	}

	public void configInit() {
		this.config = HardcoreSurvival.getInstance().getConfig();
		this.prefix = this.config.getString("prefixMessage.op.prefix");
	}
	
	@Override
	public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
		if (!(sender instanceof Player)) {
			sender.sendMessage(this.config.getString("prefixMessage.player_not"));
			return true;
		}

		Player player = (Player) sender;

		if (args.length == 0) {
			for (String s : this.config.getStringList("prefixMessage.op.info"))
				player.sendMessage(this.prefix + s);
		} else {
			if (args[0].equals("생성")) {
				if (args.length > 2) {
					try {
					String prefix = args[1].replace("&", "§");
					int value = Integer.parseInt(args[2]);
					
						try {
							player.getInventory().addItem(prefixManager.createBook(prefix, --value));
							player.sendMessage(this.prefix + this.config.getString("prefixMessage.op.create.sucess"));
						} catch (IllegalArgumentException e) {
							player.sendMessage(this.prefix + this.config.getString("prefixMessage.op.create.failed"));
						}
					} catch (NumberFormatException e) {
						player.sendMessage(this.prefix + this.config.getString("prefixMessage.op.create.format"));
					}
				} else {
					player.sendMessage(this.prefix + this.config.getString("prefixMessage.op.create.error"));
				}
			}
		}

		return true;
	}
}
