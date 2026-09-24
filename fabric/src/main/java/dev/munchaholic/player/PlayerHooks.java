package dev.munchaholic.player;

import java.util.concurrent.atomic.AtomicBoolean;

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
	private static final AtomicBoolean LOGGED_FAILURE = new AtomicBoolean();

	private PlayerHooks() {}

	/** Registers the JOIN, COPY_FROM and AFTER_RESPAWN listeners. */
	public static void register() {
		ServerPlayConnectionEvents.JOIN.register((handler, sender, server) -> guarded("join", () -> onJoin(handler.getPlayer())));
		ServerPlayerEvents.COPY_FROM.register((oldPlayer, newPlayer, alive) ->
				guarded("respawn copy", () -> onCopyFrom(oldPlayer, newPlayer, alive)));
		ServerPlayerEvents.AFTER_RESPAWN.register((oldPlayer, newPlayer, alive) ->
				guarded("respawn sync", () -> onAfterRespawn(oldPlayer, newPlayer, alive)));
	}

	/** Rebuilds lost stacks from the modifiers, repairs or rebalances the modifiers, then syncs the recipe list. */
	public static void onJoin(ServerPlayer player) {
		PlayerMunch.rebuildLostStacks(player);
		PlayerMunch.applyModifiers(player);
		RecipeSync.send(player);
	}

	/**
	 * Always: copy the discoveries (and, on End exit, the stacks) ourselves. Fabric copies attachments in its own
	 * AFTER_RESPAWN listener, whose order against ours depends on mod load order; COPY_FROM runs strictly before any
	 * AFTER_RESPAWN listener, so {@link #onAfterRespawn} always syncs the real list. Fabric later writes the same values.
	 * Death with keep-on-death ON: copy the stacks, reapply, heal to the new max. OFF: tell the player.
	 */
	public static void onCopyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		if (oldPlayer.hasAttached(MunchAttachments.DISCOVERIES)) {
			newPlayer.setAttached(MunchAttachments.DISCOVERIES, PlayerMunch.discoveries(oldPlayer));
		}
		if (alive) {
			// End exit: vanilla copies the permanent modifiers, and the stacks come along unchanged
			if (oldPlayer.hasAttached(MunchAttachments.STACKS)) {
				newPlayer.setAttached(MunchAttachments.STACKS, PlayerMunch.stacks(oldPlayer));
			}
			return;
		}
		if (MunchaholicMode.keepOnDeath(newPlayer.level().getServer())) {
			PlayerMunch.setStacks(newPlayer, PlayerMunch.stacks(oldPlayer));
			// restoreFrom set health to max BEFORE our max_health modifier came back
			newPlayer.setHealth(newPlayer.getMaxHealth());
		} else if (!PlayerMunch.stacks(oldPlayer).isEmpty() && newPlayer.connection != null) {
			// stacks and modifiers die with the old entity; discoveries survive (copied above)
			newPlayer.sendSystemMessage(Component.translatableWithFallback("munchaholic.message.lostOnDeath", "You lost your Munchaholic attribute changes"));
		}
	}

	/** Resyncs the recipe list (the client's player entity was recreated). */
	public static void onAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		RecipeSync.send(newPlayer);
	}

	/** A failing hook is logged (first at error, later at debug); it must not kick the player or break the respawn. */
	private static void guarded(String what, Runnable hook) {
		try {
			hook.run();
		} catch (VirtualMachineError e) {
			throw e;
		} catch (Throwable e) {
			if (LOGGED_FAILURE.compareAndSet(false, true)) {
				Munchaholic.LOGGER.error("Munchaholic player {} hook failed; further failures are logged at debug level", what, e);
			} else {
				Munchaholic.LOGGER.debug("Munchaholic player {} hook failed", what, e);
			}
		}
	}
}
