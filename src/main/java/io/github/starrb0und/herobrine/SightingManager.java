package io.github.starrb0und.herobrine;

import io.github.starrb0und.herobrine.mixin.MannequinInvoker;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.protocol.game.ClientboundAddEntityPacket;
import net.minecraft.network.protocol.game.ClientboundRemoveEntitiesPacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityDataPacket;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.decoration.Mannequin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.LightLayer;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * Rolls for sightings and tracks the active ones.
 *
 * <p>Herobrine is a mannequin entity that is never added to the world. Its spawn packets are sent
 * only to the player being haunted, so nobody else sees him and nothing can interact with him.
 */
public class SightingManager {
	private static final int PLACEMENT_ATTEMPTS = 24;
	/** How long a sighting that could not be placed keeps being retried. */
	private static final int PENDING_TICKS = 20 * 60 * 5;
	private static final int VERTICAL_SEARCH = 8;
	private static final double EYE_HEIGHT = 1.62;

	private final RandomSource random = RandomSource.create();
	private final Map<UUID, Sighting> active = new HashMap<>();
	private final Map<UUID, Integer> cooldownUntilTick = new HashMap<>();
	private final Map<UUID, Integer> pendingUntilTick = new HashMap<>();

	private HerobrineConfig config = new HerobrineConfig();
	private volatile ResolvableProfile profile = Mannequin.DEFAULT_PROFILE;
	/** True while a skin lookup is running or once it has succeeded. */
	private volatile boolean skinLookupPending;

	private record Sighting(ServerLevel level, int entityId, Vec3 position, int endTick) {
	}

	public void reload(MinecraftServer server) {
		config = HerobrineConfig.load();
		resolveSkin(server, config.skinPlayer);
	}

	/** Looks up the skin off-thread; until it finishes the client resolves the name itself. */
	private void resolveSkin(MinecraftServer server, String name) {
		profile = ResolvableProfile.createUnresolved(name);
		skinLookupPending = true;
		CompletableFuture
				.supplyAsync(() -> server.services().profileResolver().fetchByName(name))
				.whenComplete((found, error) -> {
					if (error != null || found.isEmpty()) {
						// Mojang's lookup is rate limited; the next sighting tries again.
						HerobrineMod.LOGGER.warn("Could not look up the skin of '{}'; will retry at the next sighting", name);
						skinLookupPending = false;
					} else {
						profile = ResolvableProfile.createResolved(found.get());
						HerobrineMod.LOGGER.info("Loaded Herobrine's skin from '{}'", found.get().name());
					}
				});
	}

