package dev.munchaholic.client;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import dev.munchaholic.core.Caps;
import dev.munchaholic.core.Direction;
import dev.munchaholic.core.Recipe;
import dev.munchaholic.net.RecipesPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;

/**
 * The client's copy of the player's discovered recipes, from {@link RecipesPayload}. A volatile immutable snapshot,
 * because tooltips can be built off the render thread (creative search). Cleared on disconnect, so one world's
 * recipes never show in another. Entries with an unknown attribute key are skipped.
 */
public final class ClientRecipeBook {
	private static final Snapshot EMPTY = new Snapshot(false, Map.of());
	private static volatile Snapshot current = EMPTY;

	private ClientRecipeBook() {}

	/** Registers the RecipesPayload receiver and the DISCONNECT -> {@link #clear()} listener. */
	public static void register() {
		ClientPlayNetworking.registerGlobalReceiver(RecipesPayload.TYPE, (payload, ctx) -> apply(payload));
		ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> clear());
	}

	/** Replaces everything with the payload's state (the server always sends the complete list). */
	public static void apply(RecipesPayload payload) {
		Map<String, Recipe> recipes = new HashMap<>();
		for (RecipesPayload.Entry entry : payload.entries()) {
			Caps.byKey(entry.attribute()).ifPresent(spec ->
					recipes.put(entry.food(), new Recipe(spec, entry.up() ? Direction.UP : Direction.DOWN)));
		}
		current = new Snapshot(payload.active(), Map.copyOf(recipes));
	}

	public static void clear() {
		current = EMPTY;
	}

	/** Whether the server has Recipes mode active (tooltips are shown only then). */
	public static boolean active() {
		return current.active();
	}

	/** The discovered recipe for this food id; empty if not discovered. */
	public static Optional<Recipe> recipe(String foodId) {
		return Optional.ofNullable(current.recipes().get(foodId));
	}

	private record Snapshot(boolean active, Map<String, Recipe> recipes) {}
}
