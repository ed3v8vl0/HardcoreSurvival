package com.gmail.ed3v8vl0.HardcoreSurvival.Vanlia;

import java.io.*;

import javax.annotation.Nullable;

import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.attribute.Attribute;
import org.bukkit.craftbukkit.v1_12_R1.CraftServer;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.entity.CraftPlayer;
import org.bukkit.plugin.Plugin;

import net.minecraft.server.v1_12_R1.*;

public class InventoryManager {
	public static File playerDir;

	static {
		InventoryManager.playerDir = new File(((CraftServer) Bukkit.getServer()).getHandle().getServer().S(), "playerdata");
		InventoryManager.playerDir.mkdirs();
	}

	public static void save(CraftPlayer player, String world) {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("Hardcore");

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				EntityPlayer entityPlayer = player.getHandle();
				NBTTagCompound nbttagcompound = entityPlayer.save(new NBTTagCompound());

				try {
					File file = new File(InventoryManager.playerDir, entityPlayer.bn() + "_" + world + ".dat.tmp");
					File file1 = new File(InventoryManager.playerDir, entityPlayer.bn() + "_" + world + ".dat");

					NBTCompressedStreamTools.a(nbttagcompound, (OutputStream) (new FileOutputStream(file)));
					if (file1.exists()) {
						file1.delete();
					}

					file.renameTo(file1);
				} catch (Exception exception) {
					System.out.println("Failed to save player data for " + entityPlayer.getName());
				}
			}
		});
	}

	@Nullable
	public static void load(CraftPlayer player, String world) {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("Hardcore");

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {
				final WorldServer worldServer = ((CraftWorld) Bukkit.getWorld(world)).getHandle();
				final EntityPlayer entityPlayer = player.getHandle();

				entityPlayer.copyFrom(new EntityPlayer(entityPlayer.server, worldServer, entityPlayer.getProfile(), new PlayerInteractManager(worldServer)), true);
				entityPlayer.reset();

				try {
					File file = new File(InventoryManager.playerDir, entityPlayer.bn() + "_" + world + ".dat");

					if (file.exists() && file.isFile()) {
						final NBTTagCompound nbttagcompound = NBTCompressedStreamTools.a((InputStream) (new FileInputStream(file)));

						file.delete();
						entityPlayer.server.getPlayerList().playerFileData.save(entityPlayer);

						Bukkit.getScheduler().runTask(plugin, new Runnable() {
							@Override
							public void run() {
								player.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(20.0D);
								player.setWalkSpeed(0.2F);
								entityPlayer.a(nbttagcompound);
							}
						});
					}
				} catch (Exception exception) {
					System.out.println("Failed to load player data for " + entityPlayer.getName());
				}
			}
		});
	}
	
	public static void nbtRestore(OfflinePlayer offlinePlayer) {
		final Plugin plugin = Bukkit.getPluginManager().getPlugin("Hardcore");

		Bukkit.getScheduler().runTaskAsynchronously(plugin, new Runnable() {
			@Override
			public void run() {

				String target = ((CraftServer) Bukkit.getServer()).getHandle().getServer().S();

				try {
					File file = new File(InventoryManager.playerDir, offlinePlayer.getUniqueId() + ".dat");
					File file1 = new File(InventoryManager.playerDir, offlinePlayer.getUniqueId() + ".dat.tmp");
					File file2 = new File(InventoryManager.playerDir, offlinePlayer.getUniqueId() + "_" + target + ".dat");

					if (file.exists() && file2.exists()) {
						file.renameTo(file1);
						file2.renameTo(file);
						file1.delete();
					}
				} catch (Exception exception) {
					System.out.println("Failed to save player data for " + offlinePlayer.getName());
				}
			}
		});
	}
	
	/*
	 * public static void remove(EntityPlayer entityPlayer, String world) { try {
	 * File file = new File(InventoryManager.playerDir, entityPlayer.bn() + "_" +
	 * world + ".dat");
	 * 
	 * file.delete(); } catch (Exception exception) {
	 * System.out.println("Failed to remove player data for " +
	 * entityPlayer.getName()); } }
	 */
}
