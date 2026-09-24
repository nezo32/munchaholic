package dev.munchaholic.client;

import java.util.Optional;

import dev.munchaholic.core.Recipe;
import dev.munchaholic.net.RecipesPayload;

/**
 * The client's copy of the player's discovered recipes, from {@link RecipesPayload}. A volatile immutable snapshot,
 * cleared on disconnect. Entries with an unknown attribute key are skipped.
 */
public final class ClientRecipeBook {
	private ClientRecipeBook() {}

	/** Registers the RecipesPayload receiver and the DISCONNECT -> {@link #clear()} listener. */
	public static void register() {
		// TODO (Dev D): Phase-0 registers nothing, so no skeleton body runs; register the receiver and DISCONNECT.
	}

	public static void apply(RecipesPayload payload) {
		throw new UnsupportedOperationException("TODO");
	}

	public static void clear() {
		throw new UnsupportedOperationException("TODO");
	}

	/** Whether the server has Recipes mode active (tooltips are shown only then). */
	public static boolean active() {
		throw new UnsupportedOperationException("TODO");
	}

	/** The discovered recipe for this food id; empty if not discovered. */
	public static Optional<Recipe> recipe(String foodId) {
		throw new UnsupportedOperationException("TODO");
	}
}