	public void tick(MinecraftServer server) {
		int now = server.getTickCount();
		tickActive(server, now);

		if (now % config.checkIntervalTicks != 0) {
			return;
		}
		pendingUntilTick.values().removeIf(expiry -> expiry <= now);
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			UUID id = player.getUUID();
			if (active.containsKey(id) || player.isSpectator()) {
				continue;
			}
			if (cooldownUntilTick.getOrDefault(id, 0) > now) {
				continue;
			}
			// A sighting that found no spot is owed for a while, so a won roll is rarely wasted.
			if (pendingUntilTick.containsKey(id) || random.nextDouble() < chance(player)) {
				if (trySpawn(player)) {
					pendingUntilTick.remove(id);
				} else {
					pendingUntilTick.putIfAbsent(id, now + PENDING_TICKS);
				}
			}
		}
	}

	/** Probability of a sighting for this player on a single roll. */
	private double chance(ServerPlayer player) {
		ServerLevel level = player.level();
		double chance = config.sightingsPerMinecraftWeek * config.checkIntervalTicks / HerobrineConfig.TICKS_PER_WEEK;
		if (level.isThundering()) {
			chance *= config.thunderstormMultiplier;
		}
		if (isInDarkCave(player, level)) {
			chance *= config.darkCaveMultiplier;
		}
		if (hasCompany(player, level)) {
			chance *= config.groupMultiplier;
		}
		return chance;
	}

	/** Underground (no sky light reaches the player, in a dimension that has a sky) and dark. */
	private boolean isInDarkCave(ServerPlayer player, ServerLevel level) {
		BlockPos head = BlockPos.containing(player.getEyePosition());
		return level.dimensionType().hasSkyLight()
				&& level.getBrightness(LightLayer.SKY, head) == 0
				&& level.getMaxLocalRawBrightness(head) <= config.darkCaveMaxLight;
	}

	private boolean hasCompany(ServerPlayer player, ServerLevel level) {
		double radiusSqr = config.groupRadius * config.groupRadius;
		for (ServerPlayer other : level.players()) {
			if (other != player && !other.isSpectator() && other.distanceToSqr(player) <= radiusSqr) {
				return true;
			}
		}
		return false;
	}

	private void tickActive(MinecraftServer server, int now) {
		Iterator<Map.Entry<UUID, Sighting>> it = active.entrySet().iterator();
		while (it.hasNext()) {
			Map.Entry<UUID, Sighting> entry = it.next();
			ServerPlayer player = server.getPlayerList().getPlayer(entry.getKey());
			if (player == null) {
				it.remove();
				continue;
			}
			Sighting sighting = entry.getValue();
			if (now >= sighting.endTick() || player.level() != sighting.level() || isNoticed(player, sighting.position())) {
				player.connection.send(new ClientboundRemoveEntitiesPacket(sighting.entityId()));
				it.remove();
			}
		}
	}

	private boolean isNoticed(ServerPlayer player, Vec3 herobrine) {
		Vec3 toHerobrine = herobrine.add(0, 1, 0).subtract(player.getEyePosition());
		if (toHerobrine.horizontalDistance() < config.vanishDistance) {
			return true;
		}
		double cosAngle = player.getViewVector(1.0F).dot(toHerobrine.normalize());
		return cosAngle > Math.cos(Math.toRadians(config.lookThresholdDegrees));
	}

	/** Shows Herobrine to the player if a suitable spot exists. Returns whether he appeared. */
	public boolean trySpawn(ServerPlayer player) {
		UUID id = player.getUUID();
		Sighting previous = active.remove(id);
		if (previous != null) {
			player.connection.send(new ClientboundRemoveEntitiesPacket(previous.entityId()));
		}

		ServerLevel level = player.level();
		if (!skinLookupPending) {
			resolveSkin(level.getServer(), config.skinPlayer);
		}
		Vec3 position = findSpot(player, level);
		if (position == null) {
			return false;
		}

		float yaw = (float) Math.toDegrees(Mth.atan2(-(player.getX() - position.x), player.getZ() - position.z));
		Mannequin herobrine = new Mannequin(EntityTypes.MANNEQUIN, level);
		herobrine.snapTo(position.x, position.y, position.z, yaw, 0.0F);
		herobrine.setYHeadRot(yaw);
		herobrine.setYBodyRot(yaw);
		herobrine.setComponent(DataComponents.PROFILE, profile);
		((MannequinInvoker) herobrine).herobrine$setHideDescription(true);

		player.connection.send(new ClientboundAddEntityPacket(
				herobrine.getId(), herobrine.getUUID(), position.x, position.y, position.z,
				0.0F, yaw, EntityTypes.MANNEQUIN, 0, Vec3.ZERO, yaw));
		List<SynchedEntityData.DataValue<?>> data = herobrine.getEntityData().getNonDefaultValues();
		if (data != null) {
			player.connection.send(new ClientboundSetEntityDataPacket(herobrine.getId(), data));
		}

		int now = level.getServer().getTickCount();
		active.put(id, new Sighting(level, herobrine.getId(), position, now + config.maxDurationTicks));
		cooldownUntilTick.put(id, now + config.cooldownSeconds * 20);
		return true;
	}

	/** Picks a standable spot off to the side of the player's view that the player can actually see. */
	private Vec3 findSpot(ServerPlayer player, ServerLevel level) {
		Vec3 eye = player.getEyePosition();
		for (int attempt = 0; attempt < PLACEMENT_ATTEMPTS; attempt++) {
			double offset = Mth.lerp(random.nextDouble(), config.minAngle, config.maxAngle);
			if (random.nextBoolean()) {
				offset = -offset;
			}
			double yaw = Math.toRadians(player.getYRot() + offset);
			double distance = Mth.lerp(random.nextDouble(), config.minDistance, config.maxDistance);
			double x = player.getX() - Math.sin(yaw) * distance;
			double z = player.getZ() + Math.cos(yaw) * distance;

			BlockPos feet = findStandable(level, BlockPos.containing(x, player.getY(), z));
			if (feet == null) {
				continue;
			}
			Vec3 position = new Vec3(feet.getX() + 0.5, feet.getY(), feet.getZ() + 0.5);
			Vec3 head = position.add(0, EYE_HEIGHT, 0);
			HitResult hit = level.clip(new ClipContext(eye, head, ClipContext.Block.VISUAL, ClipContext.Fluid.NONE, player));
			if (hit.getType() == HitResult.Type.MISS && !isNoticed(player, position)) {
				return position;
			}
		}
		return null;
	}

	/** Searches up and down from {@code start} for two free blocks above solid ground. */
	private static BlockPos findStandable(ServerLevel level, BlockPos start) {
		if (!level.isLoaded(start)) {
			return null;
		}
		for (int dy = 0; dy <= VERTICAL_SEARCH; dy++) {
			BlockPos up = start.above(dy);
			if (isStandable(level, up)) {
				return up;
			}
			BlockPos down = start.below(dy);
			if (dy != 0 && isStandable(level, down)) {
				return down;
			}
		}
		return null;
	}

	private static boolean isStandable(ServerLevel level, BlockPos feet) {
		return level.getBlockState(feet.below()).blocksMotion() && isFree(level.getBlockState(feet)) && isFree(level.getBlockState(feet.above()));
	}

	private static boolean isFree(BlockState state) {
		return !state.blocksMotion() && state.getFluidState().isEmpty();
	}

	public void clear() {
		active.clear();
		cooldownUntilTick.clear();
		pendingUntilTick.clear();
	}
}
