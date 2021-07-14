package com.gmail.ed3v8vl0.HardcoreSurvival;

import java.io.*;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;

import org.bukkit.Bukkit;
import org.bukkit.plugin.RegisteredServiceProvider;
import org.bukkit.plugin.java.JavaPlugin;

import com.gmail.ed3v8vl0.HardcoreSurvival.AirDrop.ChestPage;
import com.gmail.ed3v8vl0.HardcoreSurvival.Command.HardcoreCommandOp;
import com.gmail.ed3v8vl0.HardcoreSurvival.Command.HardcoreCommandUser;
import com.gmail.ed3v8vl0.HardcoreSurvival.Command.InventoryHelper;
import com.gmail.ed3v8vl0.HardcoreSurvival.Data.SurvivalCalc;
import com.gmail.ed3v8vl0.HardcoreSurvival.Prefix.PrefixCommandOp;
import com.gmail.ed3v8vl0.HardcoreSurvival.Prefix.PrefixCommandUser;
import com.gmail.ed3v8vl0.HardcoreSurvival.Prefix.PrefixManager;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.LogManager;
import com.gmail.ed3v8vl0.HardcoreSurvival.Season.Season;
import com.gmail.ed3v8vl0.HardcoreSurvival.World.BorderManager;
import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

import net.milkbowl.vault.economy.Economy;

public class HardcoreSurvival extends JavaPlugin {
	private static HardcoreSurvival mainClass;
	
    private Economy economy = null;
	private Season season;
	private PrefixManager prefixManager;
	private EventManager eventManager;
	
	private HardcoreCommandOp hardcoreCommandOp;
	private HardcoreCommandUser hardcoreCommandUser;
	private PrefixCommandOp prefixCommandOp;
	private PrefixCommandUser prefixCommandUser;
	private ChestPage chestPage;
	
	@Override
	public void onEnable() {
		if(!(new File(this.getDataFolder(), "config.yml")).exists()) {
			this.saveDefaultConfig();
		}
		
		try {
			final File[] libs = new File[] { new File(this.getDataFolder(), "lib/gson-2.8.6.jar"), new File(this.getDataFolder(), "lib/JNBT_1.4.jar") };
			
			for (final File lib : libs) {
				if (!lib.exists()) {
					JarUtils.extractFromJar(lib.getName(), lib.getAbsolutePath());
				}
			}
			for (final File lib : libs) {
				if (!lib.exists()) {
					getLogger().warning("There was a critical error loading My plugin! Could not find lib: " + lib.getName());
					Bukkit.getServer().getPluginManager().disablePlugin(this);
					return;
				}
				this.addClassPath(JarUtils.getJarUrl(lib));
			}
		} catch (final Exception e) {
			e.printStackTrace();
		}
		
		HardcoreSurvival.mainClass = this; //Instance Init
		this.season = new Season(this); //Instance Init
		this.prefixManager = new PrefixManager(this); //Season(BorderManager) Init -> This Init
		this.eventManager = new EventManager(this);
		
		this.chestPage = new ChestPage(this); //Season Init -> This Init (This Command)
		this.hardcoreCommandOp = new HardcoreCommandOp(this);
		this.hardcoreCommandUser = new HardcoreCommandUser(this);
		this.prefixCommandOp = new PrefixCommandOp(this);
		this.prefixCommandUser = new PrefixCommandUser(this);
		
		this.getCommand("하드코어").setExecutor(this.hardcoreCommandUser); //Finish
		this.getCommand("하드코어설정").setExecutor(this.hardcoreCommandOp); //Finish or Edit??
		this.getCommand("칭호관리").setExecutor(this.prefixCommandOp);
		this.getCommand("칭호").setExecutor(this.prefixCommandUser);
		this.getCommand("보급품설정").setExecutor(this.chestPage);
		
		this.getServer().getPluginManager().registerEvents(this.eventManager, this);
		this.getServer().getPluginManager().registerEvents(this.prefixManager, this);
		this.getServer().getPluginManager().registerEvents(this.chestPage, this);
		
		this.setupEconomy();
		SurvivalCalc.Load(this.getConfig());
		InventoryHelper.Initialization(this.getConfig());
		
		this.Deserializer(); //Season Init -> This Call
	}
	
