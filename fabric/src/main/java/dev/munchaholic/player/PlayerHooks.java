package dev.munchaholic.player;

import dev.munchaholic.Munchaholic;
import dev.munchaholic.mode.MunchaholicMode;
import net.fabricmc.fabric.api.entity.event.v1.ServerPlayerEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;

/**
 * Join, death and respawn handling (ARCHITECTURE.md §3.2). Stacks are not copyOnDeath: on death, COPY_FROM alone
 * decides (keep-on-death), and the modifiers, which vanilla drops on death, are rebuilt from the copied steps.
 */
public final class PlayerHooks {
	private PlayerHooks() {}

	/** Registers the JOIN, COPY_FROM and AFTER_RESPAWN listeners. */
	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> guarded("join", () -> onJoin(handler.getPlayer())));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				guarded("respawn copy", () -> onCopyFrom(oldPlayer, newPlayer, alive)));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				guarded("respawn sync", () -> onAfterRespawn(oldPlayer, newPlayer, alive)));
	}

	/** Repairs or rebalances the modifiers, then syncs the recipe list. */
	public static void onJoin(ServerPlayer player) {
		PlayerMunch.applyModifiers(player);
		RecipeSync.send(player);
	}

	/** Death with keep-on-death ON: copy the stacks, reapply, heal to the new max. OFF: tell the player. */
	public static void onCopyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		// End exit: vanilla copies permanent modifiers and Fabric copies every attachment
		if (alive) return;
		if (MunchaholicMode.keepOnDeath(newPlayer.level().getServer())) {
			PlayerMunch.setStacks(newPlayer, PlayerMunch.stacks(oldPlayer));
			// restoreFrom set health to max BEFORE our max_health modifier came back
			newPlayer.setHealth(newPlayer.getMaxHealth());
		} else if (!PlayerMunch.stacks(oldPlayer).isEmpty() && newPlayer.connection != null) {
			// stacks and modifiers die with the old entity; discoveries survive (copyOnDeath)
			newPlayer.sendSystemMessage(Component.translatableWithFallback("munchaholic.message.lostOnDeath", "You lost your Munchaholic attribute changes"));
		}
	}

	/** Resyncs the recipe list (the client's player entity was recreated). */
	public static void onAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		RecipeSync.send(newPlayer);
	}

	/** A failing hook is logged; it must not kick the player or break the respawn. */
	private static void guarded(String what, Runnable hook) {
		try {
			hook.run();
		} catch (RuntimeException e) {
			Munchaholic.LOGGER.error("Munchaholic player {} hook failed", what, e);
		}
	}
}
