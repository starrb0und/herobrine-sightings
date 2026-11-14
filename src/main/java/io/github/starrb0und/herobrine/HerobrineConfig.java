package io.github.starrb0und.herobrine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/** Settings loaded from {@code config/herobrine.json}. Field names are the JSON keys. */
public class HerobrineConfig {
	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("herobrine.json");

	/** How often, in ticks, each player is rolled for a sighting. */
	public int checkIntervalTicks = 200;
	/** Probability (0-1) of a sighting on each roll. */
	public double chance = 0.004;
	/** Minimum time between two sightings for the same player. */
	public int cooldownSeconds = 600;

	/** Horizontal distance range, in blocks, at which Herobrine appears. */
	public double minDistance = 16;
	public double maxDistance = 40;
	/** How far off the centre of the player's view he appears, in degrees. */
	public double minAngle = 30;
	public double maxAngle = 50;

	/** He vanishes once the player's view is within this many degrees of him. */
	public double lookThresholdDegrees = 15;
	/** He vanishes on his own after this many ticks. */
	public int maxDurationTicks = 100;
	/** He vanishes if the player gets closer than this, in blocks. */
	public double vanishDistance = 8;

	/** Player account whose skin Herobrine wears. */
	public String skinPlayer = "MHF_Herobrine";

	public static HerobrineConfig load() {
		HerobrineConfig config = new HerobrineConfig();
		if (Files.exists(PATH)) {
			try {
				HerobrineConfig read = GSON.fromJson(Files.readString(PATH), HerobrineConfig.class);
				if (read != null) {
					config = read;
				}
			} catch (IOException | JsonParseException e) {
				HerobrineMod.LOGGER.error("Could not read {}, using defaults", PATH, e);
				return config;
			}
		}
		config.sanitize();
		config.save();
		return config;
	}

	private void sanitize() {
		checkIntervalTicks = Math.max(1, checkIntervalTicks);
		chance = Math.clamp(chance, 0, 1);
		minDistance = Math.max(2, minDistance);
		maxDistance = Math.max(minDistance, maxDistance);
		minAngle = Math.clamp(minAngle, 0, 180);
		maxAngle = Math.clamp(maxAngle, minAngle, 180);
		maxDurationTicks = Math.max(1, maxDurationTicks);
		if (skinPlayer == null || skinPlayer.isBlank()) {
			skinPlayer = "MHF_Herobrine";
		}
	}

	private void save() {
		try {
			Files.createDirectories(PATH.getParent());
			Files.writeString(PATH, GSON.toJson(this));
		} catch (IOException e) {
			HerobrineMod.LOGGER.error("Could not write {}", PATH, e);
		}
	}
}
