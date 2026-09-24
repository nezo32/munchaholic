package dev.munchaholic.player;

import net.minecraft.server.level.ServerPlayer;

/** Join, death and respawn handling (ARCHITECTURE.md §3.2). */
public final class PlayerHooks {
	private PlayerHooks() {}

	/** Registers the JOIN, COPY_FROM and AFTER_RESPAWN listeners. */
	public static void register() {
		// TODO (Dev C): Phase-0 registers nothing, so no skeleton body runs at join; register the three listeners.
	}

	/** Repairs or rebalances the modifiers, then syncs the recipe list. */
	public static void onJoin(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Death with keep-on-death ON: copy the stacks, reapply, heal to the new max. OFF: tell the player. */
	public static void onCopyFrom(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Resyncs the recipe list (the client's player entity was recreated). */
	public static void onAfterRespawn(ServerPlayer oldPlayer, ServerPlayer newPlayer, boolean alive) {
		throw new UnsupportedOperationException("TODO");
	}
}
