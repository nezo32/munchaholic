package dev.munchaholic.player;

import dev.munchaholic.net.RecipesPayload;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends a player's discovered recipes (only those: the client never learns the seed) for the food tooltip. */
public final class RecipeSync {
	private RecipeSync() {}

	/** active = MunchaholicMode.recipesActive; entries = discoveries sorted, each {@code Recipes.of(seed, food)}. */
	public static RecipesPayload payloadFor(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	/** Only if the client can receive {@link RecipesPayload#TYPE}. */
	public static void send(ServerPlayer player) {
		throw new UnsupportedOperationException("TODO");
	}

	public static void sendAll(MinecraftServer server) {
		throw new UnsupportedOperationException("TODO");
	}
}
