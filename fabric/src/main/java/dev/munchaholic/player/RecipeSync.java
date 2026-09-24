package dev.munchaholic.player;

import java.util.List;

import dev.munchaholic.core.Direction;
import dev.munchaholic.core.Recipe;
import dev.munchaholic.core.Recipes;
import dev.munchaholic.mode.MunchaholicMode;
import dev.munchaholic.net.RecipesPayload;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;

/** Sends a player's discovered recipes (only those: the client never learns the seed) for the food tooltip. */
public final class RecipeSync {
	private RecipeSync() {}

	/** active = MunchaholicMode.recipesActive; entries = discoveries sorted, each {@code Recipes.of(seed, food)}. */
	public static RecipesPayload payloadFor(ServerPlayer player) {
		MinecraftServer server = player.level().getServer();
		long seed = server.overworld().getSeed();
		List<RecipesPayload.Entry> entries = PlayerMunch.discoveries(player).sorted().stream()
				.map(food -> {
					Recipe recipe = Recipes.of(seed, food);
					return new RecipesPayload.Entry(food, recipe.spec().key(), recipe.direction() == Direction.UP);
				})
				.toList();
		return new RecipesPayload(MunchaholicMode.recipesActive(server), entries);
	}

	/** Only if the client can receive {@link RecipesPayload#TYPE} (vanilla clients get nothing). */
	public static void send(ServerPlayer player) {
		if (ServerPlayNetworking.canSend(player, RecipesPayload.TYPE)) {
			ServerPlayNetworking.send(player, payloadFor(player));
		}
	}

	public static void sendAll(MinecraftServer server) {
		for (ServerPlayer player : server.getPlayerList().getPlayers()) {
			send(player);
		}
	}
}