	@Override
	public void onDisable() {
		this.season.getBorderManager().bossBarRemove();
		
		this.Serializer();
		this.prefixManager.serializer();
		this.chestPage.serializer();
		LogManager.Serializer.serialize();
	}
	
	public void configReload() {
		this.reloadConfig();
		this.season.getBorderManager().configInit();
		this.season.configInit();
		this.prefixManager.configInit();
		this.eventManager.configInit();
		this.hardcoreCommandOp.configInit();
		this.hardcoreCommandUser.configInit();
		this.prefixCommandOp.configInit();
		this.prefixCommandUser.configInit();
		this.chestPage.configInit();
		this.chestPage.getSupplyManager().configInit(true);
		SurvivalCalc.Load(this.getConfig());
		InventoryHelper.Initialization(this.getConfig());
	}
	
	public static HardcoreSurvival getInstance() {
		return HardcoreSurvival.mainClass;
	}
	
	public Season getSeason() {
		return this.season;
	}
	
	public PrefixManager getPrefixManager() {
		return this.prefixManager;
	}
	
	public Economy getEconomy() {
		return this.economy;
	}
	
	public void Serializer() {
		Gson gson = new GsonBuilder().registerTypeAdapter(BorderManager.class, new BorderManager.Seriazlier()).create();
		Gson gson_ = new GsonBuilder().registerTypeAdapter(Season.class, new Season.Seriazlier()).create();
		File folder = new File(this.getDataFolder(), "Data");
		
		if (!folder.exists())
			folder.mkdirs();
		
		try {
			JsonWriter jsonWriter = new JsonWriter(new FileWriter(new File(folder, "BorderManager.json")));
			JsonWriter jsonWriter_ = new JsonWriter(new FileWriter(new File(folder, "Season.json")));
			jsonWriter.setIndent(" ");
			jsonWriter_.setIndent(" ");

			gson.toJson(this.season.getBorderManager(), BorderManager.class, jsonWriter);
			gson_.toJson(this.season, Season.class, jsonWriter_);
			jsonWriter.close();
			jsonWriter_.close();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
	public void Deserializer() {
		try {
			File folder = new File(this.getDataFolder(), "Data");
			
			if (folder.exists()) {
				JsonParser jsonParser = new JsonParser();
				File file = new File(folder, "BorderManager.json");
				File file_ = new File(folder, "Season.json");

				if (file.exists()) {
					FileReader reader = new FileReader(file);
					JsonElement jsonElement = jsonParser.parse(reader);

					if (jsonElement.isJsonObject())
						BorderManager.Seriazlier.deserialize(this.season.getBorderManager(), jsonElement.getAsJsonObject());
					reader.close();
				}

				if (file_.exists()) {
					FileReader reader_ = new FileReader(file_);
					JsonElement jsonElement_ = jsonParser.parse(reader_);

					if (jsonElement_.isJsonObject())
						Season.Seriazlier.deserialize(this.season, jsonElement_.getAsJsonObject());
					reader_.close();
				}
			}
		} catch (JsonParseException | IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}
	
    private boolean setupEconomy() {
        if (getServer().getPluginManager().getPlugin("Vault") == null) {
            return false;
        }
        RegisteredServiceProvider<Economy> rsp = getServer().getServicesManager().getRegistration(Economy.class);
        if (rsp == null) {
            return false;
        }
        economy = rsp.getProvider();
        return economy != null;
    }
    
	private void addClassPath(final URL url) throws IOException {
		final URLClassLoader sysloader = (URLClassLoader) ClassLoader.getSystemClassLoader();
		final Class<URLClassLoader> sysclass = URLClassLoader.class;
		try {
			final Method method = sysclass.getDeclaredMethod("addURL", new Class[] { URL.class });
			method.setAccessible(true);
			method.invoke(sysloader, new Object[] { url });
		} catch (final Throwable t) {
			t.printStackTrace();
			throw new IOException("Error adding " + url + " to system classloader");
		}
	}
}
