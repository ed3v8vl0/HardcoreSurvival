package com.gmail.ed3v8vl0.HardcoreSurvival.AirDrop;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.craftbukkit.v1_12_R1.CraftWorld;
import org.bukkit.craftbukkit.v1_12_R1.util.CraftMagicNumbers;
import org.bukkit.entity.Player;
import org.bukkit.event.entity.CreatureSpawnEvent.SpawnReason;
import org.jnbt.*;

import com.gmail.ed3v8vl0.HardcoreSurvival.HardcoreSurvival;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;

public class SupplyManager {
	private FileConfiguration config = YamlConfiguration.loadConfiguration(new File("./Hardcore/airdrop/airdrop.yml"));
	public BorderManager borderManager;
	public ChestPage chestPage;

	@Nonnull
	public File schematic;
	public ParticleData particleData;
	public double BaseValue;
	public double AddVelocity;
	public double yPos;
	
	public String title;
	public String subTitle;
	public String message;
	public int fadeIn;
	public int stay;
	public int fadeOut;
	
	public SupplyManager(HardcoreSurvival mainClass, ChestPage chestPage) {
		File file = new File("./plugins/Hardcore/airdrop/airdrop.yml");
		
		if (!file.exists()) {
			mainClass.saveResource("airdrop/airdrop.yml", false);
		}
		
		try {
			this.config.load(file);
			this.config.addDefaults(YamlConfiguration.loadConfiguration(new InputStreamReader(mainClass.getResource("airdrop/airdrop.yml"))));
		} catch (IOException | InvalidConfigurationException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		file = new File("./plugins/Hardcore/airdrop/airdrop.schematic");
		
		if (!file.exists()) {
			mainClass.saveResource("airdrop/airdrop.schematic", false);
		}
		
		file = new File("./plugins/Hardcore/airdrop/particles.txt");
		
		if (!file.exists()) {
			mainClass.saveResource("airdrop/particles.txt", false);
		}
		
		this.borderManager = mainClass.getSeason().getBorderManager();
		this.chestPage = chestPage;
		this.configInit(false);
	}
	
	public void configInit(boolean isReload) {
		if (isReload)
			this.config = YamlConfiguration.loadConfiguration(new File("./plugins/Hardcore/airdrop/airdrop.yml"));
		this.schematic = new File(this.config.getString("schematic"));
		this.particleData = new ParticleData(this.config);
		this.BaseValue = this.config.getDouble("BaseValue");
		this.AddVelocity = this.config.getDouble("AddVelocity");
		this.yPos = this.config.getDouble("spawnPos") + 0.5D;
		this.title = this.config.getString("title");
		this.subTitle = this.config.getString("subTitle");
		this.message = this.config.getString("message");
		this.fadeIn = this.config.getInt("fadeIn");
		this.stay = this.config.getInt("stay");
		this.fadeOut = this.config.getInt("fadeOut");
	}
	
	public void supply(Location loc) {
		Bukkit.getScheduler().runTaskAsynchronously(Bukkit.getPluginManager().getPlugin("Hardcore"), new Runnable() {
			@Override
			public void run() {
				try {
					pasteSchematic(loc.getWorld(), loc, loadSchematic(schematic));
				} catch (IOException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
			}
		});
	}
    
    @SuppressWarnings("deprecation")
	public void pasteSchematic(World world, Location loc, Schematic schematic)
    {
        byte[] blocks = schematic.getBlocks();
        byte[] blockData = schematic.getData();
 
        short length = schematic.getLenght();
        short width = schematic.getWidth();
        short height = schematic.getHeight();
 
        List<EntityFallingBlockEx> fallings = new ArrayList<EntityFallingBlockEx>();
        int count = 0;
        int highestBlock = 0;
        
        loc.add(width / -2, 0, length / -2);
        
		for (int y = 0; y < height; ++y) {
			for (int x = 0; x < width; ++x) {
				for (int z = 0; z < length; ++z) {
					int index = y * width * length + z * width + x;

					if (blocks[index] == 0)
						continue;
					
					EntityFallingBlockEx entity = new EntityFallingBlockEx(fallings, count++, this.particleData, ((CraftWorld) world).getHandle(), loc.getBlockX() + x + 0.5D, this.yPos + y, loc.getBlockZ() + z + 0.5D, CraftMagicNumbers.getBlock(blocks[index]).fromLegacyData(blockData[index]));
					entity.ticksLived = 1;
					entity.setNoGravity(true);
					
					if (blocks[index] == 54) {
						entity.setItems(this.chestPage);
					}
					
					int temp = world.getHighestBlockYAt(loc.getBlockX() + x, loc.getBlockZ() + z);
					
					if (highestBlock < temp) {
						highestBlock = temp;
					}
					
					fallings.add(entity);
				}
			}
		}
		
		for (EntityFallingBlockEx e : fallings) {
			e.motY = (this.BaseValue + (this.AddVelocity * ((255 - highestBlock) / 5))) * -1;
			e.velocityChanged = true;
		}
		
		Bukkit.getScheduler().runTask(Bukkit.getPluginManager().getPlugin("Hardcore"), new Runnable() {
			@Override
			public void run() {
				double posX = Math.round(loc.getX() * 10) / 10;
				double posY = Math.round(yPos * 10) / 10;
				double posZ = Math.round(loc.getZ() * 10) / 10;
				
				for (Player player : world.getPlayers()) {
					player.sendTitle(title, subTitle.replace("<posX>", String.valueOf(posX)).replace("<posY>", String.valueOf(posY)).replace("<posZ>", String.valueOf(posZ)), fadeIn, stay, fadeOut);
					player.sendMessage(message.replace("<posX>", String.valueOf(posX)).replace("<posY>", String.valueOf(posY)).replace("<posZ>", String.valueOf(posZ)));
				}
				
				for (EntityFallingBlockEx entity : fallings)
					((CraftWorld) world).addEntity(entity, SpawnReason.CUSTOM);
			}
		});
    }
    
    public Schematic loadSchematic(File file) throws IOException
    {
		NBTInputStream nbtStream = new NBTInputStream(new FileInputStream(file));
 
        CompoundTag schematicTag = (CompoundTag) nbtStream.readTag();
        if (!schematicTag.getName().equals("Schematic")) {
            nbtStream.close();
            throw new IllegalArgumentException("Tag \"Schematic\" does not exist or is not first");
        }
 
        Map<String, Tag> schematic = schematicTag.getValue();
        if (!schematic.containsKey("Blocks")) {
            nbtStream.close();
            throw new IllegalArgumentException("Schematic file is missing a \"Blocks\" tag");
        }
 
        short width = getChildTag(schematic, "Width", ShortTag.class).getValue();
        short length = getChildTag(schematic, "Length", ShortTag.class).getValue();
        short height = getChildTag(schematic, "Height", ShortTag.class).getValue();
 
        String materials = getChildTag(schematic, "Materials", StringTag.class).getValue();
        if (!materials.equals("Alpha")) {
            nbtStream.close();
            throw new IllegalArgumentException("Schematic file is not an Alpha schematic");
        }
 
        byte[] blocks = getChildTag(schematic, "Blocks", ByteArrayTag.class).getValue();
        byte[] blockData = getChildTag(schematic, "Data", ByteArrayTag.class).getValue();
        
        nbtStream.close();
        return new Schematic(blocks, blockData, width, length, height);
    }
    
    /**
    * Get child tag of a NBT structure.
    *
    * @param items The parent tag map
    * @param key The name of the tag to get
    * @param expected The expected type of the tag
    * @return child tag casted to the expected type
    * @throws DataException if the tag does not exist or the tag is not of the
    * expected type
    */
    private <T extends Tag> T getChildTag(Map<String, Tag> items, String key, Class<T> expected) throws IllegalArgumentException
    {
        if (!items.containsKey(key)) {
            throw new IllegalArgumentException("Schematic file is missing a \"" + key + "\" tag");
        }
        Tag tag = items.get(key);
        if (!expected.isInstance(tag)) {
            throw new IllegalArgumentException(key + " tag is not of tag type " + expected.getName());
        }
        return expected.cast(tag);
    }
}
