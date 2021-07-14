package com.gmail.ed3v8vl0.HardcoreSurvival.Command;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.World;
import org.bukkit.WorldCreator;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;

public class HardcoreCommandOp implements CommandExecutor {
	private final HardcoreSurvival mainClass;
	private FileConfiguration config;
	private final Season season;
	public String prefix;

	public HardcoreCommandOp(HardcoreSurvival mainClass) {
		this.mainClass = mainClass;
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

		if (args.length == 0) {
			for (String s : this.config.getStringList("message.op.info"))
				player.sendMessage(this.prefix + s);
		} else {
			if (args[0].equals("초기화")) {
				if (args.length > 4) {
					try {
						long time = Integer.parseInt(args[1]) * 86400; // Days

						time += Integer.parseInt(args[2]) * 3600; // Hours
						time += Integer.parseInt(args[3]) * 60; // Minutes
						time += Integer.parseInt(args[4]) * 1; // Seconds

						if (this.season.startSeason(time)) {
							player.sendMessage(this.prefix + this.config.getString("message.op.init.message"));
						} else {
							player.sendMessage(this.prefix + this.config.getString("message.op.init.minimum"));
						}
					} catch (NumberFormatException e) {
						player.sendMessage(this.prefix + this.config.getString("message.op.init.format"));
					}
				} else {
					player.sendMessage(this.prefix + this.config.getString("message.op.init.error"));
				}
			} else if (args[0].equals("지정")) {
				if (args.length == 2) {
					World world = Bukkit.getWorld(args[1]);

					if (world == null)
						world = Bukkit.createWorld(new WorldCreator(args[1]));
					
					world.setSpawnFlags(true, true);
					this.season.getBorderManager().allowSpawn = true;
					this.config.set("worldborder.worldSpawn", Boolean.valueOf(args[1]));
					this.config.set("worldborder.world", args[1]);
					this.season.getBorderManager().setWorld(world);
					this.mainClass.saveConfig();
					player.sendMessage(this.prefix + this.config.getString("message.op.select.message"));
				} else {
					player.sendMessage(this.prefix + this.config.getString("message.op.select.error"));
				}
			} else if (args[0].equals("리셋")) {
				this.season.Reset();
				player.sendMessage(this.prefix + this.config.getString("message.op.reset.message"));
			} else if (args[0].equals("저장")) {
				this.mainClass.saveConfig();
				player.sendMessage(this.prefix + this.config.getString("message.op.save.message"));
			} else if (args[0].equals("리로드")) {
				this.mainClass.configReload();
				player.sendMessage(this.prefix + this.config.getString("message.op.reload.message"));
			} else if (args[0].equals("스폰")) {
				if (args.length > 1) {
					boolean allowSpawn = Boolean.valueOf(args[1]);
					
					this.config.set("worldborder.worldSpawn", Boolean.valueOf(args[1]));
					this.season.getBorderManager().allowSpawn = allowSpawn;
					this.mainClass.saveConfig();
					player.sendMessage(this.prefix + this.config.getString("message.op.spawn.message").replace("<BOOL>",
							String.valueOf(allowSpawn) + ChatColor.RESET));
				} else {
					player.sendMessage(this.prefix + this.config.getString("message.op.spawn.error"));
				}
			} else if (args[0].equals("언밴")) {
				if (args.length > 1) {
					Player target = Bukkit.getPlayer(args[1]);
					
					if (target != null) {
						this.season.getBorderManager().unban(target);
						player.sendMessage("[DEBUG] 벤 해제를 완료했습니다.");
					} else {
						player.sendMessage("[DEBUG] 존재하지 않는 플레이어입니다.");
					}
				} else {
					player.sendMessage("[DEBUG] 인수를 입력해주세요.");
				}
			} else if (args[0].equals("청크로드")) {
				player.sendMessage("[DEBUG] 청크로드중입니다. 서버가 잠시 멈출 수 있습니다.");
				this.season.getBorderManager().chunkLoad();
				player.sendMessage("[DEBUG] 청크로드 완료");
			} else if (args[0].equals("청크언로드")) {
				if (args.length > 1) {
					player.sendMessage("[DEBUG] 청크 언로드 값을 변경하였습니다. " + Boolean.valueOf(args[1]));
					this.season.getBorderManager().chunkUnload = Boolean.valueOf(args[1]);
				}
			} else if (args[0].equals("Disable")) {
				Bukkit.getPluginManager().disablePlugin(Bukkit.getPluginManager().getPlugin("Hardcore"));
				player.sendMessage("Disable");
			}
		}

		return true;
	}
}
