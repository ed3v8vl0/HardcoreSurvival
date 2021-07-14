package com.gmail.ed3v8vl0.HardcoreSurvival.Command;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;

public class InventoryHelper {
	private static List<Slot> slots = new ArrayList<Slot>();
	private static String inventoryName;
	
	private static int attackSlot;
	private static int resistanceSlot;
	private static int evasionSlot;
	private static int luckySlot;
	
	public static void Initialization(FileConfiguration config) {
		InventoryHelper.inventoryName = config.getString("Inventory.name");
		
		for (int i = 1; i <= 9; i++) {
			ItemStack itemStack = config.getItemStack("Inventory.itemStack_" + i);
			Slot slot = new Slot(i - 1, itemStack);
			
			InventoryHelper.slots.add(slot);
		}
		
		InventoryHelper.attackSlot = config.getInt("Inventory.attackSlot");
		InventoryHelper.resistanceSlot = config.getInt("Inventory.resistanceSlot");
		InventoryHelper.evasionSlot = config.getInt("Inventory.evasionSlot");
		InventoryHelper.luckySlot = config.getInt("Inventory.luckySlot");
	}
	
	public static boolean compareInventory(Inventory inventory) {
		if (inventory.getName().equalsIgnoreCase(InventoryHelper.inventoryName)) {
			return true;
		} else {
			return false;
		}
	}
	
	public static boolean attackClick(int slot) {
		return InventoryHelper.attackSlot == slot;
	}
	
	public static boolean resistanceClick(int slot) {
		return InventoryHelper.resistanceSlot == slot;
	}
	
	public static boolean evasionClick(int slot) {
		return InventoryHelper.evasionSlot == slot;
	}
	
	public static boolean luckyClick(int slot) {
		return InventoryHelper.luckySlot == slot;
	}
	
	public static void openGui(Player player) {
		Inventory inventory = Bukkit.createInventory(null, 9, InventoryHelper.inventoryName);
		
		for (Slot slot : InventoryHelper.slots) {
			inventory.setItem(slot.getKey(), slot.getValue());
		}
		
		player.openInventory(inventory);
	}
	
	static class Slot implements Entry<Integer, ItemStack> {
	    private Integer key;
	    private ItemStack value;

	    public Slot(Integer key, ItemStack value) {
	        this.key = key;
	        this.value = value;
	    }
	    
		@Override
		public Integer getKey() {
			// TODO Auto-generated method stub
			return key;
		}

		@Override
		public ItemStack getValue() {
			// TODO Auto-generated method stub
			return value;
		}

		public Integer setKey(Integer key) {
			Integer old = this.key;
	        this.key = key;
			return old;
		}
		
		@Override
		public ItemStack setValue(ItemStack value) {
	        ItemStack old = this.value;
	        this.value = value;
			return old;
		}
	}
}
