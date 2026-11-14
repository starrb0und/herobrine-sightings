package io.github.starrb0und.herobrine;

import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.minecraft.commands.Commands;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HerobrineMod implements ModInitializer {
	public static final String MOD_ID = "herobrine";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	private final SightingManager sightings = new SightingManager();

	@Override
	public void onInitialize() {
		ServerLifecycleEvents.SERVER_STARTED.register(sightings::reload);
		ServerLifecycleEvents.SERVER_STOPPING.register(server -> sightings.clear());
		ServerTickEvents.END_SERVER_TICK.register(sightings::tick);

		// /herobrine spawn- force a sighting for yourself (for testing)
		// /herobrine reload - re-read config/herobrine.json
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> dispatcher.register(
				Commands.literal("herobrine")
						.requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS))
						.then(Commands.literal("spawn").executes(context -> {
							ServerPlayer player = context.getSource().getPlayerOrException();
							if (sightings.trySpawn(player)) {
								context.getSource().sendSuccess(() -> Component.literal("Herobrine is watching."), false);
								return 1;
							}
							context.getSource().sendFailure(Component.literal("No suitable spot found near you. Try again somewhere more open."));
							return 0;
						}))
						.then(Commands.literal("reload").executes(context -> {
							sightings.reload(context.getSource().getServer());
							context.getSource().sendSuccess(() -> Component.literal("Herobrine config reloaded."), false);
							return 1;
						}))));
	}
}
