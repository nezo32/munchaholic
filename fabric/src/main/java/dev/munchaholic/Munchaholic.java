package dev.munchaholic;

import dev.munchaholic.command.MunchaholicCommand;
import dev.munchaholic.mode.ModeBootstrap;
import dev.munchaholic.net.RecipesPayload;
import dev.munchaholic.net.RolledPayload;
import dev.munchaholic.player.MunchAttachments;
import dev.munchaholic.player.PlayerHooks;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.minecraft.resources.Identifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Common entrypoint: payload types, player attachments, player events, world settings and the command. */
public final class Munchaholic implements ModInitializer {
	public static final String MOD_ID = "munchaholic";
	public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

	/** {@code munchaholic:<path>}. */
	public static Identifier id(String path) {
		return Identifier.fromNamespaceAndPath(MOD_ID, path);
	}

	@Override
	public void onInitialize() {
		RolledPayload.register();
		RecipesPayload.register();
		MunchAttachments.init();
		PlayerHooks.register();
		ServerLifecycleEvents.SERVER_STARTING.register(ModeBootstrap::onServerStarting);
		CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> MunchaholicCommand.register(dispatcher));
	}
}
