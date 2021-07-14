package com.gmail.ed3v8vl0.HardcoreSurvival.Season;

import java.io.*;

import javax.annotation.Nonnull;

import com.google.gson.*;
import com.google.gson.stream.JsonWriter;

public class LogManager {
	private static LogManager INSTANCE = Serializer.deserialize();
	private final Log[] logArrays = new Log[100];

	public static LogManager getInstance() {
		if (INSTANCE == null)
			INSTANCE = Serializer.deserialize();
		
		return INSTANCE;
	}
	
	@Nonnull
	private Log createLog(int seasonId) {
		Log seasonLog = this.logArrays[seasonId];

		if (seasonLog == null) {
			seasonLog = new Log(seasonId);

			this.logArrays[seasonId] = seasonLog;
		}

		return seasonLog;
	}

	@Nonnull
	public Log getSeasonLog(int seasonId) {
		Log seasonLog = this.logArrays[seasonId];

		if (seasonLog == null)
			seasonLog = this.createLog(seasonId);

		return seasonLog;
	}

	public Log getLastIndex() {
		Log log = null;
		
		for (int i = this.logArrays.length - 1; i > 0; i--) {
			
			if (this.logArrays[i] != null) {
				log = this.logArrays[i];
				break;
			}
		}
		
		return log;
	}
	
	public static class Serializer {
		private static final Gson gson = new GsonBuilder().registerTypeAdapter(Log.class, new Log.Serializer()).create();

		public static LogManager deserialize() {
			JsonParser jsonParser = new JsonParser();
			LogManager logManager = new LogManager();
			try {
				File folder = new File("./plugins/Hardcore/Logs");

				if (folder.exists()) {
					for (File file : folder.listFiles()) {
						FileReader reader = new FileReader(file);
						Log log = gson.fromJson(jsonParser.parse(reader), Log.class);
						
						logManager.logArrays[log.getSeasonId()] = log;
						reader.close();
					}
				}
			} catch (JsonIOException | JsonSyntaxException | IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}

			return logManager;
		}

		public static JsonElement serialize() {
			for (int i = 0; i < LogManager.INSTANCE.logArrays.length; i++) {
				try {
					Log log = LogManager.INSTANCE.logArrays[i];

					if (log != null && log.getData().size() > 0) {
						File folder = new File("./plugins/Hardcore/Logs");

						if (!folder.exists()) {
							folder.mkdir();
						}
						
						JsonWriter jsonWriter = new JsonWriter(new FileWriter(new File(folder, "season_" + i + ".json")));

						jsonWriter.setIndent(" ");
						gson.toJson(log, Log.class, jsonWriter);
						jsonWriter.close();
					}
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return null;
		}
	}
}
