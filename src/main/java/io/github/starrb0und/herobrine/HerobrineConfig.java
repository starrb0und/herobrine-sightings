package io.github.starrb0und.herobrine;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;
import net.fabricmc.loader.api.FabricLoader;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

/** Settings loaded from {@code config/herobrine.json}. Field names are the JSON keys. */
public class HerobrineConfig {
	/** Bumped when the settings change meaning; older files are backed up and replaced. */
	private static final int CURRENT_VERSION = 2;
	/** Seven Minecraft days of 24000 ticks. */
	public static final int TICKS_PER_WEEK = 7 * 24000;

	private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();
	private static final Path PATH = FabricLoader.getInstance().getConfigDir().resolve("herobrine.json");

	public int configVersion = CURRENT_VERSION;

	/** How often, in ticks, each player is rolled for a sighting. */
	public int checkIntervalTicks = 200;
	/** Average number of sightings per player per Minecraft week (7 in-game days), before multipliers. */
	public double sightingsPerMinecraftWeek = 1.0;
	/** The odds are multiplied by this during a thunderstorm. */
	public double thunderstormMultiplier = 4.0;
	/** The odds are multiplied by this while the player is underground in the dark. */
	public double darkCaveMultiplier = 3.0;
	/** Light level (0-15) at or below which an underground player counts as being in the dark. */
	public int darkCaveMaxLight = 4;
	/** The odds are multiplied by this while another player is within {@link #groupRadius}. */
	public double groupMultiplier = 0.15;
	public double groupRadius = 24;
	/** Minimum time between two sightings for the same player. */
	public int cooldownSeconds = 600;

	/** Horizontal distance range, in blocks, at which Herobrine appears. */
	public double minDistance = 45;
	public double maxDistance = 60;
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
				if (read != null && read.configVersion == CURRENT_VERSION) {
					config = read;
				} else {
					Path backup = PATH.resolveSibling("herobrine.json.old");
					Files.copy(PATH, backup, StandardCopyOption.REPLACE_EXISTING);
					HerobrineMod.LOGGER.warn("{} is from an older version; reset to the new defaults. Your old file was kept as {}", PATH, backup);
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
		sightingsPerMinecraftWeek = Math.max(0, sightingsPerMinecraftWeek);
		thunderstormMultiplier = Math.max(0, thunderstormMultiplier);
		darkCaveMultiplier = Math.max(0, darkCaveMultiplier);
		groupMultiplier = Math.max(0, groupMultiplier);
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
